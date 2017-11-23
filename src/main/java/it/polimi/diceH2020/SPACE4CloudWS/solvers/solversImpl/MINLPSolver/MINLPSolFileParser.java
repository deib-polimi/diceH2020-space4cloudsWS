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

import it.polimi.diceH2020.SPACE4CloudWS.solvers.MINLPSolver;
import it.polimi.diceH2020.SPACE4Cloud.shared.solution.Matrix;
import it.polimi.diceH2020.SPACE4Cloud.shared.solution.Solution;
import it.polimi.diceH2020.SPACE4Cloud.shared.solution.SolutionPerJob;
import org.apache.log4j.Logger;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.lang3.tuple.ImmutablePair;

public class MINLPSolFileParser {

	private Logger logger = Logger.getLogger(getClass());

	private void parseKnapsackSolution(Solution solution, Matrix matrix, File resultsFile) throws IOException {
		try (BufferedReader reader = new BufferedReader(new FileReader(resultsFile))) {
			solution.getLstSolutions().clear();
			int[] selectedCells = new int[matrix.numNotFailedRows()];

			String line = reader.readLine().trim();

			while (! line.contains("solve_result ")) {
				line = reader.readLine().trim();
			}

			String[] bufferStr = line.split("\\s+");
			if (bufferStr[2].equals("infeasible")) {
				logger.info("The problem is infeasible");
				MINLPSolver.initializeSolution(solution,matrix);
				return;
			}

			while (! line.contains("### Concurrency")) {
				line = reader.readLine().trim();
			}
			reader.readLine().trim();
			for(int i=0; i< selectedCells.length; i++){
				line = reader.readLine().trim();
				bufferStr = line.split("\\s+");

				String currentRow = bufferStr[0].replaceAll("\\s+", "");
				String currentH = bufferStr[1].replaceAll("\\s+", "");
				selectedCells[Integer.valueOf(currentRow)-1] = Integer.valueOf(currentH);
			}

			for(int c=0; c<selectedCells.length; c++){
				solution.addSolutionPerJob(matrix.getCell(matrix.getNotFailedRow(c+1), selectedCells[c]));
			}

			//TODO for failed rows.. add json property? (so add rows to the final solution)
			//Currently if a class has failed a partial solution is returned.
		}
	}

	private void parseBinPackingSolution(Solution solution, Matrix matrix, File resultsFile) throws IOException {
		try (BufferedReader reader = new BufferedReader(new FileReader(resultsFile))) {
			solution.getLstSolutions().clear();
			String log = matrix.getIdentifier()+" "+solution.getScenario();

			int[] selectedCells = new int[matrix.numNotFailedRows()]; //foreach rows one and only one cell has to be selected

			String line = reader.readLine().trim();

			while (! line.contains("solve_result ")) {
				line = reader.readLine().trim();
			}

			String[] bufferStr = line.split("\\s+");
			if (bufferStr[2].equals("infeasible")) {
				logger.info("The problem is infeasible");
				MINLPSolver.initializeSolution(solution,matrix);
				return;
			}

			Map<Integer,Boolean> map = new HashMap<>();
			while (! line.contains("y [*] ")) {
				line = reader.readLine().trim();
			}
			line = reader.readLine().trim();
			while(line.indexOf(';')==-1){
				bufferStr = line.split("\\s+");
				boolean activeNode = "1".equals(bufferStr[1]);

				map.put(Integer.valueOf(bufferStr[0]), activeNode);
				line = reader.readLine().trim();
			}
			solution.setActiveNodes(map);

			Map<String,Integer> map2 = new HashMap<>();
			while (! line.contains("n :=")) {
				line = reader.readLine().trim();
			}
			line = reader.readLine().trim();
			while(line.indexOf(';')==-1){
				String s = new String();
				bufferStr = line.split("\\s+");
				s += Integer.valueOf(bufferStr[0]) +" ";
				s +=Integer.valueOf(bufferStr[1])+" ";
				s +=Integer.valueOf(bufferStr[2]);
				map2.put(s, Integer.valueOf(bufferStr[3]));
				line = reader.readLine().trim();
			}
			solution.setNumVMPerNodePerClass(map2);

			while (! line.contains("p = ")) {
				line = reader.readLine().trim();
			}
			bufferStr = line.split("\\s+");
			String p = bufferStr[2];
			solution.setPenalty(Double.valueOf(p));

			while (! line.contains("### Concurrency")) {
				line = reader.readLine().trim();
			}
			reader.readLine().trim();
			for(int i=0; i< selectedCells.length; i++){
				line = reader.readLine().trim();
				bufferStr = line.split("\\s+");

				String currentRow = bufferStr[0].replaceAll("\\s+", "");
				String currentH = bufferStr[1].replaceAll("\\s+", "");
				selectedCells[Integer.valueOf(currentRow)-1] = Integer.valueOf(currentH);
			}

			for(int c=0; c<selectedCells.length; c++){
				solution.addSolutionPerJob(matrix.getCell(matrix.getNotFailedRow(c+1), selectedCells[c]));
			}

			//TODO for failed rows.. add json property? (so add rows to the final solution)
			//Currently if a class has failed a partial solution is returned.

			System.out.println("[BP-RESULTS]"+log);
			try (BufferedReader br = new BufferedReader(new FileReader(resultsFile))) {
				String line2 = null;
				while ((line2 = br.readLine()) != null) {
					System.out.println(line2);
				}
			}
		}
	}

	public void updateResults(Solution solution, Matrix matrix, File resultsFile) throws IOException {
		parseKnapsackSolution(solution, matrix, resultsFile);
	}

}
