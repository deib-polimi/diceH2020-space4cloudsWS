/*
Copyright 2016-2017 Eugenio Gianniti
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

import it.polimi.diceH2020.SPACE4Cloud.shared.settings.SPNModel;
import it.polimi.diceH2020.SPACE4Cloud.shared.solution.Phase;
import it.polimi.diceH2020.SPACE4Cloud.shared.solution.PhaseID;
import it.polimi.diceH2020.SPACE4Cloud.shared.solution.Solution;
import it.polimi.diceH2020.SPACE4Cloud.shared.solution.SolutionPerJob;
import it.polimi.diceH2020.SPACE4CloudWS.main.DS4CSettings;
import it.polimi.diceH2020.SPACE4CloudWS.solvers.Solver;
import it.polimi.diceH2020.SPACE4CloudWS.stateMachine.Events;
import it.polimi.diceH2020.SPACE4CloudWS.stateMachine.States;
import lombok.Setter;
import org.apache.commons.lang3.tuple.ImmutableTriple;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.Triple;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.statemachine.StateMachine;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.BiPredicate;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Stream;

@Component
class CoarseGrainedOptimizer extends Optimizer {

	@Setter(onMethod = @__(@Autowired))
	private DS4CSettings settings;

	@Setter(onMethod = @__(@Autowired))
	private StateMachine<States, Events> stateHandler;

	@Setter(onMethod = @__(@Autowired))
	private SolverChecker solverChecker;

	void hillClimbing(Solution solution) {
		logger.info(String.format("---------- Starting hill climbing for instance %s ----------", solution.getId()));
		SPNModel model = solverChecker.enforceSolverSettings (solution.getLstSolutions ());

		List<SolutionPerJob> lst = solution.getLstSolutions();
		Stream<SolutionPerJob> strm = settings.isParallel() ? lst.parallelStream() : lst.stream();
		AtomicLong executionTime = new AtomicLong();
		boolean overallSuccess = strm.map(s -> {
			Instant first = Instant.now();
			boolean success = hillClimbing(s, model);
			Instant after = Instant.now();
			executionTime.addAndGet(Duration.between(first, after).toMillis());
			return success;
		}).reduce(true, Boolean::logicalAnd);

		if (! overallSuccess) stateHandler.sendEvent(Events.STOP);
		else {
			solution.setEvaluated(false);
			evaluator.evaluate(solution);

			Phase phase = new Phase();
			phase.setId(PhaseID.OPTIMIZATION);
			phase.setDuration(executionTime.get());
			solution.addPhase(phase);
		}
	}

	private boolean hillClimbing(SolutionPerJob solPerJob, SPNModel technology) {
		boolean success = false;
		Pair<Optional<Double>, Long> simulatorResult = dataProcessor.simulateClass(solPerJob);
		Optional<Double> maybeResult = simulatorResult.getLeft();
		if (maybeResult.isPresent()) {
			success = true;

			Solver currentSolver = dataProcessor.getSolver ();
			Function<Double, Double> fromResult = currentSolver
					.transformationFromSolverResult (solPerJob, technology);
			Predicate<Double> feasibilityCheck = currentSolver.feasibilityCheck (solPerJob, technology);
			Consumer<Double> metricUpdater = currentSolver.metricUpdater (solPerJob, technology);

			final double tolerance = settings.getOptimization ().getTolerance ();

			BiPredicate<Double, Double> incrementCheck;
			Function<Integer, Integer> updateFunction;
			Predicate<Double> stoppingCondition;
			Predicate<Integer> vmCheck;

			double responseTime = fromResult.apply(maybeResult.get());
			if (feasibilityCheck.test(responseTime)) {
				updateFunction = n -> n - 1;
				stoppingCondition = feasibilityCheck.negate();
				vmCheck = n -> n == 1;
				incrementCheck = (prev, curr) -> false;
			} else {
				updateFunction = n -> n + 1;
				stoppingCondition = feasibilityCheck;
				vmCheck = n -> false;
				incrementCheck = (prev, curr) -> Math.abs((prev - curr) / prev) < tolerance;
			}

			List<Triple<Integer, Optional<Double>, Boolean>> resultsList = alterUntilBreakPoint(solPerJob,
					updateFunction, fromResult, feasibilityCheck, stoppingCondition, incrementCheck, vmCheck);
			Optional<Triple<Integer, Optional<Double>, Boolean>> result = resultsList.parallelStream().filter(t ->
					t.getRight() && t.getMiddle().isPresent()).min(Comparator.comparing(Triple::getLeft));
			result.ifPresent(triple ->
					triple.getMiddle ().ifPresent (output -> {
						int nVM = triple.getLeft();
						if (technology == SPNModel.MAPREDUCE) solPerJob.setThroughput(output);
						solPerJob.updateNumberVM(nVM);
						double metric = fromResult.apply(output);
						metricUpdater.accept(metric);
						logger.info(String.format(
								"class%s-> MakeFeasible ended, result = %f, other metric = %f, obtained with: %d VMs",
								solPerJob.getId(), output, metric, nVM));
					})
			);
		} else logger.info("class" + solPerJob.getId() + "-> MakeFeasible ended with ERROR");
		return success;
	}

	private List<Triple<Integer, Optional<Double>, Boolean>>
	alterUntilBreakPoint(SolutionPerJob solPerJob, Function<Integer, Integer> updateFunction,
						 Function<Double, Double> fromResult, Predicate<Double> feasibilityCheck,
						 Predicate<Double> stoppingCondition, BiPredicate<Double, Double> incrementCheck,
						 Predicate<Integer> vmCheck) {
		List<Triple<Integer, Optional<Double>, Boolean>> lst = new ArrayList<>();
		Optional<Double> previous = Optional.empty();
		boolean shouldKeepGoing = true;

		while (shouldKeepGoing) {
			Pair<Optional<Double>, Long> simulatorResult = dataProcessor.simulateClass(solPerJob);
			Optional<Double> maybeResult = simulatorResult.getLeft();
			Optional<Double> interestingMetric = maybeResult.map(fromResult);

			Integer nVM = solPerJob.getNumberVM();
			lst.add(new ImmutableTriple<>(nVM, maybeResult,
					interestingMetric.filter(feasibilityCheck).isPresent ()));

			boolean terminationCriterion = ! checkState() || vmCheck.test(nVM) ||
					interestingMetric.filter(stoppingCondition).isPresent ();
			if (previous.isPresent() && interestingMetric.isPresent()) {
				terminationCriterion |= incrementCheck.test(previous.get(), interestingMetric.get());
			}
			shouldKeepGoing = ! terminationCriterion;
			previous = interestingMetric;

			if (shouldKeepGoing) {
				String message = String.format("class %s -> num VM: %d, simulator result: %f, metric: %f",
						solPerJob.getId(), nVM, maybeResult.orElse(Double.NaN),
						interestingMetric.orElse(Double.NaN));
				logger.info(message);
				solPerJob.updateNumberVM(updateFunction.apply(nVM));
			}
		}

		return lst;
	}

	private boolean checkState() {
		return stateHandler.getState().getId().equals(States.RUNNING_LS);
	}

}
