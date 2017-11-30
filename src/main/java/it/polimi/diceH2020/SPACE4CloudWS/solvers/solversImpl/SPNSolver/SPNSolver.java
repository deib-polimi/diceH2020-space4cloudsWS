/*
Copyright 2016-2017 Eugenio Gianniti
Copyright 2016 Michele Ciavotta

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
import it.polimi.diceH2020.SPACE4Cloud.shared.settings.Scenario;
import it.polimi.diceH2020.SPACE4Cloud.shared.settings.Technology;
import it.polimi.diceH2020.SPACE4Cloud.shared.solution.SolutionPerJob;
import it.polimi.diceH2020.SPACE4CloudWS.core.DataProcessor;
import it.polimi.diceH2020.SPACE4CloudWS.performanceMetrics.LittleLaw;
import it.polimi.diceH2020.SPACE4CloudWS.performanceMetrics.Utilization;
import it.polimi.diceH2020.SPACE4CloudWS.solvers.PerformanceSolver;
import it.polimi.diceH2020.SPACE4CloudWS.solvers.settings.ConnectionSettings;
import lombok.Setter;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.validation.constraints.NotNull;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Class that manages the interactions with GreatSPN solver
 */
@Component
public class SPNSolver extends PerformanceSolver{

    private final static Pattern prefixRegex = Pattern.compile("([\\w.-]*)(?:-\\d*)\\.net");
    private final static String statSafeLabel = "SafeNameForStatFileButLongEnoughToBeImplausible";

    @Setter(onMethod = @__(@Autowired))
    private DataProcessor dataProcessor;

    private String label;
    private boolean usingInputModel;

    @Override
    protected Class<? extends ConnectionSettings> getSettingsClass() {
        return SPNSettings.class;
    }

    @Override
    protected Pair<Double, Boolean> run (Pair<List<File>, List<File>> pFiles, String remoteName,
                                         String remoteDirectory) throws Exception {
        List<File> files = pFiles.getLeft();
        if (! pFiles.getRight().isEmpty() || files.size() != 3) {
            throw new IllegalArgumentException("wrong number of input files");
        }

        File netFile = files.get(0);
        File defFile = files.get(1);
        File statFile = files.get(2);
        Matcher matcher = prefixRegex.matcher(netFile.getName());
        if (! matcher.matches()) {
            throw new RuntimeException(String.format("problem matching %s", netFile.getName()));
        }
        String prefix = matcher.group(1);

        String remotePath = remoteDirectory + File.separator + remoteName;

        boolean stillNotOk = true;
        for (int i = 0; stillNotOk && i < MAX_ITERATIONS; ++i) {
            logger.info(remoteName + "-> Starting Stochastic Petri Net simulation on the server");

            cleanRemoteSubDirectory (remoteDirectory);
            connector.exec(String.format("mkdir -p %s", remoteDirectory), getClass());
            logger.trace("Scenario is " + dataService.getScenario().getStringRepresentation());
            connector.sendFile(netFile.getAbsolutePath(), remotePath + ".net", getClass());
            logger.debug(remoteName + "-> GreatSPN .net file sent");
            connector.sendFile(defFile.getAbsolutePath(), remotePath + ".def", getClass());
            logger.debug(remoteName + "-> GreatSPN .def file sent");
            if(dataService.getScenario().getTechnology() != Technology.STORM) {
               connector.sendFile (statFile.getAbsolutePath (), remotePath + ".stat", getClass ());
               logger.debug (remoteName + "-> GreatSPN .stat file sent");
            }

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
            List<String> remoteOutput = connector.exec(String.format("ls %s", remoteDirectory), getClass());
            String remoteResultFile = remoteDirectory + File.separator;
            try (BufferedReader reader = new BufferedReader(new StringReader(remoteOutput.get(0)))) {
                remoteResultFile += reader.lines().filter(line -> line.contains("simres"))
                        .findAny().orElse(remoteName + ".simres");
            }
            File solFile = fileUtility.provideTemporaryFile(prefix, ".simres");
            connector.receiveFile(solFile.getAbsolutePath(), remoteResultFile, getClass());
            Map<String, Double> results = new PNSimResFileParser(solFile).parse();
            if (fileUtility.delete(solFile)) logger.debug(solFile + " deleted");

            double result = results.get(label);
            /* This is a workaround for the crazy behavior of the PNML transformation
             * that converts milliseconds to seconds.
             * Here we turn hertz into kilohertz to obtain results consistent
             * with our input.
             */
            if (usingInputModel && dataService.getScenario().getTechnology () != Technology.STORM) result /= 1000;
            logger.info(remoteName + "-> GreatSPN model run.");

            // TODO: this always returns false, should check if every error just throws
            return Pair.of(result, false);
        }
    }

