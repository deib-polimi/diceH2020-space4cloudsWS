package it.polimi.diceH2020.SPACE4CloudWS.solvers.solversImp.MINLPSolver;

import com.google.common.primitives.Doubles;
import com.google.common.primitives.Ints;
import it.polimi.diceH2020.SPACE4Cloud.shared.inputData.*;

import java.util.List;

public class AMPLDataFileUtils {

	public static AMPLDataFileBuilder singleClassBuilder(int gamma, JobClass jClass, TypeVM tVM, Profile prof) {

		AMPLDataFileBuilder builder = new AMPLDataFileBuilder(1);
		builder.setScalarParameter("Gamma", gamma);
		builder.setArrayParameter("HUp",   Ints.asList(jClass.getHup()));
		builder.setArrayParameter("HLow", Ints.asList(jClass.getHlow()));
		builder.setArrayParameter("job_penalty", Doubles.asList(jClass.getJob_penalty()));
		builder.setArrayParameter("NM", Ints.asList(prof.getNM()));
		builder.setArrayParameter("NR", Ints.asList(prof.getNR()));
		builder.setArrayParameter("D", Doubles.asList(jClass.getD()));
		builder.setArrayParameter("Mmax", Doubles.asList(prof.getMmax()));
		builder.setArrayParameter("Mavg", Doubles.asList(prof.getMavg()));
		builder.setArrayParameter("Rmax", Doubles.asList(prof.getRmax()));
		builder.setArrayParameter("Ravg", Doubles.asList(prof.getRavg()));
		builder.setArrayParameter("SH1max", Doubles.asList(prof.getSH1max()));
		builder.setArrayParameter("SHtypavg", Doubles.asList(prof.getSHtypavg()));
		builder.setArrayParameter("SHtypmax", Doubles.asList(prof.getSHtypmax()));
		builder.setArrayParameter("R", Ints.asList(tVM.getR()));
		builder.setArrayParameter("eta", Doubles.asList(tVM.getEta()));
		builder.setArrayParameter("cM", Ints.asList(prof.getCM()));
		builder.setArrayParameter("cR", Ints.asList(prof.getCR()));

		return builder;
	}

	public static AMPLDataFileBuilder multiClassBuilder(InstanceData data, List<TypeVMJobClassKey> lstKeys) {

		AMPLDataFileBuilder builder = new AMPLDataFileBuilder(data.getNumberJobs());
		
		List<JobClass> lstJob = data.getListJobs(lstKeys);
		
		builder.setScalarParameter("Gamma", data.getGamma());
		builder.setArrayParameter("HUp",  data.getHUp(lstJob));
		builder.setArrayParameter("HLow", data.getHLow(lstJob));
		builder.setArrayParameter("job_penalty", data.getJob_penalty());
		builder.setArrayParameter("NM", data.getNM(lstKeys));
		builder.setArrayParameter("NR", data.getNR(lstKeys));
		builder.setArrayParameter("D", data.getD(lstJob));
		builder.setArrayParameter("Mmax", data.getMmax(lstKeys));
		builder.setArrayParameter("Mavg", data.getMavg(lstKeys));
		builder.setArrayParameter("Rmax", data.getRmax(lstKeys));
		builder.setArrayParameter("Ravg", data.getRavg(lstKeys));
		builder.setArrayParameter("SH1max", data.getSH1max(lstKeys));
		builder.setArrayParameter("SHtypavg", data.getSHtypavg(lstKeys));
		builder.setArrayParameter("SHtypmax", data.getSHtypmax(lstKeys));
		builder.setArrayParameter("R", data.getR(lstKeys));
		builder.setArrayParameter("eta", data.getEta(lstKeys));

		return builder;
	}

}
