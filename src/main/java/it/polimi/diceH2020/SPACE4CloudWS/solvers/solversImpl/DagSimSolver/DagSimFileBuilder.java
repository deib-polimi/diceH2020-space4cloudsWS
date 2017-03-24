/*
Copyright 2017 Eugenio Gianniti

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
package it.polimi.diceH2020.SPACE4CloudWS.solvers.solversImpl.DagSimSolver;

import lombok.AccessLevel;
import lombok.Setter;
import lombok.experimental.Accessors;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

@Accessors(chain = true)
@Setter(AccessLevel.PACKAGE)
class DagSimFileBuilder {
    private Integer containers;
    private Integer users;
    private Distribution thinkTime;
    private Integer maxJobs;
    private Double quantile;

    @Setter(AccessLevel.NONE)
    private List<Stage> stages = new LinkedList<>();

    DagSimFileBuilder setExponentialThinkTime(double Z) {
        thinkTime = new Exponential().setRate(1 / Z);
        return this;
    }

    String build() throws IOException {
        List<String> content = new LinkedList<>();
        InputStream template = getClass().getResourceAsStream("/DagSim/template.lua");
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(template))) {
            final String stagesString = formatStages();
            String line;
            while ((line = reader.readLine()) != null) {
                String output = line
                        .replace("@@STAGES@@", stagesString)
                        .replace("@@CORES@@", containers.toString())
                        .replace("@@USERS@@", users.toString())
                        .replace("@@THINK_PDF@@", thinkTime.toString())
                        .replace("@@MAXJOBS@@", maxJobs.toString())
                        .replace("@@QUANTILE@@", quantile.toString());
                content.add(output);
            }
        }
        return String.join("\n", content);
    }

    DagSimFileBuilder addStage(Stage stage) {
        stages.add(stage);
        return this;
    }

    private String formatStages() {
        String arrayContent = stages.stream().map(Stage::toString).collect(Collectors.joining(", "));
        return String.format("{ %s }", arrayContent);
    }
}
