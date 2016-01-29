package it.polimi.diceH2020.SPACE4CloudWS.core;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.google.common.primitives.Doubles;
import com.google.common.primitives.Ints;

import it.polimi.diceH2020.SPACE4Cloud.shared.InstanceData;
import it.polimi.diceH2020.SPACE4Cloud.shared.Profile;
import it.polimi.diceH2020.SPACE4Cloud.shared.Solution;
import it.polimi.diceH2020.SPACE4Cloud.shared.SolutionPerJob;
import it.polimi.diceH2020.SPACE4CloudWS.fileManagement.AMPLDataFileBuilder;
import it.polimi.diceH2020.SPACE4CloudWS.fileManagement.AMPLDataFileUtils;
import it.polimi.diceH2020.SPACE4CloudWS.fileManagement.FileUtility;
import it.polimi.diceH2020.SPACE4CloudWS.services.DataService;
import it.polimi.diceH2020.SPACE4CloudWS.solvers.MINLPSolver;

@Service
public class InitialSolutionBuilder {
	private static Logger logger = Logger.getLogger(InitialSolutionBuilder.class.getName());

	@Autowired
	private DataService dataService;

	@Autowired
	private MINLPSolver minlpSolver;

	@Autowired
	private FileUtility fileUtility;

	private InstanceData instanceData;

	private List<Double> deltaBar;
	private List<Double> rhoBar;
	private List<Double> sigmaBar;
	private List<List<Integer>> matrixJobCores;

	private int numJobs;

	public Solution getInitialSolution() throws Exception {

		List<Float> listResults = new ArrayList<Float>();
		List<Integer> lstNumCores = new ArrayList<>();
		Solution startingSol = new Solution();

		init();
		InstanceData data = dataService.getData();
		// Phase 1
		data.getLstClass().forEach(jobClass -> {
			data.getLstTypeVM(jobClass).forEach(tVM->{
				Profile prof = data.getProfile(jobClass, tVM);
				AMPLDataFileBuilder builder = AMPLDataFileUtils.singleClassBuilder(data.getGamma(), jobClass, tVM, prof);
				
			});
			
		});
		for (int i = 0; i < numJobs; ++i) {

			for (int j = 0; j < numTypeVM; ++j) {
				builder.setArrayParameter("w", Ints.asList(matrixJobCores.get(i).get(j)))
						.setArrayParameter("sigmabar", Doubles.asList(sigmaBar.get(j)))
						.setArrayParameter("deltabar", Doubles.asList(deltaBar.get(j)))
						.setArrayParameter("rhobar", Doubles.asList(rhoBar.get(j)));
				File dataFile = fileUtility.provideTemporaryFile(String.format("partial_class%d_vm%d_", i, j), ".dat");
				fileUtility.writeContentToFile(builder.build(), dataFile);
				File resultsFile = fileUtility.provideTemporaryFile(String.format("partial_class%d_vm%d_", i, j),
						".sol");
				float result = minlpSolver.run(dataFile, resultsFile);
				if (fileUtility.delete(dataFile)) {
					logger.debug(dataFile + " deleted");
				}
				if (fileUtility.delete(resultsFile)) {
					logger.debug(resultsFile + " deleted");
				}
				listResults.add(result);
			}
			int minIndex = listResults.indexOf(Collections.min(listResults));

			SolutionPerJob solPerJob = new SolutionPerJob();
			solPerJob.setIdxVmTypeSelected(minIndex);
			String vmType = instanceData.getTypeVm(minIndex);
			solPerJob.setTypeVMselected(vmType);
			int numCores = dataService.getNumCores(vmType);
			solPerJob.setNumCores(numCores);
			lstNumCores.add(numCores);

			solPerJob.setDeltaBar(dataService.getDeltaBar(vmType));
			solPerJob.setRhoBar(dataService.getRhoBar(vmType));
			solPerJob.setSigmaBar(dataService.getSigmaBar(vmType));
			solPerJob.setEta(dataService.getData().getEta(minIndex));
			solPerJob.setR(dataService.getData().getR(minIndex));
			solPerJob.setD(dataService.getData().getD(minIndex));

			int jobId = dataService.getData().getId_job(i);
			logger.info("For job class " + jobId + " has been selected the machine " + vmType);
			listResults.clear();
			startingSol.setSolutionPerJob(solPerJob);
		}

		// Phase 2
		AMPLDataFileBuilder builder = AMPLDataFileUtils.multiClassBuilder(instanceData);
		builder.setArrayParameter("w", lstNumCores);
		filterAndAddParameters(builder, startingSol.getIdxVmTypeSelected());
		File dataFile = fileUtility.provideTemporaryFile("S4C-multi-class-", ".dat");
		fileUtility.writeContentToFile(builder.build(), dataFile);
		File resultsFile = fileUtility.provideTemporaryFile("S4C-multi-class-", ".sol");
		minlpSolver.run(dataFile, resultsFile);
		if (fileUtility.delete(dataFile)) {
			logger.debug(dataFile + " deleted");
		}
		updateWithFinalValues(startingSol, resultsFile);
		if (fileUtility.delete(resultsFile)) {
			logger.debug(resultsFile + " deleted");
		}
		Evaluator.evaluate(startingSol);
		return startingSol;
	}

