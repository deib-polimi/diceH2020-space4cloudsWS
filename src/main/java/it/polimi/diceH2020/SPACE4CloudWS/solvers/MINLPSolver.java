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
package it.polimi.diceH2020.SPACE4CloudWS.solvers;

import com.jcraft.jsch.JSchException;

import it.polimi.diceH2020.SPACE4Cloud.shared.solution.Matrix;
import it.polimi.diceH2020.SPACE4Cloud.shared.solution.MatrixHugeHoleException;
import it.polimi.diceH2020.SPACE4Cloud.shared.solution.Solution;
import it.polimi.diceH2020.SPACE4Cloud.shared.solution.SolutionPerJob;

import it.polimi.diceH2020.SPACE4CloudWS.solvers.solversImpl.MINLPSolver.MINLPDataFileBuilder;
import it.polimi.diceH2020.SPACE4CloudWS.solvers.solversImpl.MINLPSolver.MINLPSolFileParser;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;
import java.util.Optional;

import javax.validation.constraints.NotNull;

import lombok.NonNull;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;

public abstract class MINLPSolver extends AbstractSolver {
   
   protected static final String RESULTS_SOLFILE = "/results/solution.sol";
   protected static final String REMOTE_RESULTS = "/results";
   protected static final String REMOTEPATH_DATA_RUN = "data.run";

   abstract protected Pair<Double, Boolean> run (@NotNull Pair<List<File>, List<File>> pFiles, String remoteName) throws JSchException, IOException;

   public Optional<Double> evaluate(@NonNull Matrix matrix, @NonNull Solution solution)
         throws MatrixHugeHoleException {
         try {
            List<File> filesList = createWorkingFiles(matrix, solution);
            Pair<List<File>, List<File>> pair = new ImmutablePair<>(filesList, new ArrayList<>());
            Pair<Double, Boolean> result = run(pair, "Knapsack solution");
            File resultsFile = filesList.get(1);
            new MINLPSolFileParser().updateResults(solution, matrix, resultsFile);
            delete(filesList);
            return Optional.of(result.getLeft());
         } catch (IOException | JSchException e) {
            logger.error("Evaluate Matrix: no result due to an exception", e);
            return Optional.empty();
         }
   }

   public void initializeSpj(Solution solution, Matrix matrix) {
      initializeSolution(solution, matrix);
   }


   public static void initializeSolution(Solution solution, Matrix matrix) {
      solution.getLstSolutions().clear();
      for (Entry<String,SolutionPerJob[]> entry : matrix.entrySet()) {
         solution.addSolutionPerJob(matrix.getCell(matrix.getID(entry.getValue()[0].getId()),
                  entry.getValue()[0].getNumberUsers()));
      }
      solution.setFeasible(false);
   }

   protected List<File> createWorkingFiles(Matrix matrix, Solution sol) throws IOException, MatrixHugeHoleException {
      MINLPDataFileBuilder dataFileBuilder = new MINLPDataFileBuilder(dataService.getData(), matrix);
      String prefix = String.format("MINLP-%s-matrix-", sol.getId());
      File dataFile = fileUtility.provideTemporaryFile(prefix, ".dat");
      dataFileBuilder.createDataFile(dataFile);
      File resultsFile = fileUtility.provideTemporaryFile(prefix, ".sol");
      List<File> lst = new ArrayList<>(2);
      lst.add(dataFile);
      lst.add(resultsFile);
      return lst;
   }

   protected void sendFile(String localPath, String remotePath) throws Exception {
      InputStream in = this.getClass().getResourceAsStream(localPath);
      File tempFile = fileUtility.provideTemporaryFile("S4C-temp", null);
      FileOutputStream out = new FileOutputStream(tempFile);
      IOUtils.copy(in, out);
      connector.sendFile(tempFile.getAbsolutePath(), remotePath, getClass());
      if (fileUtility.delete(tempFile)) logger.debug(tempFile + " deleted");
   }
   
   protected void clearResultDir() throws JSchException, IOException {
      String command = "rm -rf " + connSettings.getRemoteWorkDir() + REMOTE_RESULTS + "/*";
      connector.exec(command, getClass());
   }

   protected Double analyzeSolution(File solFile, boolean verbose) throws IOException {
      String fileToString = FileUtils.readFileToString(solFile);
      String objective = "knapsack_obj = ";
      int startPos = fileToString.indexOf(objective);
      int endPos = fileToString.indexOf('\n', startPos);
      Double objFunctionValue = Double.parseDouble(fileToString.substring(startPos + objective.length(), endPos));

      if (verbose) {
         logger.info(fileToString);
         logger.info(objFunctionValue);
      }
      return objFunctionValue;
   }

}
