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
package it.polimi.diceH2020.SPACE4CloudWS.engines;

import it.polimi.diceH2020.SPACE4Cloud.shared.inputData.InstanceData;
import it.polimi.diceH2020.SPACE4Cloud.shared.settings.Settings;
import it.polimi.diceH2020.SPACE4Cloud.shared.solution.Solution;
import it.polimi.diceH2020.SPACE4CloudWS.core.BuilderSolution;
import it.polimi.diceH2020.SPACE4CloudWS.core.OptimizerCourseGrained;
import it.polimi.diceH2020.SPACE4CloudWS.services.DataService;
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
public class EngineService implements Engine{
	//TODO factory for this service

	private final Logger logger = Logger.getLogger(getClass());

	@Autowired
	private OptimizerCourseGrained optimizer;

	@Autowired
	private BuilderSolution solBuilder;

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
		try {
			optimizer.hillClimbing(solution);
			if (!stateHandler.getState().getId().equals(States.IDLE)) stateHandler.sendEvent(Events.FINISH);
		} catch (Exception e) {
			logger.error("Error while performing local search", e);
			stateHandler.sendEvent(Events.STOP);
		}
		logger.info(stateHandler.getState().getId());
	}

	//Used only for Tests
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
	 * Set in DataService: <br>
	 * &emsp; -inputData <br>
	 * &emsp; -num job <br>
	 * &emsp; -the provider and all its available VM retrieved from DB 
	 * @param inputData
	 *            the inputData to set
	 */
	public void setInstanceData(InstanceData inputData) {
		dataService.setInstanceData(inputData);
	}

	/**
	 *  Evaluate the Solution/matrix with the specified solver (QN, SPN)
	 */
	@Async("workExecutor")
	public void evaluatingInitSolution() {
		optimizer.evaluate(solution);
		//TODO this has to be changed. Evaluation must be placed into the evaluator

		if (!stateHandler.getState().getId().equals(States.IDLE)) stateHandler.sendEvent(Events.TO_EVALUATED_INITSOLUTION);
		logger.info(stateHandler.getState().getId());
	}

	@Override
	public Future<String> reduceMatrix() {
		return null;
	}
}
