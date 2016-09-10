package it.polimi.diceH2020.SPACE4CloudWS.solvers.solversImpl.MINLPSolver;

import com.google.common.primitives.Doubles;
import com.google.common.primitives.Ints;
import it.polimi.diceH2020.SPACE4Cloud.shared.inputData.*;
import it.polimi.diceH2020.SPACE4Cloud.shared.solution.SolutionPerJob;
import it.polimi.diceH2020.SPACE4CloudWS.core.Matrix;

import java.util.List;
import java.util.Map.Entry;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;

class AMPLDataFileUtils {

	static AMPLDataFileBuilder singleClassBuilder(int gamma, JobClass jClass, TypeVM tVM, Profile prof) {

		AMPLDataFileBuilder builder = new AMPLDataFileBuilder(1);
		builder.addScalarParameter("Gamma", gamma);
		builder.addArrayParameter("HUp",   Ints.asList(jClass.getHup()));
		builder.addArrayParameter("HLow", Ints.asList(jClass.getHlow()));
		builder.addArrayParameter("job_penalty", Doubles.asList(jClass.getJob_penalty()));
		builder.addArrayParameter("NM", Ints.asList(prof.getNm()));
		builder.addArrayParameter("NR", Ints.asList(prof.getNr()));
		builder.addArrayParameter("D", Doubles.asList(jClass.getD()));
		builder.addArrayParameter("Mmax", Doubles.asList(prof.getMmax()));
		builder.addArrayParameter("Mavg", Doubles.asList(prof.getMavg()));
		builder.addArrayParameter("Rmax", Doubles.asList(prof.getRmax()));
		builder.addArrayParameter("Ravg", Doubles.asList(prof.getRavg()));
		builder.addArrayParameter("SH1max", Doubles.asList(prof.getSh1max()));
		builder.addArrayParameter("SHtypavg", Doubles.asList(prof.getShtypavg()));
		builder.addArrayParameter("SHtypmax", Doubles.asList(prof.getShtypmax()));
		builder.addArrayParameter("R", Ints.asList(tVM.getR()));
		builder.addArrayParameter("eta", Doubles.asList(tVM.getEta()));
		builder.addArrayParameter("cM", Ints.asList(prof.getCm()));
		builder.addArrayParameter("cR", Ints.asList(prof.getCr()));

		return builder;
	}

	static AMPLDataFileBuilder multiClassBuilder(InstanceData data, List<TypeVMJobClassKey> lstKeys) {

		AMPLDataFileBuilder builder = new AMPLDataFileBuilder(data.getNumberJobs());

		List<JobClass> lstJob = data.getListJobs(lstKeys);

		builder.addScalarParameter("Gamma", data.getGamma());
		builder.addArrayParameter("HUp",  data.getHUp(lstJob));
		builder.addArrayParameter("HLow", data.getHLow(lstJob));
		builder.addArrayParameter("job_penalty", data.getJob_penalty());
		builder.addArrayParameter("NM", data.getNM(lstKeys));
		builder.addArrayParameter("NR", data.getNR(lstKeys));
		builder.addArrayParameter("D", data.getD(lstJob));
		builder.addArrayParameter("Mmax", data.getMmax(lstKeys));
		builder.addArrayParameter("Mavg", data.getMavg(lstKeys));
		builder.addArrayParameter("Rmax", data.getRmax(lstKeys));
		builder.addArrayParameter("Ravg", data.getRavg(lstKeys));
		builder.addArrayParameter("SH1max", data.getSH1max(lstKeys));
		builder.addArrayParameter("SHtypavg", data.getSHtypavg(lstKeys));
		builder.addArrayParameter("SHtypmax", data.getSHtypmax(lstKeys));
		builder.addArrayParameter("R", data.getR(lstKeys));
		builder.addArrayParameter("eta", data.getEta(lstKeys));

		return builder;
	}
	
