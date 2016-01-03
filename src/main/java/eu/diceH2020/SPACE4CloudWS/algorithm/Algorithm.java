package eu.diceH2020.SPACE4CloudWS.algorithm;

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
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.AsyncResult;
import org.springframework.stereotype.Component;

import eu.diceH2020.SPACE4CloudWS.data.Format;
import eu.diceH2020.SPACE4CloudWS.service.DataService;
import eu.diceH2020.SPACE4CloudWS.solvers.MINLPSolver;
import eu.diceH2020.SPACE4CloudWS.solvers.SPNSolver;
import eu.diceH2020.SPACE4Cloud_shared.InstanceData;
import eu.diceH2020.SPACE4Cloud_shared.Settings;

@Component
public class Algorithm {

	private static Logger logger = Logger.getLogger(Algorithm.class.getName());

	private List<Double> alfa = new ArrayList<>();
	private List<Integer> lstIndexBest = new ArrayList<>();
	private List<Double> beta = new ArrayList<>();
	@Value("${settings.cycles:100}")
	private int cycles;
	@Autowired
	DataService dataService;

	private List<Double> deltaBar;
	private File fileShared1;
	private File fileShared2;
	private File fileSharedDef;
	private File fileSharedNet;
	private List<List<String>> matrixNamesDatFiles;
	private List<List<String>> matrixNamesSolFiles;

	@Autowired
	MINLPSolver minlpSolver;

	private List<Integer> numberOfUsers = new ArrayList<>();
	private List<Integer> numContainers = new ArrayList<>();
	private int numJobs;
	private double[] numOnDemandVM;
	private double[] numReservedVM;
	private double[] numSpotVM;
	private int numTypeVM;

	private List<Double> rhoBar;

	private List<String> selectedVMtypes = new ArrayList<>();

	private List<Double> sigmaBar;

	private List<Integer> singleCore = new ArrayList<Integer>();

	@Autowired(required = true)
	SPNSolver SPNSolver;

	private Solution solution;

	public Algorithm() {
		this.fileShared1 = new File("MR-Yarn2Pcopy.def");
		this.fileShared2 = new File("MR-Yarn2Pcopy.net");
		this.fileSharedDef = new File("MR-Yarn2P.def");
		this.fileSharedNet = new File("MR-Yarn2P.net");
	}

	public double calculateResponseTime(double throughput, int numServers, double thinktime) {
		return numServers / throughput - thinktime;
	}

	private double evaluateCostPerJob(int i) {
		double deltaBar = dataService.getDeltaBar(selectedVMtypes.get(i));
		double rhoBar = dataService.getRhoBar(selectedVMtypes.get(i));
		double sigmaBar = dataService.getSigmaBar(selectedVMtypes.get(i));

		double cost = deltaBar * numOnDemandVM[i] + rhoBar * numReservedVM[i] + sigmaBar * numSpotVM[i]
				+ (alfa.get(i) / numberOfUsers.get(i) - beta.get(i));
		return cost;
	}

