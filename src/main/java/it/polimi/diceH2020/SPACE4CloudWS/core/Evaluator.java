package it.polimi.diceH2020.SPACE4CloudWS.core;

import org.springframework.stereotype.Component;

import it.polimi.diceH2020.SPACE4Cloud.shared.solution.IEvaluator;
import it.polimi.diceH2020.SPACE4Cloud.shared.solution.Solution;
import it.polimi.diceH2020.SPACE4Cloud.shared.solution.SolutionPerJob;

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
		double rhoBar = solPerJob.getRhoBar();;
		double sigmaBar = solPerJob.getSigmaBar();
		double alfa = solPerJob.getAlfa();
		double numberOfUsers = solPerJob.getNumberUsers();
		double beta = solPerJob.getBeta();
		double cost = deltaBar * solPerJob.getNumOnDemandVM() + rhoBar * solPerJob.getNumReservedVM()
				+ sigmaBar * solPerJob.getNumSpotVM() + (alfa / numberOfUsers - beta);
		solPerJob.setCost(cost);
		return cost;
	}

}
