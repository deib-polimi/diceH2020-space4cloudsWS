package it.polimi.diceH2020.SPACE4CloudWS.solvers.solversImpl.MINLPSolver;

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
    private AMPLModelType model = AMPLModelType.CENTRALIZED;

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

    AMPLRunFileBuilder setModelType(AMPLModelType amplModelType) {
        model = amplModelType;
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
        switch (model) {
            case CENTRALIZED:
                resourceFilePath = "/AMPL/main_centralized.template.run";
                break;
            case KNAPSACK:
                resourceFilePath = "/AMPL/main_knapsack.template.run";
                break;
            default:
                throw new AssertionError("The required model is still not implemented");
        }
        return getClass().getResourceAsStream(resourceFilePath);
    }

}
