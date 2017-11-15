/*
Copyright 2016 Eugenio Gianniti

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
package it.polimi.diceH2020.SPACE4CloudWS.solvers.solversImpl.AMPLSolver;


import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.LinkedList;
import java.util.List;

class AMPLRunFileBuilder {
    private String dataFile;
    private String solutionFile;
    private String solverPath;

    AMPLRunFileBuilder setDataFile(String dataFile) {
        this.dataFile = dataFile;
        return this;
    }

    AMPLRunFileBuilder setSolutionFile(String solutionFile) {
        this.solutionFile = solutionFile;
        return this;
    }

    AMPLRunFileBuilder setSolverPath(String solverPath) {
        this.solverPath = solverPath;
        return this;
    }

    String build() throws IOException {
        InputStream runTemplate = getResourceFileStream();
        BufferedReader reader = new BufferedReader(new InputStreamReader(runTemplate));
        String line;
        List<String> outputLines = new LinkedList<>();
        while ((line = reader.readLine()) != null) {
            String outputLine = line.replace("@@SOLVER_PATH@@", solverPath)
                    .replace("@@DATA_FILE_PATH@@", dataFile)
                    .replace("@@OUTPUT_PATH@@", solutionFile);
            outputLines.add(outputLine);
        }
        return String.join("\n", outputLines);
    }

    private InputStream getResourceFileStream() throws IOException {
        String resourceFilePath;
        resourceFilePath = "/AMPL/main_knapsack.template.run";
        return getClass().getResourceAsStream(resourceFilePath);
    }
}
