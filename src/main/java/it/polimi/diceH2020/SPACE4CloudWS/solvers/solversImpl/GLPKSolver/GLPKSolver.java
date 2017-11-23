/*
Copyright 2017 Marco Lattuada 

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
package it.polimi.diceH2020.SPACE4CloudWS.solvers.solversImpl.GLPKSolver;

import com.jcraft.jsch.JSchException;

import it.polimi.diceH2020.SPACE4CloudWS.solvers.MINLPSolver;
import it.polimi.diceH2020.SPACE4CloudWS.solvers.settings.ConnectionSettings;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.validation.constraints.NotNull;

import org.apache.commons.lang3.tuple.Pair;

import org.springframework.stereotype.Component;


@Component
public class GLPKSolver extends MINLPSolver{
   
   private static final String GLPK_PATH = "/GLPK";

   @Override
   public void initRemoteEnvironment() throws Exception {
      List<String> lstProfiles = Arrays.asList(this.environment.getActiveProfiles());
      String localPath = GLPK_PATH;
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
            connector.exec(cleanRemoteDirectoryTree, getClass());

            logger.info("Creating new remote work directory tree");
            String makeRemoteDirectoryTree = "mkdir -p " + root + "/{problems,utils,results}";
            connector.exec(makeRemoteDirectoryTree, getClass());
         } catch (Exception e) {
            logger.error("error preparing remote work directory", e);
         }

         logger.info("Sending GLPK files");
         System.out.print("[#       ] Sending work files\r");
         sendFile(localPath + "/model_glpk.mod", connSettings.getRemoteWorkDir() + "/problems/model_glpk.mod");

         logger.info("GLPK files sent");
      }
   }

   protected Pair<Double, Boolean> run (@NotNull Pair<List<File>, List<File>> pFiles, String remoteName) throws JSchException, IOException {
      List<File> exchangedFiles = pFiles.getLeft();
      boolean stillNotOk = true;
      for (int iteration = 0; stillNotOk && iteration < MAX_ITERATIONS; ++iteration) {
         File dataFile = exchangedFiles.get(0);
         String fullRemotePath = connSettings.getRemoteWorkDir() + REMOTEPATH_DATA_DAT;
         connector.sendFile(dataFile.getAbsolutePath(), fullRemotePath, getClass());
         logger.info(remoteName + "-> GLPK .data file sent");

         String remoteRelativeDataPath = ".." + REMOTEPATH_DATA_DAT;
         String remoteRelativeSolutionPath = ".." + RESULTS_SOLFILE;
         Matcher matcher = Pattern.compile("([\\w.-]*)(?:-\\d*)\\.dat").matcher(dataFile.getName());
         if (! matcher.matches()) {
            throw new RuntimeException(String.format("problem matching %s", dataFile.getName()));
         }
         logger.debug(remoteName + "-> Cleaning result directory");
         clearResultDir();

         logger.info(remoteName + "-> Processing execution...");
         String command = String.format("cd %s%s && %s %s", connSettings.getRemoteWorkDir(), REMOTE_SCRATCH, ((GLPKSettings) connSettings).getGlpkDirectory(), "--math problems/glpk_model.mod -d " + dataFile.getAbsolutePath());
         List<String> remoteMsg = connector.exec(command, getClass());
         if (remoteMsg.contains("exit-status: 0")) {
            stillNotOk = false;
            logger.info(remoteName + "-> The remote optimization process completed correctly");
         } else {
            logger.info("Remote exit status: " + remoteMsg);
            if (remoteMsg.get(0).contains("error processing param")) {
               iteration = MAX_ITERATIONS;
               logger.info(remoteName + "-> Wrong parameters. Aborting");
            } else {
               logger.info(remoteName + "-> Restarted. Iteration " + iteration);
            }
         }
      }

      if (stillNotOk) {
         logger.info(remoteName + "-> Error in remote optimization");
         throw new IOException("Error in the initial solution creation process");
      } else {
         File solutionFile = exchangedFiles.get(1);
         String fullRemotePath = connSettings.getRemoteWorkDir() + RESULTS_SOLFILE;
         connector.receiveFile(solutionFile.getAbsolutePath(), fullRemotePath, getClass());
         Double objFunctionValue = analyzeSolution(solutionFile, ((GLPKSettings) connSettings).isVerbose());
         logger.info(remoteName + "-> The value of the objective function is: " + objFunctionValue);
         // TODO: this always returns false, should check if every error just throws
         return Pair.of(objFunctionValue, false);
      }
   }
   @Override
   protected Class<? extends ConnectionSettings> getSettingsClass() {
      return GLPKSettings.class;
   }
}
