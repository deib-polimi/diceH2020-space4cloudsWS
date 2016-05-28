package it.polimi.diceH2020.SPACE4CloudWS.solvers.solversImpl.SPNSolver;

import it.polimi.diceH2020.SPACE4Cloud.shared.inputData.JobClass;
import it.polimi.diceH2020.SPACE4Cloud.shared.inputData.Profile;
import it.polimi.diceH2020.SPACE4Cloud.shared.solution.Solution;
import it.polimi.diceH2020.SPACE4Cloud.shared.solution.SolutionPerJob;
import it.polimi.diceH2020.SPACE4CloudWS.solvers.AbstractSolver;
import it.polimi.diceH2020.SPACE4CloudWS.solvers.ConnectionSettings;
import lombok.NonNull;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
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

    public Pair<BigDecimal, Boolean> run(List<File> pFiles, String remoteName) throws Exception {
        if (pFiles.size() != 2) throw new IllegalArgumentException("wrong number of input files");
        Pair<File, File> inputFiles = new ImmutablePair<>(pFiles.get(0), pFiles.get(1));
        return run(inputFiles, remoteName, 0);
    }

    private Pair<BigDecimal, Boolean> run(Pair<File, File> pFiles, String remoteName, Integer iter) throws Exception {
        if (iter < MAX_ITERATIONS) {
            File netFile = pFiles.getLeft();
            File defFile = pFiles.getRight();
            String remotePath = connSettings.getRemoteWorkDir() + "/" + remoteName;
            logger.info(remoteName + "-> Starting Stochastic Petri Net simulation on the server");
            connector.sendFile(netFile.getAbsolutePath(), remotePath + ".net", getClass());
            logger.debug(remoteName + "-> GreatSPN .net file sent");
            connector.sendFile(defFile.getAbsolutePath(), remotePath + ".def", getClass());
            logger.debug(remoteName + "-> GreatSPN .def file sent");
            Matcher matcher = Pattern.compile("([\\w\\.-]*)(?:-\\d*)\\.net").matcher(netFile.getName());
            if (! matcher.matches()) {
                throw new RuntimeException(String.format("problem matching %s", netFile.getName()));
            }
            String prefix = matcher.group(1);
            File statFile = fileUtility.provideTemporaryFile(prefix, ".stat");
            fileUtility.writeContentToFile("end\n", statFile);
            connector.sendFile(statFile.getAbsolutePath(), remotePath + ".stat", getClass());
            logger.debug(remoteName + "-> GreatSPN .stat file sent");
            if (fileUtility.delete(statFile))
                logger.debug(statFile + " deleted");

            String command = connSettings.getSolverPath() + " " + remotePath + " -a " + connSettings.getAccuracy()
                    + " -c 6";
            logger.debug(remoteName + "-> Starting GreatSPN model...");
            List<String> remoteMsg = connector.exec(command, getClass());
            if (remoteMsg.contains("exit-status: 0")) {
                logger.info(remoteName + "-> The remote optimization process completed correctly");
            } else {
                logger.debug(remoteName + "-> Remote exit status: " + remoteMsg);
                iter = iter + 1;
                return run(pFiles, remoteName, iter);
            }

            File solFile = fileUtility.provideTemporaryFile(prefix, ".sta");
            connector.receiveFile(solFile.getAbsolutePath(), remotePath + ".sta", getClass());
            String solFileInString = FileUtils.readFileToString(solFile);
            if (fileUtility.delete(solFile))
                logger.debug(solFile + " deleted");

            String throughputStr = "Thru_end = ";
            int startPos = solFileInString.indexOf(throughputStr);
            int endPos = solFileInString.indexOf('\n', startPos);
            double throughput = Double
                    .parseDouble(solFileInString.substring(startPos + throughputStr.length(), endPos));
            logger.debug(remoteName + "-> GreatSPN model run.");
            BigDecimal result = BigDecimal.valueOf(throughput).setScale(8, RoundingMode.HALF_EVEN);
            // TODO: this always returns false, should check if every error just throws
            return Pair.of(result, false);
        } else {
            logger.debug(remoteName + "-> Error in remote optimization");
            throw new Exception("Error in the SPN server");
        }
    }

    public List<File> createWorkingFiles(@NonNull SolutionPerJob solPerJob) throws IOException {
        return createWorkingFiles(solPerJob, Optional.empty());
    }

    @Override
    public Optional<BigDecimal> evaluate(@NonNull Solution solution) {
        return null;
    }


    private List<File> createWorkingFiles(SolutionPerJob solPerJob, Optional<Integer> iteration) throws IOException {
        int nContainers = solPerJob.getNumberContainers();
        JobClass jobClass = solPerJob.getJob();
        Profile prof = solPerJob.getProfile();
        int jobID = jobClass.getId();
        double mAvg = prof.getMavg();
        double rAvg = prof.getRavg();
        double shTypAvg = prof.getSHtypavg();
        double think = jobClass.getThink();

        int nUsers = solPerJob.getNumberUsers();
        int NM = prof.getNM();
        int NR = prof.getNR();

        String prefix;
        if (iteration.isPresent()) {
            prefix = String.format("PN-%s-class%d-iter%d-", solPerJob.getParentID(), jobID, iteration.get());
        } else {
            prefix = String.format("PN-%s-class%d-", solPerJob.getParentID(), jobID);
        }

        String netFileContent = new PNNetFileBuilder().setCores(nContainers).setMapRate(1 / mAvg)
                .setReduceRate(1 / (rAvg + shTypAvg)).setThinkRate(1 / think).build();
        File netFile = fileUtility.provideTemporaryFile(prefix, ".net");
        fileUtility.writeContentToFile(netFileContent, netFile);

        String defFileContent = new PNDefFileBuilder().setConcurrency(nUsers).setNumberOfMapTasks(NM)
                .setNumberOfReduceTasks(NR).build();
        File defFile = fileUtility.provideTemporaryFile(prefix, ".def");
        fileUtility.writeContentToFile(defFileContent, defFile);

        List<File> lst = new ArrayList<>(2);
        lst.add(netFile);
        lst.add(defFile);
        return lst;
    }

    public List<String> pwd() throws Exception {
        return connector.pwd(getClass());
    }

}
