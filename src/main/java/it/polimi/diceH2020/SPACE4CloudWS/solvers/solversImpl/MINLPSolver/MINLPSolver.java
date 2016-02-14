package it.polimi.diceH2020.SPACE4CloudWS.solvers.solversImpl.MINLPSolver;

import com.google.common.primitives.Doubles;
import com.google.common.primitives.Ints;
import it.polimi.diceH2020.SPACE4Cloud.shared.inputData.JobClass;
import it.polimi.diceH2020.SPACE4Cloud.shared.inputData.Profile;
import it.polimi.diceH2020.SPACE4Cloud.shared.inputData.TypeVM;
import it.polimi.diceH2020.SPACE4Cloud.shared.solution.Solution;
import it.polimi.diceH2020.SPACE4Cloud.shared.solution.SolutionPerJob;
import it.polimi.diceH2020.SPACE4CloudWS.services.DataService;
import it.polimi.diceH2020.SPACE4CloudWS.solvers.AbstractSolver;
import lombok.NonNull;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.validation.constraints.NotNull;
import java.io.*;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

@Component
public class MINLPSolver extends AbstractSolver {

    private static final String AMPL_FILES = "/AMPL";
    private static final String RESULTS_SOLFILE = "/results/solution.sol";
    private static final String REMOTE_SCRATCH = "/scratch";
    private static final String REMOTE_RESULTS = "/results";
    private static final String REMOTEPATH_DATA_DAT = REMOTE_SCRATCH + "/data.dat";
    private static final String REMOTEPATH_DATA_RUN = "data.run";

    @Autowired
    private DataService dataService;

    @Autowired
    public MINLPSolver(MINLPSettings settings) {
        this.connSettings = settings;
    }

    private Double analyzeSolution(File solFile, boolean verbose) throws IOException {
        String fileToString = FileUtils.readFileToString(solFile);
        String centralized = "centralized_obj = ";
        int startPos = fileToString.indexOf(centralized);
        int endPos = fileToString.indexOf('\n', startPos);
        Double objFunctionValue = Double.parseDouble(fileToString.substring(startPos + centralized.length(), endPos));

        if (verbose) {
            logger.info(fileToString);
            logger.info(objFunctionValue);
        }
        return objFunctionValue;

    }

    public void test() throws IOException {
        String path = "/AMPL/centralized.run";
        InputStream res = getClass().getResourceAsStream(path);
        String theString = IOUtils.toString(res, Charset.defaultCharset());
        logger.info(theString);
    }

    public void initRemoteEnvironment() throws Exception {
        List<String> lstProfiles = Arrays.asList(this.environment.getActiveProfiles());
        String localPath = AMPL_FILES;
        logger.info("------------------------------------------------");
        logger.info("Starting math solver service initialization phase");
        logger.info("------------------------------------------------");
        if (lstProfiles.contains("test") && !connSettings.isForceClean()) {
            logger.info("Test phase: the remote work directory tree is assumed to be ok.");
        } else {
            logger.info("Clearing remote work directory tree");
            try {
                String root = connSettings.getRemoteWorkDir();
                String cleanRemoteDirectoryTree = "rm -rf " + root;
                connector.exec(cleanRemoteDirectoryTree);

                logger.info("Creating new remote work directory tree");
                String makeRemoteDirectoryTree = "mkdir -p " + root + "/{problems,utils,solve,scratch,results}";
                connector.exec(makeRemoteDirectoryTree);
            } catch (Exception e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }

            logger.info("Sending AMPL files");
            System.out.print("[#           ] Sending work files\r");
            sendFile(localPath + "/model.mod", connSettings.getRemoteWorkDir() + "/problems/model.mod");
            System.out.print("[##          ] Sending work files\r");
            sendFile(localPath + "/compute_psi.run", connSettings.getRemoteWorkDir() + "/utils/compute_psi.run");
            System.out.print("[###         ] Sending work files\r");
            sendFile(localPath + "/compute_job_profile.run",
                    connSettings.getRemoteWorkDir() + "/utils/compute_job_profile.run");
            System.out.print("[####        ] Sending work files\r");
            sendFile(localPath + "/compute_penalties.run",
                    connSettings.getRemoteWorkDir() + "/utils/compute_penalties.run");
            System.out.print("[#####       ] Sending work files\r");
            sendFile(localPath + "/save_aux.run", connSettings.getRemoteWorkDir() + "/utils/save_aux.run");
            System.out.print("[######      ] Sending work files\r");
            sendFile(localPath + "/centralized.run", connSettings.getRemoteWorkDir() + "/problems/centralized.run");
            System.out.print("[#######     ] Sending work files\r");
            sendFile(localPath + "/save_centralized.run",
                    connSettings.getRemoteWorkDir() + "/utils/save_centralized.run");
            System.out.print("[########    ] Sending work files\r");
            sendFile(localPath + "/compute_s_d.run", connSettings.getRemoteWorkDir() + "/utils/compute_s_d.run");
            System.out.print("[#########   ] Sending work files\r");
            sendFile(localPath + "/AM_closed_form.run", connSettings.getRemoteWorkDir() + "/solve/AM_closed_form.run");
            System.out.print("[##########  ] Sending work files\r");
            sendFile(localPath + "/post_processing.run",
                    connSettings.getRemoteWorkDir() + "/utils/post_processing.run");
            System.out.print("[########### ] Sending work files\r");
            sendFile(localPath + "/save_centralized.run",
                    connSettings.getRemoteWorkDir() + "/utils/save_centralized.run");
            System.out.print("[############] Sending work files\r");
            logger.info("AMPL files sent");
        }
    }

