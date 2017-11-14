/*
Copyright 2016-2017 Eugenio Gianniti
Copyright 2016 Michele Ciavotta
Copyright 2017 Marco Lattuada

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
import it.polimi.diceH2020.SPACE4CloudWS.core.DataProcessor;

import java.io.*;
import java.util.*;
import java.util.function.Predicate;
import lombok.NonNull;
import lombok.Setter;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.beans.factory.annotation.Autowired;

public abstract class PerformanceSolver extends AbstractSolver {

    @Setter(onMethod = @__(@Autowired))
    protected DataProcessor dataProcessor;

    protected List<File> retrieveInputFiles (@NonNull SolutionPerJob solutionPerJob, String extension) {
        String solutionID = solutionPerJob.getParentID();
        String spjID = solutionPerJob.getId();
        String provider = dataProcessor.getProviderName();
        String typeVM = solutionPerJob.getTypeVMselected().getId();
        return dataProcessor.retrieveInputFiles(extension, solutionID, spjID, provider, typeVM);
    }

    @Override
    public Optional<Double> evaluate(@NonNull SolutionPerJob solPerJob) {
        Optional<Double> returnValue = Optional.of(solPerJob.getThroughput());

        if (solPerJob.getChanged()) {
            try {
                putRemoteSubDirectory (solPerJob);
                Pair<List<File>, List<File>> pFiles = createWorkingFiles (solPerJob);
                String jobID = solPerJob.getId ();
                String directory = retrieveRemoteSubDirectory (solPerJob);
                Pair<Double, Boolean> result = run (pFiles, "class" + jobID, directory);
                delete (pFiles.getLeft ());
                if (connSettings.isCleanRemote ()) cleanRemoteSubDirectory (directory);
                solPerJob.setError (result.getRight ());
                returnValue = Optional.of (result.getLeft ());
                removeRemoteSubDirectory (solPerJob);
            } catch (Exception e) {
                logger.error ("Error in SPJ evaluation", e);
                solPerJob.setError (Boolean.TRUE);
                returnValue = Optional.empty ();
            }
        }

        return returnValue;
    }

    public void setAccuracy(double accuracy) {
        connSettings.setAccuracy(accuracy);
    }

    @Override
    public Predicate<Double> feasibilityCheck (SolutionPerJob solutionPerJob, Technology technology) {
        return R -> R <= solutionPerJob.getJob ().getD ();
    }


}
