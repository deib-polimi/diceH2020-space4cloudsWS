package it.polimi.diceH2020.SPACE4CloudWS.core;

import java.io.File;
import java.util.concurrent.Future;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.AsyncResult;
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
	private SPNSolver SPNSolver;

	@Autowired
	private FileUtility fileUtility;
	
	@Async
	public Future<Double> execute(SolutionPerJob solPerJob, int cycles) throws Exception {
		double tempResponseTime = 0;
		int maxIterations = (int) Math.ceil((double) cycles / 2.0);
		JobClass jobClass = solPerJob.getJob();
		
		int jobID = jobClass.getId();
		Profile prof = solPerJob.getProfile();
		
		double deadline = jobClass.getD();
		double mAvg = prof.getMavg();
		double rAvg = prof.getRavg();
		double shTypAvg = prof.getSHtypavg();
		double think = jobClass.getThink();

		int nUsers = solPerJob.getNumberUsers();
		int NM = prof.getNM();
		int NR = prof.getNR();
		int nContainers = solPerJob.getNumberContainers();
		int hUp = jobClass.getHup();

		String netFileContent = new PNNetFileBuilder().setCores(nContainers)
				.setMapRate(1 / mAvg).setReduceRate(1 / (rAvg + shTypAvg))
				.setThinkRate(1 / think).build();
		File netFile = fileUtility.provideTemporaryFile(String.format("S4C-class%d-", jobID), ".net");
		fileUtility.writeContentToFile(netFileContent, netFile);
		String defFileContent = new PNDefFileBuilder().setConcurrency(nUsers).setNumberOfMapTasks(NM)
				.setNumberOfReduceTasks(NR).build();
		File defFile = fileUtility.provideTemporaryFile(String.format("S4C-class%d-", jobID), ".def");
		fileUtility.writeContentToFile(defFileContent, defFile);

		double throughput = SPNSolver.run(netFile, defFile, "class" + jobID);

		double responseTime = Optimizer.calculateResponseTime(throughput, nUsers, think);

		if (responseTime < deadline) {
			for (int iteration = 0; responseTime < deadline && nUsers < hUp && iteration <= maxIterations; ++iteration) {
				tempResponseTime = responseTime;
				++nUsers;

				if (fileUtility.delete(defFile)) {
					logger.debug(defFile + " deleted");
				}
				defFileContent = new PNDefFileBuilder().setConcurrency(nUsers).setNumberOfMapTasks(NM)
						.setNumberOfReduceTasks(NR).build();
				defFile = fileUtility.provideTemporaryFile(String.format("S4C-class%d-iter%d-", jobID, iteration), ".def");
				fileUtility.writeContentToFile(defFileContent, defFile);

				throughput = SPNSolver.run(netFile, defFile, String.format("class%d_iter%d", jobID, iteration));
				responseTime = Optimizer.calculateResponseTime(throughput, nUsers, think);
			}
			for (int iteration = 0; responseTime < deadline && iteration <= maxIterations && nContainers > 1; ++iteration) {
				tempResponseTime = responseTime;
				// TODO aggiungere vm invece che gli slot.
				++nContainers;

				if (fileUtility.delete(netFile)) {
					logger.debug(netFile + " deleted");
				}
				netFileContent = new PNNetFileBuilder().setCores(nContainers)
						.setMapRate(1 / mAvg).setReduceRate(1 / (rAvg + shTypAvg))
						.setThinkRate(1 / think).build();
				netFile = fileUtility.provideTemporaryFile(String.format("S4C-class%d-iter%d-", jobID, iteration), ".net");
				fileUtility.writeContentToFile(netFileContent, netFile);

				throughput = SPNSolver.run(netFile, defFile, String.format("class%d_iter%d", jobID, iteration));
				responseTime = Optimizer.calculateResponseTime(throughput, nUsers, think);
			}

		} else {
			// TODO: check on this.
			while (responseTime > deadline) {
				++nContainers;

				if (fileUtility.delete(netFile)) {
					logger.debug(netFile + " deleted");
				}
				netFileContent = new PNNetFileBuilder().setCores(nContainers)
						.setMapRate(1 / mAvg).setReduceRate(1 / (rAvg + shTypAvg))
						.setThinkRate(1 / think).build();
				netFile = fileUtility.provideTemporaryFile(String.format("S4C-class%d-", jobID), ".net");
				fileUtility.writeContentToFile(netFileContent, netFile);

				throughput = SPNSolver.run(netFile, defFile, String.format("class%d", jobID));
				responseTime = Optimizer.calculateResponseTime(throughput, nUsers, think);
				tempResponseTime = responseTime;
			}
		}

		if (fileUtility.delete(netFile)) {
			logger.debug(netFile + " deleted");
		}
		if (fileUtility.delete(defFile)) {
			logger.debug(defFile + " deleted");
		}

		Thread.sleep(1000L);
		solPerJob.setDuration(tempResponseTime);
		solPerJob.setNumberUsers(nUsers);
		solPerJob.setNumberContainers(nContainers);
		solPerJob.setNumberVM(nContainers / solPerJob.getNumCores()); // TODO check.
		return new AsyncResult<Double>(tempResponseTime);
	}

}
