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

import it.polimi.diceH2020.SPACE4Cloud.shared.settings.Technology;

import java.io.*;
import java.util.LinkedList;
import java.util.List;


class PNNetFileBuilder {
    private Double mapRate;
    private Double reduceRate;
    private Double thinkRate;
    private Integer cores;
    private Technology technology;

    PNNetFileBuilder setMapRate(Double mapRate) {
        this.mapRate = mapRate;
        return this;
    }

    PNNetFileBuilder setReduceRate(Double reduceRate) {
        this.reduceRate = reduceRate;
        return this;
    }

    PNNetFileBuilder setThinkRate(Double thinkRate) {
        this.thinkRate = thinkRate;
        return this;
    }

    PNNetFileBuilder setCores(Integer cores) {
        this.cores = cores;
        return this;
    }

    PNNetFileBuilder setTechnology(Technology technology) {
        this.technology = technology;
        return this;
    }

    String build() throws IOException {
        String filename;
        switch (technology) {
            case STORM:
                filename = File.separator + "GreatSPN" + File.separator + "Storm.net";
                break;
            case HADOOP:
            case SPARK:
            default:
                filename = File.separator + "GreatSPN" + File.separator + "SingleClass.net";
        }
        InputStream inputStream = getClass().getResourceAsStream(filename);
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
        return String.format("%s\n", String.join("\n", lines));
    }
}
