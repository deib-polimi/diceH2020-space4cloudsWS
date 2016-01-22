package it.polimi.diceH2020.SPACE4CloudWS.main;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.statemachine.StateMachine;
import org.springframework.stereotype.Component;

import it.polimi.diceH2020.SPACE4CloudWS.fs.FileUtility;
import it.polimi.diceH2020.SPACE4CloudWS.solvers.MINLPSolver;
import it.polimi.diceH2020.SPACE4CloudWS.solvers.SPNSolver;
import it.polimi.diceH2020.SPACE4CloudWS.stateMachine.Events;
import it.polimi.diceH2020.SPACE4CloudWS.stateMachine.States;

@Component
public class Initializator {
	private static Logger logger = Logger.getLogger(Initializator.class.getName());

	@Autowired
	private MINLPSolver milpSolver;

	@Autowired(required = true)
	private SPNSolver SPNSolver;

	@Autowired
	private StateMachine<States, Events> stateHandler;

	@EventListener
	public void handleContextRefresh(ContextRefreshedEvent event) throws Exception {

		stateHandler.start();
		logger.info("State machine initialized");
		try {

			FileUtility.createWorkingDir();
			milpSolver.initRemoteEnvironment();
			SPNSolver.initRemoteEnvironment();

			stateHandler.sendEvent(Events.MIGRATE);
			logger.info("Current service state: " + stateHandler.getState().getId());
		} catch (Exception e) {
			stateHandler.sendEvent(Events.STOP);
			logger.info("Current service state: " + stateHandler.getState().getId());
		}
	}
}
