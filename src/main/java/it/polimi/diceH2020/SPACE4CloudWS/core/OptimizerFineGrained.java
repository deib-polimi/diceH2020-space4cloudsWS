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

import it.polimi.diceH2020.SPACE4Cloud.shared.inputDataMultiProvider.PrivateCloudParameters;
import it.polimi.diceH2020.SPACE4Cloud.shared.solution.Matrix;
import it.polimi.diceH2020.SPACE4Cloud.shared.solution.Phase;
import it.polimi.diceH2020.SPACE4Cloud.shared.solution.PhaseID;
import it.polimi.diceH2020.SPACE4Cloud.shared.solution.SolutionPerJob;
import it.polimi.diceH2020.SPACE4CloudWS.engines.EngineProxy;
import it.polimi.diceH2020.SPACE4CloudWS.fineGrainedLogicForOptimization.ContainerLogicForOptimization;

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

	private int registeredSolutionsPerJob;
	
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
			PrivateCloudParameters p = dataService.getData().getPrivateCloudParameters();
			double m_tilde = dataService.getMemory(spj.getTypeVMselected().getId());
			double v_tilde = dataService.getNumCores(spj.getTypeVMselected().getId());
			int maxNumVM = (int)(Math.floor(Math.min(Math.ceil(p.getM()/m_tilde), Math.ceil(p.getV()/v_tilde)))*p.getN());
			System.out.println("MAX NUMVM:"+maxNumVM);
			ContainerLogicForOptimization spjOptimizer =  (ContainerLogicForOptimization) context.getBean("containerLogicForOptimization",spj,1,maxNumVM);
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
		matrix.get(spj.getId())[spj.getNumberUsers()-matrix.getHlow(spj.getId())] = spj;

		registeredSolutionsPerJob++;
		if(registeredSolutionsPerJob != matrix.getNumCells() ) finished = false;

		if(finished){
			System.out.println(matrix.asString());
			aggregateAndFinish();
		}
	}
	
}
