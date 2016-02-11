package it.polimi.diceH2020.SPACE4CloudWS.solvers;

import it.polimi.diceH2020.SPACE4Cloud.shared.solution.SolutionPerJob;
import it.polimi.diceH2020.SPACE4CloudWS.connection.SshConnector;
import it.polimi.diceH2020.SPACE4CloudWS.fileManagement.FileUtility;
import lombok.NonNull;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

/**
 * Created by ciavotta on 11/02/16.
 */
@Component
public class QNSolver implements Solver {
    private static Logger logger = Logger.getLogger(QNSolver.class.getName());
    private final Integer MAXITER = 3;
    private SshConnector connector;
    @Autowired
    private QNSettings connSettings;
    @Autowired
    private FileUtility fileUtility;
    // profile at runtime
    @Autowired
    private Environment environment; // this is to check which is the active

    public QNSolver() {
    }

    @PostConstruct
    private void init() {
        connector = new SshConnector(connSettings);
    }

//
//    public BigDecimal run(Pair<File, File> pFiles) throws Exception {
//        return run(pFiles, 0);
//    }
//
//    private BigDecimal run(Pair<File, File> pFiles, Integer iteration) throws Exception {
//        if (iteration < MAXITER) {
//            File jmtFile = pFiles.getLeft();
//            File resultFile = pFiles.getRight();
//            String remotePath = connSettings.getRemoteWorkDir() + "/" + remoteName;
//            logger.info(remoteName + "-> Starting Queuing Net resolution on the server");
//            connector.sendFile(jmtFile.getAbsolutePath(), remotePath + ".net");
//            logger.debug(remoteName + "-> JMT .net file sent");
//            connector.sendFile(resultFile.getAbsolutePath(), remotePath + ".def");
//            logger.debug(remoteName + "-> JMT .def file sent");
//            File statFile = fileUtility.provideTemporaryFile("S4C-stat-", ".stat");
//            fileUtility.writeContentToFile("end\n", statFile);
//            connector.sendFile(statFile.getAbsolutePath(), remotePath + ".stat");
//            logger.debug(remoteName + "-> JMT .stat file sent");
//            if (fileUtility.delete(statFile))
//                logger.debug(statFile + " deleted");
//
//            String command = connSettings.getSolverPath() + " " + remotePath + " -a " + connSettings.getAccuracy()
//                    + " -c 6";
//            logger.debug(remoteName + "-> Starting GreatSPN model...");
//            List<String> remoteMsg = connector.exec(command);
//            if (remoteMsg.contains("exit-status: 0")) {
//                logger.info(remoteName + "-> The remote optimization proces completed correctly");
//            } else {
//                logger.debug(remoteName + "-> Remote exit status: " + remoteMsg);
//                iteration = iteration + 1;
//                return run(pFiles, remoteName, iteration);
//            }
//
//            File solFile = fileUtility.provideTemporaryFile("S4C-" + remoteName, ".sta");
//            connector.receiveFile(solFile.getAbsolutePath(), remotePath + ".sta");
//            String solFileInString = FileUtils.readFileToString(solFile);
//            if (fileUtility.delete(solFile))
//                logger.debug(solFile + " deleted");
//
//            String throughputStr = "Thru_end = ";
//            int startPos = solFileInString.indexOf(throughputStr);
//            int endPos = solFileInString.indexOf('\n', startPos);
//            double throughput = Double
//                    .parseDouble(solFileInString.substring(startPos + throughputStr.length(), endPos));
//            logger.debug(remoteName + "-> QN model run.");
//            BigDecimal result = BigDecimal.valueOf(throughput);
//            result.setScale(2, RoundingMode.HALF_EVEN);
//            return result;
//        } else {
//            logger.debug(remoteName + "-> Error in remote optimziation");
//            throw new Exception("Error in the QN server");
//
//        }
//
//
//    }

    public BigDecimal run(Pair<File, File> pFiles, String remoteName) throws Exception {
        return null;
    }

    @Override
    public void setAccuracy(double accuracy) {

    }

    @Override
    public void initRemoteEnvironment() throws Exception {

    }

    @Override
    public List<String> pwd() throws Exception {
        return connector.pwd();
    }

    @Override
    public SshConnector getConnector() {
        return connector;
    }


    public Pair<File, File> createWorkingFiles(@NonNull SolutionPerJob solPerJob) throws IOException {
        return null;
    }

    public void delete(Pair<File, File> pFiles) {
        if (fileUtility.delete(pFiles)) logger.debug("Working files correctly deleted");
    }

    @Override
    public Optional<BigDecimal> evaluate(SolutionPerJob solPerJob) {
        return null;
    }
}
