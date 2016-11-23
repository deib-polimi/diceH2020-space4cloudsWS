/*
Copyright 2016 Michele Ciavotta
Copyright 2016 Eugenio Gianniti
Copyright 2016 Jacopo Rigoli

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
package it.polimi.diceH2020.SPACE4CloudWS.services;

import it.polimi.diceH2020.SPACE4Cloud.shared.settings.Settings;
import it.polimi.diceH2020.SPACE4Cloud.shared.settings.SolverType;
import it.polimi.diceH2020.SPACE4Cloud.shared.solution.SolutionPerJob;
import it.polimi.diceH2020.SPACE4CloudWS.solvers.Solver;
import it.polimi.diceH2020.SPACE4CloudWS.solvers.solversImpl.SolverFactory;
import lombok.NonNull;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

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
		if(settings.getSolver() != null){
			solverFactory.setType(settings.getSolver());
		}else{
			solverFactory.setType(SolverType.QNSolver); //DEFAULT
		}
		refreshSolver();
		if(settings.getAccuracy() != null) solver.setAccuracy(settings.getAccuracy());
		if(settings.getSimDuration() != null) solver.setMaxDuration(settings.getSimDuration());
	}

	public void restoreDefaults() {
		solverFactory.restoreDefaults();
		refreshSolver();
	}

	private void refreshSolver() {
		solver = solverFactory.create();
		solver.restoreDefaults();
	}

	@Cacheable(value=it.polimi.diceH2020.SPACE4CloudWS.main.Configurator.CACHE_NAME,
			keyGenerator = it.polimi.diceH2020.SPACE4CloudWS.main.Configurator.SPJ_KEYGENERATOR)
	public Pair<Optional<Double>, Long> evaluate(@NonNull SolutionPerJob solPerJob) {
		Instant first = Instant.now();
		logger.info("Cache missing. Evaluation with "+ solver.getClass().getSimpleName()+".");
		Optional<Double> optionalResult = solver.evaluate(solPerJob);
		Instant after = Instant.now();
		return new ImmutablePair<>(optionalResult, Duration.between(first, after).toMillis());
	}

	@CacheEvict(cacheNames = it.polimi.diceH2020.SPACE4CloudWS.main.Configurator.CACHE_NAME)
	public void invalidate(@NonNull SolutionPerJob solutionPerJob) {
		String message = String.format("Evicting stale cache data about %s/%s",
				solutionPerJob.getParentID(), solutionPerJob.getId());
		logger.info(message);
	}

}
