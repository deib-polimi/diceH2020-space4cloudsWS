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

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class QNFileBuilder {

    private Integer concurrency = -1;
    private Map<String,String> inputFiles = new HashMap<String,String>();
    private Map<String,String> numMR = new HashMap<String,String>();
    private Double thinkRate;
    private Integer cores;
    private Double significance;
    private Double accuracy;
    private QueueingNetworkModel model = QueueingNetworkModel.SIMPLE;

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
            for(Map.Entry<String, String> entry : inputFiles.entrySet()){
            	String furtherReplace = outputLine.replace("@@"+entry.getKey()+"PATH@@", entry.getValue());
            	outputLine = furtherReplace;
            }
            for(Map.Entry<String, String> entry : numMR.entrySet()){
            	String furtherReplace = outputLine.replace("@@"+entry.getKey()+"@@", entry.getValue()); // @@NR[0-9]*@@ & @@NM[0-9]*@@
            	outputLine = furtherReplace;
            }
            lines.add(outputLine);
        }
        return String.join("\n", lines);
    }

    public QNFileBuilder setQueueingNetworkModel(QueueingNetworkModel queueingNetworkModel) {
        model = queueingNetworkModel;
        return this;
    }
    
    public QNFileBuilder setReplayersInputFiles(Map<String,String> inputFiles){
    	this.inputFiles = inputFiles;
    	return this;
    }
    
    public QNFileBuilder setNumMR(Map<String,String> numMR){
    	this.numMR = numMR;
    	return this;
    }
}
