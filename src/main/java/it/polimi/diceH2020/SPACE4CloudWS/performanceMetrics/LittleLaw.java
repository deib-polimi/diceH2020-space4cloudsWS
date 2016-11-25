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
package it.polimi.diceH2020.SPACE4CloudWS.performanceMetrics;

import it.polimi.diceH2020.SPACE4Cloud.shared.solution.SolutionPerJob;

public class LittleLaw {

    private static double apply(int users, double throughput, double thinkTime) {
        return users / throughput - thinkTime;
    }

    public static double computeResponseTime(double throughput, SolutionPerJob solutionPerJob) {
        int numUsers = solutionPerJob.getNumberUsers();
        double thinkTime = solutionPerJob.getJob().getThink();
        return apply(numUsers, throughput, thinkTime);
    }
}
