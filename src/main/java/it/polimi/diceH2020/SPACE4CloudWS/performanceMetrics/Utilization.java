package it.polimi.diceH2020.SPACE4CloudWS.performanceMetrics;

import it.polimi.diceH2020.SPACE4Cloud.shared.solution.SolutionPerJob;

public class Utilization {

    private static double apply (double idle, double capacity) {
        return 1 - idle / capacity;
    }

    public static double computeServerUtilization (double idle, SolutionPerJob solutionPerJob) {
        return apply(idle, solutionPerJob.getNumberContainers());
    }
}
