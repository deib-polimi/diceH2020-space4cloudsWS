package it.polimi.diceH2020.SPACE4CloudWS.core;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import it.polimi.diceH2020.SPACE4Cloud.shared.Solution;
import it.polimi.diceH2020.SPACE4Cloud.shared.SolutionPerJob;
import it.polimi.diceH2020.SPACE4CloudWS.services.DataService;

@Component
public class Evaluator {

	@Autowired
	private DataService dataService;

	public double evaluate(Solution sol) {
		double cost = 0;
		
		for (SolutionPerJob solPerJob : sol.getLstSolutions()) {
			calculateNumVMsPerType(solPerJob);
			cost = cost + calculateCostPerJob(solPerJob);
		}
		sol.setCost(cost);
		return cost;
	}

	private double calculateCostPerJob(SolutionPerJob solPerJob) {
		String selectedVMtype = solPerJob.getTypeVMselected();
		double deltaBar = dataService.getDeltaBar(selectedVMtype);
		double rhoBar = dataService.getRhoBar(selectedVMtype);
		double sigmaBar = dataService.getSigmaBar(selectedVMtype);
		double alfa = solPerJob.getAlfa();
		double numberOfUsers = solPerJob.getNumberUsers();
		double beta = solPerJob.getBeta();
		double cost = deltaBar * solPerJob.getNumOnDemandVM() + rhoBar * solPerJob.getNumReservedVM()
				+ sigmaBar * solPerJob.getNumSpotVM() + (alfa / numberOfUsers - beta);
		solPerJob.setCost(cost);
		return cost;
	}

	private void calculateNumVMsPerType(SolutionPerJob solPerJob) {
		double N = dataService.getData().getEta(solPerJob.getPos());
		double R = dataService.getData().getR(solPerJob.getPos());
		int nContainers = solPerJob.getNumberContainers();
		double ratio = nContainers / solPerJob.getNumCores(); // TODO
																					// //
																					// Check
																					// this
		double numSpotVM = N * ratio;
		double numReservedVM = Math.min(R, (ratio) * (1 - N));
		double numOnDemandVM = Math.max(0, ratio - numSpotVM - numReservedVM);
		solPerJob.setNumSpotVM(numSpotVM);
		solPerJob.setNumReservedVM(numReservedVM);
		solPerJob.setNumOnDemandVM(numOnDemandVM);		

	}
}
