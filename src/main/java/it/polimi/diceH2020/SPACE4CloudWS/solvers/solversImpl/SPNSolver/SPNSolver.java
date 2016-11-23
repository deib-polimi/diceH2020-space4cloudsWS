/*
Copyright 2016 Michele Ciavotta
Copyright 2016 Eugenio Gianniti

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
*/
package it.polimi.diceH2020.SPACE4CloudWS.solvers.solversImpl.SPNSolver;

import it.polimi.diceH2020.SPACE4Cloud.shared.inputDataMultiProvider.ClassParameters;
import it.polimi.diceH2020.SPACE4Cloud.shared.inputDataMultiProvider.JobProfile;
import it.polimi.diceH2020.SPACE4Cloud.shared.solution.SolutionPerJob;
import it.polimi.diceH2020.SPACE4CloudWS.solvers.AbstractSolver;
import it.polimi.diceH2020.SPACE4CloudWS.solvers.settings.ConnectionSettings;
import lombok.NonNull;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Class that manages the interactions with GreatSPN solver
 */
@Component
public class SPNSolver extends AbstractSolver {

    @Override
    protected Class<? extends ConnectionSettings> getSettingsClass() {
        return SPNSettings.class;
    }

    @Override
    protected Pair<Double, Boolean> run(Pair<List<File>, List<File>> pFiles, String remoteName) throws Exception {
        List<File> files = pFiles.getLeft();
        if (! pFiles.getRight().isEmpty() || files.size() != 2) {
            throw new IllegalArgumentException("wrong number of input files");
        }

        File netFile = files.get(0);
        File defFile = files.get(1);
        String remotePath = connSettings.getRemoteWorkDir() + "/" + remoteName;
        Matcher matcher = Pattern.compile("([\\w.-]*)(?:-\\d*)\\.net").matcher(netFile.getName());
        if (! matcher.matches()) {
            throw new RuntimeException(String.format("problem matching %s", netFile.getName()));
        }
        String prefix = matcher.group(1);

        boolean stillNotOk = true;
        for (int i = 0; stillNotOk && i < MAX_ITERATIONS; ++i) {
            logger.info(remoteName + "-> Starting Stochastic Petri Net simulation on the server");
            connector.sendFile(netFile.getAbsolutePath(), remotePath + ".net", getClass());
            logger.debug(remoteName + "-> GreatSPN .net file sent");
            connector.sendFile(defFile.getAbsolutePath(), remotePath + ".def", getClass());
            logger.debug(remoteName + "-> GreatSPN .def file sent");
            File statFile = fileUtility.provideTemporaryFile(prefix, ".stat");
            fileUtility.writeContentToFile("end\n", statFile);
            connector.sendFile(statFile.getAbsolutePath(), remotePath + ".stat", getClass());
            logger.debug(remoteName + "-> GreatSPN .stat file sent");
            if (fileUtility.delete(statFile)) logger.debug(statFile + " deleted");

            String command = String.format("%s %s -a %f -c %d", connSettings.getSolverPath(), remotePath,
                    connSettings.getAccuracy(), ((SPNSettings) connSettings).getConfidence().getFlag());
            logger.debug(remoteName + "-> Starting GreatSPN model...");
            List<String> remoteMsg = connector.exec(command, getClass());
            if (remoteMsg.contains("exit-status: 0")) {
                stillNotOk = false;
                logger.info(remoteName + "-> The remote optimization process completed correctly");
            } else {
                logger.debug(remoteName + "-> Remote exit status: " + remoteMsg);
            }
        }

        if (stillNotOk) {
            logger.info(remoteName + "-> Error in remote optimization");
            throw new Exception("Error in the SPN server");
        } else {
            List<String> remoteOutput = connector
                    .exec(String.format("ls %s", connSettings.getRemoteWorkDir()), getClass());
            String remoteResultFile = connSettings.getRemoteWorkDir() + File.separator;
            try (BufferedReader reader = new BufferedReader(new StringReader(remoteOutput.get(0)))) {
                remoteResultFile += reader.lines().filter(line -> line.contains("simres"))
                        .findAny().orElse(remoteName + ".simres");
            }
            File solFile = fileUtility.provideTemporaryFile(prefix, ".simres");
            connector.receiveFile(solFile.getAbsolutePath(), remoteResultFile, getClass());
            Map<String, Double> results = new PNSimResFileParser(solFile).parse();
            if (fileUtility.delete(solFile)) logger.debug(solFile + " deleted");
            String label = ((SPNSettings) connSettings).getModel() == SPNModel.MAPREDUCE ? "end" : "nCores_2";
            double result = results.get(label);
            logger.info(remoteName + "-> GreatSPN model run.");
            // TODO: this always returns false, should check if every error just throws
            return Pair.of(result, false);
        }
    }

    public Pair<List<File>, List<File>> createWorkingFiles(@NonNull SolutionPerJob solPerJob) throws IOException {
        int nContainers = solPerJob.getNumberContainers();
        ClassParameters jobClass = solPerJob.getJob();
        JobProfile prof = solPerJob.getProfile();
        String jobID = solPerJob.getId();
        double mAvg = prof.get("mavg");
        double rAvg = prof.get("ravg");
        double shTypAvg = prof.get("shtypavg");
        double think = jobClass.getThink();

        int nUsers = solPerJob.getNumberUsers();
        int NM = (int) prof.get("nm");
        int NR = (int) prof.get("nr");

        String prefix = String.format("PN-%s-class%s-", solPerJob.getParentID(), jobID);

        final SPNModel model = ((SPNSettings) connSettings).getModel();
        String netFileContent = new PNNetFileBuilder().setSPNModel(model).setCores(nContainers)
                .setMapRate(1 / mAvg).setReduceRate(1 / (rAvg + shTypAvg)).setThinkRate(1 / think).build();
        File netFile = fileUtility.provideTemporaryFile(prefix, ".net");
        fileUtility.writeContentToFile(netFileContent, netFile);

        String defFileContent = new PNDefFileBuilder().setSPNModel(model).setConcurrency(nUsers)
                .setNumberOfMapTasks(NM).setNumberOfReduceTasks(NR).build();
        File defFile = fileUtility.provideTemporaryFile(prefix, ".def");
        fileUtility.writeContentToFile(defFileContent, defFile);

        List<File> lst = new ArrayList<>(2);
        lst.add(netFile);
        lst.add(defFile);
        return new ImmutablePair<>(lst, new ArrayList<>());
    }

    public List<String> pwd() throws Exception {
        return connector.pwd(getClass());
    }
}
