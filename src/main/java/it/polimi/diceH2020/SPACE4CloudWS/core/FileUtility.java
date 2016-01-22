package it.polimi.diceH2020.SPACE4CloudWS.core;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import org.apache.log4j.Logger;

import it.polimi.diceH2020.SPACE4Cloud.shared.InstanceData;

public class FileUtility {

	private static final String RESULTS_SOLUTION = "../results/solution";
	private static final String SCRATCH_DATA_DAT = "../scratch/data.dat";
	public static final String LOCAL_DYNAMIC_FOLDER = "TempWorkingDir";
	private static Logger logger = Logger.getLogger(FileUtility.class.getName());

	public static void createWorkingDir() throws IOException {

		Path folder = Paths.get(LOCAL_DYNAMIC_FOLDER);
		Files.createDirectories(folder);
		logger.info(LOCAL_DYNAMIC_FOLDER+" created.");
	}
	
	public static void destroyWorkingDir() throws IOException{
		Path folder = Paths.get(LOCAL_DYNAMIC_FOLDER);
		Files.deleteIfExists(folder);
	}

	public static void constructFile(InstanceData appli, List<List<String>> matrixNamesDatFiles, List<Double> sigmaBar,
									 List<Double> deltaBar, List<Double> rhoBar, List<List<Integer>> matrixCorePerJob) throws IOException {

		int[] vettHUp = appli.getHUp();
		int[] b = appli.getHLow();
		double[] c = appli.getJob_penalty();
		int[] d = appli.getNM();
		int[] e = appli.getNR();
		double[] f = appli.getD();
		double[] g = appli.getMmax();
		double[] k = appli.getRmax();
		double[] ka = appli.getSH1max();
		double[] kd = appli.getSHtypmax();
		double[] h1 = appli.getSHtypavg();
		double[] h2 = appli.getMavg();
		double[] h3 = appli.getRavg();
		int[][] kc = appli.getcM();
		int[][] ke = appli.getcR();
		double[] r = appli.getR();
		double[] n = appli.getEta();
		int jobNumber = appli.getNumberJobs();
		int vmNumber = appli.getNumberTypeVM();
		StringBuilder builder = new StringBuilder();
		for (int i = 0; i < jobNumber; i++) {
			for (int j = 0; j < vmNumber; j++) {

				builder.append("param nAM:= 1;\n");
				builder.append("param Gamma:=" + appli.getGamma() + ";\n");
				builder.append("param HUp := \n" + 1 + " " + vettHUp[i] + ";\n");
				builder.append("param HLow := \n" + 1 + " " + b[i] + ";\n");
				builder.append("param cM :=\n" + 1 + " " + kc[i][j] + ";\n");
				builder.append("param cR :=\n" + 1 + " " + ke[i][j] + ";\n");
				builder.append("param job_penalty :=\n" + 1 + " " + c[i] + ";\n");
				builder.append("param NM :=\n" + 1 + " " + d[i] + ";\n");
				builder.append("param NR :=\n" + 1 + " " + e[i] + ";\n");
				builder.append("param Mmax :=\n" + 1 + " " + g[j] + ";\n");
				builder.append("param Rmax :=\n" + 1 + " " + k[j] + ";\n");
				builder.append("param w :=\n" + 1 + " " + matrixCorePerJob.get(i).get(j) + ";\n");
				builder.append("param D :=\n" + 1 + " " + f[i] + ";\n");
				builder.append("param SH1max :=\n" + 1 + " " + ka[j] + ";\n");
				builder.append("param SHtypmax :=\n" + 1 + " " + kd[j] + ";\n");
				builder.append("param R :=\n" + 1 + " " + r[j] + ";\n");
				builder.append("param eta :=\n" + 1 + " " + n[i] + ";\n");
				builder.append("param Mavg :=\n" + 1 + " " + h2[j] + ";\n");
				builder.append("param sigmabar :=\n" + 1 + " " + sigmaBar.get(j) + ";\n");
				builder.append("param deltabar :=\n" + 1 + " " + deltaBar.get(j) + ";\n");
				builder.append("param rhobar :=\n" + 1 + " " + rhoBar.get(j) + ";\n");
				builder.append("param Ravg :=\n" + 1 + " " + h3[j] + ";\n");
				builder.append("param SHtypavg :=\n" + 1 + " " + h1[j] + ";\n");

				String filename = LOCAL_DYNAMIC_FOLDER +File.separator+ matrixNamesDatFiles.get(i).get(j);
				File file = new File(filename);
				if (!file.exists())
					file.createNewFile();
				try (PrintWriter out = new PrintWriter(file)) {
					out.println(builder.toString());
					out.flush();
					logger.info("File " + matrixNamesDatFiles.get(i).get(j) + " has been created");
				} catch (Exception ex) {
					ex.printStackTrace();
				}
				builder.setLength(0);

			}
		}
	}

