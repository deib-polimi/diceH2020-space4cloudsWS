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

import java.util.Optional;
import java.util.concurrent.Future;

@WithStateMachine
public abstract class EngineService implements Engine{

	protected final Logger logger = Logger.getLogger(getClass());

	@Autowired
	protected SolutionBuilder solBuilder;

	@Autowired
	protected StateMachine<States, Events> stateHandler;

	@Autowired
	protected DataProcessor dataProcessor;

	@Autowired
	protected Evaluator evaluator;

	protected Solution solution;

	protected Matrix matrix;

	@Autowired
	private MatrixBuilder matrixBuilder;

	@Autowired
   @Lazy
	protected FineGrainedOptimizer fineGrainedOptimizer; 

	protected Future<String> fineGrainedRunningInitSolution() {
		try {
			logger.trace(solBuilder.getClass().getName());
			solution = solBuilder.getInitialSolution();
			matrix = matrixBuilder.getInitialMatrix(solution);
			logger.info(matrix.asString());
			if (!stateHandler.getState().getId().equals(States.IDLE)) stateHandler.sendEvent(Events.TO_CHARGED_INITSOLUTION);
		} catch (Exception e) {
			logger.error("Error while performing optimization", e);
			stateHandler.sendEvent(Events.STOP);
		}
		logger.info(stateHandler.getState().getId());
		return new AsyncResult<>("Done");
	}

	protected void fineGrainedLocalSearch() {
		try {
			fineGrainedOptimizer.hillClimbing(matrix);
			if (!stateHandler.getState().getId().equals(States.IDLE)) stateHandler.sendEvent(Events.FINISH);
		} catch (Exception e) {
			logger.error("Error while performing local search", e);
			stateHandler.sendEvent(Events.STOP);
		}
		logger.info(stateHandler.getState().getId());
	}

	public void changeSettings(Settings settings) {
		dataProcessor.changeSettings(settings);
	}

	public void restoreDefaults() {
		dataProcessor.restoreDefaults();
	}

	protected void fineGrainedEvaluatingInitSolution() {
		evaluator.calculateDuration(matrix,solution);
	}


	public void evaluated(){
		evaluator.evaluate(solution);
		if (!stateHandler.getState().getId().equals(States.IDLE)) stateHandler.sendEvent(Events.TO_EVALUATED_INITSOLUTION);
		logger.info(stateHandler.getState().getId());
	}

	@Override
	public Future<String> reduceMatrix() {
		return null;
	}

	public Matrix getMatrix() {
		return matrix;
	}

	public void setMatrix(Matrix matrix) {
		this.matrix = matrix;
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

	public Solution getSolution() {
		return solution;
	}

	public void setSolution(Solution sol) {
		this.solution = sol;
	}

	public void error() {
		stateHandler.sendEvent(Events.STOP);
	}

}
