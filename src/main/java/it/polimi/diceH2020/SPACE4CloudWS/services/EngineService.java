package it.polimi.diceH2020.SPACE4CloudWS.services;

import it.polimi.diceH2020.SPACE4Cloud.shared.inputData.InstanceData;
import it.polimi.diceH2020.SPACE4Cloud.shared.settings.CloudType;
import it.polimi.diceH2020.SPACE4Cloud.shared.settings.Settings;
import it.polimi.diceH2020.SPACE4Cloud.shared.solution.Solution;
import it.polimi.diceH2020.SPACE4CloudWS.core.InitialSolutionBuilder;
import it.polimi.diceH2020.SPACE4CloudWS.core.Optimizer;
import it.polimi.diceH2020.SPACE4CloudWS.core.FineGrainedOptimizer;
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
	private Optimizer optimizer;
	
	@Autowired
	private FineGrainedOptimizer fineGrainedOptimizer;

	@Autowired
	private InitialSolutionBuilder solBuilder;

	@Autowired
	private DataService dataService;

	@Autowired
	private StateMachine<States, Events> stateHandler;

	private Solution solution;

	public Solution getSolution() {
		return solution;
	}

	public void setSolution(Solution sol) {
		this.solution = sol;
	}

	@Async("workExecutor")
	public Future<String> runningInitSolution() {
		try {
			solution = solBuilder.getInitialSolution();
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
		
		if(!dataService.getCloudType().equals(CloudType.Private)){
			
			try {
				optimizer.hillClimbing(solution);
				if (!stateHandler.getState().getId().equals(States.IDLE)) stateHandler.sendEvent(Events.FINISH);
			} catch (Exception e) {
				logger.error("Error while performing local search", e);
				stateHandler.sendEvent(Events.STOP);
			}
			logger.info(stateHandler.getState().getId());
		
		}else{
			
			try {
				fineGrainedOptimizer.hillClimbing(solution);
				//if (!stateHandler.getState().getId().equals(States.IDLE)) stateHandler.sendEvent(Events.FINISH);
			} catch (Exception e) {
				logger.error("Error while performing local search", e);
				stateHandler.sendEvent(Events.STOP);
			}
			logger.info(stateHandler.getState().getId());
		}
	}

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
	 * @param inputData
	 *            the inputData to set
	 */
	public void setInstanceData(InstanceData inputData) {
		this.dataService.setInstanceData(inputData);
	}

	@Async("workExecutor")
	public void evaluatingInitSolution() {
		optimizer.evaluate(solution); //TODO this has to be changed. Evaluation must be placed into the evaluator
		if (!stateHandler.getState().getId().equals(States.IDLE)) stateHandler.sendEvent(Events.TO_EVALUATED_INITSOLUTION);
		logger.info(stateHandler.getState().getId());
	}

}
