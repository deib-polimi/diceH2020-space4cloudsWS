package it.polimi.diceH2020.SPACE4CloudWS.core;

import it.polimi.diceH2020.SPACE4Cloud.shared.inputData.JobClass;
import it.polimi.diceH2020.SPACE4Cloud.shared.settings.Settings;
import it.polimi.diceH2020.SPACE4Cloud.shared.solution.*;
import it.polimi.diceH2020.SPACE4CloudWS.main.S4CSettings;
import it.polimi.diceH2020.SPACE4CloudWS.services.DataService;
import it.polimi.diceH2020.SPACE4CloudWS.services.SolverProxy;
import it.polimi.diceH2020.SPACE4CloudWS.stateMachine.Events;
import it.polimi.diceH2020.SPACE4CloudWS.stateMachine.States;
import lombok.NonNull;
import org.apache.commons.lang3.tuple.ImmutableTriple;
import org.apache.commons.lang3.tuple.Triple;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.statemachine.StateMachine;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Component
public class Optimizer {

	private final Logger logger = Logger.getLogger(getClass());

	@Autowired
	private DataService dataService;

	@Autowired
	private SolverProxy solverCache;

	@Autowired
	private S4CSettings settings;

	@Autowired
	private StateMachine<States, Events> stateHandler;

	@Autowired
	private IEvaluator evaluator;

	public void restoreDefaults() {
		solverCache.restoreDefaults();
	}

	// read an input file and type value of accuracy and cycles
	public void changeSettings(Settings settings) {
		solverCache.changeSettings(settings);
	}

	public void parallelLocalSearch(Solution solution) throws Exception {

		List<Optional<Double>> objectives = solution.getLstSolutions().parallelStream().map(this::executeMock).collect(Collectors.toList());

		objectives.clear();

	}

	public void hillClimbing(Solution solution) {
		Instant first = Instant.now();
		logger.info(String.format("---------- Starting hill climbing for instance %s ----------", solution.getId()));
		List<SolutionPerJob> lst = solution.getLstSolutions();
		Stream<SolutionPerJob> strm = settings.isParallel() ? lst.parallelStream() : lst.stream();
		strm.forEach(this::hillClimbing);
		solution.setEvaluated(false);
		evaluator.evaluate(solution);
		Instant after = Instant.now();
		Phase ph = new Phase();
		ph.setId(PhaseID.OPTIMIZATION);
		ph.setDuration(Duration.between(first, after).toMillis());
		solution.addPhase(ph);
	}

	private boolean hillClimbing(SolutionPerJob solPerJob) {
		JobClass jobClass = solPerJob.getJob();
		double deadline = jobClass.getD();
		FiveParametersFunction<Optional<BigDecimal>, Optional<BigDecimal>, Double, Integer, Integer, Boolean> checkFunction;
		Function<Integer, Integer> updateFunction;
		List<Triple<Integer, Optional<BigDecimal>, Boolean>> res;

		Optional<BigDecimal> duration = calculateDuration(solPerJob);
		if (duration.isPresent()) {
			if (duration.get().doubleValue() > deadline) {
				checkFunction = this::checkConditionToFeasibility;
				updateFunction = n -> n + 1;
			} else {
				checkFunction = this::checkConditionFromFeasibility;
				updateFunction = n -> n - 1;
			}
			res = alterUntilBreakPoint(dataService.getGamma(), checkFunction, updateFunction, solPerJob, deadline);
			Stream<Triple<Integer, Optional<BigDecimal>, Boolean>> filteredStream = res.parallelStream().filter(t -> t.getRight() && t.getMiddle().isPresent());
			Optional<Triple<Integer, Optional<BigDecimal>, Boolean>> result = filteredStream.min(Comparator.comparing(t -> t.getMiddle().get()));
			if (result.isPresent()) {
				BigDecimal dur = result.get().getMiddle().get();
				int nVM = result.get().getLeft();
				solPerJob.setDuration(dur.doubleValue());
				solPerJob.setNumberVM(nVM);
				logger.info(String.format("class%d-> MakeFeasible ended, the duration is: %s obtained with: %d vms", solPerJob.getJob().getId(), dur, nVM));
				return true;
			}
		}
		logger.info("class" + solPerJob.getJob().getId() + "-> MakeFeasible ended with ERROR");
		return false;
	}

