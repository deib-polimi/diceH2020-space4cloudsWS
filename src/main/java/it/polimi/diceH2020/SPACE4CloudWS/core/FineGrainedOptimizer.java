package it.polimi.diceH2020.SPACE4CloudWS.core;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import it.polimi.diceH2020.SPACE4Cloud.shared.settings.Settings;
import it.polimi.diceH2020.SPACE4Cloud.shared.solution.IEvaluator;
import it.polimi.diceH2020.SPACE4Cloud.shared.solution.Phase;
import it.polimi.diceH2020.SPACE4Cloud.shared.solution.PhaseID;
import it.polimi.diceH2020.SPACE4Cloud.shared.solution.Solution;
import it.polimi.diceH2020.SPACE4Cloud.shared.solution.SolutionPerJob;
import it.polimi.diceH2020.SPACE4CloudWS.FineGrainedLogicForOptimization.SpjOptimizerGivenH;
import it.polimi.diceH2020.SPACE4CloudWS.services.DataService;
import it.polimi.diceH2020.SPACE4CloudWS.services.SolverProxy;


@Component
public class FineGrainedOptimizer {
	
	private static Logger logger = Logger.getLogger(Optimizer.class.getName());

	@Autowired
	private DataService dataService;
	
	@Autowired
	private ApplicationContext context;
	
	@Autowired
	private SolverProxy solverCache;

	@Autowired
	private IEvaluator evaluator;
	
	private Solution solution;
	
	private Instant first;
	
	boolean finished = false;
	
	String matrixNVMString;
	
	//private int[][] optimalNVMGivenH; 
	
	private Map<String, SolutionPerJob[]> matrix;

	// read an input file and type value of accuracy and cycles
	public void changeDefaultSettings(Settings settings) { //TODO
		solverCache.changeSettings(settings);
	}
	
	public void hillClimbing(Solution solution) {
		first = Instant.now();
		logger.info(String.format("---------- Starting fine grained hill climbing for instance %s ----------", solution.getId()));
		
		List<SolutionPerJob> spjGivenHList = createMatrixCells(solution);
		start(spjGivenHList);
	}		
	
	private void start(List<SolutionPerJob> spjGivenHList){
		for(SolutionPerJob spj: spjGivenHList ){
			SpjOptimizerGivenH spjOptimizer =  (SpjOptimizerGivenH) context.getBean("spjOptimizerGivenH",spj,1,dataService.getGamma());
			spjOptimizer.start();
		}
	}
	
	
	//TODO single spj already evaluated... solution aggregation?
	public void finish(){
		solution.setEvaluated(false); 
		evaluator.evaluate(solution); 
		Instant after = Instant.now();
		Phase ph = new Phase();
		ph.setId(PhaseID.OPTIMIZATION);
		ph.setDuration(Duration.between(first, after).toMillis());
		solution.addPhase(ph);
	}
	
	private List<SolutionPerJob> createMatrixCells(Solution solution){
		List<SolutionPerJob> spjGivenHList = new ArrayList<SolutionPerJob>();
		matrix = new HashMap<String,SolutionPerJob[]>();
		
		solution.getLstSolutions().stream().forEach(spj->{
			int Hup = spj.getJob().getHup();
			int Hlow = spj.getJob().getHlow();
			SolutionPerJob[] matrixLine = new SolutionPerJob[Hup-Hlow+1];
			for(int i= Hup; i>= Hlow ; i--){
				matrixLine[i-Hlow] = null;
				
				SolutionPerJob spjGivenH = cloneSpj(spj);
				spjGivenH.setNumberUsers(i);
				spjGivenHList.add(spjGivenH);
			}
			matrix.put(spj.getJob().getId(), matrixLine);
		});
		return spjGivenHList;
	}
	
	public synchronized void registerSPJGivenHOptimalNVM(SolutionPerJob spj){
		finished = true;
		//optimalNVMGivenH[spj.getJob().getId()-1][h-1] = nVM;
		
		matrix.get(spj.getJob().getId())[spj.getNumberUsers()-spj.getJob().getHlow()] = spj;
		
		matrix.forEach((k,v)->{
			for(SolutionPerJob cell: v){
				if(cell == null) finished = false;
			}
		});
		
		if(finished){
			printMatrix();
		}
	}
	
	private void printMatrix(){
		matrixNVMString = new String();
		matrix.forEach((k,v)->{
			matrixNVMString += " "+v[0].getJob().getId()+" | ";
			for(SolutionPerJob cell: v){
				matrixNVMString += " H:"+cell.getNumberUsers()+",nVM:"+cell.getNumberVM();
				if(cell.getFeasible()) matrixNVMString += ",F\t|";
				else matrixNVMString += ",I\t|";
			}
			matrixNVMString += "\n   | ";
			for(SolutionPerJob cell: v){
				matrixNVMString += " dur: "+cell.getDuration().intValue()+"\t|";
			}
			matrixNVMString += "\n";
		});
		//adding title
		matrixNVMString = "Optimality Matrix(for solution"+matrix.entrySet().iterator().next().getValue()[0].getParentID()+" ):\n" + matrixNVMString;
		System.out.println(matrixNVMString);
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
