package it.polimi.diceH2020.SPACE4CloudWS.services;

import java.util.List;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Async;
import org.springframework.statemachine.StateMachine;
import org.springframework.statemachine.annotation.OnTransition;
import org.springframework.statemachine.annotation.WithStateMachine;
import org.springframework.stereotype.Service;

import it.polimi.diceH2020.SPACE4Cloud.shared.inputData.InstanceData;
import it.polimi.diceH2020.SPACE4Cloud.shared.settings.Settings;
import it.polimi.diceH2020.SPACE4Cloud.shared.solution.Solution;
import it.polimi.diceH2020.SPACE4CloudWS.core.InitialSolutionBuilder;
import it.polimi.diceH2020.SPACE4CloudWS.core.Optimizer;
import it.polimi.diceH2020.SPACE4CloudWS.stateMachine.Events;
import it.polimi.diceH2020.SPACE4CloudWS.stateMachine.States;

@Service
@WithStateMachine
public class EngineService {

	private static Logger logger = Logger.getLogger(EngineService.class.getName());

	@Autowired
	private Optimizer optimizer;

	@Autowired
	private InitialSolutionBuilder solBuilder;

	@Autowired
	private DataService dataService;

	@Autowired
	private StateMachine<States, Events> stateHandler;

	public EngineService() {
	}

	public Solution getSolution() {
		return optimizer.getSolution();

	}

	@Async("workExecutor")
	@OnTransition(target = "RUNNING")
	public void optimizationPublicCloud() {
		try {
			Solution sol = solBuilder.getInitialSolution();

			optimizer.init(sol);
			optimizer.parallelLocalSearch();

//			if (dataService.getNumberJobs() > 1)
//				optimizer.sharedCluster();

			stateHandler.sendEvent(Events.MIGRATE);
		} catch (Exception e) {
			logger.error("Error while performing optimization", e);
			stateHandler.sendEvent(Events.STOP);
		}
		logger.info(stateHandler.getState().getId());
	}
	
	@Profile("test")
	public Solution generateInitialSolution() {
		try {
			return  solBuilder.getInitialSolution();
		} catch (Exception e) {
			logger.error("Error while performing initial solution", e);
			stateHandler.sendEvent(Events.STOP);
		}
		logger.info(stateHandler.getState().getId());
		return null;
	}
	

	// nobody calls this function
	// TODO generate interface for this ?
	public void sequence() throws Exception {
		Solution sol = solBuilder.getInitialSolution();
		optimizer.init(sol);
		optimizer.sequentialLS();
		stateHandler.sendEvent(Events.MIGRATE);
		logger.info(stateHandler.getState().getId());
	}

	public void setAccuracyAndCycles(Settings settings) {
		optimizer.extractAccuracyAndCycle(settings);
	}

	/**
	 * @param inputData
	 *            the inputData to set
	 */
	public void setInstanceData(InstanceData inputData) {
		this.dataService.setInstanceData(inputData);
	}


}
