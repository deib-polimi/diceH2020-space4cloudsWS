package it.polimi.diceH2020.SPACE4CloudWS.solvers.solversImpl.MINLPSolver;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.List;
import java.util.Map.Entry;

import org.apache.log4j.Logger;
import org.springframework.stereotype.Component;

import it.polimi.diceH2020.SPACE4Cloud.shared.solution.Solution;
import it.polimi.diceH2020.SPACE4Cloud.shared.solution.SolutionPerJob;
import it.polimi.diceH2020.SPACE4CloudWS.core.Matrix;

@Component
class AMPLSolFileParser {
    private AMPLModelType model = AMPLModelType.CENTRALIZED;
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
			
			int[] selectedCells = new int[matrix.getNumRows()];
			
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
			
			int i=0;
			for(Entry<String,SolutionPerJob[]> entry : matrix.entrySet()){
				solution.setSolutionPerJob(matrix.getCell(matrix.getID(entry.getValue()[0].getJob().getId()), selectedCells[i]));
				i++;
			}
			
		}
	}
	
	protected void parseBinPackingSolution(Solution solution, Matrix matrix, File resultsFile) throws FileNotFoundException, IOException {
		try (BufferedReader reader = new BufferedReader(new FileReader(resultsFile))) {
			solution.getLstSolutions().clear();
			
			int[] selectedCells = new int[matrix.getNumRows()];
			
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
			
			int i=0;
			for(Entry<String,SolutionPerJob[]> entry : matrix.entrySet()){
				solution.setSolutionPerJob(matrix.getCell(matrix.getID(entry.getValue()[0].getJob().getId()), selectedCells[i]));
				i++;
			}
			
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
