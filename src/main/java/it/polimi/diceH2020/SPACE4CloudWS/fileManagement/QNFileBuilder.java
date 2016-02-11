package it.polimi.diceH2020.SPACE4CloudWS.fileManagement;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.LinkedList;
import java.util.List;

/**
 * Created by ciavotta on 11/02/16.
 */
public class QNFileBuilder implements FileBuilder {

    private Integer concurrency = -1;
    private Integer numberOfMapTasks = -1;
    private Integer numberOfReduceTasks = -1;
    private String mapFilePath = "";
    private String rsFilePath = "";


    @Override
    public FileBuilder setNumberOfReduceTasks(int numberOfReduceTasks) {
        this.numberOfReduceTasks = numberOfReduceTasks;
        return this;
    }

    @Override
    public FileBuilder setNumberOfMapTasks(int numberOfMapTasks) {
        this.numberOfMapTasks = numberOfMapTasks;
        return this;
    }

    @Override
    public FileBuilder setConcurrency(int concurrency) {
        this.concurrency = concurrency;
        return this;
    }

    @Override
    public String build() throws IOException {
        InputStream inputStream = getClass().getResourceAsStream("/QN/MR-multiuser.jsimg");
        List<String> lines = new LinkedList<>();
        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
        String inputLine;
        while ((inputLine = reader.readLine()) != null) {
            String outputLine = inputLine.replace("@@CONCURRENCY@@", concurrency.toString())
                    .replace("@@NUM_MAP@@", numberOfMapTasks.toString())
                    .replace("@@NUM_REDUCE@@", numberOfReduceTasks.toString())
                    .replace("@@MAPPATH@@", mapFilePath)
                    .replace("@@REDUCESHUFFLEPATH@@", rsFilePath);
            lines.add(outputLine);
        }
        return String.join("\n", lines);
    }

    public FileBuilder setMapFilePath(String mapFilePath) {
        this.mapFilePath = mapFilePath;
        return this;
    }

    public FileBuilder setRsFilePath(String rsFilePath) {
        this.rsFilePath = rsFilePath;
        return this;
    }
}

