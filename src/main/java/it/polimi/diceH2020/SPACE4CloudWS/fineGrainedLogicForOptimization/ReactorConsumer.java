/*
Copyright 2017 Eugenio Gianniti
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
package it.polimi.diceH2020.SPACE4CloudWS.fineGrainedLogicForOptimization;

import it.polimi.diceH2020.SPACE4Cloud.shared.settings.Technology;
import it.polimi.diceH2020.SPACE4Cloud.shared.solution.SolutionPerJob;
import it.polimi.diceH2020.SPACE4CloudWS.services.DataService;
import it.polimi.diceH2020.SPACE4CloudWS.services.PerformanceSolverProxy;
import it.polimi.diceH2020.SPACE4CloudWS.solvers.PerformanceSolver;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import reactor.bus.Event;
import reactor.bus.EventBus;
import reactor.fn.Consumer;

import javax.annotation.PostConstruct;
import java.util.Optional;

import static reactor.bus.selector.Selectors.$;

@Component
@Scope("prototype")
public class ReactorConsumer implements Consumer<Event<ContainerGivenHandN>> {

	private final Logger logger = Logger.getLogger(getClass());

	@Setter(onMethod = @__(@Autowired))
	private EventBus eventBus;

	@Setter(onMethod = @__(@Autowired))
	private WrapperDispatcher dispatcher;

	@Setter(onMethod = @__(@Autowired))
	private PerformanceSolverProxy solverCache;

	@Setter(onMethod = @__(@Autowired))
	private DataService dataService;

	@Getter
	private int id;

	public ReactorConsumer(int id) {
		this.id = id;
	}

	public ReactorConsumer() {
	}

	@PostConstruct
	private void register(){
		logger.info("|Q-STATUS| created consumer listening for event message 'channel"+id+"'");
		eventBus.on($("channel"+id), this);
	}

	public void accept(Event<ContainerGivenHandN> ev) {
		SolutionPerJob spj = ev.getData().getSpj();
		ContainerLogicGivenH containerLogic = ev.getData().getHandler();
		logger.info("|Q-STATUS| received spjWrapper"+spj.getId()+"."+spj.getNumberUsers()+" on channel"+id+"\n");
		Pair<Boolean, Long> solverResult = runSolver (spj);
		long exeTime = solverResult.getRight();
		if (solverResult.getLeft()) {
			containerLogic.registerCorrectSolutionPerJob(spj, exeTime);
		} else {
			containerLogic.registerFailedSolutionPerJob(spj, exeTime);
		}
		dispatcher.notifyReadyChannel(this);
	}

	private Pair<Boolean, Long> runSolver (SolutionPerJob solPerJob) {
		Pair<Optional<Double>, Long> solverResult = solverCache.evaluate(solPerJob);
		Optional<Double> solverMetric = solverResult.getLeft();
		long runtime = solverResult.getRight();

		if (solverMetric.isPresent()) {
			PerformanceSolver solver = solverCache.getPerformanceSolver ();
			Technology technology = dataService.getScenario ().getTechnology ();
			Double mainMetric = solver.transformationFromSolverResult (
					solPerJob, technology).apply (solverMetric.get ());
			solver.metricUpdater (solPerJob, technology).accept (mainMetric);
			boolean feasible = solver.feasibilityCheck (solPerJob, technology).test (mainMetric);
			solPerJob.setFeasible (feasible);
			return new ImmutablePair<>(true, runtime);
		}

		solverCache.invalidate(solPerJob);
		return new ImmutablePair<>(false, runtime);
	}
}
