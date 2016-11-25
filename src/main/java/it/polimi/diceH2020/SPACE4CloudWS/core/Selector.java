/*
Copyright 2016 Jacopo Rigoli
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

import it.polimi.diceH2020.SPACE4Cloud.shared.settings.AMPLModel;
import it.polimi.diceH2020.SPACE4Cloud.shared.solution.*;
import it.polimi.diceH2020.SPACE4CloudWS.services.DataService;
import it.polimi.diceH2020.SPACE4CloudWS.solvers.solversImpl.MINLPSolver.MINLPSolver;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;

@Component
class Selector {

	@Autowired
	private MINLPSolver minlpSolver;

	@Autowired
	private DataService dataService; //TODO dataProcessor

	private final Logger logger = Logger.getLogger(getClass());

	/**
	 * Perform the selection of matrix cells to retrieve the best combination.
	 * One and only one cell per row (one H for each Job).
	 * Dimensionality reduction due to Admission Control: 
	 * for each class only one cell is selected, 
	 * one H for each job is chosen in order to maximize the cost. 
	 */
	void selectMatrixCells(Matrix matrix, Solution solution) {
		Instant first = Instant.now();
		Phase phase = new Phase();
		try {
			switch (dataService.getScenario()) {
				case PrivateAdmissionControl:
					phase.setId(PhaseID.SELECTION_KN);
					minlpSolver.setModelType(AMPLModel.KNAPSACK);
					break;
				case PrivateAdmissionControlWithPhysicalAssignment:
					phase.setId(PhaseID.SELECTION_BP);
					minlpSolver.setModelType(AMPLModel.BIN_PACKING);
					break;
				default:
					throw new AssertionError("The required scenario does not require optimization");
			}
			minlpSolver.evaluate(matrix, solution);
		} catch (MatrixHugeHoleException e) {
			logger.error("The matrix has too few feasible alternatives", e);
			solution.setFeasible(false);
			minlpSolver.initializeSpj(solution, matrix);
			return;
		}
		Instant after = Instant.now();
		phase.setDuration(Duration.between(first, after).toMillis());
		solution.addPhase(phase);
	}
}
