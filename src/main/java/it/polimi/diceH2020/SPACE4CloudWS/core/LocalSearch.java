package it.polimi.diceH2020.SPACE4CloudWS.core;

import java.io.File;
import java.io.IOException;
import java.util.Optional;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import it.polimi.diceH2020.SPACE4Cloud.shared.inputData.JobClass;
import it.polimi.diceH2020.SPACE4Cloud.shared.inputData.Profile;
import it.polimi.diceH2020.SPACE4Cloud.shared.solution.SolutionPerJob;
import it.polimi.diceH2020.SPACE4CloudWS.fileManagement.FileUtility;
import it.polimi.diceH2020.SPACE4CloudWS.fileManagement.PNDefFileBuilder;
import it.polimi.diceH2020.SPACE4CloudWS.fileManagement.PNNetFileBuilder;
import it.polimi.diceH2020.SPACE4CloudWS.solvers.SPNSolver;

@Component
public class LocalSearch {
	private final static Logger logger = Logger.getLogger(LocalSearch.class.getName());

	@Autowired
	private static SPNSolver SPNSolver;

	@Autowired
	private FileUtility fileUtility;


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

	public Optional<Double> execute(SolutionPerJob solPerJob, int cycles) {
		double tempResponseTime = 0;
		int maxIterations = (int) Math.ceil((double) cycles / 2.0);
		JobClass jobClass = solPerJob.getJob();

		int jobID = jobClass.getId();

		double deadline = jobClass.getD();
		double think = jobClass.getThink();

		int nUsers = solPerJob.getNumberUsers();
		int nContainers = solPerJob.getNumberContainers();
		int hUp = jobClass.getHup();

		Optional<Double> res;
		try {
			Pair<File, File> pFiles = createSPNWorkingFiles(solPerJob);
			double throughput = SPNSolver.run(pFiles, "class" + jobID);
			double responseTime = Optimizer.calculateResponseTime(throughput, nUsers, think);

			if (responseTime < deadline) {
				for (int iteration = 0; responseTime < deadline && nUsers < hUp
						&& iteration <= maxIterations; ++iteration) {
					tempResponseTime = responseTime;
					++nUsers;

					if (fileUtility.delete(pFiles))
						logger.info("Working files correctly deleted");
					pFiles = createSPNWorkingFiles(solPerJob, Optional.of(iteration));
					
					throughput = SPNSolver.run(pFiles, String.format("class%d_iter%d", jobID, iteration));
					responseTime = Optimizer.calculateResponseTime(throughput, nUsers, think);
				}
				for (int iteration = 0; responseTime < deadline && iteration <= maxIterations
						&& nContainers > 1; ++iteration) {
					tempResponseTime = responseTime;
					// TODO aggiungere vm invece che gli slot.
					++nContainers;

					if (fileUtility.delete(pFiles))
						logger.info("Working files correctly deleted");

					pFiles = createSPNWorkingFiles(solPerJob, Optional.of(iteration));

					throughput = SPNSolver.run(pFiles, String.format("class%d_iter%d", jobID, iteration));
					responseTime = Optimizer.calculateResponseTime(throughput, nUsers, think);
				}

			} else {
				// TODO: check on this.
				while (responseTime > deadline) {
					++nContainers;

					if (fileUtility.delete(pFiles))
						logger.info("Working files correctly deleted");

					pFiles = createSPNWorkingFiles(solPerJob);

					throughput = SPNSolver.run(pFiles, String.format("class%d", jobID));
					responseTime = Optimizer.calculateResponseTime(throughput, nUsers, think);
					tempResponseTime = responseTime;
				}
			}

			if (fileUtility.delete(pFiles))
				logger.info("Working files correctly deleted");

			solPerJob.setDuration(tempResponseTime);
			solPerJob.setNumberUsers(nUsers);
			solPerJob.setNumberContainers(nContainers);
			solPerJob.setNumberVM(nContainers / solPerJob.getNumCores()); // TODO
																			// check.
			res = Optional.of(tempResponseTime);
		} catch (Exception e) {
			logger.error("Some error happend in the local search");
			res = Optional.empty();
		}
		return res;
	}

	private  Pair<File, File> createSPNWorkingFiles(SolutionPerJob solPerJob) throws IOException {
		return createSPNWorkingFiles(solPerJob, Optional.empty());
	}

	private  Pair<File, File> createSPNWorkingFiles(SolutionPerJob solPerJob, Optional<Integer> iteration)
			throws IOException {
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

		String netFileContent = new PNNetFileBuilder().setCores(nContainers).setMapRate(1 / mAvg)
				.setReduceRate(1 / (rAvg + shTypAvg)).setThinkRate(1 / think).build();

		File netFile;
		if (iteration.isPresent()) {
			netFile = fileUtility.provideTemporaryFile(String.format("S4C-class%d-", jobID), ".net");

		} else
			netFile = fileUtility.provideTemporaryFile(String.format("S4C-class%d-iter%d-", jobID, iteration), ".net");

		fileUtility.writeContentToFile(netFileContent, netFile);

		String defFileContent = new PNDefFileBuilder().setConcurrency(nUsers).setNumberOfMapTasks(NM)
				.setNumberOfReduceTasks(NR).build();
		File defFile;
		if (iteration.isPresent()) {
			defFile = fileUtility.provideTemporaryFile(String.format("S4C-class%d-iter%d-", jobID, iteration.get()),
					".def");
		} else
			defFile = fileUtility.provideTemporaryFile(String.format("S4C-class%d-", jobID), ".def");
		fileUtility.writeContentToFile(defFileContent, defFile);
		return new ImmutablePair<File, File>(netFile, defFile);

	}

}
