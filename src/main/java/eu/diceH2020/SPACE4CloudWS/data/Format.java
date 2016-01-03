package eu.diceH2020.SPACE4CloudWS.data;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;

import eu.diceH2020.SPACE4Cloud_shared.InstanceData;


public class Format {

	public static void contructFile(InstanceData appli, List<List<String>> matrixNamesDatFiles, int vmNumber,  int jobNumber,
			List<Double> sigmaBar, List<Double> deltaBar, List<Double> rhoBar, List<List<Integer>> matrixCorePerJob) throws IOException {

		double[] h1;
		double[] h2;
		double[] h3;
		int[] vettHUp = new int[jobNumber];
		vettHUp = appli.getHUp();
		int[] b = new int[jobNumber];
		b = appli.getHLow();
		double[] c;
		c = new double[jobNumber];
		c = appli.getJob_penalty();
		int[] d;
		d = new int[jobNumber];
		d = appli.getNM();
		int[] e;
		e = new int[jobNumber];
		e = appli.getNR();
		double[] f;
		f = new double[jobNumber];
		f = appli.getD();
		double[] g;
		g = new double[vmNumber];
		g = appli.getMmax();
		double[] k = new double[vmNumber];
		k = appli.getRmax();
		double[] ka;
		ka = new double[vmNumber];
		ka = appli.getSH1max();
		double[] kd;
		kd = new double[vmNumber];
		kd = appli.getSHtypmax();
		h1 = new double[vmNumber];
		h1 = appli.getSHtypavg();
		h2 = new double[vmNumber];
		h2 = appli.getMavg();
		h3 = new double[vmNumber];
		h3 = appli.getRavg();
		int[][] kc;
		kc = new int[jobNumber][vmNumber];
		kc = appli.getcM();
		int[][] ke;
		ke = new int[jobNumber][vmNumber];
		ke = appli.getcR();
		double[] x;
		x = new double[vmNumber];
		x = appli.getR();
		double[] y;
		y = new double[jobNumber];
		y = appli.getN();
		System.out.println(vettHUp[1]);
		StringBuilder builder = new StringBuilder();
		for (int i = 0; i < jobNumber; i++) {
			for (int j = 0; j < vmNumber; j++) {

				builder.append("param nAM:= 1;\n");
				builder.append("param gamma:=" + appli.getGamma() + ";\n");
				builder.append("param HUp := \n" + 1 + " " + vettHUp[i] + ";\n");
				builder.append("param HLow := \n" + 1 + " " + b[i] + ";\n");
				builder.append("param cM :=\n" + 1 + " " + kc[i][j] + ";\n");
				builder.append("param cR :=\n" + 1 + " " + ke[i][j] + ";\n");
				builder.append("param job_penalty :=\n" + 1 + " " + c[i] + ";\n");
				builder.append("param NM :=\n" + 1 + " " + d[i] + ";\n");
				builder.append("param NR :=\n" + 1 + " " + e[i] + ";\n");
				builder.append("param Mmax :=\n" + 1 + " " + g[j] + ";\n");
				builder.append("param Rmax :=\n" + 1 + " " + k[j] + ";\n");
				builder.append("param core :=\n" + 1 + " " + matrixCorePerJob.get(i).get(j) + ";\n");
				builder.append("param D :=\n" + 1 + " " + f[i] + ";\n");
				builder.append("param SH1max :=\n" + 1 + " " + ka[j] + ";\n");
				builder.append("param SHtypmax :=\n" + 1 + " " + kd[j] + ";\n");
				builder.append("param R :=\n" + 1 + " " + x[j] + ";\n");
				builder.append("param N :=\n" + 1 + " " + y[i] + ";\n");
				builder.append("param Mavg :=\n" + 1 + " " + h2[j] + ";\n");

				builder.append("param sigmabar :=\n" + 1 + " " + sigmaBar.get(j) + ";\n");
				builder.append("param deltabar :=\n" + 1 + " " + deltaBar.get(j) + ";\n");
				builder.append("param rhobar :=\n" + 1 + " " + rhoBar.get(j) + ";\n");
				builder.append("param Ravg :=\n" + 1 + " " + h3[j] + ";\n");// +0.8
																			// *k[j]+";\n"
																			// );
				builder.append("param SHtypavg :=\n" + 1 + " " + h1[j] + ";\n");

				boolean beta = false;
				System.out.println("file has been created");
				File file = new File(matrixNamesDatFiles.get(i).get(j));
				System.out.println(matrixNamesDatFiles.get(i).get(j));

				if (!file.exists()) {
					beta = file.createNewFile();
				}
				if (beta)
					System.out.println("file creato");
				// logger.info(file.getAbsolutePath());
				System.out.println(file.getAbsolutePath());
				//Convert.convert(builder.toString(), file);
				//generating the file
				try (PrintWriter out = new PrintWriter(file)) {
					out.println(builder.toString());
					out.flush();
				} catch (Exception ex) {
					ex.printStackTrace();
				}
				
				
				System.out.println(file.getAbsolutePath());
				builder.setLength(0);

			}
		}
	}

