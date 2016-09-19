/*
Copyright 2016 Michele Ciavotta
Copyright 2016 Eugenio Gianniti
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
package it.polimi.diceH2020.SPACE4CloudWS.solvers.solversImpl.QNSolver;

import it.polimi.diceH2020.SPACE4Cloud.shared.solution.Solution;
import it.polimi.diceH2020.SPACE4Cloud.shared.solution.SolutionPerJob;
import it.polimi.diceH2020.SPACE4CloudWS.solvers.AbstractSolver;
import it.polimi.diceH2020.SPACE4CloudWS.solvers.settings.ConnectionSettings;
import lombok.NonNull;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Created by ciavotta on 11/02/16.
 */
@Component
public class QNSolver extends AbstractSolver {

	@Override
	protected Class<? extends ConnectionSettings> getSettingsClass() {
		return QNSettings.class;
	}

	private Pair<BigDecimal, Boolean> run(List<File> pFiles, String remoteName, Integer iteration) throws Exception {
		if (iteration < MAX_ITERATIONS) {
			File jmtFile = pFiles.get(2); // it is the third in the list (.jsimg)

			String jmtFileName = jmtFile.getName();

			String remotePath = connSettings.getRemoteWorkDir() + "/" + jmtFileName;
			logger.info(remoteName + "-> Starting Queuing Net resolution on the server");

			sendFiles(pFiles);
			logger.debug(remoteName + "-> Working files sent");

			String command = ((QNSettings) connSettings).getMaxDuration() == Integer.MIN_VALUE
					? String.format("java -cp %s jmt.commandline.Jmt sim %s ", connSettings.getSolverPath(), remotePath)
					: String.format("java -cp %s jmt.commandline.Jmt sim %s -maxtime %d",
					connSettings.getSolverPath(), remotePath, ((QNSettings) connSettings).getMaxDuration());

			logger.debug(remoteName + "-> Starting JMT model...");
			List<String> remoteMsg = connector.exec(command, getClass());
			if (remoteMsg.contains("exit-status: 0")) logger.info(remoteName + "-> The remote optimization process completed correctly");
			else {
				logger.debug(remoteName + "-> Remote exit status: " + remoteMsg);
				iteration = iteration + 1;
				return run(pFiles, remoteName, iteration);
			}

			File solFile = fileUtility.provideTemporaryFile(jmtFileName + "-result", ".jsim");
			connector.receiveFile(solFile.getAbsolutePath(), remotePath + "-result" + ".jsim", getClass());
			SolutionsWrapper resultObject = SolutionsWrapper.unMarshal(solFile);
			if (fileUtility.delete(solFile)) logger.debug(solFile + " deleted");

			Double throughput = resultObject.getMeanValue();
			boolean failure = resultObject.isFailed();

			return Pair.of(BigDecimal.valueOf(throughput).setScale(8, RoundingMode.HALF_EVEN), failure);
		} else {
			logger.debug(remoteName + "-> Error in remote optimization");
			throw new Exception("Error in the QN server");
		}
	}

	@Override
	protected Pair<BigDecimal, Boolean> run(List<File> pFiles, String s) throws Exception {
		return run(pFiles, s, 0);
	}

