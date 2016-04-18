package it.polimi.diceH2020.SPACE4CloudWS.solvers;

import it.polimi.diceH2020.SPACE4Cloud.shared.inputData.JobClass;
import it.polimi.diceH2020.SPACE4Cloud.shared.solution.Solution;
import it.polimi.diceH2020.SPACE4Cloud.shared.solution.SolutionPerJob;
import it.polimi.diceH2020.SPACE4CloudWS.connection.SshConnector;
import it.polimi.diceH2020.SPACE4CloudWS.fileManagement.FileUtility;
import it.polimi.diceH2020.SPACE4CloudWS.services.SshConnectorProxy;
import lombok.NonNull;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;

import javax.annotation.PostConstruct;
import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

/**
 * Created by ciavotta on 12/02/16.
 */
public abstract class AbstractSolver implements Solver {
    protected final Integer MAX_ITERATIONS = 3;

    protected Logger logger = Logger.getLogger(this.getClass().getName());
    protected ConnectionSettings connSettings;
    @Autowired
    protected FileUtility fileUtility;
    @Autowired
    protected Environment environment; // this is to check which is the active
    @Autowired
    protected SshConnectorProxy connector;


    private static double calculateResponseTime(@NonNull double throughput, int numServers, double thinkTime) {
        return (double) numServers / throughput - thinkTime;
    }

    private static BigDecimal calculateResponseTime(@NonNull BigDecimal throughput, int numServers, double thinkTime) {
        return BigDecimal.valueOf(calculateResponseTime(throughput.doubleValue(), numServers, thinkTime))
                .setScale(8, RoundingMode.HALF_EVEN);
    }

    @PostConstruct
    private void init() {
        SshConnector sshConnector = new SshConnector(connSettings);
        connector.setConnector(sshConnector);
    }

    @Override
    public Optional<BigDecimal> evaluate(@NonNull SolutionPerJob solPerJob) {
        if (!solPerJob.getChanged()) {
            return Optional.of(BigDecimal.valueOf(solPerJob.getDuration()));
        }
        JobClass jobClass = solPerJob.getJob();
        int jobID = jobClass.getId();
        int nUsers = solPerJob.getNumberUsers();
        double think = jobClass.getThink();
        List<File> pFiles;
        try {
            pFiles = createWorkingFiles(solPerJob);
            BigDecimal throughput = run(pFiles, "class" + jobID);
            delete(pFiles);
            BigDecimal duration = calculateResponseTime(throughput, nUsers, think);
            return Optional.of(duration);
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    public void delete(List<File> pFiles) {
        if (fileUtility.delete(pFiles)) logger.debug("Working files correctly deleted");
    }

    @Override
    public void setAccuracy(double accuracy) {
        connSettings.setAccuracy(accuracy);
    }

    @Override
    public void setMaxDuration(Integer duration){
        connSettings.setMaxDuration(duration);
    }

    @Override
    public List<String> pwd() throws Exception {
        return connector.pwd();
    }

    @Override
    public SshConnectorProxy getConnector() {
        return connector;
    }

    protected abstract BigDecimal run(List<File> pFiles, String s) throws Exception;

    protected abstract List<File> createWorkingFiles(SolutionPerJob solPerJob) throws IOException;

    @Override
    public void initRemoteEnvironment() throws Exception {
        List<String> lstProfiles = Arrays.asList(environment.getActiveProfiles());
        logger.info("------------------------------------------------");
        logger.info(String.format("Starting %s service initialization phase", this.getClass().getSimpleName()));
        logger.info("------------------------------------------------");
        if (lstProfiles.contains("test") && !connSettings.isForceClean()) {
            logger.info("Test phase: the remote work directory tree is assumed to be ok.");
        } else {
            logger.info("Clearing remote work directory tree");
            connector.exec("rm -rf " + connSettings.getRemoteWorkDir());
            logger.info("Creating new remote work directory tree");
            connector.exec("mkdir -p " + connSettings.getRemoteWorkDir());

            logger.info("Done");
        }
    }

    public String getRemoteWorkingDirectory() {
        return connSettings.getRemoteWorkDir();
    }

    public abstract Optional<BigDecimal> evaluate(@NonNull Solution solution);
}
