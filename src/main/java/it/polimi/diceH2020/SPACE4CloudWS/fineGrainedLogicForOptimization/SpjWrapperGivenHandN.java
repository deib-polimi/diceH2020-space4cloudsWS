package it.polimi.diceH2020.SPACE4CloudWS.fineGrainedLogicForOptimization;

import it.polimi.diceH2020.SPACE4Cloud.shared.solution.SolutionPerJob;

public class SpjWrapperGivenHandN {
	private SolutionPerJob spj;
	private SpjOptimizerGivenH handler;
	
	public SpjWrapperGivenHandN(SolutionPerJob spj, SpjOptimizerGivenH handler){
		this.spj = spj;
		this.handler = handler;
	}
	
	public SolutionPerJob getSpj() {
		return spj;
	}
	public SpjOptimizerGivenH  getHandler() {
		return handler;
	}
}