    private void sendFile(String localPath, String remotePath) throws Exception {

        InputStream in = this.getClass().getResourceAsStream(localPath);
        File tempFile = fileUtility.provideTemporaryFile("S4C-temp", null);
        FileOutputStream out = new FileOutputStream(tempFile);
        IOUtils.copy(in, out);
        connector.sendFile(tempFile.getAbsolutePath(), remotePath);
        if (fileUtility.delete(tempFile)) {
            logger.debug(tempFile + " deleted");
        }
    }

    public List<String> clearWorkingDir() throws Exception {
        String command = "rm -rf " + connSettings.getRemoteWorkDir();
        return connector.exec(command);
    }

    private void clearResultDir() throws Exception {
        String command = "rm -rf " + connSettings.getRemoteWorkDir() + REMOTE_RESULTS + "/*";
        connector.exec(command);
    }

    private BigDecimal run(List<File> pFiles, String remoteName, Integer iteration) throws Exception {
        if (iteration < MAX_ITERATIONS) {
            File dataFile = pFiles.get(0);
            File solutionFile = pFiles.get(1);
            String fullRemotePath = connSettings.getRemoteWorkDir() + REMOTEPATH_DATA_DAT;
            connector.sendFile(dataFile.getAbsolutePath(), fullRemotePath);
            logger.info(remoteName + "-> AMPL .data file sent");

            String remoteRelativeDataPath = ".." + REMOTEPATH_DATA_DAT;
            String remoteRelativeSolutionPath = ".." + RESULTS_SOLFILE;
            File runFile = fileUtility.provideTemporaryFile("S4C-run-", ".run");
            String runFileContent = new AMPLRunFileBuilder().setDataFile(remoteRelativeDataPath)
                    .setSolverPath(connSettings.getSolverPath()).setSolutionFile(remoteRelativeSolutionPath).build();
            fileUtility.writeContentToFile(runFileContent, runFile);

            fullRemotePath = connSettings.getRemoteWorkDir() + REMOTE_SCRATCH + "/" + REMOTEPATH_DATA_RUN;
            connector.sendFile(runFile.getAbsolutePath(), fullRemotePath);
            logger.info(remoteName + "-> AMPL .run file sent");
            if (fileUtility.delete(runFile))
                logger.debug(runFile + " deleted");

            logger.info(remoteName + "-> Processing execution...");
            clearResultDir();
            String command = String.format("cd %s%s && %s %s", connSettings.getRemoteWorkDir(), REMOTE_SCRATCH, ((MINLPSettings) connSettings).getAmplDirectory(), REMOTEPATH_DATA_RUN);
            List<String> remoteMsg = connector.exec(command);
            if (remoteMsg.contains("exit-status: 0")) {
                logger.info(remoteName + "-> The remote optimization process completed correctly");
            } else {
                logger.info("Remote exit status: " + remoteMsg);
                iteration = iteration + 1;
                return run(pFiles, remoteName, iteration);
            }
            fullRemotePath = connSettings.getRemoteWorkDir() + RESULTS_SOLFILE;
            connector.receiveFile(solutionFile.getAbsolutePath(), fullRemotePath);
            Double objFunctionValue = analyzeSolution(solutionFile, ((MINLPSettings) connSettings).isVerbose());
            logger.info(remoteName + "-> The value of the objective function is: " + objFunctionValue);

            logger.info(remoteName + "-> Cleaning result directory");
            clearResultDir();
            BigDecimal result = BigDecimal.valueOf(objFunctionValue);
            result.setScale(2, RoundingMode.HALF_EVEN);
            return result;
        } else {
            logger.debug(remoteName + "-> Error in remote optimization");
            throw new Exception("Error in the QN server");

        }

    }


