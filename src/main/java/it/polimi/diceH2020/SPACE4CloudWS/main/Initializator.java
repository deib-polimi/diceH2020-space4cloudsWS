/*
Copyright 2016 Michele Ciavotta
Copyright 2016 Jacopo Rigoli

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
*/
package it.polimi.diceH2020.SPACE4CloudWS.main;

import it.polimi.diceH2020.SPACE4CloudWS.engines.EngineFactory;
import it.polimi.diceH2020.SPACE4CloudWS.fileManagement.FileUtility;
import it.polimi.diceH2020.SPACE4CloudWS.fineGrainedLogicForOptimization.WrapperDispatcher;
import it.polimi.diceH2020.SPACE4CloudWS.solvers.Solver;
import it.polimi.diceH2020.SPACE4CloudWS.solvers.solversImpl.MINLPSolver.MINLPSolver;
import it.polimi.diceH2020.SPACE4CloudWS.solvers.solversImpl.SolverFactory;
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