	private List<File> createProfileFiles(@NonNull SolutionPerJob solutionPerJob) throws IOException {
		String solID = solutionPerJob.getParentID();
		String jobID = solutionPerJob.getId();
		String vmID = solutionPerJob.getTypeVMselected().getId();

		InputStream inputStreamMap = getClass().getResourceAsStream(String.format("/QN/%sMapJ%s%s.txt",  solID, jobID, vmID));
		if (inputStreamMap == null) inputStreamMap = fileUtility.getFileAsStream(String.format("%sMapJ%s%s.txt", solID, jobID, vmID));

		File tempFileMap = null;

		if (inputStreamMap != null) {
			tempFileMap = fileUtility.provideTemporaryFile(String.format("MapJ%s", jobID), ".txt");
			FileOutputStream outputStreamTempMap = new FileOutputStream(tempFileMap);
			IOUtils.copy(inputStreamMap, outputStreamTempMap);
		}else{
			logger.info("Missing replayer Map file");
		}

		InputStream inputStreamRS = getClass().getResourceAsStream(String.format("/QN/%sRSJ%s%s.txt", solID, jobID, vmID));
		if (inputStreamRS == null) inputStreamRS = fileUtility.getFileAsStream(String.format("%sRSJ%s%s.txt", solID, jobID, vmID));

		File tempFileRS = null;
		if (inputStreamRS != null) {
			tempFileRS = fileUtility.provideTemporaryFile(String.format("RSJ%s", jobID), ".txt");
			FileOutputStream outputStreamTempRS = new FileOutputStream(tempFileRS);
			IOUtils.copy(inputStreamRS, outputStreamTempRS);
		}else{
			logger.info("Missing replayer RS file");
		}

		List<File> lst = new ArrayList<>(2);

		lst.add(tempFileMap);
		lst.add(tempFileRS);
		return lst;
	}

	private void sendFiles(List<File> lstFiles) {
		lstFiles.stream().forEach((File file) -> {
			try {
				connector.sendFile(file.getAbsolutePath(), connSettings.getRemoteWorkDir() + "/" + file.getName(),
						getClass());
			} catch (Exception e) {
				e.printStackTrace();
			}
		});
	}

	public List<File> createWorkingFiles(@NonNull SolutionPerJob solPerJob) throws IOException {
		return createWorkingFiles(solPerJob, Optional.empty());
	}

	@Override
	public Optional<BigDecimal> evaluate(@NonNull Solution solution) {
		return null;
	}

	private List<File> createWorkingFiles(SolutionPerJob solPerJob, Optional<Integer> iteration) throws IOException {
		List<File> lst = createProfileFiles(solPerJob);
		Integer nContainers = solPerJob.getNumberContainers();
		Integer concurrency = solPerJob.getNumberUsers();
		Integer numMap = (int)solPerJob.getProfile().get("nm");
		Integer numReduce = (int)solPerJob.getProfile().get("nr");
		Double think = solPerJob.getJob().getThink();
		String jobID = solPerJob.getId();
		String mapFileName = lst.get(0).getName();
		String rsFileName = lst.get(1).getName();

		String remoteMapFilePath = String.format("%s/%s", connSettings.getRemoteWorkDir(), mapFileName);
		String remoteRSFilePath = String.format("%s/%s", connSettings.getRemoteWorkDir(), rsFileName);
		String jsimgfileContent = new QNFileBuilder()
				.setQueueingNetworkModel(((QNSettings) connSettings).getModel())
				.setCores(nContainers).setConcurrency(concurrency)
				.setNumberOfMapTasks(numMap).setNumberOfReduceTasks(numReduce)
				.setMapFilePath(remoteMapFilePath).setRsFilePath(remoteRSFilePath)
				.setThinkRate(1 / think).setAccuracy(connSettings.getAccuracy() / 100)
				.setSignificance(((QNSettings) connSettings).getSignificance()).build();

		File jsimgTempFile;
		if (iteration.isPresent()) jsimgTempFile = fileUtility.provideTemporaryFile(String
				.format("QN-%s-class%s-it%d-", solPerJob.getParentID(), jobID, iteration.get()), ".jsimg");
		else jsimgTempFile = fileUtility.provideTemporaryFile(String.format("QN-%s-class%s-",
				solPerJob.getParentID(), jobID), ".jsimg");

		fileUtility.writeContentToFile(jsimgfileContent, jsimgTempFile);
		lst.add(jsimgTempFile);
		return lst;
	}

	public List<String> pwd() throws Exception {
		return connector.pwd(getClass());
	}

}
