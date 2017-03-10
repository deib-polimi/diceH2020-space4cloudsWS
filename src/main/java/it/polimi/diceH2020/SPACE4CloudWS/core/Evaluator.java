/*
Copyright 2016-2017 Eugenio Gianniti
Copyright 2016 Michele Ciavotta

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

import it.polimi.diceH2020.SPACE4Cloud.shared.settings.AMPLModel;
import it.polimi.diceH2020.SPACE4Cloud.shared.settings.SPNModel;
import it.polimi.diceH2020.SPACE4Cloud.shared.settings.Scenarios;
import it.polimi.diceH2020.SPACE4Cloud.shared.solution.*;
import it.polimi.diceH2020.SPACE4CloudWS.engines.EngineProxy;
import it.polimi.diceH2020.SPACE4CloudWS.performanceMetrics.LittleLaw;
import it.polimi.diceH2020.SPACE4CloudWS.performanceMetrics.Utilization;
import it.polimi.diceH2020.SPACE4CloudWS.services.DataService;
import lombok.NonNull;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.function.BiConsumer;
import java.util.function.Consumer;

@Component
class Evaluator implements IEvaluator {

	@Setter(onMethod = @__(@Autowired))
	private DataProcessor dataProcessor;

	@Setter(onMethod = @__(@Autowired))
	private DataService dataService;

	private Matrix matrix;

	private Solution solution;

	private int registeredSolutionsPerJob;

	private long executionTime;

	@Setter(onMethod = @__(@Autowired))
	private EngineProxy engineProxy;

	@Override
	public double evaluate(Solution solution) {
		Double cost = solution.getLstSolutions().parallelStream().mapToDouble(this::calculateCostPerJob).sum();
		solution.getLstSolutions().parallelStream().forEach(this::evaluateFeasibility);
		solution.setEvaluated(true);

		if (dataService.getScenario() == Scenarios.PrivateAdmissionControlWithPhysicalAssignment) {
			int activeNodes = 0;
			if(dataService.getScenario().getModel().equals(AMPLModel.BIN_PACKING)){
				if(solution.getActiveNodes()!=null && !solution.getActiveNodes().isEmpty()){
					for(Boolean b : solution.getActiveNodes().values()){
						if(b){
							activeNodes++;
						}
					}
				}
				cost = solution.getPrivateCloudParameters().get().getE() * activeNodes;
			}
		}

		solution.setCost(cost);
		return cost;
	}

	@Override
	public double evaluate(SolutionPerJob solutionPerJob) {
		double cost = calculateCostPerJob(solutionPerJob);
		evaluateFeasibility(solutionPerJob);
		return cost;
	}

	private double calculateCostPerJob(SolutionPerJob solPerJob) {
		double deltaBar = solPerJob.getDeltaBar();
		double rhoBar = solPerJob.getRhoBar();
		double sigmaBar = solPerJob.getSigmaBar();
		int currentNumberOfUsers = solPerJob.getNumberUsers();
		int maxNumberOfUsers =  solPerJob.getJob().getHup();
		System.out.println("Hup-H "+maxNumberOfUsers+"-"+currentNumberOfUsers);
		double cost = deltaBar * solPerJob.getNumOnDemandVM() + rhoBar * solPerJob.getNumReservedVM()
				+ sigmaBar * solPerJob.getNumSpotVM() +
				(maxNumberOfUsers - currentNumberOfUsers) * solPerJob.getJob().getPenalty();
		solPerJob.setCost(cost);
		return cost;
	}

	private boolean evaluateFeasibility(SolutionPerJob solPerJob) {
		if (solPerJob.getDuration() <= solPerJob.getJob().getD()) {
			solPerJob.setFeasible(true);
			return true;
		}
		solPerJob.setFeasible(false);
		return false;
	}

	void initialSimulation(@NonNull Solution sol) {
		SPNModel technology = SolverChecker.enforceSolverSettings(dataProcessor, sol);

		BiConsumer<SolutionPerJob, Double> resultSaver = technology == SPNModel.MAPREDUCE
				? (SolutionPerJob spj, Double value) -> {
			spj.setThroughput(value);
			spj.setDuration(LittleLaw.computeResponseTime(value, spj));
			spj.setError(false);
		} : (SolutionPerJob spj, Double value) -> {
			spj.setUtilization(Utilization.computeServerUtilization(value, spj));
			spj.setError(false);
		};

		Consumer<SolutionPerJob> errorSetter = technology == SPNModel.MAPREDUCE
				? (SolutionPerJob spj) -> {
			spj.setThroughput(Double.MAX_VALUE);
			spj.setDuration(Double.MAX_VALUE);
			spj.setError(Boolean.TRUE);
		} : (SolutionPerJob spj) -> {
			spj.setUtilization(Double.MAX_VALUE);
			spj.setError(Boolean.TRUE);
		};

		long exeTime = dataProcessor.calculateMetric(sol, resultSaver, errorSetter);
		Phase phase = new Phase(PhaseID.EVALUATION, exeTime);
		sol.addPhase(phase);
	}

	void calculateDuration(@NonNull Matrix matrix, @NonNull Solution solution){
		this.matrix = matrix;
		this.solution = solution;
		executionTime = 0L;
		registeredSolutionsPerJob = 0;
		dataProcessor.calculateDuration(matrix);
	}

	public synchronized void register(SolutionPerJob spj, long executionTime){
		boolean finished = true;
		boolean error = false;
		this.executionTime += executionTime;

		matrix.get(spj.getId())[spj.getNumberUsers()-matrix.getHlow(spj.getId())] = spj;

		++registeredSolutionsPerJob;

		if(registeredSolutionsPerJob != matrix.getNumCells() ) finished = false;

		if(matrix.getAllSolutions().stream().map(SolutionPerJob::getNumberVM).anyMatch(s->s<0)){
			error = true;
			engineProxy.getEngine().error();
		}

		if(finished&&!error){
			solution.setEvaluated(false);
			Phase phase = new Phase(PhaseID.EVALUATION, this.executionTime);
			solution.addPhase(phase);
			engineProxy.getEngine().evaluated();
		}
	}
}
