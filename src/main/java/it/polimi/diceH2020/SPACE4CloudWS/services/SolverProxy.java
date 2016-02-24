package it.polimi.diceH2020.SPACE4CloudWS.services;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.LoadingCache;
import it.polimi.diceH2020.SPACE4Cloud.shared.solution.SolutionPerJob;
import it.polimi.diceH2020.SPACE4CloudWS.core.Optimizer;
import it.polimi.diceH2020.SPACE4CloudWS.solvers.Solver;
import it.polimi.diceH2020.SPACE4CloudWS.solvers.solversImpl.SolverFactory;
import lombok.NonNull;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.concurrent.ExecutionException;

import javax.annotation.PostConstruct;

/**
 * Created by ciavotta on 15/02/16.
 */
@Service
public class SolverProxy {
	private static Logger logger = Logger.getLogger(SolverProxy.class.getName());
	@Autowired
	private SolverFactory solverFactory;
	
	Solver solver;
	
	@PostConstruct
	private void setSolver() {
		solver = solverFactory.create();
	}

	public void setAccuracy(double accuracy){
		solver.setAccuracy(accuracy);
	}
	
	@Cacheable(value="cachedEval")
	public Optional<BigDecimal> evaluate(@NonNull SolutionPerJob solPerJob) {
		logger.info("Cache missing. Evaluation.");
		return solver.evaluate(solPerJob);	
	}
	
}
