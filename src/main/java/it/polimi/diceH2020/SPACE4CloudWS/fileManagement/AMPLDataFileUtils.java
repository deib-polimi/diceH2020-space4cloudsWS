package it.polimi.diceH2020.SPACE4CloudWS.fileManagement;

import java.util.List;

import com.google.common.primitives.Doubles;
import com.google.common.primitives.Ints;

import it.polimi.diceH2020.SPACE4Cloud.shared.InstanceData;
import it.polimi.diceH2020.SPACE4Cloud.shared.JobClass;
import it.polimi.diceH2020.SPACE4Cloud.shared.Profile;
import it.polimi.diceH2020.SPACE4Cloud.shared.TypeVM;
import it.polimi.diceH2020.SPACE4Cloud.shared.TypeVMJobClassKey;

public class AMPLDataFileUtils {

	public static AMPLDataFileBuilder singleClassBuilder(int gamma, JobClass jClass, TypeVM tVM, Profile prof) {

		AMPLDataFileBuilder builder = new AMPLDataFileBuilder(1);
//		JobClass jClass = data.getLstClass().get(classId);
//		Profile prof = data.getMapProfiles().get(new TypeVMJobClassKey(jClass.getId(), vmType));
//		TypeVM tVM = data.getMapTypeVMs().get(jClass.getId()).get(index);
		
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

	public static AMPLDataFileBuilder multiClassBuilder(InstanceData data, List<TypeVMJobClassKey> lstTypeJobClass) {

		AMPLDataFileBuilder builder = new AMPLDataFileBuilder(data.getNumberJobs());

		builder.setScalarParameter("Gamma", data.getGamma());
		builder.setArrayParameter("HUp",  data.getHUp(lstTypeJobClass));
		builder.setArrayParameter("HLow", data.getHLow(lstTypeJobClass));
		builder.setArrayParameter("job_penalty", data.getJob_penalty());
		builder.setArrayParameter("NM", data.getNM(lstTypeJobClass));
		builder.setArrayParameter("NR", data.getNR(lstTypeJobClass));
		builder.setArrayParameter("D", data.getD(lstTypeJobClass));
		builder.setArrayParameter("Mmax", data.getMmax(lstTypeJobClass));
		builder.setArrayParameter("Mavg", data.getMavg(lstTypeJobClass));
		builder.setArrayParameter("Rmax", data.getRmax(lstTypeJobClass));
		builder.setArrayParameter("Ravg", data.getRavg(lstTypeJobClass));
		builder.setArrayParameter("SH1max", data.getSH1max(lstTypeJobClass));
		builder.setArrayParameter("SHtypavg", data.getSHtypavg(lstTypeJobClass));
		builder.setArrayParameter("SHtypmax", data.getSHtypmax(lstTypeJobClass));
		builder.setArrayParameter("R", data.getR(lstTypeJobClass));
		builder.setArrayParameter("eta", data.getEta(lstTypeJobClass));

		return builder;
	}

}
