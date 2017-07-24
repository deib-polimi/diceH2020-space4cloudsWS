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

import it.polimi.diceH2020.SPACE4Cloud.shared.inputDataMultiProvider.JobProfile;
import it.polimi.diceH2020.SPACE4Cloud.shared.settings.SPNModel;
import it.polimi.diceH2020.SPACE4Cloud.shared.solution.SolutionPerJob;
import it.polimi.diceH2020.SPACE4CloudWS.performanceMetrics.LittleLaw;
import it.polimi.diceH2020.SPACE4CloudWS.services.DataService;
import it.polimi.diceH2020.SPACE4CloudWS.solvers.AbstractSolver;
import it.polimi.diceH2020.SPACE4CloudWS.solvers.settings.ConnectionSettings;
import lombok.Setter;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class DagSimSolver extends AbstractSolver {

    private final static Pattern RESULT_LINE =
            Pattern.compile("0\\.0\\s+0\\.0\\s+(?<avg>[\\d.e-]+)\\s+(?<dev>[\\d.e-]+)\\s+(?<lower>[\\d.e-]+)\\s+(?<upper>[\\d.e-]+)\\s+(?<accuracy>[\\d.e-]+)");

    @Setter(onMethod = @__(@Autowired))
    private DataService dataService;

    @Override
    protected Class<? extends ConnectionSettings> getSettingsClass() {
        return DagSimSettings.class;
    }

    @Override
    protected Pair<List<File>, List<File>> createWorkingFiles(SolutionPerJob solPerJob) throws IOException {
        DagSimFileBuilder builder = new DagSimFileBuilder ()
                .setContainers(solPerJob.getNumberContainers())
                .setUsers(solPerJob.getNumberUsers())
                .setExponentialThinkTime(solPerJob.getJob().getThink())
                .setMaxJobs(((DagSimSettings) connSettings).getEvents())
                .setQuantile(((DagSimSettings) connSettings).getConfidence().getQuantile());

        List<File> replayerFiles = retrieveInputFiles (solPerJob, ".txt");
        Map<String, Set<String>> successors = dataService.getData ().getMapDags ()
                .get (solPerJob.getId ()).getSuccessors ();
        Map<String, Set<String>> predecessors = flipDirectedEdges (successors);
        Set<String> vertices = obtainVertices (successors);
        JobProfile profile = dataService.getProfile (solPerJob.getId (), solPerJob.getTypeVMselected ().getId ());

        for (String vertex: vertices) {
            Stage currentStage = new Stage ().setName (vertex);
            String tasksLabel = String.format ("nTask_%s", vertex);
            long numTasks = Math.round (profile.get (tasksLabel));
            currentStage.setTasks ((int) numTasks);

            Set<String> possiblySuccessors = successors.get (vertex);
            if (possiblySuccessors != null) {
                for (String successor: possiblySuccessors) {
                    currentStage.addSuccessor (successor);
                }
            }

            Set<String> possiblyPredecessors = predecessors.get (vertex);
            if (possiblyPredecessors != null) {
                for (String predecessor : possiblyPredecessors) {
                    currentStage.addPredecessor (predecessor);
                }
            }

            replayerFiles.stream ().map (File::getName).filter (name -> name.contains (vertex)).forEach (
                    name -> {
                        File remote = new File (retrieveRemoteSubDirectory (solPerJob), name);
                        String remoteName = remote.getPath ();
                        currentStage.setDistribution (new Empirical ().setFileName (remoteName));
                    }
            );

            builder.addStage (currentStage);
        }

        String content = builder.build ();
        File modelFile = fileUtility.provideTemporaryFile(String.format("%s-%s-",
                solPerJob.getParentID(), solPerJob.getId()), ".lua");
        fileUtility.writeContentToFile(content, modelFile);

        List<File> list = new LinkedList<>();
        list.add(modelFile);

        return Pair.of (list, replayerFiles);
    }

    @Override
    protected Pair<Double, Boolean> run (Pair<List<File>, List<File>> pFiles, String remoteName,
                                         String remoteDirectory) throws Exception {
        if (pFiles.getLeft() == null || pFiles.getLeft().size() != 1) {
            throw new Exception ("Model file missing");
        }

        if (pFiles.getRight () == null || pFiles.getRight ().isEmpty ()) {
            throw new Exception ("Replayer files missing");
        }

        double result = 0.;
        boolean success = false;
        List<String> remoteMsg = null;

        boolean stillNotOk = true;
        for (int i = 0; stillNotOk && i < MAX_ITERATIONS; ++i) {
            logger.info(remoteName + "-> Starting DagSim resolution on the server");

            File modelFile = pFiles.getLeft().get(0);
            String fileName = modelFile.getName();
            String remotePath = remoteDirectory + File.separator + fileName;

            List<File> workingFiles = pFiles.getRight ();
            workingFiles.add (modelFile);
            cleanRemoteSubDirectory (remoteDirectory);
            sendFiles (remoteDirectory, workingFiles);
            logger.debug(remoteName + "-> Working files sent");

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

        return Pair.of(result, ! success);
    }

    @Override
    public Function<Double, Double> transformationFromSolverResult (SolutionPerJob solutionPerJob,
                                                                    SPNModel model) {
        return R -> R;
    }

    @Override
    public BiConsumer<SolutionPerJob, Double> initialResultSaver (SPNModel model) {
        return (SolutionPerJob spj, Double value) -> {
            spj.setDuration (value);
            spj.setThroughput (LittleLaw.computeThroughput (value, spj));
            spj.setError (false);
        };
    }

    private Map<String, Set<String>> flipDirectedEdges (Map<String, Set<String>> dag) {
        Map<String, Set<String>> reverseDag = new HashMap<> ();

        for (Map.Entry<String, Set<String>> entry: dag.entrySet ()) {
            String nextTo = entry.getKey ();

            for (String nextFrom: entry.getValue ()) {
                if (! reverseDag.containsKey (nextFrom)) {
                    reverseDag.put (nextFrom, new HashSet<> ());
                }

                reverseDag.get (nextFrom).add (nextTo);
            }
        }

        return reverseDag;
    }

    private Set<String> obtainVertices (Map<String, Set<String>> dag) {
        Set<String> vertices = new HashSet<> ();

        for (Map.Entry<String, Set<String>> entry: dag.entrySet ()) {
            vertices.add (entry.getKey ());
            vertices.addAll (entry.getValue ());
        }

        return vertices;
    }
}
