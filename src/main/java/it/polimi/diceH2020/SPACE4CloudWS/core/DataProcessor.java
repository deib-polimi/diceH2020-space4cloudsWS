package it.polimi.diceH2020.SPACE4CloudWS.core;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import it.polimi.diceH2020.SPACE4Cloud.shared.settings.Settings;
import it.polimi.diceH2020.SPACE4Cloud.shared.solution.Matrix;
import it.polimi.diceH2020.SPACE4Cloud.shared.solution.Solution;
import it.polimi.diceH2020.SPACE4Cloud.shared.solution.SolutionPerJob;
import it.polimi.diceH2020.SPACE4CloudWS.fineGrainedLogicForOptimization.ContainerLogicForEvaluation;
import it.polimi.diceH2020.SPACE4CloudWS.services.SolverProxy;
import lombok.NonNull;

/**
 * This component interfaces with Solvers.
 */
@Component
public class DataProcessor {
	
	private final Logger logger = Logger.getLogger(getClass());
	
	@Autowired
	protected SolverProxy solverCache;
	
	@Autowired
	private ApplicationContext context;
	
	protected long calculateDuration(@NonNull Solution sol) {
		return calculateDuration(sol.getLstSolutions());
	}

	protected void calculateDuration(@NonNull Matrix matrix){
		 calculateDuration2(matrix.getAllSolutions());
	}
	
	private void calculateDuration2(@NonNull List<SolutionPerJob> spjList){ //TODO collapse also calculateDuration in this method, by implementing fineGrained Matrix also in the public case
		spjList.forEach(s -> {
				ContainerLogicForEvaluation container =  (ContainerLogicForEvaluation) context.getBean("containerLogicForEvaluation",s);
				container.start();
		});
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
		else logger.info(solPerJob.getId()+"->"+" Duration with "+solPerJob.getNumberVM()+"VM and h="+solPerJob.getNumberUsers()+"has been calculated" +result.get());
		return result;
	}
	
	public void restoreDefaults() {
		solverCache.restoreDefaults();
	}

	public void changeSettings(Settings settings) {
		solverCache.changeSettings(settings);
	}
}
