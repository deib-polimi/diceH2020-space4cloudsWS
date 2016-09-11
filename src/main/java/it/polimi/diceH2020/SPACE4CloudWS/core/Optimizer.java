/*
Copyright 2016 Michele Ciavotta
Copyright 2016 Jacopo Rigoli
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
package it.polimi.diceH2020.SPACE4CloudWS.core;

import it.polimi.diceH2020.SPACE4Cloud.shared.settings.Settings;
import it.polimi.diceH2020.SPACE4Cloud.shared.solution.IEvaluator;
import it.polimi.diceH2020.SPACE4Cloud.shared.solution.Solution;
import it.polimi.diceH2020.SPACE4Cloud.shared.solution.SolutionPerJob;
import it.polimi.diceH2020.SPACE4CloudWS.services.DataService;
import it.polimi.diceH2020.SPACE4CloudWS.services.SolverProxy;
import lombok.NonNull;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

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

	public void restoreDefaults() {
		solverCache.restoreDefaults();
	}

	// read an input file and type value of accuracy and cycles
	public void changeSettings(Settings settings) {
		solverCache.changeSettings(settings);
	}

}
