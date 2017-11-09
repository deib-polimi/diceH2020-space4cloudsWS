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
import it.polimi.diceH2020.SPACE4CloudWS.stateMachine.Events;
import it.polimi.diceH2020.SPACE4CloudWS.stateMachine.States;
import org.springframework.scheduling.annotation.Async;
import org.springframework.statemachine.annotation.WithStateMachine;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.Future;

@Service
@WithStateMachine
public class EngineServiceGeneral extends EngineService{

	@Async("workExecutor")
	public void localSearch() {
      fineGrainedLocalSearch();
	}

	@Async("workExecutor")
	public void evaluatingInitSolution() {
      logger.trace("EngineServiceGeneral::evaluatingInitSolution - Begin");
      fineGrainedEvaluatingInitSolution();
      logger.trace("EngineServiceGeneral::evaluatingInitSolution - End");
	}

	@Async("workExecutor")
	public Future<String> runningInitSolution() {
      return fineGrainedRunningInitSolution();
	}

	public Future<String> reduceMatrix() {
		fineGrainedOptimizer.finish();
		///Check that matrix is composed of asingle cell
		if(matrix.getNumCells() != 1) {
			throw new RuntimeException("Matrix size is " + matrix.getNumCells());
		}
		List<SolutionPerJob> solutionPerJobs = matrix.getAllSolutions();
		if(solutionPerJobs.size() != 1) {
			throw new RuntimeException("Number of SolutionPerJob is " + solutionPerJobs.size());
		}
		getSolution().getLstSolutions().clear();
		getSolution().addSolutionPerJob(solutionPerJobs.get(0));
		if (!stateHandler.getState().getId().equals(States.IDLE)) {
			logger.trace("Current solution is " + getSolution().toStringReduced());
			logger.trace("EngineService - Sending event FINISH");
			stateHandler.sendEvent(Events.FINISH);
		}
		return null;
	}

}
