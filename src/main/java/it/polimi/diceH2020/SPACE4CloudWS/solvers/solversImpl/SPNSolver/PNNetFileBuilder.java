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
package it.polimi.diceH2020.SPACE4CloudWS.solvers.solversImpl.SPNSolver;

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
