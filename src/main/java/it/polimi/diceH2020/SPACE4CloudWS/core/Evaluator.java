/*
Copyright 2016 Michele Ciavotta
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

import it.polimi.diceH2020.SPACE4Cloud.shared.settings.Models;
import it.polimi.diceH2020.SPACE4Cloud.shared.settings.Scenarios;
import it.polimi.diceH2020.SPACE4Cloud.shared.solution.IEvaluator;
import it.polimi.diceH2020.SPACE4Cloud.shared.solution.Matrix;
import it.polimi.diceH2020.SPACE4Cloud.shared.solution.Phase;
import it.polimi.diceH2020.SPACE4Cloud.shared.solution.PhaseID;
import it.polimi.diceH2020.SPACE4Cloud.shared.solution.Solution;
import it.polimi.diceH2020.SPACE4Cloud.shared.solution.SolutionPerJob;
import it.polimi.diceH2020.SPACE4CloudWS.engines.EngineProxy;
import it.polimi.diceH2020.SPACE4CloudWS.services.DataService;
import lombok.NonNull;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Component
class Evaluator implements IEvaluator {
	
	@Autowired
	private DataProcessor dataProcessor;
	
	@Autowired
	private DataService dataService;
	
	boolean finished = false;

	private Matrix matrix;
	
	private Solution solution;

	private int registeredSolutionsPerJob;
	
	private long executionTime;
	
	@Autowired
	private EngineProxy engineProxy;
	
	@Override
	public double evaluate(Solution solution) {
		Double cost = BigDecimal.valueOf(solution.getLstSolutions().parallelStream()
				.mapToDouble(this::calculateCostPerJob).sum()).setScale(4, RoundingMode.HALF_EVEN).doubleValue();
		solution.getLstSolutions().parallelStream().forEach(this::evaluateFeasibility);
		solution.setEvaluated(true);
		
		if(dataService.getScenario().equals(Scenarios.PrivateAdmissionControlWithPhysicalAssignment)){
			int activeNodes = 0;
			if(dataService.getScenario().getModel().equals(Models.binPacking)){
				if(solution.getActiveNodes()!=null && !solution.getActiveNodes().isEmpty()){
					for(Boolean b : solution.getActiveNodes().values()){
						if(b){
							activeNodes++;
						}
					}
				}
				cost = solution.getPrivateCloudParameters().get().getE()*activeNodes;
			}
		}
		
		solution.setCost(cost.doubleValue());
		return cost.doubleValue();
	}

	@Override
	public double evaluate(SolutionPerJob solutionPerJob) {
		BigDecimal cost = BigDecimal.valueOf(calculateCostPerJob(solutionPerJob))
				.setScale(4, BigDecimal.ROUND_HALF_EVEN);
		evaluateFeasibility(solutionPerJob);
		return cost.doubleValue();
	}

	private double calculateCostPerJob(SolutionPerJob solPerJob) {
		double deltaBar = solPerJob.getDeltaBar();
		double rhoBar = solPerJob.getRhoBar();
		double sigmaBar = solPerJob.getSigmaBar();
		int currentNumberOfUsers = solPerJob.getNumberUsers();
		int maxNumberOfUsers =  solPerJob.getJob().getHup();
		System.out.println("Hup-H "+maxNumberOfUsers+"-"+currentNumberOfUsers);
		double cost = deltaBar * solPerJob.getNumOnDemandVM() + rhoBar * solPerJob.getNumReservedVM()
		+ sigmaBar * solPerJob.getNumSpotVM() + ( maxNumberOfUsers - currentNumberOfUsers)*solPerJob.getJob().getPenalty();
		
		BigDecimal c = BigDecimal.valueOf(cost).setScale(4, RoundingMode.HALF_EVEN);
		double decCost = c.doubleValue();
		solPerJob.setCost(decCost);
		return decCost;
	}

	private boolean evaluateFeasibility(SolutionPerJob solPerJob) {
		if (solPerJob.getDuration() <= solPerJob.getJob().getD()) {
			solPerJob.setFeasible(true);
			return true;
		}
		solPerJob.setFeasible(false);
		return false;
	}
	
	protected void calculateDuration(@NonNull Solution sol) {
		long exeTime = dataProcessor.calculateDuration(sol);
		Phase ph = new Phase(PhaseID.EVALUATION, exeTime); 
		sol.addPhase(ph);
		return; 
	}

	protected void calculateDuration(@NonNull Matrix matrix, @NonNull Solution solution){
		this.matrix = matrix;
		this.solution = solution;
		executionTime = 0L;
		registeredSolutionsPerJob = 0;
		
		dataProcessor.calculateDuration(matrix);
	}
	
	public synchronized void register(SolutionPerJob spj, long executionTime){
		finished = true;
		boolean error = false;
		this.executionTime += executionTime;
		//optimalNVMGivenH[spj.getJob().getId()-1][h-1] = nVM;
		matrix.get(spj.getId())[spj.getNumberUsers()-matrix.getHlow(spj.getId())] = spj;

		registeredSolutionsPerJob++;
		
		System.out.println("evaluated solution: "+ registeredSolutionsPerJob+" of "+matrix.getNumCells());//TODOd elete
		
		if(registeredSolutionsPerJob != matrix.getNumCells() ) finished = false;

		if(matrix.getAllSolutions().stream().map(SolutionPerJob::getNumberVM).anyMatch(s->s<0)){
			error = true;
			engineProxy.getEngine().error();
		}
		
		if(finished&&!error){
			solution.setEvaluated(false);
			Phase ph = new Phase(PhaseID.EVALUATION, this.executionTime); 
			solution.addPhase(ph);
			engineProxy.getEngine().evaluated(); 
		}
	}
}
