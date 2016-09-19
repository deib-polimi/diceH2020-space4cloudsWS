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
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import reactor.bus.Event;
import reactor.bus.EventBus;
import reactor.fn.Consumer;

import javax.annotation.PostConstruct;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

import static reactor.bus.selector.Selectors.$;

@Component
@Scope("prototype")
public class ReactorConsumer implements Consumer<Event<SpjWrapperGivenHandN>>{

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

	@Override
	public void accept(Event<SpjWrapperGivenHandN> ev) {
		SolutionPerJob spj = ev.getData().getSpj();
		SpjOptimizerGivenH spjOptimizer = ev.getData().getHandler();
		logger.info("|Q-STATUS| received spjWrapper"+spj.getId()+"."+spj.getNumberUsers()+" on channel"+id+"\n");
		Instant first = Instant.now();
		if(calculateDuration(spj)){
			Instant after = Instant.now();
			double exeTime = Duration.between(first, after).toMillis();
			spjOptimizer.registerCorrectSolutionPerJob(spj, exeTime);
		}else{
			Instant after = Instant.now();
			double exeTime = Duration.between(first, after).toMillis();
			spjOptimizer.registerFailedSolutionPerJob(spj, exeTime);
		}
		dispatcher.notifyReadyChannel(this);
	}


	private boolean calculateDuration(SolutionPerJob solPerJob) {
		Optional<BigDecimal> duration = solverCache.evaluate(solPerJob);
		if (duration.isPresent()) {
			solPerJob.setDuration(duration.get().doubleValue());
			evaluateFeasibility(solPerJob);
			return true;
		}
		solverCache.invalidate(solPerJob);
		return false;
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