	private void calculateNumVMs(int i) {
		double N = dataService.getData().getN(i);
		double R = dataService.getData().getR(i);
		double ratio = numContainers.get(i) / singleCore.get(i);
		numSpotVM[i] = N * ratio;
		logger.info("The on spot VMs are " + numSpotVM[i]);
		numReservedVM[i] = Math.min(R, (ratio) * (1 - N));
		logger.info("The reserved VMs are " + numReservedVM[i]);
		numOnDemandVM[i] = Math.max(0, ratio - numSpotVM[i] - numReservedVM[i]);
		logger.info("The on demand  VMs are " + numOnDemandVM[i]);
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

	//@Async
	public void greedy_cal() throws Exception {

		List<Float> listResults = new ArrayList<Float>();
		Float result;
		int minIndex;
		lstIndexBest.clear();
		for (int i = 0; i < numJobs; i++) {
			for (int j = 0; j < numTypeVM; j++) {
				result = minlpSolver.run(matrixNamesDatFiles.get(i).get(j), matrixNamesSolFiles.get(i).get(j));
				listResults.add(result);
			}
			minIndex = listResults.indexOf(Collections.min(listResults));
			lstIndexBest.add(minIndex);
			selectedVMtypes.add(i, dataService.getData().getTypeVm(minIndex));
			this.solution.setTypeVMselected(i, selectedVMtypes.get(i));

			logger.info("For job class " + dataService.getData().getId_job(i) + " has been selected the machine "
					+ selectedVMtypes.get(i));
			listResults.clear();
		}

		singleCore = dataService.getUpdatedCores(selectedVMtypes);
		Format.createDataFile(lstIndexBest, dataService.getData(), "data.dat", sigmaBar, deltaBar, rhoBar, singleCore);

		minlpSolver.run("data.dat", "result1.sol");

		logger.info("Num. cores of the first class is: " + singleCore.get(0) + " Num. cores of the second class is: "
				+ singleCore.get(1));
		updateWithFinalValues("result1.sol");

	}

	public void init() throws IOException {

		numJobs = dataService.getNumberJobs();
		numTypeVM = dataService.getNumberTypeVM();

		matrixNamesDatFiles = initMatrixNamesDatFiles(numJobs, numTypeVM);
		matrixNamesSolFiles = initMatrixNamesSolFiles(numJobs, numTypeVM);
		solution = new Solution(numJobs);
		numReservedVM = new double[numJobs];
		numOnDemandVM = new double[numJobs];
		numSpotVM = new double[numJobs];
		sigmaBar = dataService.getLstSigmaBar();
		deltaBar = dataService.getLstDeltaBar();
		rhoBar = dataService.getLstRhoBar();
		Format.contructFile(dataService.getData(), matrixNamesDatFiles, numTypeVM, numJobs, sigmaBar, deltaBar, rhoBar,
				dataService.getMatrixJobCores());

	}

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

	@Async
	public Future<Float> localSearch(int i) throws Exception {
		double tempResponseTime = 0;
		double throughput = 0;
		int k1 = cycles;
		int k = cycles;
		int q = 0;

		create_SWN_ProfileR_Nef_File(numContainers.get(i), 1 / dataService.getData().getMavg(lstIndexBest.get(i)),
				1 / (dataService.getData().getRavg(lstIndexBest.get(i))
						+ dataService.getData().getSHtypavg(lstIndexBest.get(i))),
				1 / dataService.getData().getThink(i), i);
		create_SWN_ProfileR_Def_File(numberOfUsers.get(i), dataService.getData().getNM(i), dataService.getData().getNR(i), i);

		throughput = SPNSolver.run("SWN_ProfileR" + i, "fil" + i + ".sta");
		calculateNumVMs(i);

		double responseTime = calculateResponseTime(throughput, numberOfUsers.get(i),
				dataService.getData().getThink(i));

		if (responseTime < dataService.getData().getD(i)) {

			while (responseTime < dataService.getData().getD(i)
					&& numberOfUsers.get(i) < dataService.getData().getHUp(i) && q <= k + 1) {
				tempResponseTime = responseTime;

				numberOfUsers.set(i, numberOfUsers.get(i) + 1);

				create_SWN_ProfileR_Nef_File(numContainers.get(i), 1 / dataService.getData().getMavg(lstIndexBest.get(i)),
						1 / (dataService.getData().getRavg(lstIndexBest.get(i))
								+ dataService.getData().getSHtypavg(lstIndexBest.get(i))),
						1 / dataService.getData().getThink(i), i);

				create_SWN_ProfileR_Def_File(numberOfUsers.get(i), dataService.getData().getNM(i), dataService.getData().getNR(i), i);
				calculateNumVMs(i);
				throughput = SPNSolver.run("SWN_ProfileR" + i, "fil" + i + ".sta");
				responseTime = calculateResponseTime(throughput, numberOfUsers.get(i),
						dataService.getData().getThink(i));
				k--;
				q++;
			}
			q = 0;
			while (responseTime < dataService.getData().getD(i) && q <= k1 + 1 && numContainers.get(i) > 1) {
				k1--;
				tempResponseTime = responseTime;
				numContainers.set(i, numContainers.get(i) + 1);
				calculateNumVMs(i);
				create_SWN_ProfileR_Nef_File(numContainers.get(i), 1 / dataService.getData().getMavg(lstIndexBest.get(i)),
						1 / (dataService.getData().getRavg(lstIndexBest.get(i))
								+ dataService.getData().getSHtypavg(lstIndexBest.get(i))),
						1 / dataService.getData().getThink(i), i);
				create_SWN_ProfileR_Def_File(numberOfUsers.get(i), dataService.getData().getNM(i), dataService.getData().getNR(i), i);

				throughput = SPNSolver.run("SWN_ProfileR" + i, "fil" + i + ".sta");
				responseTime = calculateResponseTime(throughput, numberOfUsers.get(i),
						dataService.getData().getThink(i));
				q++;
			}

		} else {
			if (responseTime > dataService.getData().getD(i)) {
				q = 0;
				while (responseTime > dataService.getData().getD(i)) {
					numContainers.set(i, numContainers.get(i) + 1);
					calculateNumVMs(i);
					create_SWN_ProfileR_Nef_File(numContainers.get(i), 1 / dataService.getData().getMavg(lstIndexBest.get(i)),
							1 / (dataService.getData().getRavg(lstIndexBest.get(i))
									+ dataService.getData().getSHtypavg(lstIndexBest.get(i))),
							1 / dataService.getData().getThink(i), i);
					create_SWN_ProfileR_Def_File(numberOfUsers.get(i), dataService.getData().getNM(i), dataService.getData().getNR(i), i);

					throughput = SPNSolver.run("SWN_ProfileR" + i, "fil" + i + ".sta");

					responseTime = calculateResponseTime(throughput, numberOfUsers.get(i),
							dataService.getData().getThink(i));
					tempResponseTime = responseTime;
				}
			}
		}
		Thread.sleep(1000L);
		this.solution.setSimulated_time(i, tempResponseTime);
		this.solution.setNumUsers(i, numberOfUsers.get(i));
		this.solution.setNumber_vm(i, numContainers.get(i) / singleCore.get(i));
		return new AsyncResult<Float>((float) tempResponseTime);
	}

	public static void create_SWN_ProfileR_Def_File(int param1, int param2, int param3, int i) {
		String oldFileName = "SWN_ProfileR.def";
		String tmpFileName = "SWN_ProfileR" + i + ".def";

		BufferedReader bReader = null;
		BufferedWriter bw = null;
		try {
			bReader = new BufferedReader(new FileReader(oldFileName));
			bw = new BufferedWriter(new FileWriter(tmpFileName));
			String line;
			while ((line = bReader.readLine()) != null) {
				if (line.contains("param1"))
					line = line.replace("param1", String.valueOf(param1));
				if (line.contains("param2"))
					line = line.replace("param2", String.valueOf(param2));
				if (line.contains("param3"))
					line = line.replace("param3", String.valueOf(param3));
				bw.write(line + "\n");
			}
		} catch (Exception e) {
			return;
		} finally {
			try {
				if (bReader != null)
					bReader.close();
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

	}

	private static void create_SWN_ProfileR_Nef_File(int numContainers, double b, double c, double d, int i) {

		String oldFileName = "SWN_ProfileR.net";
		// this is the reference file. It has a set of placeholders that must be
		// filled

		String tmpFileName = "SWN_ProfileR" + i + ".net";

		BufferedReader br = null;
		BufferedWriter bw = null;
		try {
			br = new BufferedReader(new FileReader(oldFileName));
			bw = new BufferedWriter(new FileWriter(tmpFileName));
			String line;
			while ((line = br.readLine()) != null) {
				if (line.contains("param1"))
					line = line.replace("param1", String.valueOf(numContainers));
				if (line.contains("param2"))
					line = line.replace("param2", String.valueOf(b));
				if (line.contains("param3"))
					line = line.replace("param3", String.valueOf(c));
				if (line.contains("param4"))
					line = line.replace("param4", String.valueOf(d));
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

	}

	// send data of determinate job class
	public void modifinaledef(InstanceData data, int i) throws IOException {

		String oldFileName = "MR-Yarn2Pcopy.def";
		String tmpFileName = "MR-Yarn2P" + i + ".def";

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
					line = line.replace("job" + i, String.valueOf(numberOfUsers.get(i)));

				if (line.contains("paramp" + i))
					line = line.replace("paramp" + i, String.valueOf(numContainers.get(i)));

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

	public void modifinalenet(InstanceData apli, int i) throws IOException {

		String oldFileName = "MR-Yarn2Pcopy.net";
		String tmpFileName = "MR-Yarn2P" + i + ".net";

		BufferedReader br = null;
		BufferedWriter bw = null;

		try {
			br = new BufferedReader(new FileReader(oldFileName));
			bw = new BufferedWriter(new FileWriter(tmpFileName));
			String line;
			while ((line = br.readLine()) != null) {
				if (line.contains("ratemap" + i))
					line = line.replace("ratemap" + i, String.valueOf(1 / apli.getMavg(i)));

				if (line.contains("ratered" + i))
					line = line.replace("ratered" + i, String
							.valueOf(1 / (apli.getRavg(lstIndexBest.get(i)) + apli.getSHtypavg(lstIndexBest.get(i)))));

				if (line.contains("think" + i))
					line = line.replace("think" + i, String.valueOf(1 / apli.getThink(i)));

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

	public void sequentialGreedy() throws Exception {
		List<Float> listResults = new ArrayList<Float>();
		Float result;
		int minIndex;
		lstIndexBest.clear();
		for (int i = 0; i < numJobs; i++) {
			for (int j = 0; j < numTypeVM; j++) {
				result = minlpSolver.run(matrixNamesDatFiles.get(i).get(j), matrixNamesSolFiles.get(i).get(j));
				listResults.add(result);
			}
			minIndex = listResults.indexOf(Collections.min(listResults));
			lstIndexBest.add(minIndex);
			selectedVMtypes.add(i, dataService.getData().getTypeVm(minIndex));
			System.out.println(selectedVMtypes.get(i));
			solution.setTypeVMselected(i, selectedVMtypes.get(i));
			System.out.println(solution.getTypeVMselected(i));
			logger.info("For Job Class " + dataService.getData().getId_job(i) + " has been selected machine "
					+ selectedVMtypes.get(i));
			listResults.clear();
		}

		Format.createDataFile(lstIndexBest, dataService.getData(), "data.dat", sigmaBar, deltaBar, rhoBar, singleCore);

		minlpSolver.run("data.dat", "result1.sol");
		singleCore = dataService.getUpdatedCores(selectedVMtypes);
		updateWithFinalValues("result1.sol");
		for (int j = 0; j < numJobs; j++)
			sequentialLS(j);
	}

	public double sequentialLS(int i) throws Exception {
		InstanceData data = dataService.getData();
		double tempSystemTime = 0;
		int k1 = cycles;
		int k = cycles;
		int q = 0;
		double systemTime = 0;

		calculateNumVMs(i);

		create_SWN_ProfileR_Nef_File(numContainers.get(i), 1 / data.getMavg(lstIndexBest.get(i)),
				1 / data.getRavg(lstIndexBest.get(i)) + 1 / data.getSHtypavg(lstIndexBest.get(i)), 1 / data.getThink(i),
				i);
		create_SWN_ProfileR_Def_File(numberOfUsers.get(i), data.getNM(i), data.getNR(i), i);
		double throughput = SPNSolver.run("SWN_ProfileR" + i, "fil" + i + ".sta");

		systemTime = calculateResponseTime(throughput, numberOfUsers.get(i), data.getThink(i));

		if (systemTime < data.getD(i)) {

			while (systemTime < data.getD(i) && numberOfUsers.get(i) < data.getHUp(i) && q <= k) {
				tempSystemTime = systemTime;
				numberOfUsers.set(i, numberOfUsers.get(i) + 1);
				create_SWN_ProfileR_Nef_File(numContainers.get(i), 1 / data.getMavg(lstIndexBest.get(i)),
						1 / data.getRavg(lstIndexBest.get(i)) + 1 / data.getSHtypavg(lstIndexBest.get(i)),
						1 / data.getThink(i), i);

				create_SWN_ProfileR_Def_File(numberOfUsers.get(i), data.getNM(i), data.getNR(i), i);
				throughput = SPNSolver.run("SWN_ProfileR" + i, "fil" + i + ".sta");
				systemTime = calculateResponseTime(throughput, numberOfUsers.get(i), data.getThink(i));
				k--;
				q++;
			}
			q = 0;
			while (systemTime < data.getD(i) && q <= k1 && numContainers.get(i) > 1) {
				k1--;
				tempSystemTime = systemTime;
				numContainers.set(i, numContainers.get(i) + 1);
				calculateNumVMs(i);
				logger.info("il tipo di vm e:" + selectedVMtypes.get(i));
				create_SWN_ProfileR_Nef_File(numContainers.get(i), 1 / data.getMavg(lstIndexBest.get(i)),
						1 / data.getRavg(lstIndexBest.get(i)) + 1 / data.getSHtypavg(lstIndexBest.get(i)),
						1 / data.getThink(i), i);
				create_SWN_ProfileR_Def_File(numberOfUsers.get(i), data.getNM(i), data.getNR(i), i);
				throughput = SPNSolver.run("SWN_ProfileR" + i, "fil" + i + ".sta");
				systemTime = calculateResponseTime(throughput, numberOfUsers.get(i), data.getThink(i));
				q++;
			}

		} else {
			if (systemTime > data.getD(i)) {
				int k3 = cycles;
				q = 0;
				while (systemTime > data.getD(i) && q <= k3) {
					k3--;
					numContainers.set(i, numContainers.get(i) + 1);
					calculateNumVMs(i);
					create_SWN_ProfileR_Nef_File(numContainers.get(i), 1 / data.getMavg(lstIndexBest.get(i)),
							1 / data.getRavg(lstIndexBest.get(i)) + 1 / data.getSHtypavg(lstIndexBest.get(i)),
							1 / data.getThink(i), i);
					create_SWN_ProfileR_Def_File(numberOfUsers.get(i), data.getNM(i), data.getNR(i), i);
					throughput = SPNSolver.run("SWN_ProfileR" + i, "fil" + i + ".sta");
					systemTime = calculateResponseTime(throughput, numberOfUsers.get(i), data.getThink(i));
					tempSystemTime = systemTime;
					q++;
				}

			}
		}
		solution.setSimulated_time(i, tempSystemTime);
		solution.setNumUsers(i, numberOfUsers.get(i));
		solution.setNumber_vm(i, numContainers.get(i) / singleCore.get(i));
		return tempSystemTime;

	}

	public List<Double> sharedCluster() throws Exception {

		InstanceData data = dataService.getData();
		List<Double> tempArray = new ArrayList<>();
		double cost = 0;
		List<Double> arrayThroughput = new ArrayList<>(3);
		List<Double> arrayResponseTime = new ArrayList<>();
		int countIter = 0;
		for (int i = 0; i < numJobs; i++) {
			calculateNumVMs(i);
			cost = cost + evaluateCostPerJob(i);
		}

		FileUtils.copyFile(fileSharedNet, fileShared2);
		FileUtils.copyFile(fileSharedDef, fileShared1);

		for (int i = 0; i < numJobs; i++) {
			modifinalenet(data, i);
			modifinaledef(data, i);
		}

		long startTime = System.currentTimeMillis();
		arrayThroughput = SPNSolver.run2Classes("MR-Yarn2Pcopy", "resulfinale.sta");
		long runTimeMillis = (System.currentTimeMillis() - startTime);
		double runTime = runTimeMillis / 1000.0;

		for (int i = 0; i < numJobs; i++) {
			arrayResponseTime
					.add(calculateResponseTime(arrayThroughput.get(i), numberOfUsers.get(i), data.getThink(i)));
			logger.info("Throughput of class " + i + " is: " + arrayResponseTime.get(i));
		}

		logger.info("The initial runtime time is :" + runTime);
		cost = 0;
		for (int i = 0; i < numJobs; i++) {
			calculateNumVMs(i);
			cost = cost + evaluateCostPerJob(i);
		}

		logger.info("il costo  iniziale 2  e:" + cost);
		Collections.copy(tempArray, arrayResponseTime);
		for (int i = 0; i < numJobs; i++) {
			if (arrayResponseTime.get(i) > data.getD(i)) {
				while (arrayResponseTime.get(i) > data.getD(i)) {
					numContainers.set(i, numContainers.get(i) + 1);

					FileUtils.copyFile(fileSharedDef, fileShared1);

					for (int j = 0; j < numJobs; j++)
						modifinaledef(data, j);

					startTime = System.currentTimeMillis();
					arrayThroughput = SPNSolver.run2Classes("MR-Yarn2Pcopy", "resulfinale.sta");
					runTimeMillis = (System.currentTimeMillis() - startTime);
					runTime = runTimeMillis / 1000.0;
					arrayResponseTime.clear();
					for (int j = 0; j < numJobs; j++) {
						arrayResponseTime.add(
								calculateResponseTime(arrayThroughput.get(j), numberOfUsers.get(j), data.getThink(j)));
						logger.info("shared cluster il througput della classe" + j + " e: " + arrayThroughput.get(j));
					}
					logger.info("The runtime time is :" + runTime);
					logger.info("The simulated response time for class " + i + " is: " + arrayResponseTime.get(i)
							+ " the deadline is: " + data.getD(i));
					Collections.copy(tempArray, arrayResponseTime);
					for (int j = 0; j < numJobs; j++) {
						calculateNumVMs(j);
						cost = cost + evaluateCostPerJob(j);
					}

					logger.info("the cost of iteration " + countIter + "  is: " + cost);
					countIter = countIter + 1;
				}

			}
		}
		for (int j = 0; j < numJobs; j++) {
			solution.setSimulated_time(j, tempArray.get(j));
			solution.setNumUsers(j, numberOfUsers.get(j));
			solution.setNumber_vm(j, numContainers.get(j) / singleCore.get(j)); // TODO
			// check
			// this
		}

		return tempArray;
	}

	// read result.sol and take number of containers and users
	private void updateWithFinalValues(String nameSolFile) throws IOException {
		List<Double> simulationTimes = new ArrayList<>(numJobs);
		int ncontainers = 0;
		int nusers;
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

		for (int i = 0; i < dataService.getNumberJobs(); i++) {
			line = reader.readLine();
			bufferStr = line.split("\\s+");
			System.out.println(bufferStr[1]);
			String localStr = bufferStr[1].replaceAll("\\s+", "");
			simulationTimes.add(Double.parseDouble(localStr));
		}
		while (!line.contains("Variables")) {
			line = reader.readLine();
			System.out.println(line);
		}

		line = reader.readLine();
		System.out.println(line);
		double users;
		numContainers.clear();
		numberOfUsers.clear();
		for (int i = 0; i < numJobs; i++) {
			line = reader.readLine();
			bufferStr = line.split("\\s+");
			String x = bufferStr[2].replaceAll("\\s+", "");
			ncontainers = Integer.parseInt(x);
			numContainers.add(ncontainers * singleCore.get(i));
			logger.info("il numero di vm per il job " + i + "e " + ncontainers);
			x = bufferStr[5].replaceAll("\\s+", "");
			System.out.println(x);
			users = Double.parseDouble(x);
			users = 1 / users;
			nusers = (int) users;
			System.out.println(nusers);
			numberOfUsers.add(nusers);
			logger.info("Number of users for job " + i + "is " + numberOfUsers.get(i));
		}

		while (!line.contains("### alphabeta"))
			line = reader.readLine();

		line = reader.readLine();

		for (int i = 0; i < dataService.getNumberJobs(); i++) {
			line = reader.readLine();
			bufferStr = line.split("\\s+");

			String x = bufferStr[2].replaceAll("\\s+", "");
			alfa.add(Double.parseDouble(x));

			x = bufferStr[3].replaceAll("\\s+", "");

			beta.add(Double.parseDouble(x));

		}
		reader.close();

	}
}
