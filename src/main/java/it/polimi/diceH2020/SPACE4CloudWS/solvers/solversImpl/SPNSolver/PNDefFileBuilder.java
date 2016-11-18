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

class PNDefFileBuilder {
    private Integer concurrency;
    private Integer numberOfMapTasks;
    private Integer numberOfReduceTasks;

    PNDefFileBuilder setNumberOfReduceTasks(int numberOfReduceTasks) {
        this.numberOfReduceTasks = numberOfReduceTasks;
        return this;
    }

    PNDefFileBuilder setNumberOfMapTasks(int numberOfMapTasks) {
        this.numberOfMapTasks = numberOfMapTasks;
        return this;
    }

    PNDefFileBuilder setConcurrency(int concurrency) {
        this.concurrency = concurrency;
        return this;
    }

    String build() throws IOException {
        InputStream inputStream = getClass().getResourceAsStream("/GreatSPN/SingleClass.def");
        List<String> lines = new LinkedList<>();
        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
        String inputLine;
        while ((inputLine = reader.readLine()) != null) {
            String outputLine = inputLine.replace("@@CONCURRENCY@@", concurrency.toString())
                    .replace("@@NUM_MAP@@", numberOfMapTasks.toString())
                    .replace("@@NUM_REDUCE@@", numberOfReduceTasks.toString());
            lines.add(outputLine);
        }
        return String.join("\n", lines);
    }
}
