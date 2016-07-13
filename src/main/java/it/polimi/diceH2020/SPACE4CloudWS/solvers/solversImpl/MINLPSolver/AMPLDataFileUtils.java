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
		builder.addArrayParameter("NM", Ints.asList(prof.getNM()));
		builder.addArrayParameter("NR", Ints.asList(prof.getNR()));
		builder.addArrayParameter("D", Doubles.asList(jClass.getD()));
		builder.addArrayParameter("Mmax", Doubles.asList(prof.getMmax()));
		builder.addArrayParameter("Mavg", Doubles.asList(prof.getMavg()));
		builder.addArrayParameter("Rmax", Doubles.asList(prof.getRmax()));
		builder.addArrayParameter("Ravg", Doubles.asList(prof.getRavg()));
		builder.addArrayParameter("SH1max", Doubles.asList(prof.getSH1max()));
		builder.addArrayParameter("SHtypavg", Doubles.asList(prof.getSHtypavg()));
		builder.addArrayParameter("SHtypmax", Doubles.asList(prof.getSHtypmax()));
		builder.addArrayParameter("R", Ints.asList(tVM.getR()));
		builder.addArrayParameter("eta", Doubles.asList(tVM.getEta()));
		builder.addArrayParameter("cM", Ints.asList(prof.getCM()));
		builder.addArrayParameter("cR", Ints.asList(prof.getCR()));

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
	static AMPLDataFileBuilder knapsackBuilder(InstanceData data, Matrix matrix) {

		AMPLDataFileBuilder builder = new AMPLDataFileBuilder(data.getNumberJobs());
		builder.addScalarParameter("N", data.getPrivateCloudParameters().getN());
		builder.addDoubleParameter("V", data.getPrivateCloudParameters().getV());
		builder.addDoubleParameter("M", data.getPrivateCloudParameters().getM());
		
		
		Pair<Iterable<Integer>, Iterable<Double>>[] bigC,mTilde,vTilde; 
		bigC=mTilde=vTilde=new Pair[matrix.getNumRows()-1];
		
		Pair<Iterable<Integer>, Iterable<Integer>>[] nu = new Pair[matrix.getNumRows()-1];
		
		Pair<Iterable<Integer>, Iterable<Double>> bigCFirst,mTildeFirst,vTildeFirst; 
		bigCFirst=mTildeFirst=vTildeFirst= null;
		Pair<Iterable<Integer>, Iterable<Integer>> nuFirst = null;
		
		//Pair<Iterable<Integer>, Iterable<Double>>[] bigC = new Pair[matrix.getNumRows()-1];
		//Pair<Iterable<Integer>, Iterable<Double>> bigCFirst = null;
		
		
		int i = 1;
		for(Entry<String,SolutionPerJob[]> row : matrix.entrySet()){
			Iterable<Integer> rowH = matrix.getAllH(row.getKey());
			Iterable<Double> rowCost = matrix.getAllCost(row.getKey());
			Iterable<Double> rowMtilde = matrix.getAllMtilde(row.getKey(), data.getMapVMConfigurations());
			Iterable<Double> rowVtilde = matrix.getAllVtilde(row.getKey(), data.getMapVMConfigurations());
			Iterable<Integer> rowNu = matrix.getAllNu(row.getKey());
			
			if(i!=1){
				Pair<Iterable<Integer>, Iterable<Double>> c = new ImmutablePair<Iterable<Integer>, Iterable<Double>>(rowH, rowCost);
				Pair<Iterable<Integer>, Iterable<Double>> m = new ImmutablePair<Iterable<Integer>, Iterable<Double>>(rowH, rowMtilde);
				Pair<Iterable<Integer>, Iterable<Double>> v = new ImmutablePair<Iterable<Integer>, Iterable<Double>>(rowH, rowVtilde);
				Pair<Iterable<Integer>, Iterable<Integer>> n = new ImmutablePair<Iterable<Integer>, Iterable<Integer>>(rowH, rowNu);
				bigC[i-1] = c;
				mTilde[i-1] = m;
				vTilde[i-1] = v;
				nu[i-1] = n;
			}else{
				bigCFirst = new ImmutablePair<Iterable<Integer>, Iterable<Double>>(rowH, rowCost);
				mTildeFirst = new ImmutablePair<Iterable<Integer>, Iterable<Double>>(rowH, rowMtilde);
				vTildeFirst = new ImmutablePair<Iterable<Integer>, Iterable<Double>>(rowH, rowVtilde);
				nuFirst = new ImmutablePair<Iterable<Integer>, Iterable<Integer>>(rowH, rowNu);
			}
			builder.addIndexedSet("H", i, rowH);
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

}
