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
package it.polimi.diceH2020.SPACE4CloudWS.core;

import it.polimi.diceH2020.SPACE4Cloud.shared.solution.Matrix;
import it.polimi.diceH2020.SPACE4Cloud.shared.solution.Phase;
import it.polimi.diceH2020.SPACE4Cloud.shared.solution.PhaseID;
import it.polimi.diceH2020.SPACE4Cloud.shared.solution.SolutionPerJob;
import it.polimi.diceH2020.SPACE4CloudWS.engines.EngineProxy;
import it.polimi.diceH2020.SPACE4CloudWS.fineGrainedLogicForOptimization.SpjOptimizerGivenH;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

@Component
public class OptimizerFineGrained extends Optimizer{

	private static Logger logger = Logger.getLogger(OptimizerCourseGrained.class.getName());

	@Autowired
	private ApplicationContext context;

	@Autowired
	private EngineProxy engineProxy;

	boolean finished = false;

	private Matrix matrix;

	String matrixNVMString;

	int registeredSolutionsPerJob;
	
	private long executionTime;
	

	public void hillClimbing(Matrix matrix) {
		this.matrix = matrix;
		this.registeredSolutionsPerJob = 0;
		logger.info(String.format("---------- Starting fine grained hill climbing for instance %s ----------", engineProxy.getEngine().getSolution().getId()));
		start();
	}

	private void start(){
		executionTime = 0L;
		for(SolutionPerJob spj: matrix.getAllSolutions() ){
			SpjOptimizerGivenH spjOptimizer =  (SpjOptimizerGivenH) context.getBean("spjOptimizerGivenH",spj,1,dataService.getGamma());
			spjOptimizer.start();
		}
	}

	private void aggregateAndFinish(){
		
		for(SolutionPerJob spj : matrix.getAllSolutions()){
			evaluator.evaluate(spj);
		}
		Phase ph = new Phase();
		ph.setId(PhaseID.OPTIMIZATION);
		ph.setDuration(executionTime);
		engineProxy.getEngine().getSolution().addPhase(ph);
		engineProxy.getEngine().reduceMatrix(); //TODO modify automata in order to avoid this backward call
	}

	public void finish(){
		engineProxy.getEngine().getSolution().setEvaluated(false);
		evaluator.evaluate(engineProxy.getEngine().getSolution());
	}

	public synchronized void registerSPJGivenHOptimalNVM(SolutionPerJob spj,long executionTime){
		finished = true;
		this.executionTime += executionTime;
		//optimalNVMGivenH[spj.getJob().getId()-1][h-1] = nVM;
		matrix.get(spj.getJob().getId())[spj.getNumberUsers()-matrix.getHlow(spj.getJob().getId())] = spj;

		registeredSolutionsPerJob++;
		if(registeredSolutionsPerJob != matrix.getNumCells() ) finished = false;

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
		newSpj.setXi(oldSpj.getXi());
		return newSpj;
	}
}
