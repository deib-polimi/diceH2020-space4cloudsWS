package it.polimi.diceH2020.SPACE4CloudWS.solvers.solversImpl.MINLPSolver;

import com.google.common.primitives.Doubles;
import com.google.common.primitives.Ints;
import it.polimi.diceH2020.SPACE4Cloud.shared.inputData.*;

import java.util.List;

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

}