    @Override
    protected Pair<List<File>, List<File>> createWorkingFiles(@NotNull SolutionPerJob solutionPerJob)
            throws IOException {
        Pair<List<File>, List<File>> returnValue;

        List<File> netFileList = dataProcessor.retrieveInputFiles (".net", solutionPerJob.getParentID(),
                solutionPerJob.getId(), dataProcessor.getProviderName(), solutionPerJob.getTypeVMselected().getId());
        List<File> defFileList = dataProcessor.retrieveInputFiles (".def", solutionPerJob.getParentID(),
                solutionPerJob.getId(), dataProcessor.getProviderName(), solutionPerJob.getTypeVMselected().getId());
        List<File> statFileList = dataProcessor.retrieveInputFiles (".stat", solutionPerJob.getParentID(),
                solutionPerJob.getId(), dataProcessor.getProviderName(), solutionPerJob.getTypeVMselected().getId());

        final String experiment = String.format ("%s, class %s, provider %s, VM %s, # %d",
                solutionPerJob.getParentID (), solutionPerJob.getId (), dataProcessor.getProviderName (),
                solutionPerJob.getTypeVMselected ().getId (), solutionPerJob.getNumberVM ());

        if (netFileList.isEmpty () || defFileList.isEmpty () || statFileList.isEmpty ()) {
            logger.debug (String.format ("Generating SPN model for %s", experiment));
            usingInputModel = false;
            returnValue = generateSPNModel (solutionPerJob);
        } else {
            logger.debug (String.format ("Using input SPN model for %s", experiment));
            usingInputModel = true;

            // TODO now it just takes the first file, I would expect a single file per list
            File inputNetFile = netFileList.get (0);
            File inputDefFile = defFileList.get (0);
            File inputStatFile = statFileList.get (0);

            String prefix = filePrefix (solutionPerJob);
            final String originalLabel = String.join ("", Files.readAllLines (inputStatFile.toPath ()));
            label = statSafeLabel;

            Map<String, String> placeHolders = new TreeMap<>();
            placeHolders.put ("@@CONCURRENCY@@",
                    Long.toUnsignedString (solutionPerJob.getNumberUsers ().longValue ()));
            placeHolders.put ("@@CORES@@",
                    Long.toUnsignedString (solutionPerJob.getNumberContainers ().longValue ()));
            logger.trace("@@CORES@@ replaced with " + solutionPerJob.getNumberContainers ().toString());
            placeHolders.put (originalLabel, label);
            List<String> outcomes = processPlaceholders (inputDefFile, placeHolders);

            File defFile = fileUtility.provideTemporaryFile (prefix, ".def");
            writeLinesToFile (outcomes, defFile);
            outcomes = processPlaceholders (inputNetFile, placeHolders);

            File netFile = fileUtility.provideTemporaryFile (prefix, ".net");
            writeLinesToFile (outcomes, netFile);

            File statFile = fileUtility.provideTemporaryFile (prefix, ".stat");
            List<String> statContent = new ArrayList<> (1);
            statContent.add (label);
            writeLinesToFile (statContent, statFile);

            List<File> model = new ArrayList<> (3);
            model.add (netFile);
            model.add (defFile);
            model.add (statFile);
            returnValue = new ImmutablePair<> (model, new ArrayList<> ());
        }

        return returnValue;
    }

