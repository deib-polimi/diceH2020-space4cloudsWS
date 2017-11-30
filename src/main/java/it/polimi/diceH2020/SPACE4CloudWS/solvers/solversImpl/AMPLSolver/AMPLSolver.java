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
package it.polimi.diceH2020.SPACE4CloudWS.solvers.solversImpl.AMPLSolver;

import com.jcraft.jsch.JSchException;
import it.polimi.diceH2020.SPACE4Cloud.shared.settings.Technology;
import it.polimi.diceH2020.SPACE4Cloud.shared.solution.Matrix;
import it.polimi.diceH2020.SPACE4Cloud.shared.solution.MatrixHugeHoleException;
import it.polimi.diceH2020.SPACE4Cloud.shared.solution.Solution;
import it.polimi.diceH2020.SPACE4Cloud.shared.solution.SolutionPerJob;
import it.polimi.diceH2020.SPACE4CloudWS.services.DataService;
import it.polimi.diceH2020.SPACE4CloudWS.solvers.MINLPSolver;
import it.polimi.diceH2020.SPACE4CloudWS.solvers.settings.ConnectionSettings;
import lombok.NonNull;
import lombok.Setter;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.validation.constraints.NotNull;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class AMPLSolver extends MINLPSolver{

	private static final String AMPL_FILES = "/AMPL";
	private static final String REMOTE_SCRATCH = "/scratch";
	private static final String REMOTEPATH_DATA_DAT = REMOTE_SCRATCH + "/data.dat";

	@Override
	protected Class<? extends ConnectionSettings> getSettingsClass() {
		return AMPLSettings.class;
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
			System.out.print("[#       ] Sending work files\r");
			sendFile(localPath + "/model.run", connSettings.getRemoteWorkDir() + "/problems/model.mod");
			System.out.print("[##      ] Sending work files\r");
			sendFile(localPath + "/save_aux.run", connSettings.getRemoteWorkDir() + "/utils/save_aux.run");
			System.out.print("[###     ] Sending work files\r");
			sendFile(localPath + "/knapsack.run", connSettings.getRemoteWorkDir() + "/problems/knapsack.run");
			System.out.print("[####    ] Sending work files\r");
			sendFile(localPath + "/bin_packing.run", connSettings.getRemoteWorkDir() + "/problems/bin_packing.run");
			System.out.print("[#####   ] Sending work files\r");
			sendFile(localPath + "/post_processing.run",
					connSettings.getRemoteWorkDir() + "/utils/post_processing.run");
			System.out.print("[######  ] Sending work files\r");
			sendFile(localPath + "/save_knapsack.run", connSettings.getRemoteWorkDir() + "/utils/save_knapsack.run");
			System.out.print("[####### ] Sending work files\r");
			sendFile(localPath + "/save_bin_packing.run",
					connSettings.getRemoteWorkDir() + "/utils/save_bin_packing.run");
			System.out.print("[########] Sending work files\r");

			logger.info("AMPL files sent");
		}
	}

	protected Pair<Double, Boolean> run (@NotNull Pair<List<File>, List<File>> pFiles, String remoteName)
			throws JSchException, IOException {
		List<File> amplFiles = pFiles.getLeft();
		boolean stillNotOk = true;
		for (int iteration = 0; stillNotOk && iteration < MAX_ITERATIONS; ++iteration) {
			File dataFile = amplFiles.get(0);
			String fullRemotePath = connSettings.getRemoteWorkDir() + REMOTEPATH_DATA_DAT;
			connector.sendFile(dataFile.getAbsolutePath(), fullRemotePath, getClass());
			logger.info(remoteName + "-> AMPL .data file sent");

			String remoteRelativeDataPath = ".." + REMOTEPATH_DATA_DAT;
			String remoteRelativeSolutionPath = ".." + RESULTS_SOLFILE;
			Matcher matcher = Pattern.compile("([\\w.-]*)(?:-\\d*)\\.dat").matcher(dataFile.getName());
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
			if (fileUtility.delete(runFile))
				logger.debug(runFile + " deleted");

			logger.debug(remoteName + "-> Cleaning result directory");
			clearResultDir();

			logger.info(remoteName + "-> Processing execution...");
			String command = String.format("cd %s%s && %s %s", connSettings.getRemoteWorkDir(), REMOTE_SCRATCH,
					((AMPLSettings) connSettings).getAmplDirectory(), REMOTEPATH_DATA_RUN);
			List<String> remoteMsg = connector.exec(command, getClass());
			if (remoteMsg.contains("exit-status: 0")) {
				stillNotOk = false;
				logger.info(remoteName + "-> The remote optimization process completed correctly");
			} else {
				logger.info("Remote exit status: " + remoteMsg);
				if (remoteMsg.get(0).contains("error processing param")) {
					iteration = MAX_ITERATIONS;
					logger.info(remoteName + "-> Wrong parameters. Aborting");
				} else {
					logger.info(remoteName + "-> Restarted. Iteration " + iteration);
				}
			}
		}

		if (stillNotOk) {
			logger.info(remoteName + "-> Error in remote optimization");
			throw new IOException("Error in the initial solution creation process");
		} else {
			File solutionFile = amplFiles.get(1);
			String fullRemotePath = connSettings.getRemoteWorkDir() + RESULTS_SOLFILE;
			connector.receiveFile(solutionFile.getAbsolutePath(), fullRemotePath, getClass());
			Double objFunctionValue = analyzeSolution(solutionFile, ((AMPLSettings) connSettings).isVerbose());
			logger.info(remoteName + "-> The value of the objective function is: " + objFunctionValue);
			// TODO: this always returns false, should check if every error just throws
			return Pair.of(objFunctionValue, false);
		}
	}
}
