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

import it.polimi.diceH2020.SPACE4Cloud.shared.solution.IEvaluator;
import it.polimi.diceH2020.SPACE4Cloud.shared.solution.SolutionPerJob;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

@Component
@Scope("prototype")
public class ContainerLogicForEvaluation implements ContainerLogicGivenH{
	private final Logger logger = Logger.getLogger(ContainerLogicForEvaluation.class.getName());

	@Autowired
	private WrapperDispatcher dispatcher;

	private SolutionPerJob initialSpjWithGivenH;
	
	@Autowired
	private IEvaluator evaluator;

	private long executionTime;
	
	public ContainerLogicForEvaluation(SolutionPerJob spj){
		this.initialSpjWithGivenH = spj;
		this.executionTime = 0L;
	}

	public void start(){
		SolutionPerJob nextJob = initialSpjWithGivenH.clone();
		sendJob(nextJob);
	}

	private synchronized void sendJob(SolutionPerJob job){
		logger.info("J"+initialSpjWithGivenH.getId()+"."+initialSpjWithGivenH.getNumberUsers()+" enqueued job with NVM:"+job.getNumberVM());
		ContainerForEvaluation spjWrapper =  new ContainerForEvaluation(job,this);
		dispatcher.enqueueJob(spjWrapper);
	}

	public synchronized void registerCorrectSolutionPerJob(SolutionPerJob spj, double executionTime){
		this.executionTime += executionTime;
		finished(spj.getNumberVM());
	}

	public synchronized void registerFailedSolutionPerJob(SolutionPerJob spj, double executionTime){
		this.executionTime += executionTime;
		logger.info("class" + initialSpjWithGivenH.getId() +"."+	initialSpjWithGivenH.getNumberUsers()+"-> MakeFeasible ended with ERROR - duration not received");
		finished(-1);
	}
	
	private void finished(int nVM){
		initialSpjWithGivenH.updateNumberVM(nVM);
		evaluator.register(initialSpjWithGivenH,executionTime);
		dispatcher.dequeue(this);
		
	}

}
