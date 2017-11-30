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
package it.polimi.diceH2020.SPACE4CloudWS.solvers.settings;

import it.polimi.diceH2020.SPACE4Cloud.shared.settings.MINLPSolverType;
import it.polimi.diceH2020.SPACE4Cloud.shared.settings.PerformanceSolverType;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
@Getter
@Setter
@ConfigurationProperties(prefix = "solvers")
public class SolversSettings {

    private PerformanceSolverType performanceSolverType = PerformanceSolverType.SPNSolver;

    private MINLPSolverType mINLPSolverType = MINLPSolverType.AMPLSOLVER;

    SolversSettings(SolversSettings that) {
        performanceSolverType = that.getPerformanceSolverType();
        mINLPSolverType = that.getMINLPSolverType();
    }

    SolversSettings() {
        super();
    }
}
