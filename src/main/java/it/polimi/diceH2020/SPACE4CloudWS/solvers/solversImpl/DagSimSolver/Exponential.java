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

@Accessors(chain = true)
@Setter(AccessLevel.PACKAGE)
class Exponential extends Distribution {
    private double rate;

    @Override
    protected String formatType() {
        return "exp";
    }

    @Override
    protected String formatParameters() {
        return String.format("{ rate = %g }", rate);
    }
}