	public static String createDataFile(List<Integer> lstIndexBest, InstanceData data, String x, List<Double> sigmaBar ,
			List<Double> deltaBar, List<Double> rhoBar, List<Integer> singleCore) {

		StringBuilder builder = new StringBuilder();
		builder.append("param nAM:=" + data.getNumberJobs() + ";\n");

		builder.append("param gamma:=" + data.getGamma() + ";\n");
		builder.append("param: HUp := \n");
		for (int i = 0; i < data.getNumberJobs(); i++)
			builder.append(+i + 1 + " " + data.getHUp(i) + "\n");
		builder.append(";\n");
		builder.append("param: HLow := \n");
		for (int i = 0; i < data.getNumberJobs(); i++)
			builder.append(+i + 1 + " " + data.getHLow(i) + "\n");
		builder.append(";\n");
		builder.append("param: cM :=\n");
		for (int i = 0; i < data.getNumberJobs(); i++) 
			builder.append(+i + 1 + " " + data.getcM(i,lstIndexBest.get(i)) + "\n");
		
		builder.append(";\n");
		builder.append("param: cR :=\n");
		for (int i = 0; i < data.getNumberJobs(); i++) 
			builder.append(+i + 1 + " " + data.getcR(i,lstIndexBest.get(i)) + "\n");

		builder.append(";\n");
		builder.append("param: job_penalty :=\n");
		for (int i = 0; i < data.getNumberJobs(); i++)
			builder.append(+i + 1 + " " + data.getJob_penalty(i)  + "\n");
		builder.append(";\n");
		builder.append("param: NM :=\n");
		for (int i = 0; i < data.getNumberJobs(); i++)
			builder.append(+i + 1 + " " + data.getNM(i) + "\n");
		builder.append(";\n");
		builder.append("param: NR :=\n");
		for (int i = 0; i < data.getNumberJobs(); i++)
			builder.append(+i + 1 + " " + data.getNR(i) + "\n");
		builder.append(";\n");
		builder.append("param: Mmax :=\n");
		for (int i = 0; i < data.getNumberJobs(); i++) {

			builder.append(+i + 1 + " " + data.getMmax(lstIndexBest.get(i)) + "\n");

		}
		builder.append(";\n");
		builder.append("param: Rmax :=\n");
		for (int i = 0; i < data.getNumberJobs(); i++) 
			builder.append(+i + 1 + " " + data.getRmax(lstIndexBest.get(i)) + "\n");

		builder.append(";\n");
		builder.append("param: core := \n");
		for (int i = 0; i < data.getNumberJobs(); i++)
			builder.append(+i + 1 + " " + singleCore.get(i) + "\n");
		builder.append(";\n");
		builder.append("param D :=\n");
		for (int i = 0; i < data.getNumberJobs(); i++)
			builder.append(+i + 1 + " " + data.getD(i) + "\n");
		builder.append(";\n");
		builder.append("param: SH1max :=\n");
		for (int i = 0; i < data.getNumberJobs(); i++) 
			builder.append(+i + 1 + " " + data.getSH1max(lstIndexBest.get(i)) + "\n");

		builder.append(";\n");
		builder.append("param: SHtypmax :=\n");
		for (int i = 0; i < data.getNumberJobs(); i++) {

			builder.append(+i + 1 + " " + data.getSHtypmax(lstIndexBest.get(i)) + "\n");

		}
		builder.append(";\n");
		builder.append("param: R :=\n");
		for (int i = 0; i < data.getNumberJobs(); i++) {

			builder.append(+i + 1 + " " + data.getR(lstIndexBest.get(i)) + "\n");

		}
		builder.append(";\n");
		builder.append("param: N :=\n");
		for (int i = 0; i < data.getNumberJobs(); i++)
			builder.append(+i + 1 + " " + data.getN(i) + "\n");
		builder.append(";\n");
		builder.append("param: Mavg :=\n");
		for (int i = 0; i < data.getNumberJobs(); i++) 
			builder.append(+i + 1 + " " + data.getMavg(lstIndexBest.get(i))  + "\n");

		builder.append(";\n");
		builder.append("param: sigmabar :=\n");
		for (int i = 0; i < data.getNumberJobs(); i++) {
			builder.append(+i + 1 + " " + sigmaBar.get(lstIndexBest.get(i)) + "\n");
		}
		
		builder.append(";\n");
		builder.append("param: deltabar :=\n");
		for (int i = 0; i < data.getNumberJobs(); i++) {
			builder.append(+i + 1 + " " + deltaBar.get(lstIndexBest.get(i)) + "\n");
		}
		builder.append(";\n");
		builder.append("param: rhobar :=\n");
		for (int i = 0; i < data.getNumberJobs(); i++) {

			builder.append(+i + 1 + " " + rhoBar.get(lstIndexBest.get(i)) + "\n");

		}
		builder.append(";\n");
		builder.append("param: Ravg :=\n");
		for (int i = 0; i < data.getNumberJobs(); i++) {
			builder.append(+i + 1 + " " + data.getRavg(lstIndexBest.get(i)) + "\n");
		}

		builder.append(";\n");
		builder.append("param: SHtypavg :=\n");
		for (int i = 0; i < data.getNumberJobs(); i++) {

			builder.append(+i + 1 + " " + data.getSHtypavg(lstIndexBest.get(i)) + "\n");

		}
		builder.append(";\n");
		System.out.println("file creato");
		File file = new File(x);
		
		//generating the file
		try (PrintWriter out = new PrintWriter(file)) {
			out.println(builder.toString());
			out.flush();
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		builder.setLength(0);
		return x;

	}

}