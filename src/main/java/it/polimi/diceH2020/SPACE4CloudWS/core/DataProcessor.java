package it.polimi.diceH2020.SPACE4CloudWS.core;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import it.polimi.diceH2020.SPACE4Cloud.shared.settings.Settings;
import it.polimi.diceH2020.SPACE4Cloud.shared.solution.Matrix;
import it.polimi.diceH2020.SPACE4Cloud.shared.solution.Solution;
import it.polimi.diceH2020.SPACE4Cloud.shared.solution.SolutionPerJob;
import it.polimi.diceH2020.SPACE4CloudWS.services.SolverProxy;
import lombok.NonNull;

/**
 * This component interfaces with Solvers.
 */
@Component
public class DataProcessor {
	@Autowired
	protected SolverProxy solverCache;
	
	
	protected long calculateDuration(@NonNull Solution sol) {
		return calculateDuration(sol.getLstSolutions());
	}

	protected long calculateDuration(@NonNull Matrix matrix){
		return calculateDuration(matrix.getAllSolutions());
	}

	private long calculateDuration(@NonNull List<SolutionPerJob> spjList){
		AtomicLong executionTime = new AtomicLong(); //to support also parallel stream.
		
		spjList.forEach(s -> {
			Instant first = Instant.now();
			Optional<BigDecimal> duration = calculateDuration(s);
			Instant after = Instant.now();
			executionTime.addAndGet(Duration.between(first, after).toMillis());
			
			if (duration.isPresent()) s.setDuration(duration.get().doubleValue());
			else {
				s.setDuration(Double.MAX_VALUE);
				s.setError(Boolean.TRUE);
			}
		});
		
		return executionTime.get();
	}

	protected Optional<BigDecimal> calculateDuration(@NonNull SolutionPerJob solPerJob) {
		Optional<BigDecimal> result = solverCache.evaluate(solPerJob);
		if (! result.isPresent()) solverCache.invalidate(solPerJob);
		return result;
	}
	
	public void restoreDefaults() {
		solverCache.restoreDefaults();
	}

	public void changeSettings(Settings settings) {
		solverCache.changeSettings(settings);
	}
}
