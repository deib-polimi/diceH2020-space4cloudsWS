package it.polimi.diceH2020.SPACE4CloudWS.solvers;

import it.polimi.diceH2020.SPACE4Cloud.shared.inputData.JobClass;
import it.polimi.diceH2020.SPACE4Cloud.shared.inputData.Profile;
import it.polimi.diceH2020.SPACE4Cloud.shared.solution.SolutionPerJob;
import it.polimi.diceH2020.SPACE4CloudWS.connection.SshConnector;
import it.polimi.diceH2020.SPACE4CloudWS.fileManagement.FileUtility;
import it.polimi.diceH2020.SPACE4CloudWS.fileManagement.PNDefFileBuilder;
import it.polimi.diceH2020.SPACE4CloudWS.fileManagement.PNNetFileBuilder;
import lombok.NonNull;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

/**
 * Class that manages the interactions with GreatSPN solver
 */
@Component
public class SPNSolver implements Solver {
	private static Logger logger = Logger.getLogger(SPNSolver.class.getName());
	private final Integer MAXITER = 3;
	private SshConnector connector;
	@Autowired
	private SPNSettings connSettings;
	@Autowired
	private FileUtility fileUtility;
										// profile at runtime
										@Autowired
										private Environment environment; // this is to check which is the active

	public SPNSolver() {
	}

	@PostConstruct
	private void init() {
		connector = new SshConnector(connSettings);
	}

	public BigDecimal run(Pair<File, File> pFiles, String remoteName) throws Exception {
		return run(pFiles, remoteName, 0);
	}

	/**
	 * @param pFiles
	 * @param remoteName
	 * @param iter
	 * @return
	 * @throws Exception
	 */
	private BigDecimal run(Pair<File, File> pFiles, String remoteName, Integer iter) throws Exception {
		if (iter < MAXITER) {
			File netFile = pFiles.getLeft();
			File defFile = pFiles.getRight();
			String remotePath = connSettings.getRemoteWorkDir() + "/" + remoteName;
			logger.info(remoteName + "-> Starting Stochastic Petri Net simulation on the server");
			connector.sendFile(netFile.getAbsolutePath(), remotePath + ".net");
			logger.debug(remoteName + "-> GreatSPN .net file sent");
			connector.sendFile(defFile.getAbsolutePath(), remotePath + ".def");
			logger.debug(remoteName + "-> GreatSPN .def file sent");
			File statFile = fileUtility.provideTemporaryFile("S4C-stat-", ".stat");
			fileUtility.writeContentToFile("end\n", statFile);
			connector.sendFile(statFile.getAbsolutePath(), remotePath + ".stat");
			logger.debug(remoteName + "-> GreatSPN .stat file sent");
			if (fileUtility.delete(statFile))
				logger.debug(statFile + " deleted");

			String command = connSettings.getSolverPath() + " " + remotePath + " -a " + connSettings.getAccuracy()
					+ " -c 6";
			logger.debug(remoteName + "-> Starting GreatSPN model...");
			List<String> remoteMsg = connector.exec(command);
			if (remoteMsg.contains("exit-status: 0")) {
				logger.info(remoteName + "-> The remote optimization proces completed correctly");
			} else {
				logger.debug(remoteName + "-> Remote exit status: " + remoteMsg);
				iter = iter+1;
				return run(pFiles, remoteName, iter);
			}

			File solFile = fileUtility.provideTemporaryFile("S4C-" + remoteName, ".sta");
			connector.receiveFile(solFile.getAbsolutePath(), remotePath + ".sta");
			String solFileInString = FileUtils.readFileToString(solFile);
			if (fileUtility.delete(solFile))
				logger.debug(solFile + " deleted");

			String throughputStr = "Thru_end = ";
			int startPos = solFileInString.indexOf(throughputStr);
			int endPos = solFileInString.indexOf('\n', startPos);
			double throughput = Double
					.parseDouble(solFileInString.substring(startPos + throughputStr.length(), endPos));
			logger.debug(remoteName + "-> GreatSPN model run.");
			BigDecimal result = BigDecimal.valueOf(throughput);
			result.setScale(2, RoundingMode.HALF_EVEN);
			return result;
		}
		else
		{
			logger.debug(remoteName + "-> Error in remote optimziation" );
			throw new Exception("Error in the SPN server");

		}


	}

	@Override
	public void setAccuracy(double accuracy) {
		connSettings.setAccuracy(accuracy);
	}

	@Override
	public void initRemoteEnvironment() throws Exception {
		List<String> lstProfiles = Arrays.asList(environment.getActiveProfiles());
		logger.info("------------------------------------------------");
		logger.info("Starting SPN solver service initialization phase");
		logger.info("------------------------------------------------");
		if (lstProfiles.contains("test") && !connSettings.isForceClean()) {
			logger.info("Test phase: the remote work directory tree is assumed to be ok.");

		} else {
			logger.info("Clearing remote work directory tree");

			connector.exec("rm -rf " + connSettings.getRemoteWorkDir());
			logger.info("Creating new remote work directory tree");
			connector.exec("mkdir -p " + connSettings.getRemoteWorkDir());

			logger.info("Done");
		}

	}

	@Override
	public List<String> pwd() throws Exception {
		return connector.pwd();
	}

	@Override
	public SshConnector getConnector() {
		return connector;
	}


	public Pair<File, File> createWorkingFiles(@NonNull SolutionPerJob solPerJob) throws IOException {
		return createWorkingFiles(solPerJob, Optional.empty());
	}

	public void delete(Pair<File, File> pFiles) {
		if (fileUtility.delete(pFiles)) logger.debug("Working files correctly deleted");
	}

	@Override
	public Optional<BigDecimal> evaluate(@NonNull SolutionPerJob solPerJob) {
		if (!solPerJob.getChanged()) {
			return Optional.of(BigDecimal.valueOf(solPerJob.getDuration()));
		}
		JobClass jobClass = solPerJob.getJob();
		int jobID = jobClass.getId();
		int nUsers = solPerJob.getNumberUsers();
		double think = jobClass.getThink();
		Pair<File, File> pFiles;
		try {
			pFiles = createWorkingFiles(solPerJob);
			BigDecimal throughput = run(pFiles, "class" + jobID);
			delete(pFiles);
			BigDecimal duration = Solver.calculateResponseTime(throughput, nUsers, think);
			return Optional.of(duration);
		} catch (Exception e) {
			return Optional.empty();
		}
	}

	private Pair<File, File> createWorkingFiles(SolutionPerJob solPerJob, Optional<Integer> iteration) throws IOException {
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
		if (iteration.isPresent())
			netFile = fileUtility.provideTemporaryFile(String.format("S4C-class%d-iter%d-", jobID, iteration), ".net");
		else netFile = fileUtility.provideTemporaryFile(String.format("S4C-class%d-", jobID), ".net");

		fileUtility.writeContentToFile(netFileContent, netFile);

		String defFileContent = new PNDefFileBuilder().setConcurrency(nUsers).setNumberOfMapTasks(NM).setNumberOfReduceTasks(NR).build();
		File defFile;
		if (iteration.isPresent())
			defFile = fileUtility.provideTemporaryFile(String.format("S4C-class%d-iter%d-", jobID, iteration.get()), ".def");
		else defFile = fileUtility.provideTemporaryFile(String.format("S4C-class%d-", jobID), ".def");

		fileUtility.writeContentToFile(defFileContent, defFile);
		return new ImmutablePair<File, File>(netFile, defFile);

	}




}
