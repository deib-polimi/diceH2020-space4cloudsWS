/*
Copyright 2016 Michele Ciavotta
Copyright 2016 Eugenio Gianniti
Copyright 2016 Jacopo Rigoli

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
package it.polimi.diceH2020.SPACE4CloudWS.solvers.solversImpl.QNSolver;

import java.io.*;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

class QNFileBuilder {
    private Integer concurrency = -1;
    private Map<String, String> inputFiles = new HashMap<>();
    private Map<String, String> numMR = new HashMap<>();
    private Double thinkRate;
    private Integer cores;
    private Double significance;
    private Double accuracy;
    private QueueingNetworkModel model = QueueingNetworkModel.SIMPLE;

    QNFileBuilder setConcurrency(int concurrency) {
        this.concurrency = concurrency;
        return this;
    }

    QNFileBuilder setThinkRate(Double thinkRate) {
        this.thinkRate = thinkRate;
        return this;
    }

    QNFileBuilder setCores(Integer cores) {
        this.cores = cores;
        return this;
    }

    QNFileBuilder setSignificance(Double significance) {
        this.significance = significance;
        return this;
    }

    QNFileBuilder setAccuracy(Double accuracy) {
        this.accuracy = accuracy;
        return this;
    }

    String build() throws IOException {
        String fileName;
        switch (model) {
            case Q1:
                fileName = File.separator+"QN"+File.separator+"Q1-Distro.jsimg";
                break;
            case CLASS_SWITCH:
                fileName = File.separator+"QN"+File.separator+"MR-multiUser-classSwitch.jsimg";
                break;
            case SIMPLE:
            default:
                fileName = File.separator+"QN"+File.separator+"MR-multiuser.jsimg";
        }
        InputStream inputStream = getClass().getResourceAsStream(fileName);
        List<String> lines = new LinkedList<>();
        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
        String inputLine;
        while ((inputLine = reader.readLine()) != null) {
            String outputLine = inputLine
                    .replace("@@CONCURRENCY@@", concurrency.toString())
                    .replace("@@CAPACITY@@", cores.toString())
                    .replace("@@THINK_RATE@@", thinkRate.toString())
                    .replace("@@ALPHA@@", significance.toString())
                    .replace("@@ACCURACY@@", accuracy.toString());
            for (Map.Entry<String, String> entry : inputFiles.entrySet()) {
                outputLine = outputLine.replace("@@"+entry.getKey()+"PATH@@", entry.getValue());
            }
            for (Map.Entry<String, String> entry : numMR.entrySet()) {
                outputLine = outputLine.replace("@@"+entry.getKey()+"@@", entry.getValue());
            }
            lines.add(outputLine);
        }
        return String.join("\n", lines);
    }

    QNFileBuilder setQueueingNetworkModel(QueueingNetworkModel queueingNetworkModel) {
        model = queueingNetworkModel;
        return this;
    }

    QNFileBuilder setReplayersInputFiles(Map<String,String> inputFiles){
        this.inputFiles = inputFiles;
        return this;
    }

    QNFileBuilder setNumMR(Map<String,String> numMR){
        this.numMR = numMR;
        return this;
    }
}
