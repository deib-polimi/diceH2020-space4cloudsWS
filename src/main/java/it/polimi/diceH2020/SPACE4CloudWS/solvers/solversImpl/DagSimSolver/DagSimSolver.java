/*
Copyright 2017 Eugenio Gianniti

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
package it.polimi.diceH2020.SPACE4CloudWS.solvers.solversImpl.DagSimSolver;

import it.polimi.diceH2020.SPACE4Cloud.shared.solution.SolutionPerJob;
import it.polimi.diceH2020.SPACE4CloudWS.solvers.AbstractSolver;
import it.polimi.diceH2020.SPACE4CloudWS.solvers.settings.ConnectionSettings;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class DagSimSolver extends AbstractSolver {

    private final static Pattern RESULT_LINE =
            Pattern.compile("0\\.0\\s+0\\.0\\s+(?<avg>[\\d.e-]+)\\s+(?<dev>[\\d.e-]+)\\s+(?<lower>[\\d.e-]+)\\s+(?<upper>[\\d.e-]+)\\s+(?<accuracy>[\\d.e-]+)");

    @Override
    protected Class<? extends ConnectionSettings> getSettingsClass() {
        return DagSimSettings.class;
    }

    @Override
    protected Pair<List<File>, List<File>> createWorkingFiles(SolutionPerJob solPerJob) throws IOException {
        String content = new DagSimFileBuilder()
                .setContainers(solPerJob.getNumberContainers())
                .setUsers(solPerJob.getNumberUsers())
                .setExponentialThinkTime(solPerJob.getJob().getThink())
                .setMaxJobs(((DagSimSettings) connSettings).getEvents())
                .setQuantile(((DagSimSettings) connSettings).getConfidence().getQuantile())
                .addStage(new Stage().setName("Map")
                        .setTasks(141).addSuccessor("Reduce").setDistribution(new Exponential().setRate(0.5)))
                .addStage(new Stage().setName("Reduce")
                        .setTasks(150).addPredecessor("Map").setDistribution(new Exponential().setRate(1)))
                .build();

        File modelFile = fileUtility.provideTemporaryFile(String.format("%s-%s-",
                solPerJob.getParentID(), solPerJob.getId()), ".lua");
        fileUtility.writeContentToFile(content, modelFile);

        List<File> list = new LinkedList<>();
        list.add(modelFile);

        return Pair.of(list, null);
    }

    @Override
    protected Pair<Double, Boolean> run(Pair<List<File>, List<File>> pFiles, String remoteName) throws Exception {
        if (pFiles.getLeft() == null || pFiles.getLeft().size() != 1) {
            throw new Exception("Model file missing");
        }

        File modelFile = pFiles.getLeft().get(0);
        String fileName = modelFile.getName();
        String remotePath = connSettings.getRemoteWorkDir() + File.separator + fileName;

        double result = 0.;
        boolean success = false;
        List<String> remoteMsg = null;

        boolean stillNotOk = true;
        for (int i = 0; stillNotOk && i < MAX_ITERATIONS; ++i) {
            logger.info(remoteName + "-> Starting DagSim resolution on the server");

            connector.sendFile(modelFile.getAbsolutePath(), remotePath, getClass());
            logger.debug(remoteName + "-> Working file sent");

            logger.debug(remoteName + "-> Starting DagSim model...");
            String command = String.format("%s %s", connSettings.getSolverPath(), remotePath);
            remoteMsg = connector.exec(command, getClass());

            if (remoteMsg.contains("exit-status: 0")) {
                stillNotOk = false;
                logger.info(remoteName + "-> The remote simulation process completed correctly");
            } else {
                logger.debug(remoteName + "-> Remote exit status: " + remoteMsg);
            }
        }

        if (stillNotOk) {
            logger.info(remoteName + "-> Error in remote simulation on DagSim");
            throw new Exception("Error in the DagSim server");
        } else {
            final String stdout = remoteMsg.get(0);
            final String stderr = remoteMsg.get(1);

            try (Scanner scanner = new Scanner(stdout)) {
                while (! success && scanner.hasNextLine()) {
                    Matcher matcher = RESULT_LINE.matcher(scanner.nextLine());
                    if (matcher.find()) {
                        result = Double.valueOf(matcher.group("avg"));
                        success = true;
                    }
                }
            }

            if (! success) {
                logger.error(String.format("%s -> Error in remote DagSim simulation", remoteName));
                logger.error(String.format("%s -> stdout:\n%s", remoteName, stdout));
                logger.error(String.format("%s -> stderr:\n%s", remoteName, stderr));
            }
        }

        return Pair.of(result, success);
    }
}
