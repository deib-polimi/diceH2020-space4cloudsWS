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

import it.polimi.diceH2020.SPACE4Cloud.shared.settings.Technology;
import it.polimi.diceH2020.SPACE4Cloud.shared.solution.SolutionPerJob;

import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;

public interface Solver {

    void setMaxDuration(Integer duration);

    void initRemoteEnvironment() throws Exception;

    Optional<Double> evaluate(SolutionPerJob solPerJob);

    void restoreDefaults();

    Function<Double, Double> transformationFromSolverResult (SolutionPerJob solutionPerJob, Technology technology);

    Predicate<Double> feasibilityCheck (SolutionPerJob solutionPerJob, Technology technology);

    Consumer<Double> metricUpdater (SolutionPerJob solutionPerJob, Technology technology);

    BiConsumer<SolutionPerJob, Double> initialResultSaver (Technology technology);

}