	public static String createFullModelFile(List<Integer> lstIndexBest, InstanceData data, String fileName,
			List<Double> sigmaBar, List<Double> deltaBar, List<Double> rhoBar, List<Integer> singleCore) {
		StringBuilder builder = new StringBuilder();
		int numJob = data.getNumberJobs();

		builder.append("param nAM:=" + numJob + ";\n");

		builder.append("param Gamma:=" + data.getGamma() + ";\n");
		builder.append("param: HUp := \n");
		for (int i = 0; i < numJob; i++)
			builder.append(+i + 1 + " " + data.getHUp(i) + "\n");

		builder.append(";\n");
		builder.append("param: HLow := \n");
		for (int i = 0; i < numJob; i++)
			builder.append(+i + 1 + " " + data.getHLow(i) + "\n");
		builder.append(";\n");

		builder.append("param: cM :=\n");
		for (int i = 0; i < numJob; i++)
			builder.append(+i + 1 + " " + data.getcM(i, lstIndexBest.get(i)) + "\n");

		builder.append(";\n");
		builder.append("param: cR :=\n");
		for (int i = 0; i < numJob; i++)
			builder.append(+i + 1 + " " + data.getcR(i, lstIndexBest.get(i)) + "\n");

		builder.append(";\n");
		builder.append("param: job_penalty :=\n");
		for (int i = 0; i < numJob; i++)
			builder.append(+i + 1 + " " + data.getJob_penalty(i) + "\n");
		builder.append(";\n");
		builder.append("param: NM :=\n");

		for (int i = 0; i < numJob; i++)
			builder.append(+i + 1 + " " + data.getNM(i) + "\n");
		builder.append(";\n");
		builder.append("param: NR :=\n");
		for (int i = 0; i < numJob; i++)
			builder.append(+i + 1 + " " + data.getNR(i) + "\n");

		builder.append(";\n");
		builder.append("param: Mmax :=\n");
		for (int i = 0; i < numJob; i++)
			builder.append(+i + 1 + " " + data.getMmax(lstIndexBest.get(i)) + "\n");

		builder.append(";\n");
		builder.append("param: Rmax :=\n");
		for (int i = 0; i < numJob; i++)
			builder.append(+i + 1 + " " + data.getRmax(lstIndexBest.get(i)) + "\n");

		builder.append(";\n");
		builder.append("param: w := \n");
		for (int i = 0; i < numJob; i++)
			builder.append(+i + 1 + " " + singleCore.get(i) + "\n");
		builder.append(";\n");
		builder.append("param D :=\n");
		for (int i = 0; i < numJob; i++)
			builder.append(+i + 1 + " " + data.getD(i) + "\n");
		builder.append(";\n");
		builder.append("param: SH1max :=\n");
		for (int i = 0; i < numJob; i++)
			builder.append(+i + 1 + " " + data.getSH1max(lstIndexBest.get(i)) + "\n");

		builder.append(";\n");
		builder.append("param: SHtypmax :=\n");
		for (int i = 0; i < numJob; i++)
			builder.append(+i + 1 + " " + data.getSHtypmax(lstIndexBest.get(i)) + "\n");

		builder.append(";\n");
		builder.append("param: R :=\n");
		for (int i = 0; i < numJob; i++)
			builder.append(+i + 1 + " " + data.getR(lstIndexBest.get(i)) + "\n");

		builder.append(";\n");
		builder.append("param: eta :=\n");
		for (int i = 0; i < numJob; i++)
			builder.append(+i + 1 + " " + data.getEta(i) + "\n");
		builder.append(";\n");
		builder.append("param: Mavg :=\n");
		for (int i = 0; i < numJob; i++)
			builder.append(+i + 1 + " " + data.getMavg(lstIndexBest.get(i)) + "\n");

		builder.append(";\n");
		builder.append("param: sigmabar :=\n");
		for (int i = 0; i < numJob; i++)
			builder.append(+i + 1 + " " + sigmaBar.get(lstIndexBest.get(i)) + "\n");

		builder.append(";\n");
		builder.append("param: deltabar :=\n");
		for (int i = 0; i < numJob; i++)
			builder.append(+i + 1 + " " + deltaBar.get(lstIndexBest.get(i)) + "\n");

		builder.append(";\n");
		builder.append("param: rhobar :=\n");
		for (int i = 0; i < numJob; i++)
			builder.append(+i + 1 + " " + rhoBar.get(lstIndexBest.get(i)) + "\n");

		builder.append(";\n");
		builder.append("param: Ravg :=\n");
		for (int i = 0; i < numJob; i++)
			builder.append(+i + 1 + " " + data.getRavg(lstIndexBest.get(i)) + "\n");

		builder.append(";\n");
		builder.append("param: SHtypavg :=\n");
		for (int i = 0; i < numJob; i++)
			builder.append(+i + 1 + " " + data.getSHtypavg(lstIndexBest.get(i)) + "\n");

		builder.append(";\n");
		String fName = LOCAL_DYNAMIC_FOLDER +File.separator+ fileName;
		File file = new File(fName);

		try (PrintWriter out = new PrintWriter(file)) {
			out.println(builder.toString());
			out.flush();
			logger.info("File "+fileName+" has been created");
		} catch (Exception e) {
			e.printStackTrace();
		}

		builder.setLength(0);
		return fileName;

	}

	
	public static String generateRunFile(String solverPath) {

		String data = SCRATCH_DATA_DAT;
		String result = RESULTS_SOLUTION;

		StringBuilder builder = new StringBuilder();
		builder.append("reset;\n");

		builder.append("option solver \"" + solverPath + "\";\n");

		builder.append("model ../problems/model.mod;\n");
		builder.append("data " + data + ";\n");

		builder.append("option randseed 1;\n");
		builder.append("include ../utils/compute_psi.run;\n");
		builder.append("include ../utils/compute_job_profile.run;\n");
		builder.append("include ../utils/compute_penalties.run;\n");

		builder.append("include ../utils/save_aux.run;\n");
		builder.append("let outfile := \"" + result + "\";\n");

		builder.append("include ../problems/centralized.run;\n");
		builder.append("solve centralized_prob;\n");

		builder.append("include ../utils/compute_s_d.run;\n");
		builder.append("include ../solve/AM_closed_form.run;\n");
		builder.append("include ../utils/post_processing.run;\n");

		builder.append("include ../utils/save_centralized.run;\n");

		String filename = LOCAL_DYNAMIC_FOLDER +File.separator+ "dat.run";
		File file = new File(filename);

		// generating the file
		try (PrintWriter out = new PrintWriter(file)) {
			out.println(builder.toString());
			out.flush();
			logger.info("File dat.run has been created");
		} catch (Exception e) {
			e.printStackTrace();
		}

		return "dat.run";
	}
	
	
	
	public static void createPNNetFile(int numContainers, double b, double c, double d, int i) {

		String oldFileName = LOCAL_DYNAMIC_FOLDER+File.separator+"SWN_ProfileR.net";
		// this is the reference file. It has a set of placeholders that must be
		// filled

		String tmpFileName = LOCAL_DYNAMIC_FOLDER+File.separator+"SWN_ProfileR" + i + ".net";

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

	public static void createPNDefFile(int param1, int param2, int param3, int i) {
		String oldFileName = LOCAL_DYNAMIC_FOLDER+File.separator+"SWN_ProfileR.def";
		String tmpFileName = LOCAL_DYNAMIC_FOLDER+File.separator+"SWN_ProfileR" + i + ".def";

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

	public static String createLocalSolFile(String nameSolFile) throws IOException {
		String solFilePath = FileUtility.LOCAL_DYNAMIC_FOLDER+"/"+nameSolFile;
		File file = new File(solFilePath);
		if (!file.exists())
			file.createNewFile();
		return solFilePath;
	}

}
