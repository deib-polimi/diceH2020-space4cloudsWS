package it.polimi.diceH2020.SPACE4CloudWS.core;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.ImmutableTriple;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.Triple;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.gs.collections.impl.list.mutable.primitive.BooleanArrayList;

import it.polimi.diceH2020.SPACE4Cloud.shared.inputData.JobClass;
import it.polimi.diceH2020.SPACE4Cloud.shared.inputData.Profile;
import it.polimi.diceH2020.SPACE4Cloud.shared.settings.Settings;
import it.polimi.diceH2020.SPACE4Cloud.shared.solution.Solution;
import it.polimi.diceH2020.SPACE4Cloud.shared.solution.SolutionPerJob;
import it.polimi.diceH2020.SPACE4CloudWS.fileManagement.FileUtility;
import it.polimi.diceH2020.SPACE4CloudWS.fileManagement.PNDefFileBuilder;
import it.polimi.diceH2020.SPACE4CloudWS.fileManagement.PNNetFileBuilder;
import it.polimi.diceH2020.SPACE4CloudWS.services.DataService;
import it.polimi.diceH2020.SPACE4CloudWS.solvers.SPNSolver;

@Component
public class Optimizer {

	private static Logger logger = Logger.getLogger(Optimizer.class.getName());

	@Value("${settings.cycles:100}")
	private int cycles;

	@Autowired
	private FileUtility fileUtility;

	@Autowired
	private DataService dataService;

	@Autowired(required = true)
	SPNSolver SPNSolver;

	public static double calculateResponseTime(double throughput, int numServers, double thinkTime) {
		return (double) numServers / throughput - thinkTime;
	}

	public static BigDecimal calculateResponseTime(BigDecimal throughput, int numServers, double thinkTime) {
		return BigDecimal.valueOf(calculateResponseTime(throughput.doubleValue(), numServers, thinkTime)).setScale(2, RoundingMode.HALF_EVEN);
	}

	// read an input file and type value of accuracy and cycles
	public void extractAccuracyAndCycle(Settings settings) {
		SPNSolver.setAccuracy(settings.getAccuracy());
		this.cycles = settings.getCycles();
	}

	public void parallelLocalSearch(Solution solution) throws Exception {

		List<Optional<Double>> objectives = solution.getLstSolutions().parallelStream().map(s -> executeMock(s, cycles)).collect(Collectors.toList());

		objectives.clear();

	}

	public void hillClimbing(Solution solution) {
		solution.getLstSolutions().parallelStream().forEach(solPerJob -> hillClimbing(solPerJob));
		
	}

	private boolean hillClimbing(SolutionPerJob solPerJob) {
		JobClass jobClass = solPerJob.getJob();
		double deadline = jobClass.getD();
		FiveParmsFunction<Optional<BigDecimal>, Optional<BigDecimal>, Double, Integer, Integer, Boolean> checkFunction;
		Function<Integer, Integer> updateFunction;
		List<Triple<Integer, Optional<BigDecimal>, Boolean>> res = null;

		Optional<BigDecimal> duration = calculateDuration(solPerJob);
		if (duration.isPresent()) {
			if (duration.get().doubleValue() > deadline) {
				checkFunction = (pDur, dur, dead, nVM, max) -> checkConditionToFeasibility(pDur, dur, dead, nVM, max);
				updateFunction = n -> n + 1;
			} else {
				checkFunction = (pDur, dur, dead, nVM, max) -> checkConditionFromFeasibility(pDur, dur, dead, nVM, max);
				updateFunction = n -> n - 1;
			}
			res = alterUntilBreakPoint(dataService.getGamma(), checkFunction, updateFunction, solPerJob, deadline);
			Stream<Triple<Integer, Optional<BigDecimal>, Boolean>> filteredStream = res.parallelStream().filter(t -> t.getRight() && t.getMiddle().isPresent());
			Optional<Triple<Integer, Optional<BigDecimal>, Boolean>> result = filteredStream.min(Comparator.comparing(t -> t.getMiddle().get()));
			if (result.isPresent()) {
				BigDecimal dur = result.get().getMiddle().get();
				int nVM = result.get().getLeft();
				logger.info("class" + solPerJob.getJob().getId() + "-> MakeFisible ended, the duration is: " + dur + " obtained with: " + nVM + " vms");
				return true;
			}
		}
		logger.info("class" + solPerJob.getJob().getId() + "-> MakeFisible ended with ERROR");
		return false;
	}

