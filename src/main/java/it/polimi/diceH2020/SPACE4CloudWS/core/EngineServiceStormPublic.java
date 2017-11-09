/*
Copyright 2017 Marco Lattuada

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

import it.polimi.diceH2020.SPACE4Cloud.shared.solution.SolutionPerJob;
import it.polimi.diceH2020.SPACE4CloudWS.stateMachine.States;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.AsyncResult;
import org.springframework.statemachine.annotation.WithStateMachine;
import org.springframework.stereotype.Service;
import it.polimi.diceH2020.SPACE4Cloud.shared.solution.Matrix;
import it.polimi.diceH2020.SPACE4CloudWS.stateMachine.Events;

import java.util.concurrent.Future;

@Service
@WithStateMachine
public class EngineServiceStormPublic extends EngineService {
	@Autowired
	@Lazy
	private CoarseGrainedOptimizer coarseGrainedOptimizer;

	@Async("workExecutor")
	public void evaluatingInitSolution() {
		evaluator.initialSimulation(solution);
		if (solution.getLstSolutions().stream().map(SolutionPerJob::getError)
				.reduce(false, Boolean::logicalOr)) {
			logger.info("The simulator failed while evaluating the initial solution");
			stateHandler.sendEvent(Events.STOP);
		} else if (stateHandler.getState().getId() != States.IDLE) {
			stateHandler.sendEvent(Events.TO_EVALUATED_INITSOLUTION);
		}
	}

	@Async("workExecutor")
	public Future<String> runningInitSolution() {
		try {
			logger.trace(solBuilder.getClass().getName());
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
		try {
			coarseGrainedOptimizer.hillClimbing(solution);
			if (!stateHandler.getState().getId().equals(States.IDLE)) {
				logger.trace("EngineServiceStormPublic - Sending event FINISH");
				stateHandler.sendEvent(Events.FINISH);
			}
		} catch (Exception e) {
			logger.error("Error while performing local search", e);
			stateHandler.sendEvent(Events.STOP);
		}
		logger.info(stateHandler.getState().getId());
	}
}