	private List<Triple<Integer, Optional<BigDecimal>, Boolean>> alterUntilBreakPoint(Integer MaxVM, FiveParametersFunction<Optional<BigDecimal>, Optional<BigDecimal>, Double, Integer, Integer, Boolean> checkFunction,
																					  Function<Integer, Integer> updateFunction, SolutionPerJob solPerJob, double deadline) {
		List<Triple<Integer, Optional<BigDecimal>, Boolean>> lst = new ArrayList<>();
		recursiveOptimize(MaxVM, checkFunction, updateFunction, solPerJob, deadline, lst);
		return lst;
	}

	private void recursiveOptimize(Integer maxVM, FiveParametersFunction<Optional<BigDecimal>, Optional<BigDecimal>, Double, Integer, Integer, Boolean> checkFunction, Function<Integer, Integer> updateFunction,
								   SolutionPerJob solPerJob, double deadline, List<Triple<Integer, Optional<BigDecimal>, Boolean>> lst) {
		Optional<BigDecimal> optDuration = calculateDuration(solPerJob);
		Integer nVM = solPerJob.getNumberVM();
		Optional<BigDecimal> previous;
		if (lst.size() > 0) previous = lst.get(lst.size() - 1).getMiddle();
		else previous = Optional.empty();

		lst.add(new ImmutableTriple<>(nVM, optDuration, optDuration.isPresent() && (optDuration.get().doubleValue() < deadline)));
		Boolean condition = checkFunction.apply(previous, optDuration, deadline, nVM, maxVM);
		if (!condition) {
			logger.info("class" + solPerJob.getJob().getId() + "-> num VM: " + nVM + " duration: " + (optDuration.isPresent() ? optDuration.get() : "null ") + " deadline: " + deadline);
			solPerJob.setNumberVM(updateFunction.apply(nVM));
			recursiveOptimize(maxVM, checkFunction, updateFunction, solPerJob, deadline, lst);
		}
	}

	private boolean checkConditionToFeasibility(Optional<BigDecimal> previousDuration, Optional<BigDecimal> duration, double deadline, Integer nVM, Integer maxVM) {
		boolean returnValue = false;
		if (duration.isPresent() && duration.get().doubleValue() <= deadline) returnValue = true;
		//|previousDuration-duration|≤0.1 return true
		if (previousDuration.isPresent() && duration.isPresent() && (previousDuration.get().subtract(duration.get()).abs().compareTo(new BigDecimal("0.1")) != 1)) returnValue = true;
		if (nVM > maxVM) returnValue = true;
		if (!checkState()) returnValue = true;
		return returnValue;
	}

	private boolean checkConditionFromFeasibility(Optional<BigDecimal> previousDuration, Optional<BigDecimal> duration, double deadline, Integer nVM, Integer maxVM) {
		boolean returnValue = false;
		if (duration.isPresent() && duration.get().doubleValue() > deadline) returnValue = true;
		//|previousDuration-duration|≤0.1 return true
		if (previousDuration.isPresent() && duration.isPresent() && (previousDuration.get().subtract(duration.get()).abs().compareTo(new BigDecimal("0.1")) != 1)) returnValue = true;
		if (nVM == 1) returnValue = true;
		if (!checkState()) returnValue = true;
		return returnValue;
	}

	private boolean checkState() {
		return stateHandler.getState().getId().equals(States.RUNNING_LS);
	}

	private Optional<BigDecimal> calculateDuration(@NonNull SolutionPerJob solPerJob) {
		Optional<BigDecimal> result = solverCache.evaluate(solPerJob);
		if (! result.isPresent()) solverCache.invalidate(solPerJob);
		return result;
	}

	private Optional<Double> executeMock(SolutionPerJob solPerJob) {
		try {
			Thread.sleep(1000);
		} catch (InterruptedException e) {
			// pass
		}
		Optional<Double> res = Optional.of(10.0);
		logger.info("Local search num " + solPerJob.getPos() + " finished");
		return res;
	}

	public void evaluate(@NonNull Solution sol) {
		sol.getLstSolutions().forEach(s -> {
			Optional<BigDecimal> duration = calculateDuration(s);
			if (duration.isPresent()) s.setDuration(duration.get().doubleValue());
			else {
				s.setDuration(Double.MAX_VALUE);
				s.setError(Boolean.TRUE);
			}
		});
		sol.setEvaluated(false);
		evaluator.evaluate(sol);
	}
	
}
