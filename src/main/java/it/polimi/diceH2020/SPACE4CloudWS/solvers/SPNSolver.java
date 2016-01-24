package it.polimi.diceH2020.SPACE4CloudWS.solvers;

import it.polimi.diceH2020.SPACE4CloudWS.connection.SshConnector;
import it.polimi.diceH2020.SPACE4CloudWS.fileManagement.FileUtility;

import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Component
public class SPNSolver {
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
	public double run(File netFile, File defFile, String remoteName) throws Exception {
		String remotePath = connSettings.getRemoteWorkDir() + "/" + remoteName;

		connector.sendFile(netFile.getAbsolutePath(), remotePath + ".net");
		logger.info("GreatSPN .net file sent");
		connector.sendFile(defFile.getAbsolutePath(), remotePath + ".def");
		logger.info("GreatSPN .def file sent");
		File statFile = fileUtility.provideTemporaryFile("S4C-stat-", ".stat");
		fileUtility.writeContentToFile("end\n", statFile);
		connector.sendFile(statFile.getAbsolutePath(), remotePath + ".stat");
		logger.info("GreatSPN .stat file sent");
		if (fileUtility.delete(statFile)) {
			logger.debug(statFile + " deleted");
		}

		String command = connSettings.getSolverPath() + " " + remotePath + " -a "
				+ connSettings.getAccuracy() + " -c 6";
		logger.info("Starting GreatSPN model...");
		logger.info("Remote exit status: " + connector.exec(command));

		File solFile = fileUtility.provideTemporaryFile("S4C-" + remoteName, ".sta");
		connector.receiveFile(solFile.getAbsolutePath(), remotePath + ".sta");
		String solFileInString = FileUtils.readFileToString(solFile);
		if (fileUtility.delete(solFile)) {
			logger.debug(solFile + " deleted");
		}

		String throughputStr = "Thru_end = ";
		int startPos = solFileInString.indexOf(throughputStr);
		int endPos = solFileInString.indexOf('\n', startPos);
		double throughput = Double.parseDouble(solFileInString.substring(startPos + throughputStr.length(), endPos));
		logger.info("GreatSPN model run.");

		return throughput;
	}

	public List<Double> run2Classes(String nameInputFile, String nameSolutionFile) throws Exception {
		List<Double> throughputArray = new ArrayList<>(3);
		double thr;
		String solFileInString = null;

		logger.info("sto per eseguire");
		connector.sendFile(nameInputFile + ".net", connSettings.getRemoteWorkDir() + "/" + nameInputFile + ".net");
		logger.info("file" + nameInputFile + ".net has been sent");
		logger.info("file" + nameInputFile + "has been sent");

		connector.sendFile(nameInputFile + ".def", connSettings.getRemoteWorkDir() + "/" + nameInputFile + ".def");
		logger.info("file run have been sent");
		logger.info("file run have been sent");

		String command = connSettings.getSolverPath() + " " + connSettings.getRemoteWorkDir() + "/" + nameInputFile
				+ " -M 10000";
		connector.exec(command);
		logger.info("processing execution..." + nameInputFile);
		logger.info("processing execution..." + nameInputFile);

		File file = new File(nameSolutionFile);
		if (!file.exists())
			file.createNewFile();

		connector.receiveFile(nameSolutionFile, connSettings.getRemoteWorkDir() + "/" + nameInputFile + ".sta");
		solFileInString = FileUtils.readFileToString(file);
		logger.info(nameSolutionFile);
		String throughputStr = "Thru_end = ";
		int startPos = solFileInString.indexOf(throughputStr);
		int endPos = solFileInString.indexOf('\n', startPos);
		thr = Double.parseDouble(solFileInString.substring(startPos + throughputStr.length(), endPos));
		throughputArray.add(thr);

		String throughputStr1 = "Thru_join2Cb = ";
		startPos = solFileInString.indexOf(throughputStr1);
		endPos = solFileInString.indexOf('\n', startPos);
		thr = Double.parseDouble(solFileInString.substring(startPos + throughputStr.length(), endPos));
		throughputArray.add(thr);
		String throughputStr2 = "Thru_join2Cb = ";
		startPos = solFileInString.indexOf(throughputStr2);
		endPos = solFileInString.indexOf('\n', startPos);
		thr = Double.parseDouble(solFileInString.substring(startPos + throughputStr.length(), endPos));
		throughputArray.add(thr);
		return throughputArray;
	}

	public void setAccuracy(double accuracy) {
		connSettings.setAccuracy(accuracy);
	}

	public void initRemoteEnvironment() throws Exception {
		List<String> lstProfiles = Arrays.asList(this.environment.getActiveProfiles());
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