	private List<Triple<Integer, Optional<BigDecimal>, Boolean>> alterUntilBreakPoint(Integer MaxVM, FiveParmsFunction<Optional<BigDecimal>, Optional<BigDecimal>, Double, Integer, Integer, Boolean> checkFunction,
			Function<Integer, Integer> updateFunction, SolutionPerJob solPerJob, double deadline) {
		List<Triple<Integer, Optional<BigDecimal>, Boolean>> lst = new ArrayList<>();
		recursiveOptimize(MaxVM, checkFunction, updateFunction, solPerJob, deadline, lst);
		return lst;
	}

	private void recursiveOptimize(Integer maxVM, FiveParmsFunction<Optional<BigDecimal>, Optional<BigDecimal>, Double, Integer, Integer, Boolean> checkFunction, Function<Integer, Integer> updateFunction,
			SolutionPerJob solPerJob, double deadline, List<Triple<Integer, Optional<BigDecimal>, Boolean>> lst) {
		// TODO Auto-generated method stub
		Optional<BigDecimal> optDuration = calculateDuration(solPerJob);
		Integer nVM = solPerJob.getNumberVM();
		Optional<BigDecimal> previous;
		if (lst.size() > 0) previous = lst.get(lst.size() - 1).getMiddle();
		else previous = Optional.empty();

		lst.add(new ImmutableTriple<Integer, Optional<BigDecimal>, Boolean>(nVM, optDuration, optDuration.isPresent() && (optDuration.get().doubleValue() < deadline)));
		Boolean condition = checkFunction.apply(previous, optDuration, deadline, nVM, maxVM);
		if (!condition) {
			logger.info("class" + solPerJob.getJob().getId() + "->  num VM: " + nVM + " duration: " + (optDuration.isPresent() ? optDuration.get() : "null ") + " deadline: " + deadline);
			solPerJob.setNumberVM(updateFunction.apply(nVM));
			recursiveOptimize(maxVM, checkFunction, updateFunction, solPerJob, deadline, lst);
		}
	}

	private boolean checkConditionToFeasibility(Optional<BigDecimal> previousDuration, Optional<BigDecimal> duration, double deadline, Integer nVM, Integer maxVM) {
		return previousDuration.isPresent() && duration.isPresent() && (duration.get().doubleValue() < deadline) && (nVM < maxVM)
				&& (previousDuration.get().subtract(duration.get()).abs().compareTo(new BigDecimal("0.1")) == 1);
	}

	private boolean checkConditionFromFeasibility(Optional<BigDecimal> previousDuration, Optional<BigDecimal> duration, double deadline, Integer nVM, Integer maxVM) {
		return previousDuration.isPresent() && duration.isPresent() && (duration.get().doubleValue() > deadline) && (nVM < maxVM)
				&& (previousDuration.get().subtract(duration.get()).abs().compareTo(new BigDecimal("0.1")) == 1);
	}

	private Optional<BigDecimal> calculateDuration(SolutionPerJob solPerJob) {
		if (!solPerJob.getChanged()) {
			return Optional.of(BigDecimal.valueOf(solPerJob.getDuration()));
		}
		JobClass jobClass = solPerJob.getJob();
		int jobID = jobClass.getId();
		int nUsers = solPerJob.getNumberUsers();
		double think = jobClass.getThink();
		Pair<File, File> pFiles;
		try {
			pFiles = createSPNWorkingFiles(solPerJob);
			BigDecimal throughput = SPNSolver.run(pFiles, "class" + jobID);
			if (fileUtility.delete(pFiles)) logger.debug("Working files correctly deleted");
			BigDecimal duration = calculateResponseTime(throughput, nUsers, think);
			return Optional.of(duration);
		} catch (Exception e) {
			return Optional.empty();
		}

	}