    @Override
    protected BigDecimal run(@NotNull List<File> pFiles, String s) throws Exception {
        return run(pFiles, s, 0);
    }

    @Override
    protected List<File> createWorkingFiles(SolutionPerJob solPerJob) throws IOException {
        Profile prof = solPerJob.getProfile();
        JobClass jobClass = solPerJob.getJob();
        TypeVM tVM = solPerJob.getTypeVMselected();
        AMPLDataFileBuilder builder = AMPLDataFileUtils.singleClassBuilder(dataService.getGamma(), jobClass, tVM, prof);
        builder.setArrayParameter("w", Ints.asList(dataService.getNumCores(tVM)))
                .setArrayParameter("sigmabar", Doubles.asList(dataService.getSigmaBar(tVM)))
                .setArrayParameter("deltabar", Doubles.asList(dataService.getDeltaBar(tVM)))
                .setArrayParameter("rhobar", Doubles.asList(dataService.getRhoBar(tVM)));
        File dataFile = fileUtility
                .provideTemporaryFile(String.format("partial_class%d_vm%s_", jobClass.getId(), tVM.getId()), ".dat");
        fileUtility.writeContentToFile(builder.build(), dataFile);
        File resultsFile = fileUtility
                .provideTemporaryFile(String.format("partial_class%d_vm%s_", jobClass.getId(), tVM.getId()), ".sol");
        List<File> lst = new ArrayList<>(2);
        lst.add(dataFile);
        lst.add(resultsFile);
        return lst;
    }

