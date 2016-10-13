package it.polimi.diceH2020.SPACE4CloudWS.core;

import java.time.Duration;
import java.time.Instant;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import it.polimi.diceH2020.SPACE4Cloud.shared.settings.Scenarios;
import it.polimi.diceH2020.SPACE4Cloud.shared.solution.Matrix;
import it.polimi.diceH2020.SPACE4Cloud.shared.solution.Phase;
import it.polimi.diceH2020.SPACE4Cloud.shared.solution.PhaseID;
import it.polimi.diceH2020.SPACE4Cloud.shared.solution.Solution;
import it.polimi.diceH2020.SPACE4CloudWS.services.DataService;
import it.polimi.diceH2020.SPACE4CloudWS.solvers.solversImpl.MINLPSolver.AMPLModelType;
import it.polimi.diceH2020.SPACE4CloudWS.solvers.solversImpl.MINLPSolver.MINLPSolver;

@Component
public class Selector {
	
	@Autowired
	private MINLPSolver minlpSolver;
	
	@Autowired
	private DataService dataService;
	
	private final Logger logger = Logger.getLogger(getClass());
	
	/**
	 * Perform the selection of matrix cells to retrieve the best combination.
	 * One and only one cell per row (one H for each Job).
	 * Dimensionality reduction due to Admission Control: 
	 * for each class only one cell is selected, 
	 * one H for each job is chosen in order to maximize the cost. 
	 */
	public void selectMatrixCells(Matrix matrix, Solution solution){
		Instant first = Instant.now();
		Phase ph = new Phase();
		try{
			if(dataService.getScenario().equals(Scenarios.PrivateAdmissionControl)){
				cellsSelectionWithKnapsack(matrix, solution);
				ph.setId(PhaseID.SELECTION_KN);
			}else if(dataService.getScenario().equals(Scenarios.PrivateAdmissionControlWithPhysicalAssignment)) {
				cellsSelectionWithBinPacking(matrix, solution);
				ph.setId(PhaseID.SELECTION_BP);
			}
		}catch(IllegalStateException e){
			logger.info(e.getMessage());
			solution.setFeasible(false);
			minlpSolver.initializeSpj(solution, matrix);
			return;
		}
		Instant after = Instant.now();
		ph.setDuration(Duration.between(first, after).toMillis());
		solution.addPhase(ph);
	}
	
	public void cellsSelectionWithKnapsack(Matrix matrix, Solution solution) throws IllegalStateException{
		minlpSolver.setModelType(AMPLModelType.KNAPSACK);
		minlpSolver.evaluate(matrix,solution);
	}

	public void cellsSelectionWithBinPacking(Matrix matrix, Solution solution) throws IllegalStateException{
		minlpSolver.setModelType(AMPLModelType.BIN_PACKING);
		minlpSolver.evaluate(matrix,solution);
	}

}
