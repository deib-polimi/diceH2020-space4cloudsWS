package it.polimi.diceH2020.SPACE4CloudWS.fileManagement;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.LinkedList;
import java.util.List;

public class PNNetFileBuilder {
    private Double mapRate;
    private Double reduceRate;
    private Double thinkRate;
    private Integer cores;

    public PNNetFileBuilder setMapRate(Double mapRate) {
        this.mapRate = mapRate;
        return this;
    }

    public PNNetFileBuilder setReduceRate(Double reduceRate) {
        this.reduceRate = reduceRate;
        return this;
    }

    public PNNetFileBuilder setThinkRate(Double thinkRate) {
        this.thinkRate = thinkRate;
        return this;
    }

    public PNNetFileBuilder setCores(Integer cores) {
        this.cores = cores;
        return this;
    }

    public String build() throws IOException {
        InputStream inputStream = getClass().getResourceAsStream("/GreatSPN/SingleClass.net");
        List<String> lines = new LinkedList<>();
        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
        String inputLine;
        while ((inputLine = reader.readLine()) != null) {
            String outputLine = inputLine.replace("@@CORES@@", cores.toString())
                    .replace("@@MAP_RATE@@", mapRate.toString())
                    .replace("@@REDUCE_RATE@@", reduceRate.toString())
                    .replace("@@THINK_RATE@@", thinkRate.toString());
            lines.add(outputLine);
        }
        return String.join("\n", lines);
    }

}