	public Optional<Double> executeMock(SolutionPerJob solPerJob, int cycles) {
		try {
			Thread.sleep(1000);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		Optional<Double> res = Optional.of(10.0);
		logger.info("Local search num " + solPerJob.getPos() + " finished");
		return res;
	}

	// public Optional<Double> execute(SolutionPerJob solPerJob, int cycles) {
	// double tempResponseTime = 0;
	// int maxIterations = (int) Math.ceil((double) cycles / 2.0);
	// JobClass jobClass = solPerJob.getJob();
	//
	// int jobID = jobClass.getId();
	//
	// double deadline = jobClass.getD();
	// double think = jobClass.getThink();
	//
	// int nUsers = solPerJob.getNumberUsers();
	// int nContainers = solPerJob.getNumberContainers();
	// int hUp = jobClass.getHup();
	//
	// Optional<Double> res;
	// try {
	// Pair<File, File> pFiles = createSPNWorkingFiles(solPerJob);
	// BigDecimal throughput = SPNSolver.run(pFiles, "class" + jobID);
	// BigDecimal responseTime = Optimizer.calculateResponseTime(throughput,
	// nUsers, think);
	//
	// if (responseTime < deadline) {
	// for (int iteration = 0; responseTime < deadline && nUsers < hUp
	// && iteration <= maxIterations; ++iteration) {
	// tempResponseTime = responseTime;
	// ++nUsers;
	//
	// if (fileUtility.delete(pFiles))
	// logger.info("Working files correctly deleted");
	// pFiles = createSPNWorkingFiles(solPerJob, Optional.of(iteration));
	//
	// throughput = SPNSolver.run(pFiles, String.format("class%d_iter%d", jobID,
	// iteration));
	// responseTime = Optimizer.calculateResponseTime(throughput, nUsers,
	// think);
	// }
	// for (int iteration = 0; responseTime < deadline && iteration <=
	// maxIterations
	// && nContainers > 1; ++iteration) {
	// tempResponseTime = responseTime;
	// // TODO aggiungere vm invece che gli slot.
	// ++nContainers;
	//
	// if (fileUtility.delete(pFiles))
	// logger.info("Working files correctly deleted");
	//
	// pFiles = createSPNWorkingFiles(solPerJob, Optional.of(iteration));
	//
	// throughput = SPNSolver.run(pFiles, String.format("class%d_iter%d", jobID,
	// iteration));
	// responseTime = Optimizer.calculateResponseTime(throughput, nUsers,
	// think);
	// }
	//
	// } else {
	// // TODO: check on this.
	// while (responseTime > deadline) {
	// ++nContainers;
	//
	// if (fileUtility.delete(pFiles))
	// logger.info("Working files correctly deleted");
	//
	// pFiles = createSPNWorkingFiles(solPerJob);
	//
	// throughput = SPNSolver.run(pFiles, String.format("class%d", jobID));
	// responseTime = Optimizer.calculateResponseTime(throughput, nUsers,
	// think);
	// tempResponseTime = responseTime;
	// }
	// }
	//
	// if (fileUtility.delete(pFiles))
	// logger.info("Working files correctly deleted");
	//
	// solPerJob.setDuration(tempResponseTime);
	// solPerJob.setNumberUsers(nUsers);
	// solPerJob.setNumberContainers(nContainers);
	// solPerJob.setNumberVM(nContainers / solPerJob.getNumCores()); // TODO
	// // check.
	// res = Optional.of(tempResponseTime);
	// } catch (Exception e) {
	// logger.error("Some error happend in the local search");
	// res = Optional.empty();
	// }
	// return res;
	// }

	private Pair<File, File> createSPNWorkingFiles(SolutionPerJob solPerJob) throws IOException {
		return createSPNWorkingFiles(solPerJob, Optional.empty());
	}

	private Pair<File, File> createSPNWorkingFiles(SolutionPerJob solPerJob, Optional<Integer> iteration) throws IOException {
		int nContainers = solPerJob.getNumberContainers();
		JobClass jobClass = solPerJob.getJob();
		Profile prof = solPerJob.getProfile();
		int jobID = jobClass.getId();
		double mAvg = prof.getMavg();
		double rAvg = prof.getRavg();
		double shTypAvg = prof.getSHtypavg();
		double think = jobClass.getThink();

		int nUsers = solPerJob.getNumberUsers();
		int NM = prof.getNM();
		int NR = prof.getNR();

		String netFileContent = new PNNetFileBuilder().setCores(nContainers).setMapRate(1 / mAvg).setReduceRate(1 / (rAvg + shTypAvg)).setThinkRate(1 / think).build();

		File netFile;
		if (iteration.isPresent()) netFile = fileUtility.provideTemporaryFile(String.format("S4C-class%d-iter%d-", jobID, iteration), ".net");
		else netFile = fileUtility.provideTemporaryFile(String.format("S4C-class%d-", jobID), ".net");

		fileUtility.writeContentToFile(netFileContent, netFile);

		String defFileContent = new PNDefFileBuilder().setConcurrency(nUsers).setNumberOfMapTasks(NM).setNumberOfReduceTasks(NR).build();
		File defFile;
		if (iteration.isPresent()) defFile = fileUtility.provideTemporaryFile(String.format("S4C-class%d-iter%d-", jobID, iteration.get()), ".def");
		else defFile = fileUtility.provideTemporaryFile(String.format("S4C-class%d-", jobID), ".def");

		fileUtility.writeContentToFile(defFileContent, defFile);
		return new ImmutablePair<File, File>(netFile, defFile);

	}

}
