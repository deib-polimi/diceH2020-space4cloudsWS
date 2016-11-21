/*
Copyright 2016 Jacopo Rigoli
Copyright 2016 Eugenio Gianniti

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
package it.polimi.diceH2020.SPACE4CloudWS.core;

import it.polimi.diceH2020.SPACE4Cloud.shared.settings.Settings;
import it.polimi.diceH2020.SPACE4Cloud.shared.solution.Matrix;
import it.polimi.diceH2020.SPACE4Cloud.shared.solution.Solution;
import it.polimi.diceH2020.SPACE4Cloud.shared.solution.SolutionPerJob;
import it.polimi.diceH2020.SPACE4CloudWS.fileManagement.FileUtility;
import it.polimi.diceH2020.SPACE4CloudWS.fineGrainedLogicForOptimization.ContainerLogicForEvaluation;
import it.polimi.diceH2020.SPACE4CloudWS.services.DataService;
import it.polimi.diceH2020.SPACE4CloudWS.services.SolverProxy;
import it.polimi.diceH2020.SPACE4CloudWS.stateMachine.Events;
import it.polimi.diceH2020.SPACE4CloudWS.stateMachine.States;
import lombok.NonNull;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.statemachine.StateMachine;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

/**
 * This component interfaces with Solvers.
 */
@Component
public class DataProcessor {

	private final Logger logger = Logger.getLogger(getClass());

	@Autowired
	protected SolverProxy solverCache;

	@Autowired
	private ApplicationContext context;

	@Autowired
	private DataService dataService;

	@Autowired
	private StateMachine<States, Events> stateHandler;

	long calculateDuration(@NonNull Solution sol) {
		return calculateDuration(sol.getLstSolutions());
	}

	void calculateDuration(@NonNull Matrix matrix) {
		calculateDuration2(matrix.getAllSolutions());
	}

	//TODO collapse also calculateDuration in this method, by implementing fineGrained Matrix also in the public case
	private void calculateDuration2(@NonNull List<SolutionPerJob> spjList) {
		spjList.forEach(s -> {
			ContainerLogicForEvaluation container =  (ContainerLogicForEvaluation) context.getBean("containerLogicForEvaluation",s);
			container.start();
		});
	}

	private long calculateDuration(@NonNull List<SolutionPerJob> spjList) {
		AtomicLong executionTime = new AtomicLong(); //to support also parallel stream.

		spjList.forEach(s -> {
			Pair<Optional<Double>, Double> duration = calculateDuration(s);
			executionTime.addAndGet(duration.getRight().longValue());

			if (duration.getLeft().isPresent()) s.setDuration(duration.getLeft().get());
			else {
				s.setDuration(Double.MAX_VALUE);
				s.setError(Boolean.TRUE);
			}
		});

		return executionTime.get();
	}

	Pair<Optional<Double>, Double> calculateDuration(@NonNull SolutionPerJob solPerJob) {
		Pair<Optional<Double>, Double> result = solverCache.evaluate(solPerJob);
		if (! result.getLeft().isPresent()) solverCache.invalidate(solPerJob);
		else logger.info(solPerJob.getId()+"->"+" Duration with "+solPerJob.getNumberVM()+"VM and h="+solPerJob.getNumberUsers()+"has been calculated" +result.getLeft().get());
		return result;
	}

	public void restoreDefaults() {
		solverCache.restoreDefaults();
	}

	void changeSettings(Settings settings) {
		solverCache.changeSettings(settings);
	}

	public String getCurrentInputsSubFolderPath() {
		String currentInputSubFolder = dataService.getSimFoldersName();
		File f = new File(currentInputSubFolder);
		if(!f.exists()){
			logger.error("Error with Current Input Folder! It doesn't exist!");
			stateHandler.sendEvent(Events.STOP);
		}
		return currentInputSubFolder;
	}

	public String getCurrentInputsSubFolderName() {
		return Paths.get(getCurrentInputsSubFolderPath()).getFileName().toString();
	}

	private List<File> getCurrentReplayerInputFiles() throws IOException {
		List<File> txtList = new ArrayList<>();
		for(String fileName: FileUtility.listFile(getCurrentInputsSubFolderPath(),  ".txt")){
			File file = new File(getCurrentInputsSubFolderPath()+File.separator+fileName);
			txtList.add(file);
		}
		return txtList;
	}

	public String getProviderName() {
		return dataService.getProviderName();
	}

	public List<File> getCurrentReplayerInputFiles(String solutionID, String spjID, String provider, String typeVM)
			throws IOException {
		return getCurrentReplayerInputFiles().stream().filter(s->s.getName().contains(spjID+provider+typeVM))
				.filter(s->s.getName().contains(solutionID)).collect(Collectors.toList());
	}

}
