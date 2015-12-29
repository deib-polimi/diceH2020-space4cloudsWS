package eu.diceH2020.SPACE4CloudWS.app;

import javax.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.statemachine.StateMachine;
import org.springframework.statemachine.annotation.WithStateMachine;
import org.springframework.stereotype.Component;

import eu.diceH2020.SPACE4CloudWS.solvers.MINLPSolver;
import eu.diceH2020.SPACE4CloudWS.solvers.SPNSolver;
import eu.diceH2020.SPACE4CloudWS.stateMachine.Events;
import eu.diceH2020.SPACE4CloudWS.stateMachine.States;

/**
 * @author ciavotta
 * this class is used to initialize the environments on the remote servers
 * Creation of directories mainly. 
 */
@Component
public class Initializator {

	@Autowired
	private MINLPSolver milpSolver;
	
	@Autowired(required = true)
	private SPNSolver SPNSolver;
	
	@Autowired
	private StateMachine<States, Events> stateHandler;
	
	@PostConstruct
	private void init(){
		stateHandler.start();	
		try {
			//This function is a stub, must be completed. Now I have no time.
			milpSolver.initRemoteEnvironment();
			SPNSolver.init("...");
			
			stateHandler.sendEvent(Events.MIGRATE);
		} catch (Exception e) {
			stateHandler.sendEvent(Events.STOP);
		}
	}

	
}
