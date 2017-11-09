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
package it.polimi.diceH2020.SPACE4CloudWS.core;

import it.polimi.diceH2020.SPACE4Cloud.shared.settings.Settings;
import it.polimi.diceH2020.SPACE4Cloud.shared.solution.Matrix;
import it.polimi.diceH2020.SPACE4Cloud.shared.solution.Solution;
import it.polimi.diceH2020.SPACE4CloudWS.engines.Engine;
import it.polimi.diceH2020.SPACE4CloudWS.stateMachine.Events;
import it.polimi.diceH2020.SPACE4CloudWS.stateMachine.States;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.AsyncResult;
import org.springframework.statemachine.StateMachine;
import org.springframework.statemachine.annotation.WithStateMachine;
import org.springframework.stereotype.Service;

import java.util.concurrent.Future;

@Service
@WithStateMachine
public class EngineServiceWithACService extends EngineService{

	@Autowired
	private Selector selector;

	@Async("workExecutor")
	public Future<String> runningInitSolution() {
      return fineGrainedRunningInitSolution();
	}

	@Async("workExecutor")
	public void localSearch() {
      fineGrainedLocalSearch();
	}

	@Async("workExecutor")
	public void evaluatingInitSolution() {
      fineGrainedEvaluatingInitSolution();
	}

	@Async("workExecutor")
	public Future<String> reduceMatrix() {
		try {
			selector.selectMatrixCells(matrix, solution);
			fineGrainedOptimizer.finish();
			if (!stateHandler.getState().getId().equals(States.IDLE)) {
				logger.trace("EngineServiceWithACService - Sending event FINISH");
				stateHandler.sendEvent(Events.FINISH);
			}
		} catch (Exception e) {
			logger.info("Error while performing optimization", e);
			stateHandler.sendEvent(Events.STOP);
		}
		logger.info(stateHandler.getState().getId());
		return new AsyncResult<>("Done");
	}

	public void evaluated(){
		if (!stateHandler.getState().getId().equals(States.IDLE)) stateHandler.sendEvent(Events.TO_EVALUATED_INITSOLUTION);
		logger.info(stateHandler.getState().getId());
	}

	public void error(){
		logger.error("Error while performing evaluation!\n Are the simulators working?");
		stateHandler.sendEvent(Events.STOP);
	}

}
