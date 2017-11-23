/*
Copyright 2016-2017 Eugenio Gianniti
Copyright 2016 Michele Ciavotta
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
import it.polimi.diceH2020.SPACE4Cloud.shared.settings.MINLPSolverType;
import it.polimi.diceH2020.SPACE4Cloud.shared.solution.SolutionPerJob;
import it.polimi.diceH2020.SPACE4CloudWS.solvers.MINLPSolver;
import it.polimi.diceH2020.SPACE4CloudWS.solvers.solversImpl.SPNSolver.SPNSolver;
import it.polimi.diceH2020.SPACE4CloudWS.solvers.MINLPSolverFactory;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
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
public class MINLPSolverProxy {

	private final Logger logger = Logger.getLogger(getClass());

	@Setter(onMethod = @__(@Autowired))
	private MINLPSolverFactory minlpSolverFactory;

	@Getter
	private MINLPSolver minlpSolver;

	@PostConstruct
	private void createMINLPSolver () {
		minlpSolver = minlpSolverFactory.create();
	}

	public void changeSettings (Settings settings) {
		if (settings.getMINLPSolverType() != null) {
			minlpSolverFactory.setType(settings.getMINLPSolverType());
		} else {
			minlpSolverFactory.restoreDefaults ();
		}
		refreshSolver();
	}

	public void restoreDefaults() {
		minlpSolverFactory.restoreDefaults ();
		refreshSolver ();
	}

	private void refreshSolver() {
		createMINLPSolver ();
		minlpSolver.restoreDefaults ();
	}
}