    @Override
    public Optional<BigDecimal> evaluate(@NonNull SolutionPerJob solPerJob) {
        if (!solPerJob.getChanged())
            return Optional.of(BigDecimal.valueOf(solPerJob.getDuration()));

        JobClass jobClass = solPerJob.getJob();
        int jobID = jobClass.getId();
        List<File> pFiles;
        try {
            pFiles = createWorkingFiles(solPerJob);
            BigDecimal duration = run(pFiles, "class" + jobID);
            delete(pFiles);
            return Optional.of(duration);
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    @Override
    public Optional<BigDecimal> evaluate(@NonNull Solution solution) {

        List<File> pFiles;
        try {
            pFiles = createWorkingFiles(solution);
            BigDecimal duration = run(pFiles, "full solution");
            delete(pFiles);
            File resultsFile = pFiles.get(1);
            updateWithFinalValues(solution, resultsFile);
            if (fileUtility.delete(pFiles))
                logger.info("Working files correctly deleted");

            return Optional.of(duration);
        } catch (Exception e) {
            return Optional.empty();
        }
    }


    private void updateWithFinalValues(Solution sol, File solutionFile) throws IOException {
        int numJobs = dataService.getJobNumber();
        int ncontainers = 0;
        int nusers;
        List<Integer> numCores = dataService.getNumCores(sol.getTypeVMSelected());

        BufferedReader reader = new BufferedReader(new FileReader(solutionFile));

        String line = reader.readLine();
        String[] bufferStr = new String[7];

        // looking for the line with solve_result info
        while (!line.contains("solve_result ")) {
            line = reader.readLine();
        }

        bufferStr = line.split("\\s+");
        if (bufferStr[2] == "infeasible") {
            logger.info("The problem is infeasible");
            reader.close();
            return;
        }

        while (!line.contains("t [*]")) {
            line = reader.readLine();
            // System.out.println(line);
        }

        for (int i = 0; i < numJobs; i++) {
            line = reader.readLine();
            bufferStr = line.split("\\s+");
            // System.out.println(bufferStr[1]);

            String localStr = bufferStr[1].replaceAll("\\s+", "");
            sol.getLstSolutions().get(i).setDuration(Double.parseDouble(localStr));
        }
        while (!line.contains("Variables")) {
            line = reader.readLine();
            // System.out.println(line);
        }
        line = reader.readLine();
        // System.out.println(line);
        while (line.contains(":")) {
            line = reader.readLine();
            // System.out.println(line);
        }
        double users;

        for (int i = 0; i < numJobs; i++) {
            bufferStr = line.split("\\s+");
            String x = bufferStr[2].replaceAll("\\s+", "");
            ncontainers = Math.round(Float.parseFloat(x));
            SolutionPerJob solPerJob = sol.getSolutionPerJob(i);
            solPerJob.setNumberVM(ncontainers / numCores.get(i));
            solPerJob.setNumberContainers(ncontainers);
            x = bufferStr[5].replaceAll("\\s+", "");
            users = Double.parseDouble(x);
            users = 1 / users;
            nusers = (int) users;
            solPerJob.setNumberUsers(nusers);
            line = reader.readLine();
        }

        while (!line.contains("### alphabeta"))
            line = reader.readLine();

        reader.readLine();
        for (int i = 0; i < numJobs; i++) {
            line = reader.readLine();
            bufferStr = line.split("\\s+");

            String x = bufferStr[2].replaceAll("\\s+", "");
            SolutionPerJob solPerJob = sol.getSolutionPerJob(i);
            solPerJob.setAlfa(Double.parseDouble(x));

            x = bufferStr[3].replaceAll("\\s+", "");
            solPerJob.setBeta(Double.parseDouble(x));

        }
        reader.close();

    }


    private List<File> createWorkingFiles(@NotNull Solution sol) throws IOException {
//    private Pair<File, File> createMultiClassWorkingFiles(@NotNull Solution sol) throws IOException {
        AMPLDataFileBuilder builder = AMPLDataFileUtils.multiClassBuilder(dataService.getData(),
                sol.getPairsTypeVMJobClass());

        builder.setArrayParameter("w", sol.getLstNumberCores());
        builder.setArrayParameter("cM", sol.getListCM());
        builder.setArrayParameter("cR", sol.getListCR());
        builder.setArrayParameter("deltabar", sol.getListDeltabar());
        builder.setArrayParameter("rhobar", sol.getListRhobar());
        builder.setArrayParameter("sigmabar", sol.getListSigmaBar());

        File dataFile = fileUtility.provideTemporaryFile("S4C-multi-class-", ".dat");
        fileUtility.writeContentToFile(builder.build(), dataFile);
        File resultsFile = fileUtility.provideTemporaryFile("S4C-multi-class-", ".sol");
        List<File> lst = new ArrayList<>(2);
        lst.add(dataFile);
        lst.add(resultsFile);
        return lst;
    }

/*    private Pair<File, File> createSingleClassWorkingFiles(@NotNull JobClass jobClass, @NotNull TypeVM tVM) throws IOException {
        Profile prof = dataService.getProfile(jobClass, tVM);
        AMPLDataFileBuilder builder = AMPLDataFileUtils.singleClassBuilder(dataService.getGamma(), jobClass, tVM, prof);
        builder.setArrayParameter("w", Ints.asList(dataService.getNumCores(tVM)))
                .setArrayParameter("sigmabar", Doubles.asList(dataService.getSigmaBar(tVM)))
                .setArrayParameter("deltabar", Doubles.asList(dataService.getDeltaBar(tVM)))
                .setArrayParameter("rhobar", Doubles.asList(dataService.getRhoBar(tVM)));
        File dataFile = fileUtility
                .provideTemporaryFile(String.format("partial_class%d_vm%s_", jobClass.getId(), tVM.getId()), ".dat");
        fileUtility.writeContentToFile(builder.build(), dataFile);
        File resultsFile = fileUtility
                .provideTemporaryFile(String.format("partial_class%d_vm%s_", jobClass.getId(), tVM.getId()), ".sol");
        return new ImmutablePair<File, File>(dataFile, resultsFile);

    }*/
}
