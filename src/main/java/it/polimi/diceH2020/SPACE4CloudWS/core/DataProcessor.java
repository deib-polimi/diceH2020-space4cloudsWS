package it.polimi.diceH2020.SPACE4CloudWS.core;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.statemachine.StateMachine;
import org.springframework.stereotype.Component;

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
	
	protected long calculateDuration(@NonNull Solution sol) {
		return calculateDuration(sol.getLstSolutions());
	}

	protected void calculateDuration(@NonNull Matrix matrix){
		 calculateDuration2(matrix.getAllSolutions());
	}
	
	private void calculateDuration2(@NonNull List<SolutionPerJob> spjList){ //TODO collapse also calculateDuration in this method, by implementing fineGrained Matrix also in the public case
		spjList.forEach(s -> {
				ContainerLogicForEvaluation container =  (ContainerLogicForEvaluation) context.getBean("containerLogicForEvaluation",s);
				container.start();
		});
	}
	
	private long calculateDuration(@NonNull List<SolutionPerJob> spjList){
		AtomicLong executionTime = new AtomicLong(); //to support also parallel stream.
		
		spjList.forEach(s -> {
			Pair<Optional<BigDecimal>,Double> duration = calculateDuration(s);
			executionTime.addAndGet(duration.getRight().longValue());
			
			if (duration.getLeft().isPresent()) s.setDuration(duration.getLeft().get().doubleValue());
			else {
				s.setDuration(Double.MAX_VALUE);
				s.setError(Boolean.TRUE);
			}
		});
		
		return executionTime.get();
	}

	protected Pair<Optional<BigDecimal>,Double> calculateDuration(@NonNull SolutionPerJob solPerJob) {
		Pair<Optional<BigDecimal>,Double> result = solverCache.evaluate(solPerJob);
		if (! result.getLeft().isPresent()) solverCache.invalidate(solPerJob);
		else logger.info(solPerJob.getId()+"->"+" Duration with "+solPerJob.getNumberVM()+"VM and h="+solPerJob.getNumberUsers()+"has been calculated" +result.getLeft().get());
		return result;
	}
	
	public void restoreDefaults() {
		solverCache.restoreDefaults();
	}

	public void changeSettings(Settings settings) {
		solverCache.changeSettings(settings);
	}

	public String getCurrentInputsSubFolderPath(){
		String currentInputSubFolder = dataService.getSimFoldersName();
		File f = new File(currentInputSubFolder);
		if(!f.exists()){
			logger.error("Error with Current Input Folder! It doesn't exist!");
			stateHandler.sendEvent(Events.STOP);
		}
		return currentInputSubFolder;
	}
	
	public String getCurrentInputsSubFolderName(){
		return Paths.get(getCurrentInputsSubFolderPath()).getFileName().toString();
	}
	
	public List<File> getCurrentReplayersInputFiles() throws IOException{
		List<File> txtList = new ArrayList<>();
		for(String fileName: FileUtility.listFile(getCurrentInputsSubFolderPath(),  ".txt")){
			File file = new File(getCurrentInputsSubFolderPath()+File.separator+fileName);
			txtList.add(file);
		}
		return txtList;
	}
	
	public String getProviderName(){
		return dataService.getProviderName();
	}
	
	public List<File> getCurrentReplayersInputFiles(String solutionID, String spjID, String provider, String typeVM) throws IOException{
		return getCurrentReplayersInputFiles().stream().filter(s->s.getName().contains(spjID+provider+typeVM)).filter(s->s.getName().contains(solutionID)).collect(Collectors.toList());
	}
	
}