    private Pair<List<File>, List<File>> generateSPNModel(@NotNull SolutionPerJob solPerJob) throws IOException {
        int nContainers = solPerJob.getNumberContainers();
        ClassParameters jobClass = solPerJob.getJob();
        JobProfile prof = solPerJob.getProfile();
        double mAvg = prof.get("mavg");
        double rAvg = prof.get("ravg");
        double shTypAvg = prof.get("shtypavg");
        double think = jobClass.getThink();

        int nUsers = solPerJob.getNumberUsers();
        int NM = (int) prof.get("nm");
        int NR = (int) prof.get("nr");

        String prefix = filePrefix (solPerJob);

        final Technology technology = dataService.getScenario().getTechnology();
        String netFileContent = new PNNetFileBuilder().setTechnology(technology).setCores(nContainers)
                .setMapRate(1 / mAvg).setReduceRate(1 / (rAvg + shTypAvg)).setThinkRate(1 / think).build();
        File netFile = fileUtility.provideTemporaryFile(prefix, ".net");
        fileUtility.writeContentToFile(netFileContent, netFile);

        String defFileContent = new PNDefFileBuilder().setTechnology(technology).setConcurrency(nUsers)
                .setNumberOfMapTasks(NM).setNumberOfReduceTasks(NR).build();
        File defFile = fileUtility.provideTemporaryFile(prefix, ".def");
        fileUtility.writeContentToFile(defFileContent, defFile);

        label = dataService.getScenario().getTechnology() != Technology.STORM ? "end" : "nCores_2";
        File statFile = writeStatFile (solPerJob, label);

        List<File> lst = new ArrayList<>(3);
        lst.add(netFile);
        lst.add(defFile);
        lst.add(statFile);
        return new ImmutablePair<>(lst, new ArrayList<>());
    }

    @Override
    public Function<Double, Double> transformationFromSolverResult (SolutionPerJob solutionPerJob, Technology technology) {
        switch(technology) {
            case STORM:
               return Nk -> Utilization.computeServerUtilization (Nk, solutionPerJob);
            case HADOOP:
            case SPARK:
               return X -> LittleLaw.computeResponseTime (X, solutionPerJob);
            default:
               throw new RuntimeException("Unexpected technology");
        }
    }

    @Override
    public Predicate<Double> feasibilityCheck (SolutionPerJob solutionPerJob, Technology technology) {
        switch(technology) {
            case HADOOP:
            case SPARK:
               return R -> R <= solutionPerJob.getJob ().getD ();
            case STORM:
               return Uk -> Uk <= solutionPerJob.getJob ().getU ();
            default:
               throw new RuntimeException("Unexpected technology");
        }
    }

    @Override
    public Consumer<Double> metricUpdater (SolutionPerJob solutionPerJob, Technology technology) {
        switch(technology) {
            case HADOOP:
            case SPARK:
               return solutionPerJob::setDuration;
            case STORM:
               return solutionPerJob::setUtilization;
            default:
               throw new RuntimeException("Unexpected technology");
        }
    }

    @Override
    public BiConsumer<SolutionPerJob, Double> initialResultSaver (Technology technology) {
        switch(technology) {
            case HADOOP:
            case SPARK:
               return (SolutionPerJob spj, Double value) -> {
                  spj.setThroughput (value);
                  spj.setDuration (LittleLaw.computeResponseTime (value, spj));
                 spj.setError (false);
               };
            case STORM:
               return (SolutionPerJob spj, Double value) -> {
                  spj.setUtilization (Utilization.computeServerUtilization (value, spj));
                  spj.setError (false);
               };
            default:
               throw new RuntimeException("Unexpected technology");
        }
    }

    private String filePrefix(@NotNull SolutionPerJob solutionPerJob) {
        return String.format ("PN-%s-class%s-", solutionPerJob.getParentID (), solutionPerJob.getId ());
    }

    private File writeStatFile(@NotNull SolutionPerJob solutionPerJob, @NotNull String label) throws IOException {
        String prefix = filePrefix (solutionPerJob);
        File statFile = fileUtility.provideTemporaryFile(prefix, ".stat");
        fileUtility.writeContentToFile(String.format ("%s\n", label), statFile);
        return statFile;
    }
}
