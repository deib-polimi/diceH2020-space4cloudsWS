/*
Copyright 2016-2017 Eugenio Gianniti
Copyright 2016 Michele Ciavotta

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
package it.polimi.diceH2020.SPACE4CloudWS.solvers;

import it.polimi.diceH2020.SPACE4Cloud.shared.settings.PerformanceSolverType;
import it.polimi.diceH2020.SPACE4CloudWS.solvers.Solver;
import it.polimi.diceH2020.SPACE4CloudWS.solvers.settings.SettingsDealer;
import it.polimi.diceH2020.SPACE4CloudWS.solvers.solversImpl.DagSimSolver.DagSimSolver;
import it.polimi.diceH2020.SPACE4CloudWS.solvers.solversImpl.QNSolver.QNSolver;
import it.polimi.diceH2020.SPACE4CloudWS.solvers.solversImpl.SPNSolver.SPNSolver;
import lombok.Getter;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;

@Component
public class PerformanceSolverFactory {

    @Autowired
    private ApplicationContext ctx;
    @Autowired
    private SettingsDealer dealer;

    @Setter
    @Getter
    private PerformanceSolverType type;

    @PostConstruct
    public void restoreDefaults() {
        type = dealer.getPerformanceSolverDefaults().getType();
    }

    public PerformanceSolver create() throws RuntimeException {
        switch (type) {
            case SPNSolver:
                return ctx.getBean(SPNSolver.class);
            case QNSolver:
                return ctx.getBean(QNSolver.class);
            case DagSimSolver:
                return ctx.getBean(DagSimSolver.class);
            default:
                throw new RuntimeException("Unrecognized solver type");
        }
    }
}
