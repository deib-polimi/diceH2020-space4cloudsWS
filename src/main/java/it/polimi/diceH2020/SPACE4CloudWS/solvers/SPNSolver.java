package it.polimi.diceH2020.SPACE4CloudWS.solvers;

import java.io.File;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Arrays;
import java.util.List;

import javax.annotation.PostConstruct;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import it.polimi.diceH2020.SPACE4CloudWS.connection.SshConnector;
import it.polimi.diceH2020.SPACE4CloudWS.fileManagement.FileUtility;

@Component
public class SPNSolver {
	private final Integer MAXITER = 3;

	private SshConnector connector;

	@Autowired
	private SPNSettings connSettings;

	@Autowired
	private FileUtility fileUtility;

	@Autowired
	private Environment environment; // this is to check which is the active
										// profile at runtime

	private static Logger logger = Logger.getLogger(SPNSolver.class.getName());

	public SPNSolver() {
	}

	@PostConstruct
	private void init() {
		connector = new SshConnector(connSettings);
	}

	/**
	 * @param netFile
	 *            GreatSPN .net file
	 * @param defFile
	 *            GreatSPN .def file
	 * @param remoteName
	 *            base name for remote network files
	 * @return the throughput of the simulation
	 * @throws Exception
	 */
	public BigDecimal run(Pair<File, File> pFiles, String remoteName) throws Exception {
		return run(pFiles, remoteName, 0);
	}

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

	public void setAccuracy(double accuracy) {
		connSettings.setAccuracy(accuracy);
	}

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

	public List<String> pwd() throws Exception {
		return connector.pwd();
	}

	public SshConnector getConnector() {
		return connector;
	}
}
