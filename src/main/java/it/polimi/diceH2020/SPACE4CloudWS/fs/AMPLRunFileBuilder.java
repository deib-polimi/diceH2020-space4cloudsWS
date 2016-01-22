package it.polimi.diceH2020.SPACE4CloudWS.fs;

import java.io.*;

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
        InputStream runTemplate = getResourceFileStream("/myFiles/main.run.template");
        BufferedReader reader = new BufferedReader(new InputStreamReader(runTemplate));
        File file = File.createTempFile("S4C-run-", ".run", new File(FileUtility.LOCAL_DYNAMIC_FOLDER));
        BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file)));
        String line;
        while ((line = reader.readLine()) != null) {
            String outputLine = line.replace("@@SOLVER_PATH@@", solverPath)
                    .replace("@@DATA_FILE_PATH@@", dataFile)
                    .replace("@@OUTPUT_PATH@@", solutionFile);
            writer.write(outputLine);
        }
        writer.close();
        return file.getAbsolutePath();
    }

    private InputStream getResourceFileStream(String resourceFilePath) throws IOException {
        return getClass().getResourceAsStream(resourceFilePath);
    }

}
