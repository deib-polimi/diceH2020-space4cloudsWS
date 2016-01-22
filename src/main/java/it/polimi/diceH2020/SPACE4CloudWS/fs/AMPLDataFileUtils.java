package it.polimi.diceH2020.SPACE4CloudWS.fs;

import com.google.common.primitives.Doubles;
import com.google.common.primitives.Ints;
import it.polimi.diceH2020.SPACE4Cloud.shared.InstanceData;

public class AMPLDataFileUtils {

    public static AMPLDataFileBuilder singleClassBuilder(InstanceData data, int classId, int vmType) {

        AMPLDataFileBuilder builder = new AMPLDataFileBuilder(1);

        builder.setScalarParameter("Gamma", data.getGamma());

        builder.setArrayParameter("HUp", Ints.asList(data.getHUp(classId)));
        builder.setArrayParameter("HLow", Ints.asList(data.getHLow(classId)));
        builder.setArrayParameter("job_penalty", Doubles.asList(data.getJob_penalty(classId)));
        builder.setArrayParameter("NM", Ints.asList(data.getNM(classId)));
        builder.setArrayParameter("NR", Ints.asList(data.getNR(classId)));
        builder.setArrayParameter("D", Doubles.asList(data.getD(classId)));
        builder.setArrayParameter("Mmax", Doubles.asList(data.getMmax(classId)));
        builder.setArrayParameter("Mavg", Doubles.asList(data.getMavg(classId)));
        builder.setArrayParameter("Rmax", Doubles.asList(data.getRmax(classId)));
        builder.setArrayParameter("Ravg", Doubles.asList(data.getRavg(classId)));
        builder.setArrayParameter("SH1max", Doubles.asList(data.getSH1max(classId)));
        builder.setArrayParameter("SHtypavg", Doubles.asList(data.getSHtypavg(classId)));
        builder.setArrayParameter("SHtypmax", Doubles.asList(data.getSHtypmax(classId)));
        builder.setArrayParameter("R", Ints.asList(data.getR(classId)));
        builder.setArrayParameter("eta", Doubles.asList(data.getEta(classId)));

        builder.setArrayParameter("cM", Ints.asList(data.getcM(classId, vmType)));
        builder.setArrayParameter("cR", Ints.asList(data.getcR(classId, vmType)));

        return builder;
    }

    public static AMPLDataFileBuilder multiClassBuilder(InstanceData data) {

        AMPLDataFileBuilder builder = new AMPLDataFileBuilder(data.getNumberJobs());

        builder.setScalarParameter("Gamma", data.getGamma());

        builder.setArrayParameter("HUp", Ints.asList(data.getHUp()));
        builder.setArrayParameter("HLow", Ints.asList(data.getHLow()));
        builder.setArrayParameter("job_penalty", Doubles.asList(data.getJob_penalty()));
        builder.setArrayParameter("NM", Ints.asList(data.getNM()));
        builder.setArrayParameter("NR", Ints.asList(data.getNR()));
        builder.setArrayParameter("D", Doubles.asList(data.getD()));
        builder.setArrayParameter("Mmax", Doubles.asList(data.getMmax()));
        builder.setArrayParameter("Mavg", Doubles.asList(data.getMavg()));
        builder.setArrayParameter("Rmax", Doubles.asList(data.getRmax()));
        builder.setArrayParameter("Ravg", Doubles.asList(data.getRavg()));
        builder.setArrayParameter("SH1max", Doubles.asList(data.getSH1max()));
        builder.setArrayParameter("SHtypavg", Doubles.asList(data.getSHtypavg()));
        builder.setArrayParameter("SHtypmax", Doubles.asList(data.getSHtypmax()));
        builder.setArrayParameter("R", Ints.asList(data.getR()));
        builder.setArrayParameter("eta", Doubles.asList(data.getEta()));

        return builder;
    }

}
