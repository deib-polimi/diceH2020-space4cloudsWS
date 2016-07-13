package it.polimi.diceH2020.SPACE4CloudWS.services;

import it.polimi.diceH2020.SPACE4Cloud.shared.inputData.InstanceData;
import it.polimi.diceH2020.SPACE4Cloud.shared.settings.Scenarios;
import it.polimi.diceH2020.SPACE4Cloud.shared.settings.Settings;
import it.polimi.diceH2020.SPACE4Cloud.shared.solution.Solution;
import it.polimi.diceH2020.SPACE4CloudWS.core.BuilderSolution;
import it.polimi.diceH2020.SPACE4CloudWS.core.Matrix;
import it.polimi.diceH2020.SPACE4CloudWS.core.OptimizerCourseGrained;
import it.polimi.diceH2020.SPACE4CloudWS.core.OptimizerFineGrained;
import it.polimi.diceH2020.SPACE4CloudWS.core.BuilderMatrix;
import it.polimi.diceH2020.SPACE4CloudWS.stateMachine.Events;
import it.polimi.diceH2020.SPACE4CloudWS.stateMachine.States;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.AsyncResult;
import org.springframework.statemachine.StateMachine;
import org.springframework.statemachine.annotation.WithStateMachine;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.concurrent.Future;

@Service
@WithStateMachine
public class EngineService {

	private final Logger logger = Logger.getLogger(getClass());

	@Autowired
	private OptimizerCourseGrained optimizer;
	
	@Autowired
	private OptimizerFineGrained fineGrainedOptimizer;

	@Autowired
	private BuilderSolution solBuilder;
	
	@Autowired
	private BuilderMatrix matrixBuilder;

	@Autowired
	private DataService dataService;

	@Autowired
	private StateMachine<States, Events> stateHandler;

	private Solution solution;
	
	private Matrix matrix; //with admission control till now used only in private 

	public Solution getSolution() {
		return solution;
	}

	public void setSolution(Solution sol) {
		this.solution = sol;
	}

	@Async("workExecutor")
	public Future<String> runningInitSolution() {
		try {
			if(!dataService.getCloudType().equals(Scenarios.PrivateAdmissionControl)){
					solution = solBuilder.getInitialSolution();
					matrix = null;
			}else{
				solution = matrixBuilder.getInitialSolution();
				matrix = matrixBuilder.getInitialMatrix(solution);
			}
			if (!stateHandler.getState().getId().equals(States.IDLE)) stateHandler.sendEvent(Events.TO_CHARGED_INITSOLUTION);
		} catch (Exception e) {
			logger.error("Error while performing optimization", e);
			stateHandler.sendEvent(Events.STOP);
		}
		logger.info(stateHandler.getState().getId());
		return new AsyncResult<>("Done");
	}

	@Async("workExecutor")
	public void localSearch() {
		try {
			if(!dataService.getCloudType().equals(Scenarios.PrivateAdmissionControl)){
				optimizer.hillClimbing(solution);
				if (!stateHandler.getState().getId().equals(States.IDLE)) stateHandler.sendEvent(Events.FINISH);
			}else{
				fineGrainedOptimizer.hillClimbing(matrix,solution);
			}
		} catch (Exception e) {
			logger.error("Error while performing local search", e);
			stateHandler.sendEvent(Events.STOP);
		}
		logger.info(stateHandler.getState().getId());
	}
	
	//Used only for Tests
	public Optional<Solution> generateInitialSolution() {
		try {
			solution = solBuilder.getInitialSolution();
			return Optional.of(solution);
		} catch (Exception e) {
			logger.error("Error while performing initial solution", e);
			stateHandler.sendEvent(Events.STOP);
		}
		logger.info(stateHandler.getState().getId());
		return Optional.empty();
	}

	public void changeSettings(Settings settings) {
		optimizer.changeSettings(settings);
	}

	public void restoreDefaults() {
		optimizer.restoreDefaults();
	}

	/**
	 * Set in DataService: <br>
	 * &emsp; -inputData <br>
	 * &emsp; -num job <br>
	 * &emsp; -the provider and all its available VM retrieved from DB 
	 * @param inputData
	 *            the inputData to set
	 */
	public void setInstanceData(InstanceData inputData) {
		this.dataService.setInstanceData(inputData);
	}

	/**
	 *  Evaluate the Solution/matrix with the specified solver (QN, SPN)
	 */
	@Async("workExecutor")
	public void evaluatingInitSolution() {
		if(!dataService.getCloudType().equals(Scenarios.PrivateAdmissionControl)){
			optimizer.evaluate(solution); //TODO this has to be changed. Evaluation must be placed into the evaluator
		}else{
			optimizer.evaluate(matrix);
			solution.setEvaluated(false);
		}
		
		if (!stateHandler.getState().getId().equals(States.IDLE)) stateHandler.sendEvent(Events.TO_EVALUATED_INITSOLUTION);
		logger.info(stateHandler.getState().getId());
	}
	

}
