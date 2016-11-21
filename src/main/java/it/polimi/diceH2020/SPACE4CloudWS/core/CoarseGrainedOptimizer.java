/*
Copyright 2016 Michele Ciavotta
Copyright 2016 Jacopo Rigoli
Copyright 2016 Eugenio Gianniti

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

import it.polimi.diceH2020.SPACE4Cloud.shared.solution.Phase;
import it.polimi.diceH2020.SPACE4Cloud.shared.solution.PhaseID;
import it.polimi.diceH2020.SPACE4Cloud.shared.solution.Solution;
import it.polimi.diceH2020.SPACE4Cloud.shared.solution.SolutionPerJob;
import it.polimi.diceH2020.SPACE4CloudWS.main.DS4CSettings;
import it.polimi.diceH2020.SPACE4CloudWS.stateMachine.Events;
import it.polimi.diceH2020.SPACE4CloudWS.stateMachine.States;
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
import java.util.function.Function;
import java.util.stream.Stream;

@Component
class CoarseGrainedOptimizer extends Optimizer {

	@Autowired
	private DS4CSettings settings;

	@Autowired
	private StateMachine<States, Events> stateHandler;

	void hillClimbing(Solution solution) {
		logger.info(String.format("---------- Starting hill climbing for instance %s ----------", solution.getId()));
		List<SolutionPerJob> lst = solution.getLstSolutions();
		Stream<SolutionPerJob> strm = settings.isParallel() ? lst.parallelStream() : lst.stream();
		AtomicLong executionTime = new AtomicLong();
		strm.forEach(s -> {
			Instant first = Instant.now();
			hillClimbing(s);
			Instant after = Instant.now();
			executionTime.addAndGet(Duration.between(first, after).toMillis());
		});
		solution.setEvaluated(false);
		evaluator.evaluate(solution);

		Phase phase = new Phase();
		phase.setId(PhaseID.OPTIMIZATION);
		phase.setDuration(executionTime.get());
		solution.addPhase(phase);
	}

	private boolean hillClimbing(SolutionPerJob solPerJob) {
		Pair<Optional<Double>, Double> simulatorResult = dataProcessor.calculateDuration(solPerJob);
		if (simulatorResult.getLeft().isPresent()) {
			double deadline = solPerJob.getJob().getD();
			FiveParametersFunction<Optional<Double>, Optional<Double>, Double, Integer, Integer, Boolean> checkFunction;
			Function<Integer, Integer> updateFunction;
			if (simulatorResult.getLeft().get() > deadline) {
				checkFunction = this::checkConditionToFeasibility;
				updateFunction = n -> n + 1;
			} else {
				checkFunction = this::checkConditionFromFeasibility;
				updateFunction = n -> n - 1;
			}
			List<Triple<Integer, Optional<Double>, Boolean>> res = alterUntilBreakPoint(dataService.getGamma(),
					checkFunction, updateFunction, solPerJob, deadline);
			Stream<Triple<Integer, Optional<Double>, Boolean>> filteredStream = res.parallelStream().filter(t ->
					t.getRight() && t.getMiddle().isPresent());
			Optional<Triple<Integer, Optional<Double>, Boolean>> result =
					filteredStream.min(Comparator.comparing(t -> t.getMiddle().get()));
			if (result.isPresent()) {
				double duration = result.get().getMiddle().get();
				int nVM = result.get().getLeft();
				solPerJob.setDuration(duration);
				solPerJob.updateNumberVM(nVM);
				logger.info(String.format("class%s-> MakeFeasible ended, the duration is: %f obtained with: %d vms",
						solPerJob.getId(), duration, nVM));
				return true;
			}
		}
		logger.info("class" + solPerJob.getId() + "-> MakeFeasible ended with ERROR");
		return false;
	}

	private List<Triple<Integer, Optional<Double>, Boolean>>
	alterUntilBreakPoint(Integer maxVM,
						 FiveParametersFunction<Optional<Double>, Optional<Double>, Double, Integer,
								 Integer, Boolean> checkFunction,
						 Function<Integer, Integer> updateFunction, SolutionPerJob solPerJob, double deadline) {
		List<Triple<Integer, Optional<Double>, Boolean>> lst = new ArrayList<>();
		Optional<Double> previous = Optional.empty();
		boolean shouldKeepGoing = true;

		while (shouldKeepGoing) {
			Pair<Optional<Double>, Double> simulatorResult = dataProcessor.calculateDuration(solPerJob);

			Integer nVM = solPerJob.getNumberVM();
			lst.add(new ImmutableTriple<>(nVM, simulatorResult.getLeft(), simulatorResult.getLeft().isPresent() &&
					(simulatorResult.getLeft().get() < deadline)));

			shouldKeepGoing = ! checkFunction.apply(previous, simulatorResult.getLeft(), deadline, nVM, maxVM);
			previous = simulatorResult.getLeft();

			if (shouldKeepGoing) {
				logger.info("class" + solPerJob.getId() + "-> num VM: " + nVM + " duration: " +
						(simulatorResult.getLeft().isPresent() ? simulatorResult.getLeft().get() : "null ") +
						" deadline: " + deadline);
				solPerJob.updateNumberVM(updateFunction.apply(nVM));
			}
		}

		return lst;
	}

	private boolean checkConditionToFeasibility(Optional<Double> previousDuration, Optional<Double> duration,
												double deadline, Integer nVM, Integer maxVM) {
		return (duration.isPresent() && duration.get() <= deadline) ||
				//|previousDuration-duration|≤0.1 return true
				(previousDuration.isPresent() && duration.isPresent() &&
						Math.abs(previousDuration.get() - duration.get()) <= 0.1) ||
				nVM > maxVM || ! checkState();
	}

	private boolean checkConditionFromFeasibility(Optional<Double> previousDuration, Optional<Double> duration,
												  double deadline, Integer nVM, Integer maxVM) {
		return (duration.isPresent() && duration.get() > deadline) ||
				//|previousDuration-duration|≤0.1 return true
				(previousDuration.isPresent() && duration.isPresent() &&
						Math.abs(previousDuration.get() -  duration.get()) <= 0.1) ||
				nVM == 1 || ! checkState();
	}

	private boolean checkState() {
		return stateHandler.getState().getId().equals(States.RUNNING_LS);
	}

}
