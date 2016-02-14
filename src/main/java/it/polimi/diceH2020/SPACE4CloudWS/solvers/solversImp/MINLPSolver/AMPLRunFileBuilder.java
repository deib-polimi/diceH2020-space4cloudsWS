package it.polimi.diceH2020.SPACE4CloudWS.solvers.solversImp.MINLPSolver;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.LinkedList;
import java.util.List;

public class AMPLRunFileBuilder {
    private String dataFile;
    private String solutionFile;
    private String solverPath;

    public AMPLRunFileBuilder setDataFile(String dataFile) {
        this.dataFile = dataFile;
        return this;
    }

    public AMPLRunFileBuilder setSolutionFile(String solutionFile) {
        this.solutionFile = solutionFile;
        return this;
    }

    public AMPLRunFileBuilder setSolverPath(String solverPath) {
        this.solverPath = solverPath;
        return this;
    }

    public String build() throws IOException {
        InputStream runTemplate = getResourceFileStream("/AMPL/main.run.template");
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

    private InputStream getResourceFileStream(String resourceFilePath) throws IOException {
        return getClass().getResourceAsStream(resourceFilePath);
    }

}
