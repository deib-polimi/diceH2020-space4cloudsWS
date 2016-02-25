package it.polimi.diceH2020.SPACE4CloudWS.solvers.solversImpl.QNSolver;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.LinkedList;
import java.util.List;

/**
 * Created by ciavotta on 11/02/16.
 */
public class QNFileBuilder {

    private Integer concurrency = -1;
    private Integer numberOfMapTasks = -1;
    private Integer numberOfReduceTasks = -1;
    private String mapFilePath = "";
    private String RSFilePath = "";
    private Double thinkRate;
    private Integer cores;
    private Double significance;
    private Double accuracy;

    public QNFileBuilder setNumberOfReduceTasks(int numberOfReduceTasks) {
        this.numberOfReduceTasks = numberOfReduceTasks;
        return this;
    }

    public QNFileBuilder setNumberOfMapTasks(int numberOfMapTasks) {
        this.numberOfMapTasks = numberOfMapTasks;
        return this;
    }

    public QNFileBuilder setConcurrency(int concurrency) {
        this.concurrency = concurrency;
        return this;
    }

    public QNFileBuilder setThinkRate(Double thinkRate) {
        this.thinkRate = thinkRate;
        return this;
    }

    public QNFileBuilder setCores(Integer cores) {
        this.cores = cores;
        return this;
    }

    public QNFileBuilder setSignificance(Double significance) {
        this.significance = significance;
        return this;
    }

    public QNFileBuilder setAccuracy(Double accuracy) {
        this.accuracy = accuracy;
        return this;
    }

    public String build() throws IOException {
        InputStream inputStream = getClass().getResourceAsStream("/QN/MR-multiuser.jsimg");
        List<String> lines = new LinkedList<>();
        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
        String inputLine;
        while ((inputLine = reader.readLine()) != null) {
            String outputLine = inputLine
                    .replace("@@CONCURRENCY@@", concurrency.toString())
                    .replace("@@NUM_MAP@@", numberOfMapTasks.toString())
                    .replace("@@NUM_REDUCE@@", numberOfReduceTasks.toString())
                    .replace("@@MAPPATH@@", mapFilePath)
                    .replace("@@REDUCESHUFFLEPATH@@", RSFilePath)
                    .replace("@@NCORES@@", cores.toString())
                    .replace("@@THINK_RATE@@", thinkRate.toString())
                    .replace("@@ALPHA@@", significance.toString())
                    .replace("@@ACCURACY@@", accuracy.toString());
            lines.add(outputLine);
        }
        return String.join("\n", lines);
    }

    public QNFileBuilder setMapFilePath(String mapFilePath) {
        this.mapFilePath = mapFilePath;
        return this;
    }

    public QNFileBuilder setRsFilePath(String rsFilePath) {
        this.RSFilePath = rsFilePath;
        return this;
    }
}

