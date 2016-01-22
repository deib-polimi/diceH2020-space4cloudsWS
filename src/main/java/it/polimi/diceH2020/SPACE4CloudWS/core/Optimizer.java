package it.polimi.diceH2020.SPACE4CloudWS.core;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Future;

import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import it.polimi.diceH2020.SPACE4Cloud.shared.InstanceData;
import it.polimi.diceH2020.SPACE4Cloud.shared.Settings;
import it.polimi.diceH2020.SPACE4Cloud.shared.Solution;
import it.polimi.diceH2020.SPACE4Cloud.shared.SolutionPerJob;
import it.polimi.diceH2020.SPACE4CloudWS.services.DataService;
import it.polimi.diceH2020.SPACE4CloudWS.solvers.SPNSolver;

@Component
public class Optimizer {

	private static Logger logger = Logger.getLogger(Optimizer.class.getName());

	@Value("${settings.cycles:100}")
	private int cycles;

	@Autowired
	private Evaluator evaluator;

	@Autowired
	private LocalSearch localSearch;

	private File fileShared1;
	private File fileShared2;
	private File fileSharedDef;
	private File fileSharedNet;

	private int numJobs;
	private Solution solution;

	@Autowired
	DataService dataService;

	@Autowired(required = true)
	SPNSolver SPNSolver;

	public Optimizer() {
		this.fileShared1 = new File("MR-Yarn2Pcopy.def");
		this.fileShared2 = new File("MR-Yarn2Pcopy.net");
		this.fileSharedDef = new File("MR-Yarn2P.def");
		this.fileSharedNet = new File("MR-Yarn2P.net");
	}

	public static double calculateResponseTime(double throughput, int numServers, double thinkTime) {
		return (double) numServers / throughput - thinkTime;
	}

	// read an input file and type value of accuracy and cycles
	public void extractAccuracyAndCycle(Settings settings) {
		SPNSolver.setAccuracy(settings.getAccuracy());
		this.cycles = settings.getCycles();
	}

	/**
	 * @return the solution
	 */
	public Solution getSolution() {
		return this.solution;
	}

	public void init(Solution sol) throws IOException {

		numJobs = dataService.getNumberJobs();
		solution = sol;
	}

	// send data of determinate job class
	public void modifinaledef(int i) throws IOException {

		InstanceData data = dataService.getData();
		String oldFileName = "MR-Yarn2Pcopy.def";
		String tmpFileName = "MR-Yarn2P" + i + ".def";

		int nUsers = solution.getLstSolutions().get(i).getNumberUsers();
		int nContainers = solution.getLstSolutions().get(i).getNumberContainers();
		BufferedReader br = null;
		BufferedWriter bw = null;
		try {
			br = new BufferedReader(new FileReader(oldFileName));
			bw = new BufferedWriter(new FileWriter(tmpFileName));
			String line;
			while ((line = br.readLine()) != null) {
				if (line.contains("paramr" + i))
					line = line.replace("paramr" + i, String.valueOf(data.getNR(i)));

				if (line.contains("paramm" + i))
					line = line.replace("paramm" + i, String.valueOf(data.getNM(i)));

				if (line.contains("job" + i))
					line = line.replace("job" + i, String.valueOf(nUsers));

				if (line.contains("paramp" + i))
					line = line.replace("paramp" + i, String.valueOf(nContainers));

				bw.write(line + "\n");
			}
		} catch (Exception e) {
			return;
		} finally {
			try {
				if (br != null)
					br.close();
			} catch (IOException e) {
				//
			}
			try {
				if (bw != null)
					bw.close();
			} catch (IOException e) {
				//
			}
			File share = new File("MR-Yarn2P" + i + ".def");
			FileUtils.copyFile(share, fileShared1);
		}
	}

