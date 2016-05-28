package it.polimi.diceH2020.SPACE4CloudWS.services;

import it.polimi.diceH2020.SPACE4Cloud.shared.settings.Settings;
import it.polimi.diceH2020.SPACE4Cloud.shared.solution.SolutionPerJob;
import it.polimi.diceH2020.SPACE4CloudWS.solvers.Solver;
import it.polimi.diceH2020.SPACE4CloudWS.solvers.solversImpl.SolverFactory;
import lombok.NonNull;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.math.BigDecimal;
import java.util.Optional;

/**
 * Created by ciavotta on 15/02/16.
 */
@Service
public class SolverProxy {
	private final Logger logger = Logger.getLogger(getClass());

	@Autowired
	private SolverFactory solverFactory;

	private Solver solver;

	@PostConstruct
	private void setSolver() {
		solver = solverFactory.create();
	}

	public void changeSettings(Settings settings){
		solverFactory.setType(settings.getSolver());
		refreshSolver();
		solver.setAccuracy(settings.getAccuracy());
		solver.setMaxDuration(settings.getSimDuration());
	}

	public void restoreDefaults() {
		solverFactory.restoreDefaults();
		refreshSolver();
	}

	private void refreshSolver() {
		solver = solverFactory.create();
		solver.restoreDefaults();
	}

	@Cacheable(value="cachedEval")
	public Optional<BigDecimal> evaluate(@NonNull SolutionPerJob solPerJob) {
		logger.info("Cache missing. Evaluation.");
		return solver.evaluate(solPerJob);
	}

	@CacheEvict(cacheNames = "cachedEval")
	public void invalidate(@NonNull SolutionPerJob solutionPerJob) {
		logger.info("Evicting stale cache data.");
	}

}
