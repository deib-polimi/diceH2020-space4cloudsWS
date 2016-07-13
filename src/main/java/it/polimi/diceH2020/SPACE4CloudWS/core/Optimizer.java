package it.polimi.diceH2020.SPACE4CloudWS.core;


import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;

import it.polimi.diceH2020.SPACE4Cloud.shared.solution.IEvaluator;
import it.polimi.diceH2020.SPACE4Cloud.shared.solution.Solution;
import it.polimi.diceH2020.SPACE4Cloud.shared.solution.SolutionPerJob;
import it.polimi.diceH2020.SPACE4CloudWS.services.DataService;
import it.polimi.diceH2020.SPACE4CloudWS.services.SolverProxy;
import lombok.NonNull;

public abstract class Optimizer {
	
	@Autowired
	protected DataService dataService;
	
	@Autowired
	protected SolverProxy solverCache;

	@Autowired
	protected IEvaluator evaluator;
	
	
	public void evaluate(@NonNull Solution sol) {
		evaluate(sol.getLstSolutions());
		
		sol.setEvaluated(false);
		evaluator.evaluate(sol);
	}
	
	public void evaluate(@NonNull Matrix matrix){
		evaluate(matrix.getAllSolutions());
	}
	
	protected void evaluate(@NonNull List<SolutionPerJob> spjList){
		spjList.forEach(s -> {
			Optional<BigDecimal> duration = calculateDuration(s);
			if (duration.isPresent()) s.setDuration(duration.get().doubleValue());
			else {
				s.setDuration(Double.MAX_VALUE);
				s.setError(Boolean.TRUE);
			}
		});
	}
	
	protected Optional<BigDecimal> calculateDuration(@NonNull SolutionPerJob solPerJob) {
		Optional<BigDecimal> result = solverCache.evaluate(solPerJob);
		if (! result.isPresent()) solverCache.invalidate(solPerJob);
		return result;
	}
	
	
}
