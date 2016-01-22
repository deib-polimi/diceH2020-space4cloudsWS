package it.polimi.diceH2020.SPACE4CloudWS.core;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import it.polimi.diceH2020.SPACE4CloudWS.fs.FileUtility;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import it.polimi.diceH2020.SPACE4Cloud.shared.Solution;
import it.polimi.diceH2020.SPACE4Cloud.shared.SolutionPerJob;
import it.polimi.diceH2020.SPACE4CloudWS.services.DataService;
import it.polimi.diceH2020.SPACE4CloudWS.solvers.MINLPSolver;

@Service
public class InitialSolutionBuilder {
	private static Logger logger = Logger.getLogger(InitialSolutionBuilder.class.getName());

	@Autowired
	private DataService dataService;

	private List<Double> deltaBar;

	private List<List<String>> matrixNamesDatFiles;

	private List<List<String>> matrixNamesSolFiles;

	@Autowired
	private MINLPSolver minlpSolver;

	private int numJobs;

	private int numTypeVM;

	private List<Double> rhoBar;

	private List<Double> sigmaBar;

	private static List<List<String>> initMatrixNamesDatFiles(int numJobs, int numTypeVM) {

		List<List<String>> matrixNames = new ArrayList<List<String>>();
		StringBuilder builder = new StringBuilder();
		for (int i = 0; i < numJobs; i++) {
			List<String> lstNameFiles = new ArrayList<>();
			for (int j = 0; j < numTypeVM; j++) {
				builder.append("path" + i + "" + j + ".dat");
				lstNameFiles.add(builder.toString());
				builder.setLength(0);
			}
			matrixNames.add(lstNameFiles);
		}
		return matrixNames;
	}

	private static List<List<String>> initMatrixNamesSolFiles(int numJobs, int numTypeVM) {
		List<List<String>> matrixNamesFiles = new ArrayList<List<String>>();
		StringBuilder builder = new StringBuilder();
		List<String> lstNamesFiles = new ArrayList<>();
		for (int i = 0; i < numJobs; i++) {
			for (int j = 0; j < numTypeVM; j++) {
				builder.append("result" + i + "" + j + ".sol");
				lstNamesFiles.add(builder.toString());
				builder.setLength(0);
			}
			matrixNamesFiles.add(lstNamesFiles);
		}
		return matrixNamesFiles;
	}

	public Solution getInitialSolution() throws Exception {

		List<Float> listResults = new ArrayList<Float>();
		List<Integer> lstNumCores = new ArrayList<>();
		Float result;
		int minIndex;
		Solution startingSol = new Solution();

		this.init(); // initialization

		// Phase 1
		for (int i = 0; i < numJobs; i++) {
			for (int j = 0; j < numTypeVM; j++) {
				result = minlpSolver.run(matrixNamesDatFiles.get(i).get(j), matrixNamesSolFiles.get(i).get(j));
				listResults.add(result);
			}
			minIndex = listResults.indexOf(Collections.min(listResults));

			SolutionPerJob solPerJob = new SolutionPerJob();
			solPerJob.setIdxVmTypeSelected(minIndex);
			String vmType = dataService.getData().getTypeVm(minIndex);
			solPerJob.setTypeVMselected(vmType);
			int numCores = dataService.getNumCores(vmType);
			solPerJob.setNumCores(numCores);
			lstNumCores.add(numCores);
			int jobId = dataService.getData().getId_job(i);
			logger.info("For job class " + jobId + " has been selected the machine " + vmType);
			listResults.clear();
			startingSol.setSolutionPerJob(solPerJob);
		}

		// Phase 2

		FileUtility.createFullModelFile(startingSol.getIdxVmTypeSelected(), dataService.getData(), "data.dat", sigmaBar,
				deltaBar, rhoBar, lstNumCores);
		String resultsFileName = "result1.sol";
		minlpSolver.run("data.dat", resultsFileName);
		String resultsPath = FileUtility.LOCAL_DYNAMIC_FOLDER + File.separator + resultsFileName;
		updateWithFinalValues(startingSol, resultsPath);

		return startingSol;

	}

	public void init() throws IOException {
		numJobs = dataService.getNumberJobs();
		numTypeVM = dataService.getNumberTypeVM();
		matrixNamesDatFiles = initMatrixNamesDatFiles(numJobs, numTypeVM);
		matrixNamesSolFiles = initMatrixNamesSolFiles(numJobs, numTypeVM);
		sigmaBar = dataService.getLstSigmaBar();
		deltaBar = dataService.getLstDeltaBar();
		rhoBar = dataService.getLstRhoBar();
		FileUtility.constructFile(dataService.getData(), matrixNamesDatFiles, sigmaBar, deltaBar, rhoBar,
				dataService.getMatrixJobCores());
	}

	private void updateWithFinalValues(Solution sol, String nameSolFile) throws IOException {
		int ncontainers = 0;
		int nusers;
		List<Integer> numCores = dataService.getNumCores(sol.getTypeVMSelected());

		File file = new File(nameSolFile);
		BufferedReader reader = new BufferedReader(new FileReader(file));

		String line = reader.readLine();
		String[] bufferStr = new String[7];

		// looking for the line with solve_result info
		while (!line.contains("solve_result ")) {
			line = reader.readLine();
		}

		bufferStr = line.split("\\s+");
		if (bufferStr[2] == "infeasible") {
			logger.info("The problem is infeasible");
			reader.close();
			return;
		}

		while (!line.contains("t [*]")) {
			line = reader.readLine();
			System.out.println(line);
		}

		for (int i = 0; i < numJobs; i++) {
			line = reader.readLine();
			bufferStr = line.split("\\s+");
			System.out.println(bufferStr[1]);
			String localStr = bufferStr[1].replaceAll("\\s+", "");
			sol.getLstSolutions().get(i).setSimulatedTime(Double.parseDouble(localStr));
		}
		while (!line.contains("Variables")) {
			line = reader.readLine();
			System.out.println(line);
		}

		line = reader.readLine();
		double users;

		for (int i = 0; i < numJobs; i++) {
			line = reader.readLine();
			bufferStr = line.split("\\s+");
			String x = bufferStr[2].replaceAll("\\s+", "");
			ncontainers = Integer.parseInt(x);
			SolutionPerJob solPerJob = sol.getSolutionPerJob(i);
			solPerJob.setNumberVM(ncontainers * numCores.get(i));
			solPerJob.setNumberContainers(ncontainers);
			x = bufferStr[5].replaceAll("\\s+", "");
			users = Double.parseDouble(x);
			users = 1 / users;
			nusers = (int) users;
			solPerJob.setNumberUsers(nusers);
		}

		while (!line.contains("### alphabeta"))
			line = reader.readLine();

		line = reader.readLine();
		for (int i = 0; i < dataService.getNumberJobs(); i++) {
			line = reader.readLine();
			bufferStr = line.split("\\s+");

			String x = bufferStr[2].replaceAll("\\s+", "");
			SolutionPerJob solPerJob = sol.getSolutionPerJob(i);
			solPerJob.setAlfa(Double.parseDouble(x));

			x = bufferStr[3].replaceAll("\\s+", "");
			solPerJob.setBeta(Double.parseDouble(x));

		}
		reader.close();

	}

}
