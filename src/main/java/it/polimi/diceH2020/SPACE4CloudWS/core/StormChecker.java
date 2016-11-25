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
package it.polimi.diceH2020.SPACE4CloudWS.core;

import it.polimi.diceH2020.SPACE4Cloud.shared.settings.SPNModel;
import it.polimi.diceH2020.SPACE4Cloud.shared.settings.Scenarios;
import it.polimi.diceH2020.SPACE4Cloud.shared.settings.Settings;
import it.polimi.diceH2020.SPACE4Cloud.shared.settings.SolverType;
import it.polimi.diceH2020.SPACE4Cloud.shared.solution.Solution;

class StormChecker {
    static SPNModel enforceSolverSettings (DataProcessor dataProcessor, Solution solution) {
        solution.getScenario().ifPresent(scenario -> {
            SPNModel technology = scenario.getSwn();
            if (technology == SPNModel.STORM) {
                Settings override = new Settings();
                override.setSolver(SolverType.SPNSolver);
                override.setTechnology(technology);
                dataProcessor.changeSettings(override);
            }
        });
        return solution.getScenario().map(Scenarios::getSwn).orElse(SPNModel.MAPREDUCE);
    }
}
