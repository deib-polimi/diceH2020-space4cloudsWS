package it.polimi.diceH2020.SPACE4CloudWS.main;

import it.polimi.diceH2020.SPACE4CloudWS.engines.EngineFactory;
import it.polimi.diceH2020.SPACE4CloudWS.fileManagement.FileUtility;
import it.polimi.diceH2020.SPACE4CloudWS.fineGrainedLogicForOptimization.WrapperDispatcher;
import it.polimi.diceH2020.SPACE4CloudWS.solvers.Solver;
import it.polimi.diceH2020.SPACE4CloudWS.solvers.solversImpl.SolverFactory;
import it.polimi.diceH2020.SPACE4CloudWS.solvers.solversImpl.MINLPSolver.MINLPSolver;
import it.polimi.diceH2020.SPACE4CloudWS.stateMachine.Events;
import it.polimi.diceH2020.SPACE4CloudWS.stateMachine.States;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.statemachine.StateMachine;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;

@Component
public class Initializator {
	private static Logger logger = Logger.getLogger(Initializator.class.getName());

	@Autowired
	private MINLPSolver milpSolver;

	@Autowired
	WrapperDispatcher dispatcher;
	
	private Solver solver;

	@Autowired
	private SolverFactory solverFactory;

	@Autowired
	private StateMachine<States, Events> stateHandler;

	@Autowired
	private FileUtility fileUtility;
	
	@Autowired
	private EngineFactory engineFactory;

	@PostConstruct
	private void setSolver() {
		solver = solverFactory.create();
		engineFactory.create();
	}


	@EventListener
	public void handleContextRefresh(ContextRefreshedEvent event) throws Exception {

		stateHandler.start();
		logger.info("State machine initialized");
		try {
			fileUtility.createWorkingDir();
			milpSolver.initRemoteEnvironment();
			solver.initRemoteEnvironment();
			
			stateHandler.sendEvent(Events.MIGRATE);
			logger.info("Current service state: " + stateHandler.getState().getId());
		} catch (Exception e) {
			stateHandler.sendEvent(Events.STOP);
			logger.info("Current service state: " + stateHandler.getState().getId());
		}
	}
}
