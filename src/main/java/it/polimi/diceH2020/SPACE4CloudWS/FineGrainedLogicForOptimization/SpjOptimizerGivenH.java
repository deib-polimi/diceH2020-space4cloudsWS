package it.polimi.diceH2020.SPACE4CloudWS.FineGrainedLogicForOptimization;

import java.util.ArrayList;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import java.util.function.Function;

import javax.validation.constraints.Min;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import it.polimi.diceH2020.SPACE4Cloud.shared.solution.SolutionPerJob;
import it.polimi.diceH2020.SPACE4CloudWS.core.OptimizerFineGrained;

@Component
@Scope("prototype")
public class SpjOptimizerGivenH {
	private final Logger logger = Logger.getLogger(SpjOptimizerGivenH.class.getName());

	@Autowired
	private WrapperDispatcher dispatcher;
	
	@Autowired
	private OptimizerFineGrained optimizer;
	
	private SolutionPerJob initialSpjWithGivenH;
	
	private ArrayList<Function<Integer,Integer>> batchFunctionsList = new ArrayList<>();
	
	private TreeMap<Integer,SolutionPerJob> nVMxSPJ; 
	private ArrayList<Integer> sentNVM;
	
	@Min(1)
	private int minVM;
	@Min(1)
	private int maxVM;
	
	private int batchStep = 1; //optimality works only with step = 1 till now
	private boolean finished = false;

	public SpjOptimizerGivenH(SolutionPerJob spj, int minVM, int maxVM){
		nVMxSPJ = new TreeMap<Integer,SolutionPerJob>(); //from each spj I essentially need (nVM, feasibility) and (nVM, duration) 
		this.initialSpjWithGivenH = spj;
		this.minVM = minVM;
		this.maxVM = maxVM;
		
		/*
		 * To send |n| parallel executions of the same SolutionPerJob with a fixed H but variable number of VM, just add |n| function to batchFunctionList
		 * the initial N (for the first iteration) is obtained by the Initialization Phase, or can be obtained by a ML SVR model.
		 */
		batchFunctionsList.add(n -> n); 
		//Neighborhood: 
		batchFunctionsList.add(n -> n + batchStep);  
		batchFunctionsList.add(n -> n - batchStep);
		
		sentNVM = new ArrayList<Integer>();
	}
	
	public void start(){
		int initialNVM = getInitialNVM();
		batchFunctionsList.stream().forEach(fun->{
			int nVMtmp = fun.apply(initialNVM);
			if(1<=nVMtmp && nVMtmp<=maxVM){
				SolutionPerJob nextJob = optimizer.cloneSpj(initialSpjWithGivenH);
				
				nextJob.setNumberVM(nVMtmp);
				sendJob(nextJob);
			}
		});		
	}
	
	private synchronized void sendJob(SolutionPerJob job){
		logger.info("J"+initialSpjWithGivenH.getJob().getId()+"."+initialSpjWithGivenH.getNumberUsers()+" enqueued job with NVM:"+job.getNumberVM());
		
		SpjWrapperGivenHandN spjWrapper =  new SpjWrapperGivenHandN(job,this);
		
		dispatcher.enqueueJob(spjWrapper);
		sentNVM.add(job.getNumberVM());
	}
	
	public synchronized void registerCorrectSolutionPerJob(SolutionPerJob spj){
		nVMxSPJ.put(spj.getNumberVM(), spj); 
		printStatus();

		if (finished) return;
		
		if(!verifyFinalAssumption()){ //true se non posso piu ottenere migliori soluzioni(feasible or infeasible)
				if(acceptableDurationDecrease()) //duration is decreasing enough
					try{ if(!sendNextEvent() && sentNVM.size()==nVMxSPJ.size()){ //encode Next spj
						logger.info("class" + initialSpjWithGivenH.getJob().getId() +" with H:"+initialSpjWithGivenH.getNumberUsers()+"-> MakeFeasible ended with ERROR - VM limits exceeded.");
							finished(-1); //not enough VM
						 }
					}catch(Exception e){logger.error("Exception sending a new spj "+e);}
				else{
					logger.info("class" + initialSpjWithGivenH.getJob().getId() +" with H:"+initialSpjWithGivenH.getNumberUsers()+"-> MakeFeasible ended with ERROR - its durations aren't decreasing enough");
					finished(-2);
				}
		}
		else{
			int optimalNVM = getOptimalVMNum();
			if(optimalNVM != -1){ //optimal nVM founded
				logger.info("J"+initialSpjWithGivenH.getJob().getId()+"."+initialSpjWithGivenH.getNumberUsers()+" optimal VM number is"+ optimalNVM );
			}
			finished(optimalNVM);
		}
	}
	
