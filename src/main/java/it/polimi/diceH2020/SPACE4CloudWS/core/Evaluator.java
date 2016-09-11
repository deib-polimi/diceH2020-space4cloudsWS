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

import it.polimi.diceH2020.SPACE4Cloud.shared.solution.IEvaluator;
import it.polimi.diceH2020.SPACE4Cloud.shared.solution.Solution;
import it.polimi.diceH2020.SPACE4Cloud.shared.solution.SolutionPerJob;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Component
class Evaluator implements IEvaluator {

	@Override
	public double evaluate(Solution solution) {
		BigDecimal cost = BigDecimal.valueOf(solution.getLstSolutions().parallelStream()
				.mapToDouble(this::calculateCostPerJob).sum()).setScale(4, RoundingMode.HALF_EVEN);
		solution.getLstSolutions().parallelStream().forEach(this::evaluateFeasibility);
		solution.setEvaluated(true);
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
		double alpha = solPerJob.getAlfa();
		double beta = solPerJob.getBeta();
		double numberOfUsers = solPerJob.getNumberUsers();
		double cost = deltaBar * solPerJob.getNumOnDemandVM() + rhoBar * solPerJob.getNumReservedVM()
				+ sigmaBar * solPerJob.getNumSpotVM() + (alpha / numberOfUsers - beta);
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

}
