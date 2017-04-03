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
package it.polimi.diceH2020.SPACE4CloudWS.solvers;

import com.jcraft.jsch.JSchException;
import it.polimi.diceH2020.SPACE4Cloud.shared.settings.SPNModel;
import it.polimi.diceH2020.SPACE4Cloud.shared.solution.SolutionPerJob;
import it.polimi.diceH2020.SPACE4CloudWS.connection.SshConnector;
import it.polimi.diceH2020.SPACE4CloudWS.core.DataProcessor;
import it.polimi.diceH2020.SPACE4CloudWS.fileManagement.FileUtility;
import it.polimi.diceH2020.SPACE4CloudWS.services.SshConnectorProxy;
import it.polimi.diceH2020.SPACE4CloudWS.solvers.settings.ConnectionSettings;
import it.polimi.diceH2020.SPACE4CloudWS.solvers.settings.SettingsDealer;
import lombok.NonNull;
import lombok.Setter;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;

import javax.annotation.PostConstruct;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Predicate;

public abstract class AbstractSolver implements Solver {

    protected final Integer MAX_ITERATIONS = 3;

    protected Logger logger = Logger.getLogger(getClass());

    @Setter(onMethod = @__(@Autowired))
    protected FileUtility fileUtility;

    @Setter(onMethod = @__(@Autowired))
    protected Environment environment;

    @Setter(onMethod = @__(@Autowired))
    protected SshConnectorProxy connector;

    @Setter(onMethod = @__(@Autowired))
    private SettingsDealer settingsDealer;

    @Setter(onMethod = @__(@Autowired))
    protected DataProcessor dataProcessor;

    protected ConnectionSettings connSettings;

    protected abstract Class<? extends ConnectionSettings> getSettingsClass();

    @PostConstruct
    @Override
    public void restoreDefaults() {
        connSettings = settingsDealer.getConnectionDefaults(getSettingsClass());
        SshConnector sshConnector = new SshConnector(connSettings);
        connector.registerConnector(sshConnector, getClass());
        logger.debug(String.format("<%s> Restored default solver settings",
                getClass().getCanonicalName()));
    }

    @Override
    public Optional<Double> evaluate(@NonNull SolutionPerJob solPerJob) {
        if (! solPerJob.getChanged()) {
            return Optional.of(solPerJob.getThroughput());
        }
        try {
            Pair<List<File>, List<File>> pFiles = createWorkingFiles(solPerJob);
            String jobID = solPerJob.getId();
            Pair<Double, Boolean> result = run(pFiles, "class" + jobID);
            delete(pFiles.getLeft());
            solPerJob.setError(result.getRight());
            return Optional.of(result.getLeft());
        } catch (Exception e) {
            logger.error("Error in SPJ evaluation", e);
            solPerJob.setError(Boolean.TRUE);
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

    /**
     * Execute the model on the remote server.
     * @param pFiles the first List contains the main model files, the second one allows for providing
     *               also replayer files.
     * @param remoteName is the human readable name presented in the logs.
     * @return a Pair containing the value obtained via the solver and a Boolean that is set to true
     *         in case of failure.
     * @throws Exception in case of problems.
     */
    protected abstract Pair<Double, Boolean> run(Pair<List<File>, List<File>> pFiles, String remoteName) throws Exception;

    /**
     * Prepare the working files needed for a subsequent call to {@link #run(Pair, String) run}.
     * @param solPerJob partial solution for the class of interest.
     * @return a Pair suitable for {@link #run(Pair, String) run}.
     * @throws IOException if creating or writing these files fails.
     */
    protected abstract Pair<List<File>, List<File>> createWorkingFiles(SolutionPerJob solPerJob) throws IOException;

    @Override
    public void initRemoteEnvironment() throws Exception {
        List<String> lstProfiles = Arrays.asList(environment.getActiveProfiles());
        logger.info("------------------------------------------------");
        logger.info(String.format("Starting %s service initialization phase", this.getClass().getSimpleName()));
        logger.info("------------------------------------------------");
        if (lstProfiles.contains("test") && ! connSettings.isForceClean()) {
            logger.info("Test phase: the remote work directory tree is assumed to be ok.");
        } else {
            logger.info("Clearing remote work directory tree");
            connector.exec("rm -rf " + connSettings.getRemoteWorkDir(), getClass());
            logger.info("Creating new remote work directory tree");
            connector.exec("mkdir -p " + connSettings.getRemoteWorkDir(), getClass());
            logger.info("Done");
        }
    }

    protected String getRemoteWorkSubDirectory () {
        return connSettings.getRemoteWorkDir () + File.separator + dataProcessor.getCurrentInputsSubFolderName ();
    }

    protected List<File> retrieveReplayerFiles (@NonNull SolutionPerJob solutionPerJob) {
        String solutionID = solutionPerJob.getParentID();
        String spjID = solutionPerJob.getId();
        String provider = dataProcessor.getProviderName();
        String typeVM = solutionPerJob.getTypeVMselected().getId();
        return dataProcessor.getCurrentReplayerInputFiles(solutionID, spjID, provider, typeVM);
    }

    protected void sendFiles(List<File> lstFiles) {
        try {
            connector.exec("mkdir -p " + getRemoteWorkSubDirectory (), getClass());
        } catch (JSchException | IOException e1) {
            logger.error("Cannot create new Simulation Folder!", e1);
        }

        lstFiles.forEach((File file) -> {
            try {
                connector.sendFile(file.getAbsolutePath(),
                        getRemoteWorkSubDirectory () + File.separator + file.getName(),
                        getClass());
            } catch (JSchException | IOException e) {
                logger.error("Error sending file: " + file.toString(), e);
            }
        });
    }

    @Override
    public Predicate<Double> feasibilityCheck (SolutionPerJob solutionPerJob, SPNModel model) {
        return R -> R <= solutionPerJob.getJob ().getD ();
    }

    @Override
    public Consumer<Double> metricUpdater (SolutionPerJob solutionPerJob, SPNModel model) {
        return solutionPerJob::setDuration;
    }
}