	public synchronized void registerFailedSolutionPerJob(SolutionPerJob spj){
		nVMxSPJ.put(spj.getNumberVM(), spj); 
		printStatus();
		if (finished) return;
		logger.info("class" + initialSpjWithGivenH.getJob().getId() +"."+	initialSpjWithGivenH.getNumberUsers()+"-> MakeFeasible ended with ERROR - duration not received");
		finished(-3); 
	}
	
	private void printStatus(){
		String printText = new String();
		printText += "\nJ"+initialSpjWithGivenH.getJob().getId()+"."+initialSpjWithGivenH.getNumberUsers()+"\n";
		printText += "nVMmin"+minVM+" nVMmax:"+maxVM+" initial NVM:"+initialSpjWithGivenH.getNumberVM()+"\n";
		printText += "sent nVM: "+sentNVM.stream().map(e->e.toString()).reduce((t,u)->t+","+u).get()+"\n";
		printText += "finished: "+finished+"\n";
		printText += "deadline: "+initialSpjWithGivenH.getJob().getD()+"\n";
		printText += "nVM feas duration\n";
		
		for(int i = nVMxSPJ.firstKey(); i <= nVMxSPJ.lastKey(); i++){
			printText +=  i;
			if( ! nVMxSPJ.containsKey(i) ){
				printText +=  "   -  -  -\n";
				continue;
			}else{
				double currentDuration = nVMxSPJ.get(i).getDuration();
				if(nVMxSPJ.get(i).getFeasible()) printText +=  " F ";
				else printText +=  " I ";
				printText +=  currentDuration+"\n";
			}
		}
		System.out.println(printText);
	}
	
	private void finished(int nVM){
		finished = true;
		
		if(nVM>0){
			optimizer.registerSPJGivenHOptimalNVM(nVMxSPJ.get(nVM));
		}else{
			initialSpjWithGivenH.setNumberVM(nVM);
			optimizer.registerSPJGivenHOptimalNVM(initialSpjWithGivenH);
		}
		
		dispatcher.dequeue(this);
	}
	
	/**
	 * FEASIBLE SOLUTIONPERJOB ZONE	  |  UNFEASIBLE SOLUTIONPERJOB ZONE
	 * 				   				  |
	 *				   	•N+3<---•N+2<-|-- •N+1<---• N
	 *				   				  |
	 *				   				  |
	 *				   --------------------------------->duration
	 *				   				  |
	 *				   				  |
	 *				   			   deadline
	 *
	 * sentNlist = [unfeasible,unfeasible,feasible,feasible]
	 *                                        ^
	 *                                        |
	 *                                        |
	 *                                   this is the optimal solution if step between N is 1
	 * 
	 * N+2 is the optimal solution.
	 * Just by knowing at least one solution's feasibility 
	 * I can decide  if retrieve the adjacent right or left N from the list of sent N.
	 * 
	 */
	private boolean sendNextEvent(){
		int nextN = getNextN();
		
		if(nextN == -1) return false; //nVM exceeded limits 
		
		SolutionPerJob nextJob =  optimizer.cloneSpj(initialSpjWithGivenH); 
		nextJob.setNumberVM(nextN);
		sendJob(nextJob);
		
		return true;
	}
	
	/**
	 * Precondition:
	 * nVMxSPJ !verifyFinalAssumption()
	 * so or FFFFF or IIIII not both so it cannot contain solution
	 * and cannot be empty (called after adding a spj to it)
	 * 
	 * 
	 * @return -1 if nVMxSPJ is empty (precondition violation) or contains solution (precondition violation)
	 * 			  or nVM exceeds limits
	 */
	private synchronized int getNextN(){
		boolean left = false;
		boolean right = false;
		int nextN = -1;
		
		for (Map.Entry<Integer, SolutionPerJob> entry : nVMxSPJ.entrySet())
		{
			if(entry.getValue().getFeasible()) left = true;
			else right = true;
		}
		
		if(left&&!right || !left&&right){ 
			if(left) nextN = getLeftN();
			if(right) nextN = getRightN();
		}else{
			logger.info("Error with precondition!");
		}
		
		return nextN;
	}
	
