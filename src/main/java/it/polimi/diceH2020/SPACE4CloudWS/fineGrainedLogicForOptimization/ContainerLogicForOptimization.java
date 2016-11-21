/*
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
package it.polimi.diceH2020.SPACE4CloudWS.fineGrainedLogicForOptimization;

import it.polimi.diceH2020.SPACE4Cloud.shared.solution.SolutionPerJob;
import it.polimi.diceH2020.SPACE4CloudWS.core.FineGrainedOptimizer;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import javax.validation.constraints.Min;
import java.util.Comparator;
import java.util.Optional;
import java.util.TreeMap;

@Component
@Scope("prototype")
public class ContainerLogicForOptimization implements ContainerLogicGivenH {
	private final Logger logger = Logger.getLogger(ContainerLogicForOptimization.class.getName());

	@Autowired
	private WrapperDispatcher dispatcher;

	@Autowired
	private FineGrainedOptimizer optimizer;

	private SolutionPerJob initialSpjWithGivenH;

	private TreeMap<Integer, SolutionPerJob> nVMxSPJ;
	private long executionTime;

	private int predictedNVM_SVR = 0;
	private int predictedNVM_Hyperbola = 0;

	@Min(1)
	private int minVM;
	@Min(1)
	private int maxVM;

	private boolean finished = false;

	public ContainerLogicForOptimization(SolutionPerJob spj, int minVM, int maxVM){
		nVMxSPJ = new TreeMap<>(); //from each spj I essentially need (nVM, feasibility) and (nVM, duration)
		this.initialSpjWithGivenH = spj;
		this.minVM = minVM;
		this.maxVM = maxVM;
		this.executionTime = 0L;
		/*
		 * To send |n| parallel executions of the same SolutionPerJob with a fixed H but variable number of VM, just add |n| function to batchFunctionList
		 * the initial N (for the first iteration) is obtained by the Initialization Phase, or can be obtained by a ML SVR model.
		 */
	}

	public void start(){
		int initialNVM = getInitialNVM();
		predictedNVM_SVR = initialNVM;
		SolutionPerJob nextJob = initialSpjWithGivenH.clone();
		nextJob.updateNumberVM(initialNVM);
		sendJob(nextJob);
		//check minVM≤initialNVM≤maxVM is not really useful (this spj has already been cached i don't waste time on it), and i can also use a point out of range to study hyperbola
	}

	private synchronized void sendJob(SolutionPerJob job){
		logger.info("J"+initialSpjWithGivenH.getId()+"."+initialSpjWithGivenH.getNumberUsers()+" enqueued job with NVM:"+job.getNumberVM());

		ContainerForOptimization spjWrapper =  new ContainerForOptimization(job,this);

		dispatcher.enqueueJob(spjWrapper);
	}

	public synchronized void registerCorrectSolutionPerJob(SolutionPerJob spj, double executionTime){
		nVMxSPJ.put(spj.getNumberVM(), spj);
		printStatus();
		if (finished) return;

		this.executionTime += executionTime;
		if(!verifyFinalAssumption()){ //true se non posso piu ottenere migliori soluzioni(feasible or infeasible)
			if(acceptableDurationDecrease()) //duration is decreasing enough
				try{
					if(!sendNextEvent()){ //encode Next spj
						logger.info("class" + initialSpjWithGivenH.getId() +" with H:"+initialSpjWithGivenH.getNumberUsers()+"-> MakeFeasible ended with ERROR - VM limits exceeded.");
						finished(-1); //not enough VM
					}
				}catch(Exception e){logger.error("Exception sending a new spj "+e);finished(-1);}
			else{
				logger.info("class" + initialSpjWithGivenH.getId() +" with H:"+initialSpjWithGivenH.getNumberUsers()+"-> MakeFeasible ended with ERROR - its durations aren't decreasing enough");
				finished(-2);
			}
		}
		else{
			int optimalNVM = getOptimalVMNum();
			if(optimalNVM != -1){ //optimal nVM founded
				logger.info("J"+initialSpjWithGivenH.getId()+"."+initialSpjWithGivenH.getNumberUsers()+" optimal VM number is"+ optimalNVM );
			}
			finished(optimalNVM);
		}
	}

	public synchronized void registerFailedSolutionPerJob(SolutionPerJob spj, double executionTime){
		nVMxSPJ.put(spj.getNumberVM(), spj);
		printStatus();
		if (finished) return;
		this.executionTime += executionTime;
		logger.info("class" + initialSpjWithGivenH.getId() +"."+	initialSpjWithGivenH.getNumberUsers()+"-> MakeFeasible ended with ERROR - duration not received");
		finished(-3);
	}

	private void printStatus(){
		String printText = "";
		printText += "\nJ"+initialSpjWithGivenH.getId()+"."+initialSpjWithGivenH.getNumberUsers()+"typeVMselected:"+ initialSpjWithGivenH.getTypeVMselected().getId()+"\n";
		printText += "nVMmin"+minVM+" nVMmax:"+maxVM+" initial NVM:"+initialSpjWithGivenH.getNumberVM()+"\n";
		printText += "Calculated nVM: "+nVMxSPJ.values().stream().map(SolutionPerJob::getNumberVM).map(e->e.toString()).reduce((t,u)->t+","+u).get()+"\n";
		printText += "finished: "+finished+"\n";
		printText += "deadline: "+initialSpjWithGivenH.getJob().getD()+"\n";
		printText += "nVM feas duration\n";

		for(SolutionPerJob nvm : nVMxSPJ.values()){
			printText += nvm.getNumberVM();
			double currentDuration = nvm.getDuration();
			if(nvm.getFeasible()) printText +=  " F ";
			else printText +=  " I ";
			printText +=  currentDuration+"\n";
		}
		System.out.println(printText);
	}

	private void finished(int nVM){
		finished = true;
		System.out.println("[J"+initialSpjWithGivenH.getId()+"."+initialSpjWithGivenH.getNumberUsers()+"]LS finished, sol_VM="+nVM+" SVR_VM="+predictedNVM_SVR+" hyperbola_VM="+predictedNVM_Hyperbola);
		if(nVM>0){
			optimizer.registerSPJGivenHOptimalNVM(nVMxSPJ.get(nVM),executionTime);
		}else{
			initialSpjWithGivenH.updateNumberVM(nVM);
			optimizer.registerSPJGivenHOptimalNVM(initialSpjWithGivenH,executionTime);
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

		SolutionPerJob nextJob =  initialSpjWithGivenH.clone();
		nextJob.updateNumberVM(nextN);
		sendJob(nextJob);

		return true;
	}

	/**
	 * Precondition: I've not a final solution yet, so i'm guaranteed that there are some holes in the ThreeMap 
	 */
	private int updateN(int nVM){
		if(nVMxSPJ.containsKey(nVM)){
			if(!nVMxSPJ.get(nVM).getFeasible()) return updateN(nVM+1);
			else return updateN(nVM-1);
		}else{
			return nVM;
		}
	}

	/**
	 * Preconditions:
	 * <ul>
	 * <li>At least 1 solution in the TreeMap, it can also exceeds limits imposed by minNVM and maxNVM</li>
	 * <li>I've not a final solution yet, so i'm guaranteed that there are some holes in the ThreeMap and updateN() will return one of them</li>
	 * </ul>
	 *	Constraint:
	 *	QN, with negative nVM give me the result of |nVM|, so i cannot use negative nVM.... if SVR gives negative nVM evaluator must evaluate nVM > 0
	 */
	private synchronized int getNextN(){
		int nextN = -1;

		if(nVMxSPJ.size() == 1){
			nextN = updateN(Math.max(1,nVMxSPJ.firstEntry().getKey()));
			nextN = checkNVMAgainstRange(nextN);
		}else{ //size≥2
			nextN = hyperbolicAssestment(nVMxSPJ.firstEntry().getValue(),nVMxSPJ.lastEntry().getValue());
			System.out.println("[J"+initialSpjWithGivenH.getId()+"."+initialSpjWithGivenH.getNumberUsers()+"]Hyperbolic prevision: "+nextN);
			predictedNVM_Hyperbola = nextN;
			nextN = updateN(nextN);
		}

		if(nextN>maxVM || nextN<minVM || nVMxSPJ.containsKey(nextN)){
			System.out.println(" max:"+maxVM+" min:"+minVM+" next:"+nextN+" contain:"+nVMxSPJ.containsKey(nextN));
			logger.info("Error with preconditions!");
			return -1;
		}

		return nextN;
	}

	private int checkNVMAgainstRange(int nVM){
		nVM = Math.max(nVM, minVM);
		nVM = Math.min(nVM, maxVM);
		return nVM;
	}

	private int hyperbolicAssestment(SolutionPerJob spj1, SolutionPerJob spj2){
		int nVM = (int) Math.ceil(getPointCoordinateOnHyperbola(spj1.getNumberVM(), spj1.getDuration(), spj2.getNumberVM(), spj2.getDuration(), initialSpjWithGivenH.getJob().getD())); //ceil, because first i look for the feasible sol
		nVM = checkNVMAgainstRange(nVM);
		System.out.println("NVM with Hyperbola:"+ nVM);
		return nVM;
	}

	/**
	 * From a hyperbola given two points coordinates and a third point y coordinate
	 * retrieves this third point x coordinate.
	 */
	private double getPointCoordinateOnHyperbola(int x1, double y1, int x2, double y2, double y){
		double x = 0.0;
		double a =  x1*x2*(y1-y2)/(double)(x2-x1);
		double b = (x2*y2-x1*y1)/(double)(x2-x1);
		x = a / (y - b);
		System.out.println("Hyperbola: x="+x+"="+"a/(y-n)"+a+"/("+y+"-"+b+")");
		return x;
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
		logger.info("class" + initialSpjWithGivenH.getId() +" with H:"+initialSpjWithGivenH.getNumberUsers()+"-> MakeFeasible ended with ERROR - VM limits exceeded.");
		return -1; //maxVM isn't big enough. (case maxVM is I)
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
		if(nVMxSPJ.firstEntry().getValue().getFeasible() && nVMxSPJ.firstEntry().getValue().getNumberVM()==minVM) return true;
		if(!nVMxSPJ.lastEntry().getValue().getFeasible() && nVMxSPJ.lastEntry().getValue().getNumberVM()==maxVM) return true;
		if(solutionPresent()) return true;

		return false;
	}

	private boolean solutionPresent(){
		if(nVMxSPJ.size()<2) return false;
		Optional<SolutionPerJob> sol = nVMxSPJ.values().stream().filter(s->s.getNumberVM()>=minVM).filter(s->s.getNumberVM()<=maxVM).filter(SolutionPerJob::getFeasible).min(Comparator.comparingInt(SolutionPerJob::getNumberVM));
		if(!sol.isPresent()) return false;

		if(nVMxSPJ.containsKey(sol.get().getNumberVM()-1)){
			if(!nVMxSPJ.get(sol.get().getNumberVM()-1).getFeasible()) return true;
		}
		return false;
	}

	public boolean isFinished() {
		return finished;
	}

	private int getInitialNVM(){
		return initialSpjWithGivenH.getNumberVM();
	}
}
