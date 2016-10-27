package it.polimi.diceH2020.SPACE4CloudWS.fineGrainedLogicForOptimization;

import it.polimi.diceH2020.SPACE4Cloud.shared.solution.SolutionPerJob;

interface ContainerLogicGivenH {

	void registerCorrectSolutionPerJob(SolutionPerJob spj, double exeTime);

	void registerFailedSolutionPerJob(SolutionPerJob spj, double exeTime);

}
