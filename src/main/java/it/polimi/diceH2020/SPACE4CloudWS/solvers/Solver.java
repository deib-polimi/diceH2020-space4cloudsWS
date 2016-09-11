/*
Copyright 2016 Michele Ciavotta
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
package it.polimi.diceH2020.SPACE4CloudWS.solvers;

import it.polimi.diceH2020.SPACE4Cloud.shared.solution.SolutionPerJob;
import it.polimi.diceH2020.SPACE4CloudWS.services.SshConnectorProxy;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

/**
 * Created by ciavotta on 11/02/16.
 */
public interface Solver {

    void setAccuracy(double accuracy);

    void setMaxDuration(Integer duration);

    void initRemoteEnvironment() throws Exception;

    List<String> pwd() throws Exception;

    SshConnectorProxy getConnector();

    Optional<BigDecimal> evaluate(SolutionPerJob solPerJob);

    void restoreDefaults();

}
