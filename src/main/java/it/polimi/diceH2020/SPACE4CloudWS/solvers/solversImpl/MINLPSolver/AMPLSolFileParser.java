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
package it.polimi.diceH2020.SPACE4CloudWS.solvers.solversImpl.MINLPSolver;

import it.polimi.diceH2020.SPACE4Cloud.shared.solution.Matrix;
import it.polimi.diceH2020.SPACE4Cloud.shared.solution.Solution;
import it.polimi.diceH2020.SPACE4Cloud.shared.solution.SolutionPerJob;

import org.apache.log4j.Logger;
import org.springframework.stereotype.Component;

import java.io.*;
import java.util.List;
import java.util.Map.Entry;

@Component
class AMPLSolFileParser {
	private AMPLModelType model = AMPLModelType.KNAPSACK;
	private Logger logger = Logger.getLogger(getClass());

	protected void parseSolution(List<SolutionPerJob> solutions, File solutionFile) throws IOException {
		try (BufferedReader reader = new BufferedReader(new FileReader(solutionFile))) {
			String line = reader.readLine();

			while (! line.contains("solve_result ")) {
				line = reader.readLine();
			}

			String[] bufferStr = line.split("\\s+");
			if (bufferStr[2].equals("infeasible")) {
				logger.info("The problem is infeasible");
				return;
			}

			while (! line.contains("t [*]")) {
				line = reader.readLine();
			}

			for (SolutionPerJob solutionPerJob : solutions) {
				line = reader.readLine();
				bufferStr = line.split("\\s+");

				String localStr = bufferStr[1].replaceAll("\\s+", "");
				solutionPerJob.setDuration(Double.parseDouble(localStr));
			}

			while (! line.contains("Variables")) {
				line = reader.readLine();
			}
			line = reader.readLine();
			while (line.contains(":")) {
				line = reader.readLine();
			}

			for (SolutionPerJob solutionPerJob : solutions) {
				bufferStr = line.split("\\s+");
				double gamma = Double.parseDouble(bufferStr[2]);
				int numVM = (int) Math.ceil(gamma);
				solutionPerJob.setNumberVM(numVM);
				double psi = Double.parseDouble(bufferStr[5]);

				double users = 1 / psi;
				int numUsers = (int)Math.round(users);
				solutionPerJob.setNumberUsers(numUsers);
				line = reader.readLine();
			}

			while (! line.contains("### alphabeta")) {
				line = reader.readLine();
			}

			reader.readLine();
			for (SolutionPerJob solutionPerJob : solutions) {
				line = reader.readLine();
				bufferStr = line.split("\\s+");

				String x = bufferStr[2].replaceAll("\\s+", "");
				solutionPerJob.setAlfa(Double.parseDouble(x));

				x = bufferStr[3].replaceAll("\\s+", "");
				solutionPerJob.setBeta(Double.parseDouble(x));
			}
		}
	}


	protected void parseKnapsackSolution(Solution solution, Matrix matrix, File resultsFile) throws FileNotFoundException, IOException {
		try (BufferedReader reader = new BufferedReader(new FileReader(resultsFile))) {
			solution.getLstSolutions().clear();
			int[] selectedCells = new int[matrix.numNotFailedRows()];

			String line = reader.readLine();

			while (! line.contains("solve_result ")) {
				line = reader.readLine();
			}

			String[] bufferStr = line.split("\\s+");
			if (bufferStr[2].equals("infeasible")) {
				logger.info("The problem is infeasible");
				initializeSolution(solution,matrix);
				return;
			}

			while (! line.contains("### Concurrency")) {
				line = reader.readLine();
			}
			reader.readLine();
			for(int i=0; i< selectedCells.length; i++){
				line = reader.readLine();
				bufferStr = line.split("\\s+");

				String currentRow = bufferStr[0].replaceAll("\\s+", "");
				String currentH = bufferStr[1].replaceAll("\\s+", "");
				selectedCells[Integer.valueOf(currentRow)-1] = Integer.valueOf(currentH);
			}

			for(int c=0; c<selectedCells.length; c++){
				solution.setSolutionPerJob(matrix.getCell(matrix.getNotFailedRow(c+1), selectedCells[c]));
			}

			//TODO for failed rows.. add json property? (so add rows to the final solution)
			//Currently if a class has failed a partial solution is returned.

		}
	}

	protected void parseBinPackingSolution(Solution solution, Matrix matrix, File resultsFile) throws FileNotFoundException, IOException {
		try (BufferedReader reader = new BufferedReader(new FileReader(resultsFile))) {
			solution.getLstSolutions().clear();

			int[] selectedCells = new int[matrix.numNotFailedRows()]; //foreach rows one and only one cell has to be selected

			String line = reader.readLine();

			while (! line.contains("solve_result ")) {
				line = reader.readLine();
			}

			String[] bufferStr = line.split("\\s+");
			if (bufferStr[2].equals("infeasible")) {
				logger.info("The problem is infeasible");
				initializeSolution(solution,matrix);
				return;
			}

			while (! line.contains("### Concurrency")) {
				line = reader.readLine();
			}
			reader.readLine();
			for(int i=0; i< selectedCells.length; i++){
				line = reader.readLine();
				bufferStr = line.split("\\s+");

				String currentRow = bufferStr[0].replaceAll("\\s+", "");
				String currentH = bufferStr[1].replaceAll("\\s+", "");
				selectedCells[Integer.valueOf(currentRow)-1] = Integer.valueOf(currentH);
			}



			for(int c=0; c<selectedCells.length; c++){
				solution.setSolutionPerJob(matrix.getCell(matrix.getNotFailedRow(c+1), selectedCells[c]));
			}

			//TODO for failed rows.. add json property? (so add rows to the final solution)
			//Currently if a class has failed a partial solution is returned.

		}
	}

	AMPLSolFileParser setModelType(AMPLModelType amplModelType) {
		model = amplModelType;
		return this;
	}

	protected void updateResults(List<SolutionPerJob> solutions, File solutionFile) throws IOException{
		parseSolution(solutions, solutionFile);
	}

	protected void updateResults(Solution solution, Matrix matrix, File resultsFile) throws FileNotFoundException, IOException{
		parseSolution(solution,matrix,resultsFile);
	}

	private void initializeSolution(Solution solution, Matrix matrix){
		for(Entry<String,SolutionPerJob[]> entry : matrix.entrySet()){
			solution.setSolutionPerJob(matrix.getCell(matrix.getID(entry.getValue()[0].getJob().getId()), entry.getValue()[0].getNumberUsers()));
		}
	}

	private void parseSolution(Solution solution, Matrix matrix, File resultsFile) throws FileNotFoundException, IOException  {
		switch (model) {
			case KNAPSACK:
				parseKnapsackSolution(solution, matrix, resultsFile);
				break;
			case BIN_PACKING:
				parseBinPackingSolution(solution, matrix, resultsFile);
				break;
			default:
				throw new AssertionError("The required model is still not implemented");
		}
	}

}
