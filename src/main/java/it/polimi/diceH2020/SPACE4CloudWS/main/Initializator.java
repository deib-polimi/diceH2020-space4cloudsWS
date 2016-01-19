package it.polimi.diceH2020.SPACE4CloudWS.main;

import java.io.IOException;

import javax.annotation.PostConstruct;

import org.apache.catalina.core.ApplicationContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.statemachine.StateMachine;
import org.springframework.stereotype.Component;

import it.polimi.diceH2020.SPACE4CloudWS.core.FileUtiliy;
import it.polimi.diceH2020.SPACE4CloudWS.solvers.MINLPSolver;
import it.polimi.diceH2020.SPACE4CloudWS.solvers.SPNSolver;
import it.polimi.diceH2020.SPACE4CloudWS.stateMachine.Events;
import it.polimi.diceH2020.SPACE4CloudWS.stateMachine.States;

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
	private void init() throws Exception{
		stateHandler.start();	
//		try {
//			
			FileUtiliy.createWorkingDir();
			milpSolver.initRemoteEnvironment();
			SPNSolver.initRemoteEnvironment();
			
			stateHandler.sendEvent(Events.MIGRATE);
			System.out.println(stateHandler.getState());
//		} catch (Exception e) {
//			e.printStackTrace();
//			stateHandler.sendEvent(Events.STOP);
//			System.out.println(stateHandler.getState());
//		}
	}

	
}
