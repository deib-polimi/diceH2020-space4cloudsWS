package it.polimi.diceH2020.SPACE4CloudWS.core;

import java.time.Duration;
import java.time.Instant;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import it.polimi.diceH2020.SPACE4Cloud.shared.settings.Settings;
import it.polimi.diceH2020.SPACE4Cloud.shared.solution.Phase;
import it.polimi.diceH2020.SPACE4Cloud.shared.solution.PhaseID;
import it.polimi.diceH2020.SPACE4Cloud.shared.solution.Solution;
import it.polimi.diceH2020.SPACE4Cloud.shared.solution.SolutionPerJob;
import it.polimi.diceH2020.SPACE4CloudWS.FineGrainedLogicForOptimization.SpjOptimizerGivenH;
import it.polimi.diceH2020.SPACE4CloudWS.services.EngineService;
import it.polimi.diceH2020.SPACE4CloudWS.services.SolverProxy;


@Component
public class OptimizerFineGrained extends Optimizer{
	
	private static Logger logger = Logger.getLogger(OptimizerCourseGrained.class.getName());

	@Autowired
	private ApplicationContext context;
	
	@Autowired
	private SolverProxy solverCache;
	
	@Autowired
	private EngineService engineService;

	private Solution solution;
	
	private Instant first;
	
	boolean finished = false;
	
	private Matrix matrix;
	
	String matrixNVMString;
	
	// read an input file and type value of accuracy and cycles
	public void changeDefaultSettings(Settings settings) { //TODO
		solverCache.changeSettings(settings);
	}
	
	public void hillClimbing(Matrix matrix,Solution solution) {
		this.solution = solution;
		first = Instant.now();
		logger.info(String.format("---------- Starting fine grained hill climbing for instance %s ----------", solution.getId()));
		this.matrix = matrix;
		start();
	}		
	
	private void start(){
		for(SolutionPerJob spj: matrix.getAllSolutions() ){
			SpjOptimizerGivenH spjOptimizer =  (SpjOptimizerGivenH) context.getBean("spjOptimizerGivenH",spj,1,dataService.getGamma());
			spjOptimizer.start();
		}
	}
	
	private void aggregateAndFinish(){
		
		for(SolutionPerJob spj : matrix.getAllSolutions()){
			evaluator.evaluate(spj);
		}
		engineService.knapsack(); //TODO modify automata in order to avoid this backward call
		finish();
	}
	
	private void finish(){
		solution.setEvaluated(false); 
		evaluator.evaluate(solution); 
		Instant after = Instant.now();
		Phase ph = new Phase();
		ph.setId(PhaseID.OPTIMIZATION);
		ph.setDuration(Duration.between(first, after).toMillis());
		solution.addPhase(ph);
	}
	
	public synchronized void registerSPJGivenHOptimalNVM(SolutionPerJob spj){
		finished = true;
		//optimalNVMGivenH[spj.getJob().getId()-1][h-1] = nVM;
		
		matrix.get(spj.getJob().getId())[spj.getNumberUsers()-spj.getJob().getHlow()] = spj;
		
		for(SolutionPerJob cell: matrix.getAllSolutions()){
			if(cell == null) finished = false;
		}
		
		if(finished){
			System.out.println(matrix.asString());
			aggregateAndFinish();
		}
	}
	
	public SolutionPerJob cloneSpj(SolutionPerJob oldSpj){
		SolutionPerJob newSpj = new SolutionPerJob();
		newSpj.setAlfa(oldSpj.getAlfa());
		newSpj.setBeta(oldSpj.getBeta());
		newSpj.setChanged(oldSpj.getChanged());
		newSpj.setCost(oldSpj.getCost());
		newSpj.setDeltaBar(oldSpj.getDeltaBar());
		newSpj.setDuration(oldSpj.getDuration());
		newSpj.setError(oldSpj.getError());
		newSpj.setFeasible(oldSpj.getFeasible());
		newSpj.setJob(oldSpj.getJob());
		newSpj.setNumberContainers(oldSpj.getNumberContainers());
		newSpj.setNumberUsers(oldSpj.getNumberUsers());
		newSpj.setNumberVM(oldSpj.getNumberVM());
		newSpj.setNumCores(oldSpj.getNumCores());
		newSpj.setNumOnDemandVM(oldSpj.getNumOnDemandVM());
		newSpj.setNumReservedVM(oldSpj.getNumReservedVM());
		newSpj.setNumSpotVM(oldSpj.getNumSpotVM());
		newSpj.setParentID(oldSpj.getParentID());
		newSpj.setPos(oldSpj.getPos());
		newSpj.setProfile(oldSpj.getProfile());
		newSpj.setRhoBar(oldSpj.getRhoBar());
		newSpj.setSigmaBar(oldSpj.getSigmaBar());
		newSpj.setTypeVMselected(oldSpj.getTypeVMselected());
		return newSpj;
	}
}