	public List<Double> sharedCluster() throws Exception {

		InstanceData data = dataService.getData();

		List<Double> tempArray = new ArrayList<>();

		List<Double> arrayThroughput = new ArrayList<>(3);
		List<Double> arrayResponseTime = new ArrayList<>();
		int countIter = 0;

		double cost = evaluator.evaluate(solution);

		FileUtils.copyFile(fileSharedNet, fileShared2);
		FileUtils.copyFile(fileSharedDef, fileShared1);

		for (int i = 0; i < numJobs; i++) {
			modifinalenet(i);
			modifinaledef(i);
		}

		long startTime = System.currentTimeMillis();
		arrayThroughput = SPNSolver.run2Classes("MR-Yarn2Pcopy", "resulfinale.sta");
		long runTimeMillis = (System.currentTimeMillis() - startTime);
		double runTime = runTimeMillis / 1000.0;

		for (int i = 0; i < numJobs; i++) {
			SolutionPerJob solPerJob = solution.getSolutionPerJob(i);
			int nUsers = solPerJob.getNumberUsers();
			double think = data.getThink(i); // TODO move think in solPerJob
			arrayResponseTime.add(calculateResponseTime(arrayThroughput.get(i), nUsers, think));
		}

		logger.info("The initial runtime time is :" + runTime);
		cost = evaluator.evaluate(solution);

		logger.info("il costo  iniziale 2  e:" + cost);
		Collections.copy(tempArray, arrayResponseTime);
		for (int i = 0; i < numJobs; i++) {
			SolutionPerJob solPerJob = solution.getSolutionPerJob(i);
			double deadline = data.getD(i); // TODO move deadline inside
											// solPerJob
			int nContainers = solPerJob.getNumberContainers();
			if (arrayResponseTime.get(i) > deadline) {
				while (arrayResponseTime.get(i) > deadline) {
					nContainers++;
					FileUtils.copyFile(fileSharedDef, fileShared1);

					for (int j = 0; j < numJobs; j++)
						modifinaledef(j);

					startTime = System.currentTimeMillis();
					arrayThroughput = SPNSolver.run2Classes("MR-Yarn2Pcopy", "resulfinale.sta");
					runTimeMillis = (System.currentTimeMillis() - startTime);
					runTime = runTimeMillis / 1000.0;
					arrayResponseTime.clear();
					for (int j = 0; j < numJobs; j++) {
						SolutionPerJob solPerJob2 = solution.getSolutionPerJob(j);
						int nUsers = solPerJob2.getNumberUsers();
						double think = data.getThink(j);
						arrayResponseTime.add(calculateResponseTime(arrayThroughput.get(j), nUsers, think));
					}
					Collections.copy(tempArray, arrayResponseTime);
					cost = evaluator.evaluate(solution);
					logger.info("the cost of iteration " + countIter + "  is: " + cost);
					countIter = countIter + 1;
				}

			}
			solPerJob.setNumberContainers(nContainers);
			solPerJob.setNumberVM(nContainers / solPerJob.getNumCores()); // TODO
			// check

		}
		for (int j = 0; j < numJobs; j++)
			solution.getLstSolutions().get(j).setSimulatedTime(tempArray.get(j));

		return tempArray;
	}

	private void modifinalenet(int i) throws IOException {

		InstanceData data = dataService.getData();
		String oldFileName = "MR-Yarn2Pcopy.net";
		String tmpFileName = "MR-Yarn2P" + i + ".net";
		int index = solution.getIdxVmTypeSelected().get(i);
		double mAvg = data.getMavg(index);
		double rAvg = data.getRavg(index);
		double shTypAvg = data.getSHtypavg(index);
		double think = data.getThink(index);

		BufferedReader br = null;
		BufferedWriter bw = null;

		try {
			br = new BufferedReader(new FileReader(oldFileName));
			bw = new BufferedWriter(new FileWriter(tmpFileName));
			String line;
			while ((line = br.readLine()) != null) {
				if (line.contains("ratemap" + i))
					line = line.replace("ratemap" + i, String.valueOf(1 / mAvg));

				if (line.contains("ratered" + i))
					line = line.replace("ratered" + i, String.valueOf(1 / (rAvg + shTypAvg)));

				if (line.contains("think" + i))
					line = line.replace("think" + i, String.valueOf(1 / think));

				bw.write(line + "\n");
			}
		} catch (Exception e) {
			return;
		} finally {
			try {
				if (br != null)
					br.close();
			} catch (IOException e) {
				//
			}
			try {
				if (bw != null)
					bw.close();
			} catch (IOException e) {
				//
			}
		}

		File share1 = new File("MR-Yarn2P" + i + ".net");
		FileUtils.copyFile(share1, fileShared2);
	}

	public void parallelLocalSearch() throws Exception {
		List<Future<Float>> objectives = new ArrayList<Future<Float>>();

		for (SolutionPerJob solPerJob : solution.getLstSolutions()) {
			objectives.add(localSearch.execute(solPerJob, cycles));
		}

		int i = 1;
		for (Future<Float> obj : objectives) {
			while (!obj.isDone())
				Thread.sleep(1000);
			logger.info("Local search num " + i + " finished");
			i++;
		}
		objectives.clear();

	}

	public void sequentialLS() {

		int i = 1;
		for (SolutionPerJob solPerJob : solution.getLstSolutions()) {
			Future<Float> objective;
			try {
				objective = localSearch.execute(solPerJob, cycles);

				while (!objective.isDone())
					Thread.sleep(1000);
				logger.info("Local search num " + i + " finished");
				i++;
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

	}

}
