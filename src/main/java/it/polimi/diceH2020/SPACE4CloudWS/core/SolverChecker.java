/*
Copyright 2016-2017 Eugenio Gianniti

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

class SolverChecker {
    static SPNModel enforceSolverSettings (DataProcessor dataProcessor, Solution solution) {
        solution.getScenario().ifPresent(
                scenario -> {
                    SPNModel technology = scenario.getSwn();
                    Settings override = new Settings();
                    override.setTechnology(technology);

                    switch (technology) {
                        case STORM:
                            override.setSolver(SolverType.SPNSolver);
                            dataProcessor.changeSettings(override);
                            break;
                        case MAPREDUCE:
                            boolean needsSPN = hasSPNInputFiles (dataProcessor, solution);
                            if (needsSPN) {
                                override.setSolver (SolverType.SPNSolver);
                                dataProcessor.changeSettings (override);
                            }
                            break;
                        default:
                            throw new RuntimeException ("The required technology is still not implemented");
                    }
                });
        return solution.getScenario().map(Scenarios::getSwn).orElse(SPNModel.MAPREDUCE);
    }

    private static boolean hasSPNInputFiles (DataProcessor dataProcessor, Solution solution) {
        long netFilesCount = solution.getLstSolutions ().stream ().mapToLong (solutionPerJob ->
                dataProcessor.getSPNFiles (".net", solution.getId (), solutionPerJob.getId (),
                        solution.getProvider (), solutionPerJob.getTypeVMselected ().getId ()).size ()).sum ();
        return netFilesCount > 0;
    }
}
