package it.polimi.diceH2020.SPACE4CloudWS.core;

import it.polimi.diceH2020.SPACE4Cloud.shared.solution.IEvaluator;
import it.polimi.diceH2020.SPACE4Cloud.shared.solution.Solution;
import it.polimi.diceH2020.SPACE4Cloud.shared.solution.SolutionPerJob;

import java.math.BigDecimal;
import java.math.RoundingMode;

import org.springframework.stereotype.Component;

@Component
public class Evaluator implements IEvaluator {

	private static final IEvaluator instance = new Evaluator();
	
	public static double evaluate(Solution sol) {
		IEvaluator ev = sol.getEvaluator();
		if (ev == null || ev instanceof Evaluator == false)
			sol.setEvaluator(instance);
		return sol.evaluate();
	}
	


	@Override
	public  double calculateCostPerJob(SolutionPerJob solPerJob) {
		double deltaBar = solPerJob.getDeltaBar();
		double rhoBar = solPerJob.getRhoBar();
		double sigmaBar = solPerJob.getSigmaBar();
		double alfa = solPerJob.getAlfa();
		double numberOfUsers = solPerJob.getNumberUsers();
		double beta = solPerJob.getBeta();
		double cost = deltaBar * solPerJob.getNumOnDemandVM() + rhoBar * solPerJob.getNumReservedVM()
				+ sigmaBar * solPerJob.getNumSpotVM() + (alfa / numberOfUsers - beta);
		BigDecimal c = BigDecimal.valueOf(cost).setScale(2, RoundingMode.HALF_EVEN);
		double decCost = Double.parseDouble(c.toString());
		solPerJob.setCost(decCost);
		return decCost;
	}



	@Override
	public boolean evaluateFeasibility(SolutionPerJob solPerJob) {
			if (solPerJob.getDuration()<solPerJob.getJob().getD()) {
				solPerJob.setFeasible(true);
				return true;
			}
			solPerJob.setFeasible(false);
			return false;
	}

}
