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
import it.polimi.diceH2020.SPACE4CloudWS.core.DataProcessor;
import it.polimi.diceH2020.SPACE4CloudWS.solvers.AbstractSolver;
import it.polimi.diceH2020.SPACE4CloudWS.solvers.settings.ConnectionSettings;
import lombok.NonNull;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.jcraft.jsch.JSchException;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by ciavotta on 11/02/16.
 */
@Component
public class QNSolver extends AbstractSolver {
	@Autowired
	private DataProcessor dataProcessor;
	
	private Pattern patternMap = Pattern.compile("(.*)(Map[0-9]*)(J)(.*)");
	private Pattern patternRS = Pattern.compile("(.*)(RS[0-9]*)(J)(.*)");
	private Pattern patternNMR = Pattern.compile("((nm[0-9]*)|(nr[0-9]*))");
	
	@Override
	protected Class<? extends ConnectionSettings> getSettingsClass() {
		return QNSettings.class;
	}

	private Pair<BigDecimal, Boolean> run(List<File> pFiles, String remoteName, Integer iteration) throws Exception {
		if (iteration < MAX_ITERATIONS) {
			File jmtFile = pFiles.stream().filter(s->s.getName().contains(".jsimg")).findFirst().get(); // it is the third in the list (.jsimg)

			String jmtFileName = jmtFile.getName();

			String remotePath = connSettings.getRemoteWorkDir() + File.separator + dataProcessor.getCurrentInputsSubFolderName() + File.separator + jmtFileName;
			logger.info(remoteName + "-> Starting Queuing Net resolution on the server");

			sendFiles(pFiles);
			logger.debug(remoteName + "-> Working files sent");

			String command = ((QNSettings) connSettings).getMaxDuration() == Integer.MIN_VALUE
					? String.format("java -cp %s jmt.commandline.Jmt sim %s ", connSettings.getSolverPath(), remotePath)
					: String.format("java -cp %s jmt.commandline.Jmt sim %s -maxtime %d",
					connSettings.getSolverPath(), remotePath, ((QNSettings) connSettings).getMaxDuration());

			logger.debug(remoteName + "-> Starting JMT model...");			List<String> remoteMsg = connector.exec(command, getClass());
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
		String solutionID = solutionPerJob.getParentID();
		String spjID = solutionPerJob.getId();
		String provider = dataProcessor.getProviderName();
		String typeVM = solutionPerJob.getTypeVMselected().getId();
		
		return dataProcessor.getCurrentReplayersInputFiles(solutionID, spjID, provider, typeVM);
	}

	private void sendFiles(List<File> lstFiles) {
		try {
			connector.exec("mkdir -p "+connSettings.getRemoteWorkDir() + File.separator + dataProcessor.getCurrentInputsSubFolderName(), getClass());
		} catch (JSchException | IOException e1) {
			logger.debug("Cannot create new Simulation Folder!\n"+e1.getStackTrace());
		}
		
		
		lstFiles.stream().forEach((File file) -> {
			try {
				connector.sendFile(file.getAbsolutePath(), connSettings.getRemoteWorkDir() + File.separator + dataProcessor.getCurrentInputsSubFolderName() + File.separator + file.getName(),
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
		Double think = solPerJob.getJob().getThink();
		String jobID = solPerJob.getId();

		
		QueueingNetworkModel model = ((QNSettings) connSettings).getModel();
		int nMR = (int)solPerJob.getProfile().getProfileMap().keySet().stream().filter(s->{Matcher m = patternNMR.matcher(s);return m.matches();}).count();
		if(nMR>2){ //TODO verify
			model = QueueingNetworkModel.Q1;
			System.out.println("QN MODEL SET TO Q1");
		}
		
		
		Map<String,String> inputFilesSet = new HashMap<>(); 
		for(File file : lst){
			String name = file.getName();
			Matcher mapMatcher = patternMap.matcher(name);
			Matcher rsMatcher = patternRS.matcher(name);
			String stringToBeReplaced = new String();
			if(mapMatcher.find()){
				stringToBeReplaced = mapMatcher.group(2).toUpperCase();
			}
			else if(rsMatcher.find()){
				stringToBeReplaced = rsMatcher.group(2).toUpperCase();
			}
			else{
				logger.error("Replayer file name does not match the required regex");
			}
			System.out.println("Pattern to replace in jsimg: "+ stringToBeReplaced);//TODO delete
			inputFilesSet.put(stringToBeReplaced, connSettings.getRemoteWorkDir()+File.separator+dataProcessor.getCurrentInputsSubFolderName()+File.separator+file.getName()); //TODO + subfolder creation on Simulator
		}
		
		Map<String,String> numMR = new HashMap<>();
		
		for(Entry<String, Double> entry : solPerJob.getProfile().getProfileMap().entrySet()){
			Matcher m = patternNMR.matcher(entry.getKey());
			if(m.matches()){
				numMR.put(entry.getKey().toUpperCase(),String.valueOf(entry.getValue().intValue()));
			}
		}
		
		String jsimgfileContent = new QNFileBuilder()
				.setQueueingNetworkModel(model)
				.setCores(nContainers).setConcurrency(concurrency)
				.setReplayersInputFiles(inputFilesSet)
				.setNumMR(numMR)
				.setThinkRate(1 / think).setAccuracy(connSettings.getAccuracy() / 100)
				.setSignificance(((QNSettings) connSettings).getSignificance()).build();

		File jsimgTempFile;
		if (iteration.isPresent()) jsimgTempFile = fileUtility.provideTemporaryFile(String
				.format("QN-%s-class%s%s%s-it%d-", solPerJob.getParentID(), jobID,dataProcessor.getProviderName(),solPerJob.getTypeVMselected().getId(), iteration.get()), ".jsimg");
		else jsimgTempFile = fileUtility.provideTemporaryFile(String.format("QN-%s-class%s%s%s-",
				solPerJob.getParentID(), jobID,dataProcessor.getProviderName(),solPerJob.getTypeVMselected().getId()), ".jsimg");

		fileUtility.writeContentToFile(jsimgfileContent, jsimgTempFile);
		lst.add(jsimgTempFile);
		return lst;
	}

	public List<String> pwd() throws Exception {
		return connector.pwd(getClass());
	}

}