	private void filterAndAddParameters(AMPLDataFileBuilder builder, List<Integer> indices) {
		List<Integer> cM = new LinkedList<>();
		List<Integer> cR = new LinkedList<>();
		List<Double> delta = new LinkedList<>();
		List<Double> rho = new LinkedList<>();
		List<Double> sigma = new LinkedList<>();

		for (int i = 0; i < indices.size(); ++i) {
			cM.add(instanceData.getcM(i, indices.get(i)));
			cR.add(instanceData.getcR(i, indices.get(i)));
			delta.add(deltaBar.get(i));
			rho.add(rhoBar.get(i));
			sigma.add(sigmaBar.get(i));
		}

		builder.setArrayParameter("cM", cM);
		builder.setArrayParameter("cR", cR);
		builder.setArrayParameter("deltabar", delta);
		builder.setArrayParameter("rhobar", rho);
		builder.setArrayParameter("sigmabar", sigma);
	}

	public void init() throws IOException {
		numJobs = dataService.getNumberJobs();
		sigmaBar = dataService.getLstSigmaBar();
		deltaBar = dataService.getLstDeltaBar();
		rhoBar = dataService.getLstRhoBar();
		instanceData = dataService.getData();
		matrixJobCores = dataService.getMatrixJobCores();
	}

	private void updateWithFinalValues(Solution sol, File solutionFile) throws IOException {
		int ncontainers = 0;
		int nusers;
		List<Integer> numCores = dataService.getNumCores(sol.getTypeVMSelected());

		BufferedReader reader = new BufferedReader(new FileReader(solutionFile));

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
			sol.getLstSolutions().get(i).setDuration(Double.parseDouble(localStr));
		}
		while (!line.contains("Variables")) {
			line = reader.readLine();
			System.out.println(line);
		}
		line = reader.readLine();
		System.out.println(line);
		while (line.contains(":")) {
			line = reader.readLine();
			System.out.println(line);
		}
		double users;

		for (int i = 0; i < numJobs; i++) {
			bufferStr = line.split("\\s+");
			String x = bufferStr[2].replaceAll("\\s+", "");
			ncontainers = Math.round(Float.parseFloat(x));
			SolutionPerJob solPerJob = sol.getSolutionPerJob(i);
			solPerJob.setNumberVM(ncontainers * numCores.get(i));
			solPerJob.setNumberContainers(ncontainers);
			x = bufferStr[5].replaceAll("\\s+", "");
			users = Double.parseDouble(x);
			users = 1 / users;
			nusers = (int) users;
			solPerJob.setNumberUsers(nusers);
			line = reader.readLine();
		}

		while (!line.contains("### alphabeta"))
			line = reader.readLine();

		reader.readLine();
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
