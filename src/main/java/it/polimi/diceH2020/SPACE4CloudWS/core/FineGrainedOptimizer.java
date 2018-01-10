/*
Copyright 2017 Eugenio Gianniti
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
import it.polimi.diceH2020.SPACE4Cloud.shared.settings.CloudType;
import it.polimi.diceH2020.SPACE4Cloud.shared.settings.Scenario;
import it.polimi.diceH2020.SPACE4Cloud.shared.solution.Matrix;
import it.polimi.diceH2020.SPACE4Cloud.shared.solution.Phase;
import it.polimi.diceH2020.SPACE4Cloud.shared.solution.PhaseID;
import it.polimi.diceH2020.SPACE4Cloud.shared.solution.SolutionPerJob;
import it.polimi.diceH2020.SPACE4CloudWS.engines.EngineProxy;
import it.polimi.diceH2020.SPACE4CloudWS.fineGrainedLogicForOptimization.ContainerLogicForOptimization;
import lombok.Setter;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

@Component
public class FineGrainedOptimizer extends Optimizer {
	private final Logger logger = Logger.getLogger(getClass());

	@Setter(onMethod = @__(@Autowired))
	private ApplicationContext context;

	@Setter(onMethod = @__(@Autowired))
	private EngineProxy engineProxy;

	@Setter(onMethod = @__(@Autowired))
	private SolverChecker solverChecker;

	private Matrix matrix;

	private int registeredSolutionsPerJob;

	private long executionTime;

	void hillClimbing(Matrix matrix) {
		solverChecker.enforceSolverSettings (matrix.getAllSolutions ());
		this.matrix = matrix;
		this.registeredSolutionsPerJob = 0;
		logger.info(String.format("---------- Starting fine grained hill climbing for instance %s ----------", engineProxy.getEngine().getSolution().getId()));
		start();
	}

	private void start() {
		executionTime = 0L;
		for (SolutionPerJob spj: matrix.getAllSolutions()) {
			int maxNumVM = 0;
			Scenario scenario = dataService.getScenario();
			if (scenario.getCloudType() == CloudType.PRIVATE) {
				PrivateCloudParameters p = dataService.getData().getPrivateCloudParameters();
				double m_tilde = dataService.getMemory(spj.getTypeVMselected().getId());
				double v_tilde = dataService.getNumCores(spj.getTypeVMselected().getId());
				maxNumVM = (int) Math.floor(Math.min(Math.ceil(p.getM() / m_tilde),
						Math.ceil(p.getV() / v_tilde))) * p.getN();
			} else {
				///maxNumVM = Integer.MAX_VALUE;
				maxNumVM = 1000;
			}
			System.out.println("MAX NUMVM:"+maxNumVM);
			ContainerLogicForOptimization spjOptimizer =
					(ContainerLogicForOptimization) context.getBean("containerLogicForOptimization", spj, 1, maxNumVM);
			spjOptimizer.start();
		}
	}

	private void aggregateAndFinish() {
      logger.trace("aggregateAndFinish: " + matrix.getAllSolutions().size());
		for (SolutionPerJob spj : matrix.getAllSolutions()) {
			evaluator.evaluate(spj);
		}
		Phase ph = new Phase();
		ph.setId(PhaseID.OPTIMIZATION);
		ph.setDuration(executionTime);
		engineProxy.getEngine().getSolution().addPhase(ph);
		engineProxy.getEngine().reduceMatrix(); //TODO modify automata in order to avoid this backward call
	}

	public void finish() {
		logger.trace("FineGrainedOptimizer - Finish");
		engineProxy.getEngine().getSolution().setEvaluated(false);
		evaluator.evaluate(engineProxy.getEngine().getSolution());
	}

	public synchronized void registerSPJGivenHOptimalNVM(SolutionPerJob spj, long executionTime) {
		boolean finished = true;
		this.executionTime += executionTime;
		//optimalNVMGivenH[spj.getJob().getId()-1][h-1] = nVM;
		matrix.get(spj.getId())[spj.getNumberUsers()-matrix.getHlow(spj.getId())] = spj;

		registeredSolutionsPerJob++;
		if (registeredSolutionsPerJob != matrix.getNumCells()) finished = false;

		if (finished) {
			System.out.println(matrix.asString());
			aggregateAndFinish();
		}
	}

}
