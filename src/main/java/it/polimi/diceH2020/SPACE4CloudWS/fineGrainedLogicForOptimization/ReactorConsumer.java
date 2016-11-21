/*
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

import it.polimi.diceH2020.SPACE4Cloud.shared.solution.SolutionPerJob;
import it.polimi.diceH2020.SPACE4CloudWS.services.SolverProxy;
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
public class ReactorConsumer implements Consumer<Event<ContainerGivenHandN>>{

	private final Logger logger = Logger.getLogger(ReactorConsumer.class.getName());

	@Autowired
	private EventBus eventBus;

	@Autowired
	private WrapperDispatcher dispatcher;

	@Autowired
	private SolverProxy solverCache;

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
		Pair<Boolean,Double> solverResult = calculateDuration(spj);
		if(solverResult.getLeft()){
			double exeTime = solverResult.getRight();
			containerLogic.registerCorrectSolutionPerJob(spj, exeTime);
		}else{
			double exeTime = solverResult.getRight();
			containerLogic.registerFailedSolutionPerJob(spj, exeTime);
		}
		dispatcher.notifyReadyChannel(this);
	}

	private Pair<Boolean, Double> calculateDuration(SolutionPerJob solPerJob) {
		Pair<Optional<Double>, Double> solverResult = solverCache.evaluate(solPerJob);
		Optional<Double> duration = solverResult.getLeft();
		double runtime = solverResult.getRight();
		if (duration.isPresent()) {
			solPerJob.setDuration(duration.get());
			evaluateFeasibility(solPerJob);
			return new ImmutablePair<>(Boolean.TRUE, runtime);
		}
		solverCache.invalidate(solPerJob);
		return new ImmutablePair<>(Boolean.FALSE, runtime);
	}

	private boolean evaluateFeasibility(SolutionPerJob solPerJob) {
		if (solPerJob.getDuration() <= solPerJob.getJob().getD()) {
			solPerJob.setFeasible(true);
			return true;
		}
		solPerJob.setFeasible(false);
		return false;
	}

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

}
