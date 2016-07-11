package it.polimi.diceH2020.SPACE4CloudWS.solvers.solversImpl.MINLPSolver;

import com.google.common.primitives.Doubles;
import com.google.common.primitives.Ints;
import it.polimi.diceH2020.SPACE4Cloud.shared.inputData.JobClass;
import it.polimi.diceH2020.SPACE4Cloud.shared.inputData.Profile;
import it.polimi.diceH2020.SPACE4Cloud.shared.inputData.TypeVM;
import it.polimi.diceH2020.SPACE4Cloud.shared.solution.Solution;
import it.polimi.diceH2020.SPACE4Cloud.shared.solution.SolutionPerJob;
import it.polimi.diceH2020.SPACE4CloudWS.services.DataService;
import it.polimi.diceH2020.SPACE4CloudWS.services.SshConnectorProxy;
import it.polimi.diceH2020.SPACE4CloudWS.solvers.AbstractSolver;
import it.polimi.diceH2020.SPACE4CloudWS.solvers.settings.ConnectionSettings;
import lombok.NonNull;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.validation.constraints.NotNull;
import java.io.*;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.Charset;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class MINLPSolver extends AbstractSolver {

	private static final String AMPL_FILES = "/AMPL";
	private static final String RESULTS_SOLFILE = "/results/solution.sol";
	private static final String REMOTE_SCRATCH = "/scratch";
	private static final String REMOTE_RESULTS = "/results";
	private static final String REMOTEPATH_DATA_DAT = REMOTE_SCRATCH + "/data.dat";
	private static final String REMOTEPATH_DATA_RUN = "data.run";

	@Autowired
	private DataService dataService;

	@Override
	protected Class<? extends ConnectionSettings> getSettingsClass() {
		return MINLPSettings.class;
	}

	private Double analyzeSolution(File solFile, boolean verbose) throws IOException {
		String fileToString = FileUtils.readFileToString(solFile);
		String centralized = "centralized_obj = ";
		int startPos = fileToString.indexOf(centralized);
		int endPos = fileToString.indexOf('\n', startPos);
		Double objFunctionValue = Double.parseDouble(fileToString.substring(startPos + centralized.length(), endPos));

		if (verbose) {
			logger.info(fileToString);
			logger.info(objFunctionValue);
		}
		return objFunctionValue;
	}

	public void test() throws IOException {
		String path = "/AMPL/centralized.run";
		InputStream res = getClass().getResourceAsStream(path);
		String theString = IOUtils.toString(res, Charset.defaultCharset());
		logger.info(theString);
	}

	@Override
	public void initRemoteEnvironment() throws Exception {
		List<String> lstProfiles = Arrays.asList(this.environment.getActiveProfiles());
		String localPath = AMPL_FILES;
		logger.info("------------------------------------------------");
		logger.info("Starting math solver service initialization phase");
		logger.info("------------------------------------------------");
		if (lstProfiles.contains("test") && !connSettings.isForceClean()) {
			logger.info("Test phase: the remote work directory tree is assumed to be ok.");
		} else {
			logger.info("Clearing remote work directory tree");
			try {
				String root = connSettings.getRemoteWorkDir();
				String cleanRemoteDirectoryTree = "rm -rf " + root;
				connector.exec(cleanRemoteDirectoryTree, getClass());

				logger.info("Creating new remote work directory tree");
				String makeRemoteDirectoryTree = "mkdir -p " + root + "/{problems,utils,solve,scratch,results}";
				connector.exec(makeRemoteDirectoryTree, getClass());
			} catch (Exception e) {
				logger.error("error preparing remote work directory", e);
			}

			logger.info("Sending AMPL files");
			System.out.print("[#             ] Sending work files\r");
			sendFile(localPath + "/model.run", connSettings.getRemoteWorkDir() + "/problems/model.mod");
			System.out.print("[##            ] Sending work files\r");
			sendFile(localPath + "/compute_psi.run", connSettings.getRemoteWorkDir() + "/utils/compute_psi.run");
			System.out.print("[###           ] Sending work files\r");
			sendFile(localPath + "/compute_job_profile.run", connSettings.getRemoteWorkDir() + "/utils/compute_job_profile.run");
			System.out.print("[####          ] Sending work files\r");
			sendFile(localPath + "/compute_penalties.run", connSettings.getRemoteWorkDir() + "/utils/compute_penalties.run");
			System.out.print("[#####         ] Sending work files\r");
			sendFile(localPath + "/save_aux.run", connSettings.getRemoteWorkDir() + "/utils/save_aux.run");
			System.out.print("[######        ] Sending work files\r");
			sendFile(localPath + "/centralized.run", connSettings.getRemoteWorkDir() + "/problems/centralized.run");
			System.out.print("[#######       ] Sending work files\r");
			sendFile(localPath + "/knapsack.run", connSettings.getRemoteWorkDir() + "/problems/knapsack.run");
			System.out.print("[########      ] Sending work files\r");
			sendFile(localPath + "/save_centralized.run", connSettings.getRemoteWorkDir() + "/utils/save_centralized.run");
			System.out.print("[#########     ] Sending work files\r");
			sendFile(localPath + "/compute_s_d.run", connSettings.getRemoteWorkDir() + "/utils/compute_s_d.run");
			System.out.print("[##########    ] Sending work files\r");
			sendFile(localPath + "/AM_closed_form.run", connSettings.getRemoteWorkDir() + "/solve/AM_closed_form.run");
			System.out.print("[###########   ] Sending work files\r");
			sendFile(localPath + "/post_processing.run", connSettings.getRemoteWorkDir() + "/utils/post_processing.run");
			System.out.print("[############  ] Sending work files\r");
			sendFile(localPath + "/save_centralized.run", connSettings.getRemoteWorkDir() + "/utils/save_centralized.run");
			System.out.print("[############# ] Sending work files\r");
			sendFile(localPath + "/save_knapsack.run", connSettings.getRemoteWorkDir() + "/utils/save_knapsack.run");
			System.out.print("[##############] Sending work files\r");
			logger.info("AMPL files sent");
		}
	}

	private void sendFile(String localPath, String remotePath) throws Exception {
		InputStream in = this.getClass().getResourceAsStream(localPath);
		File tempFile = fileUtility.provideTemporaryFile("S4C-temp", null);
		FileOutputStream out = new FileOutputStream(tempFile);
		IOUtils.copy(in, out);
		connector.sendFile(tempFile.getAbsolutePath(), remotePath, getClass());
		if (fileUtility.delete(tempFile))
			logger.debug(tempFile + " deleted");
	}

	public List<String> clearWorkingDir() throws Exception {
		String command = "rm -rf " + connSettings.getRemoteWorkDir();
		return connector.exec(command, getClass());
	}

	private void clearResultDir() throws Exception {
		String command = "rm -rf " + connSettings.getRemoteWorkDir() + REMOTE_RESULTS + "/*";
		connector.exec(command, getClass());
	}

	private Pair<BigDecimal, Boolean> run(List<File> pFiles, String remoteName, Integer iteration) throws Exception {
		if (iteration < MAX_ITERATIONS) {
			File dataFile = pFiles.get(0);
			File solutionFile = pFiles.get(1);
			String fullRemotePath = connSettings.getRemoteWorkDir() + REMOTEPATH_DATA_DAT;
			connector.sendFile(dataFile.getAbsolutePath(), fullRemotePath, getClass());
			logger.info(remoteName + "-> AMPL .data file sent");

			String remoteRelativeDataPath = ".." + REMOTEPATH_DATA_DAT;
			String remoteRelativeSolutionPath = ".." + RESULTS_SOLFILE;
			Matcher matcher = Pattern.compile("([\\w\\.-]*)(?:-\\d*)\\.dat").matcher(dataFile.getName());
			if (! matcher.matches()) {
				throw new RuntimeException(String.format("problem matching %s", dataFile.getName()));
			}
			String prefix = matcher.group(1);
			File runFile = fileUtility.provideTemporaryFile(prefix, ".run");
			String runFileContent = new AMPLRunFileBuilder().setDataFile(remoteRelativeDataPath)
					.setSolverPath(connSettings.getSolverPath()).setSolutionFile(remoteRelativeSolutionPath).build();
			fileUtility.writeContentToFile(runFileContent, runFile);

			fullRemotePath = connSettings.getRemoteWorkDir() + REMOTE_SCRATCH + "/" + REMOTEPATH_DATA_RUN;
			connector.sendFile(runFile.getAbsolutePath(), fullRemotePath, getClass());
			logger.info(remoteName + "-> AMPL .run file sent");
			if (fileUtility.delete(runFile)) logger.debug(runFile + " deleted");

			logger.debug(remoteName + "-> Cleaning result directory");
			clearResultDir();

			logger.info(remoteName + "-> Processing execution...");
			String command = String.format("cd %s%s && %s %s", connSettings.getRemoteWorkDir(),
					REMOTE_SCRATCH, ((MINLPSettings) connSettings).getAmplDirectory(), REMOTEPATH_DATA_RUN);
			List<String> remoteMsg = connector.exec(command, getClass());
			if (remoteMsg.contains("exit-status: 0")) {
				logger.info(remoteName + "-> The remote optimization process completed correctly");
			} else {
				logger.info("Remote exit status: " + remoteMsg);
				if (remoteMsg.get(0).contains("error processing param")) {
					iteration = MAX_ITERATIONS;
					logger.info(remoteName + "-> Wrong parameters. Aborting");
				} else {
					iteration = iteration + 1;
					logger.info(remoteName + "-> Restarted. Iteration " + iteration);
				}
				return run(pFiles, remoteName, iteration);
			}
			fullRemotePath = connSettings.getRemoteWorkDir() + RESULTS_SOLFILE;
			connector.receiveFile(solutionFile.getAbsolutePath(), fullRemotePath, getClass());
			Double objFunctionValue = analyzeSolution(solutionFile, ((MINLPSettings) connSettings).isVerbose());
			logger.info(remoteName + "-> The value of the objective function is: " + objFunctionValue);

			// TODO: this always returns false, should check if every error just throws
			return Pair.of(BigDecimal.valueOf(objFunctionValue).setScale(8, RoundingMode.HALF_EVEN), false);
		} else {
			logger.debug(remoteName + "-> Error in remote optimization");
			throw new Exception("Error in the initial solution creation process");
		}

	}

	@Override
	protected Pair<BigDecimal, Boolean> run(@NotNull List<File> pFiles, String s) throws Exception {
		return run(pFiles, s, 0);
	}

	@Override
	protected List<File> createWorkingFiles(SolutionPerJob solPerJob) throws IOException {
		Profile prof = solPerJob.getProfile();
		JobClass jobClass = solPerJob.getJob();
		TypeVM tVM = solPerJob.getTypeVMselected();
		AMPLDataFileBuilder builder = AMPLDataFileUtils.singleClassBuilder(dataService.getGamma(), jobClass, tVM, prof);
		builder.addArrayParameter("w",	Doubles.asList(dataService.getNumCores(tVM)))
				.addArrayParameter("sigmabar", Doubles.asList(dataService.getSigmaBar(tVM)))
				.addArrayParameter("deltabar", Doubles.asList(dataService.getDeltaBar(tVM)))
				.addArrayParameter("rhobar", Doubles.asList(dataService.getRhoBar(tVM)));

		String prefix = String.format("AMPL-%s-class%d-vm%s-", solPerJob.getParentID(), jobClass.getId(), tVM.getId());
		File dataFile = fileUtility.provideTemporaryFile(prefix, ".dat");
		fileUtility.writeContentToFile(builder.build(), dataFile);
		File resultsFile = fileUtility.provideTemporaryFile(prefix, ".sol");
		List<File> lst = new ArrayList<>(2);
		lst.add(dataFile);
		lst.add(resultsFile);
		return lst;
	}

	@Override
	public Optional<BigDecimal> evaluate(@NonNull SolutionPerJob solPerJob) {
		if (! solPerJob.getChanged()) return Optional.of(BigDecimal.valueOf(solPerJob.getDuration()));

		JobClass jobClass = solPerJob.getJob();
		String jobID = jobClass.getId();
		List<File> pFiles;
		try {
			pFiles = createWorkingFiles(solPerJob);
			Pair<BigDecimal, Boolean> result = run(pFiles, "class" + jobID);
			File resultsFile = pFiles.get(1);
			updateResults(Collections.singletonList(solPerJob), resultsFile);
			delete(pFiles);
			return Optional.of(result.getLeft());
		} catch (Exception e) {
			logger.debug("no result due to an exception", e);
			return Optional.empty();
		}
	}

	@Override
	public Optional<BigDecimal> evaluate(@NonNull Solution solution) {
		List<File> pFiles;
		try {
			pFiles = createWorkingFiles(solution);
			Pair<BigDecimal, Boolean> result = run(pFiles, "full solution");
			File resultsFile = pFiles.get(1);
			updateResults(solution.getLstSolutions(), resultsFile);
			delete(pFiles);
			return Optional.of(result.getLeft());
		} catch (Exception e) {
			logger.debug("no result due to an exception", e);
			return Optional.empty();
		}
	}

	private void updateResults(List<SolutionPerJob> solutions, File solutionFile) throws IOException {
		try (BufferedReader reader = new BufferedReader(new FileReader(solutionFile))) {
			String line = reader.readLine();

			while (! line.contains("solve_result ")) {
				line = reader.readLine();
			}

			String[] bufferStr = line.split("\\s+");
			if (bufferStr[2].equals("infeasible")) {
				logger.info("The problem is infeasible");
				return;
			}

			while (! line.contains("t [*]")) {
				line = reader.readLine();
			}
			for (SolutionPerJob solutionPerJob : solutions) {
				line = reader.readLine();
				bufferStr = line.split("\\s+");

				String localStr = bufferStr[1].replaceAll("\\s+", "");
				solutionPerJob.setDuration(Double.parseDouble(localStr));
			}

			while (! line.contains("Variables")) {
				line = reader.readLine();
			}
			line = reader.readLine();
			while (line.contains(":")) {
				line = reader.readLine();
			}

			for (SolutionPerJob solutionPerJob : solutions) {
				bufferStr = line.split("\\s+");
				double gamma = Double.parseDouble(bufferStr[2]);
				int numVM = (int) Math.ceil(gamma);
				solutionPerJob.setNumberVM(numVM);
				double psi = Double.parseDouble(bufferStr[5]);
				double users = 1 / psi;
				int numUsers = (int) users;
				solutionPerJob.setNumberUsers(numUsers);
				line = reader.readLine();
			}

			while (! line.contains("### alphabeta")) {
				line = reader.readLine();
			}

			reader.readLine();
			for (SolutionPerJob solutionPerJob : solutions) {
				line = reader.readLine();
				bufferStr = line.split("\\s+");

				String x = bufferStr[2].replaceAll("\\s+", "");
				solutionPerJob.setAlfa(Double.parseDouble(x));

				x = bufferStr[3].replaceAll("\\s+", "");
				solutionPerJob.setBeta(Double.parseDouble(x));
			}
		}
	}

	private List<File> createWorkingFiles(@NotNull Solution sol) throws IOException {
		AMPLDataFileBuilder builder = AMPLDataFileUtils.multiClassBuilder(dataService.getData(), sol.getPairsTypeVMJobClass());

		builder.addArrayParameter("w", sol.getLstNumberCores());
		builder.addArrayParameter("cM", sol.getListCM());
		builder.addArrayParameter("cR", sol.getListCR());
		builder.addArrayParameter("deltabar", sol.getListDeltabar());
		builder.addArrayParameter("rhobar", sol.getListRhobar());
		builder.addArrayParameter("sigmabar", sol.getListSigmaBar());

		String prefix = String.format("AMPL-%s-complete-", sol.getId());
		File dataFile = fileUtility.provideTemporaryFile(prefix, ".dat");
		fileUtility.writeContentToFile(builder.build(), dataFile);
		File resultsFile = fileUtility.provideTemporaryFile(prefix, ".sol");
		List<File> lst = new ArrayList<>(2);
		lst.add(dataFile);
		lst.add(resultsFile);
		return lst;
	}

	@Override
	public List<String> pwd() throws Exception {
		return connector.pwd(getClass());
	}

	@Override
	public SshConnectorProxy getConnector() {
		return connector;
	}

}