	@SuppressWarnings("unchecked")
	static AMPLDataFileBuilder knapsackBuilder(InstanceData data, Matrix matrixWithoutHoles) {
		boolean tail = false;
		Matrix matrix = matrixWithoutHoles.removeFailedSimulations();
		
		AMPLDataFileBuilder builder = new AMPLDataFileBuilder(matrix.getNumRows());
		builder.addScalarParameter("N", data.getPrivateCloudParameters().get().getN());
		builder.addDoubleParameter("V", data.getPrivateCloudParameters().get().getV());
		builder.addDoubleParameter("M", data.getPrivateCloudParameters().get().getM());
		
		Pair<Iterable<Integer>, Iterable<Double>>[] bigC = null;
		Pair<Iterable<Integer>, Iterable<Double>>[] mTilde = null;
		Pair<Iterable<Integer>, Iterable<Double>>[] vTilde = null; 
		Pair<Iterable<Integer>, Iterable<Integer>>[] nu = null;
		
		if(matrix.getNumRows()>1){
			tail = true;
			
			bigC=new Pair[matrix.getNumRows()-1];
			mTilde=new Pair[matrix.getNumRows()-1];
			vTilde=new Pair[matrix.getNumRows()-1];
			nu = new Pair[matrix.getNumRows()-1];
		}
		
		Pair<Iterable<Integer>, Iterable<Double>> bigCFirst = null;
		Pair<Iterable<Integer>, Iterable<Double>> mTildeFirst=null;
		Pair<Iterable<Integer>, Iterable<Double>> vTildeFirst=null;
		Pair<Iterable<Integer>, Iterable<Integer>> nuFirst = null;
		
		int i = 0;
		for(Entry<String,SolutionPerJob[]> row : matrix.entrySet()){
			Iterable<Integer> rowH = matrix.getAllH(row.getKey());
			Iterable<Double> rowCost = matrix.getAllCost(row.getKey());
			System.out.println(rowCost);
			Iterable<Double> rowMtilde = matrix.getAllMtilde(row.getKey(), data.getMapVMConfigurations().get());
			Iterable<Double> rowVtilde = matrix.getAllVtilde(row.getKey(), data.getMapVMConfigurations().get());
			Iterable<Integer> rowNu = matrix.getAllNu(row.getKey());
			
			if(i==0){
				bigCFirst = new ImmutablePair<Iterable<Integer>, Iterable<Double>>(rowH, rowCost);
				mTildeFirst = new ImmutablePair<Iterable<Integer>, Iterable<Double>>(rowH, rowMtilde);
				vTildeFirst = new ImmutablePair<Iterable<Integer>, Iterable<Double>>(rowH, rowVtilde);
				nuFirst = new ImmutablePair<Iterable<Integer>, Iterable<Integer>>(rowH, rowNu);
			}else if(tail){
				Pair<Iterable<Integer>, Iterable<Double>> c = new ImmutablePair<Iterable<Integer>, Iterable<Double>>(rowH, rowCost);
				Pair<Iterable<Integer>, Iterable<Double>> m = new ImmutablePair<Iterable<Integer>, Iterable<Double>>(rowH, rowMtilde);
				Pair<Iterable<Integer>, Iterable<Double>> v = new ImmutablePair<Iterable<Integer>, Iterable<Double>>(rowH, rowVtilde);
				Pair<Iterable<Integer>, Iterable<Integer>> n = new ImmutablePair<Iterable<Integer>, Iterable<Integer>>(rowH, rowNu);
				//first received i, i=1
				
				bigC[i-1] = c;
				mTilde[i-1] = m;
				vTilde[i-1] = v;
				nu[i-1] = n;
			}
			builder.addIndexedSet("H", i+1, rowH);
			matrixWithoutHoles.addNotFailedRow(i+1, row.getKey());
			i++;
		}

//		for(Entry<String,SolutionPerJob[]> row : matrix.entrySet()){
//		}
		builder.addIndexedArrayParameter("bigC", bigCFirst, bigC);
		builder.addIndexedArrayParameter("nu", nuFirst, nu);
		builder.addIndexedArrayParameter("Mtilde", mTildeFirst, mTilde);
		builder.addIndexedArrayParameter("Vtilde", vTildeFirst, vTilde);
		return builder;
	}
	
	
	@SuppressWarnings("unchecked")
	static AMPLDataFileBuilder binPackingBuilder(InstanceData data, Matrix matrixWithoutHoles) {
		boolean tail = false;
		Matrix matrix = matrixWithoutHoles.removeFailedSimulations();
		
		AMPLDataFileBuilder builder = new AMPLDataFileBuilder(matrix.getNumRows());
		builder.addScalarParameter("N", data.getPrivateCloudParameters().get().getN());
		builder.addDoubleParameter("V", data.getPrivateCloudParameters().get().getV());
		builder.addDoubleParameter("M", data.getPrivateCloudParameters().get().getM());
		builder.addDoubleParameter("bigE", data.getPrivateCloudParameters().get().getE());
		
		
		Pair<Iterable<Integer>, Iterable<Double>>[] bigP = null;
		Pair<Iterable<Integer>, Iterable<Double>>[] mTilde = null;
		Pair<Iterable<Integer>, Iterable<Double>>[] vTilde = null;
		Pair<Iterable<Integer>, Iterable<Integer>>[] nu = null;
		
		
		
		if(matrix.getNumRows()>1){
			tail = true;
			
			bigP=new Pair[matrix.getNumRows()-1];
			mTilde=new Pair[matrix.getNumRows()-1];
			vTilde=new Pair[matrix.getNumRows()-1];
			nu = new Pair[matrix.getNumRows()-1];
		}
		
		Pair<Iterable<Integer>, Iterable<Double>> bigPFirst = null;
		Pair<Iterable<Integer>, Iterable<Double>> mTildeFirst = null;
		Pair<Iterable<Integer>, Iterable<Double>> vTildeFirst = null;
		Pair<Iterable<Integer>, Iterable<Integer>> nuFirst = null;
		
		int i = 0;
		for(Entry<String,SolutionPerJob[]> row : matrix.entrySet()){
			Iterable<Integer> rowH = matrix.getAllH(row.getKey());
			Iterable<Double> rowPenalty = matrix.getAllPenalty(row.getKey());
			Iterable<Double> rowMtilde = matrix.getAllMtilde(row.getKey(), data.getMapVMConfigurations().get());
			Iterable<Double> rowVtilde = matrix.getAllVtilde(row.getKey(), data.getMapVMConfigurations().get());
			Iterable<Integer> rowNu = matrix.getAllNu(row.getKey());
			
			if(i==0){
				bigPFirst = new ImmutablePair<Iterable<Integer>, Iterable<Double>>(rowH, rowPenalty);
				mTildeFirst = new ImmutablePair<Iterable<Integer>, Iterable<Double>>(rowH, rowMtilde);
				vTildeFirst = new ImmutablePair<Iterable<Integer>, Iterable<Double>>(rowH, rowVtilde);
				nuFirst = new ImmutablePair<Iterable<Integer>, Iterable<Integer>>(rowH, rowNu);
			}else if(tail){
				Pair<Iterable<Integer>, Iterable<Double>> p = new ImmutablePair<Iterable<Integer>, Iterable<Double>>(rowH, rowPenalty);
				Pair<Iterable<Integer>, Iterable<Double>> m = new ImmutablePair<Iterable<Integer>, Iterable<Double>>(rowH, rowMtilde);
				Pair<Iterable<Integer>, Iterable<Double>> v = new ImmutablePair<Iterable<Integer>, Iterable<Double>>(rowH, rowVtilde);
				Pair<Iterable<Integer>, Iterable<Integer>> n = new ImmutablePair<Iterable<Integer>, Iterable<Integer>>(rowH, rowNu);
				//first received i, i=1
				bigP[i-1] = p;
				mTilde[i-1] = m;
				vTilde[i-1] = v;
				nu[i-1] = n;
				
			}
			builder.addIndexedSet("H", i+1, rowH);
			matrixWithoutHoles.addNotFailedRow(i+1, row.getKey());
			i++;
		}
		
//		for(Entry<String,SolutionPerJob[]> row : matrix.entrySet()){
//		}
		
		builder.addIndexedArrayParameter("bigP", bigPFirst, bigP);
		builder.addIndexedArrayParameter("Mtilde", mTildeFirst, mTilde);
		builder.addIndexedArrayParameter("Vtilde", vTildeFirst, vTilde);
		builder.addIndexedArrayParameter("nu", nuFirst, nu);
		return builder;
	}

}
