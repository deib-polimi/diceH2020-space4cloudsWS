package it.polimi.diceH2020.SPACE4CloudWS.core;

import com.google.common.primitives.Doubles;
import com.google.common.primitives.Ints;
import it.polimi.diceH2020.SPACE4Cloud.shared.inputData.JobClass;
import it.polimi.diceH2020.SPACE4Cloud.shared.inputData.Profile;
import it.polimi.diceH2020.SPACE4Cloud.shared.inputData.TypeVM;
import it.polimi.diceH2020.SPACE4Cloud.shared.solution.Solution;
import it.polimi.diceH2020.SPACE4Cloud.shared.solution.SolutionPerJob;
import it.polimi.diceH2020.SPACE4CloudWS.fileManagement.FileUtility;
import it.polimi.diceH2020.SPACE4CloudWS.services.DataService;
import it.polimi.diceH2020.SPACE4CloudWS.solvers.solversImp.MINLPSolver.AMPLDataFileBuilder;
import it.polimi.diceH2020.SPACE4CloudWS.solvers.solversImp.MINLPSolver.AMPLDataFileUtils;
import it.polimi.diceH2020.SPACE4CloudWS.solvers.solversImp.MINLPSolver.MINLPSolver;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.validation.constraints.NotNull;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class InitialSolutionBuilder {
	private static Logger logger = Logger.getLogger(InitialSolutionBuilder.class.getName());

	@Autowired
	private DataService dataService;

	@Autowired
	private MINLPSolver minlpSolver;

	@Autowired
	private FileUtility fileUtility;

	public Solution getInitialSolution() throws Exception {
		
		Solution startingSol = new Solution();
		startingSol.setGamma(dataService.getGamma());
		// Phase 1
		dataService.getListJobClass().forEach(jobClass -> {
			Map<TypeVM, Double> mapResults = new HashMap<TypeVM, Double>();
			dataService.getListTypeVM(jobClass).forEach(tVM -> {
				logger.info("---------- Starting optimization jobClass " + jobClass.getId() + " considering VM type "
						+ tVM.getId() + " ----------");
				try {
					Pair<File, File> pFiles = createSingleClassWorkingFiles(jobClass, tVM);
					mapResults.put(tVM, minlpSolver.run(pFiles));
					if (fileUtility.delete(pFiles))
						logger.info("Working files correctly deleted");

				} catch (Exception e) {
					e.printStackTrace();
				}

			});

			Map.Entry<TypeVM, Double> min = mapResults.entrySet().stream()
					.min(Map.Entry.comparingByValue(Double::compareTo)).get();

			TypeVM minTVM = min.getKey();
			logger.info("For job class " + jobClass.getId() + " has been selected the machine " + minTVM.getId());
			startingSol.setSolutionPerJob(createSolPerJob(jobClass, minTVM));
		});

		// Phase 2
		Pair<File, File> multiClassPairFiles = createMultiClassWorkingFiles(startingSol);
		minlpSolver.run(multiClassPairFiles);		
		File resultsFile = multiClassPairFiles.getRight();
		updateWithFinalValues(startingSol,resultsFile);
		if (fileUtility.delete(multiClassPairFiles))
				logger.info("Working files correctly deleted");

		Evaluator.evaluate(startingSol);
		logger.info("---------- Initial solution correctly created ----------");
		return startingSol;
	}
	
	private Pair<File, File> createMultiClassWorkingFiles(@NotNull Solution sol) throws IOException{
		AMPLDataFileBuilder builder = AMPLDataFileUtils.multiClassBuilder(dataService.getData(),
				sol.getPairsTypeVMJobClass());

		builder.setArrayParameter("w", sol.getLstNumberCores());
		builder.setArrayParameter("cM", sol.getListCM());
		builder.setArrayParameter("cR", sol.getListCR());
		builder.setArrayParameter("deltabar", sol.getListDeltabar());
		builder.setArrayParameter("rhobar", sol.getListRhobar());
		builder.setArrayParameter("sigmabar", sol.getListSigmaBar());

		File dataFile = fileUtility.provideTemporaryFile("S4C-multi-class-", ".dat");
		fileUtility.writeContentToFile(builder.build(), dataFile);
		File resultsFile = fileUtility.provideTemporaryFile("S4C-multi-class-", ".sol");
		return new ImmutablePair<File, File>(dataFile, resultsFile);
	}

	private Pair<File, File> createSingleClassWorkingFiles(@NotNull JobClass jobClass, @NotNull TypeVM tVM) throws IOException {
		Profile prof = dataService.getProfile(jobClass, tVM);
		AMPLDataFileBuilder builder = AMPLDataFileUtils.singleClassBuilder(dataService.getGamma(), jobClass, tVM, prof);
		builder.setArrayParameter("w", Ints.asList(dataService.getNumCores(tVM)))
				.setArrayParameter("sigmabar", Doubles.asList(dataService.getSigmaBar(tVM)))
				.setArrayParameter("deltabar", Doubles.asList(dataService.getDeltaBar(tVM)))
				.setArrayParameter("rhobar", Doubles.asList(dataService.getRhoBar(tVM)));
		File dataFile = fileUtility
				.provideTemporaryFile(String.format("partial_class%d_vm%s_", jobClass.getId(), tVM.getId()), ".dat");
		fileUtility.writeContentToFile(builder.build(), dataFile);
		File resultsFile = fileUtility
				.provideTemporaryFile(String.format("partial_class%d_vm%s_", jobClass.getId(), tVM.getId()), ".sol");
		return new ImmutablePair<File, File>(dataFile, resultsFile);

	}

	private SolutionPerJob createSolPerJob(@NotNull JobClass jobClass, @NotNull TypeVM minTVM) {
		SolutionPerJob solPerJob = new SolutionPerJob();
		solPerJob.setJob(jobClass);
		solPerJob.setTypeVMselected(minTVM);
		solPerJob.setNumCores(dataService.getNumCores(minTVM));
		solPerJob.setDeltaBar(dataService.getDeltaBar(minTVM));
		solPerJob.setRhoBar(dataService.getRhoBar(minTVM));
		solPerJob.setSigmaBar(dataService.getSigmaBar(minTVM));
		solPerJob.setProfile(dataService.getProfile(jobClass, minTVM));
		return solPerJob;

	}

	private void updateWithFinalValues(Solution sol, File solutionFile) throws IOException {
		int numJobs = dataService.getJobNumber();
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
			// System.out.println(line);
		}

		for (int i = 0; i < numJobs; i++) {
			line = reader.readLine();
			bufferStr = line.split("\\s+");
			// System.out.println(bufferStr[1]);

			String localStr = bufferStr[1].replaceAll("\\s+", "");
			sol.getLstSolutions().get(i).setDuration(Double.parseDouble(localStr));
		}
		while (!line.contains("Variables")) {
			line = reader.readLine();
			// System.out.println(line);
		}
		line = reader.readLine();
		// System.out.println(line);
		while (line.contains(":")) {
			line = reader.readLine();
			// System.out.println(line);
		}
		double users;

		for (int i = 0; i < numJobs; i++) {
			bufferStr = line.split("\\s+");
			String x = bufferStr[2].replaceAll("\\s+", "");
			ncontainers = Math.round(Float.parseFloat(x));
			SolutionPerJob solPerJob = sol.getSolutionPerJob(i);
			solPerJob.setNumberVM(ncontainers / numCores.get(i));
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
		for (int i = 0; i < numJobs; i++) {
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