	private int getRightN(){
		Optional<Integer> maxN = sentNVM.stream().max(Integer::compareTo);
		if(maxN.isPresent())
			if( maxN.get() + batchStep <= maxVM)
				return maxN.get() + batchStep;
		return -1;
	}
	
	private int getLeftN(){
		Optional<Integer> minN = sentNVM.stream().min(Integer::compareTo);
		if(minN.isPresent())
			if( minN.get() - batchStep >= minVM)
				return minN.get() - batchStep;
		return -1;
	}
	
	/**
	 * 
	 *  @return -1: optimal spj isn't already present (but has already been sent) <br>
	 * 				otherwise return the number of VM that optimize the initial spj
	 */
	private int getOptimalVMNum(){
		
		if(!verifyFinalAssumption()){ logger.info("Error2 with precondition!");return -1;} //redundant always true if here
		
		int prevFeasibility = -1;
		
		if(nVMxSPJ.firstEntry().getValue().getFeasible() && nVMxSPJ.firstEntry().getValue().getNumberVM() == minVM) return minVM; 
		
		//Check for alternate feasibility: I->F
		for(int i = nVMxSPJ.firstKey(); i <= nVMxSPJ.lastKey(); i++){
			
			if( ! nVMxSPJ.containsKey(i) ){
				prevFeasibility = -1;
				continue;
			}else{
				//adjacent spj check
				int currFeasibility;
				if(nVMxSPJ.get(i).getFeasible()) currFeasibility = 1;
				else currFeasibility = 0;
				
				if(prevFeasibility == 0 && currFeasibility == 1 ) return nVMxSPJ.get(i).getNumberVM();
				prevFeasibility = currFeasibility;
			}
		}
		logger.info("class" + initialSpjWithGivenH.getJob().getId() +" with H:"+initialSpjWithGivenH.getNumberUsers()+"-> MakeFeasible ended with ERROR - VM limits exceeded.");
		return -1; //maxVM isn't big enough.
	}
	
	private boolean acceptableDurationDecrease(){
		double prevDuration = 0;
		for(int i = nVMxSPJ.firstKey(); i <= nVMxSPJ.lastKey(); i++){
			if( ! nVMxSPJ.containsKey(i) ){
				continue;
			}else{
				double currentDuration = nVMxSPJ.get(i).getDuration();
				if(!nVMxSPJ.get(i).getFeasible()){
					if(Math.abs(currentDuration - prevDuration) < 80 ){ //abs not required
						prevDuration = currentDuration;  
						return false;
					}
					prevDuration = currentDuration; 
				}
			}
		}
		return true;
	}
	
	/**
	 *   
	 * 
	 *  assumption:         for every spj with the same instance data and the same H:
	 *          				if spj1 is infeasible cannot exist an spj2 feasible with less nVM than spj1  
	 *          				if spj1 is feasible cannot exist an spj2 infeasible with more nVM than spj1  
	 *          								So... (with given H and instancedata)
	 *          			incrementing nVM from a feasible spj cannot produce an infeasible one, 
	 *          			vice versa decrementing nVM from an infeasible spj cannot produce a feasible one
	 * 
	 * @return true if enough spjs have already been sent, so it's possible to retrieve the optimal result now or
	 * 			when already sent spjs arrive
	 */
	private boolean verifyFinalAssumption(){
		if(!nVMxSPJ.firstEntry().getValue().getFeasible() && nVMxSPJ.lastEntry().getValue().getFeasible() ) return true;
		if(nVMxSPJ.firstEntry().getValue().getFeasible() && nVMxSPJ.firstEntry().getValue().getNumberVM()==minVM) return true;
		if(!nVMxSPJ.lastEntry().getValue().getFeasible() && nVMxSPJ.lastEntry().getValue().getNumberVM()==maxVM) return true;
		
		return false;
	}
	

	public boolean isFinished() {
		return finished;
	}
	
	private int getInitialNVM(){
		return initialSpjWithGivenH.getNumberVM();
	}
}
