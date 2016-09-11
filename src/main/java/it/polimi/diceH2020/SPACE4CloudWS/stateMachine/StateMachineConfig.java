/*
Copyright 2016 Michele Ciavotta

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
package it.polimi.diceH2020.SPACE4CloudWS.stateMachine;

import org.springframework.context.annotation.Configuration;
import org.springframework.statemachine.config.EnableStateMachine;
import org.springframework.statemachine.config.EnumStateMachineConfigurerAdapter;
import org.springframework.statemachine.config.builders.StateMachineStateConfigurer;
import org.springframework.statemachine.config.builders.StateMachineTransitionConfigurer;

import java.util.EnumSet;

@Configuration
@EnableStateMachine
public class StateMachineConfig
		extends EnumStateMachineConfigurerAdapter<States, Events> {

	@Override
	public void configure(StateMachineStateConfigurer<States, Events> states)
			throws Exception {
		states
				.withStates()
				.initial(States.STARTING)
				.states(EnumSet.allOf(States.class));
	}

	@Override
	public void configure(StateMachineTransitionConfigurer<States, Events> transitions)
			throws Exception {
		transitions
				.withExternal()
				.source(States.STARTING)
				.target(States.IDLE)
				.event(Events.MIGRATE)
				.and()
				.withExternal()
				.source(States.STARTING)
				.target(States.ERROR)
				.event(Events.STOP);

		transitions
				.withExternal()
				.source(States.IDLE)
				.target(States.CHARGED_INPUTDATA)
				.event(Events.TO_CHARGED_INPUTDATA)
				.and()
				.withExternal()
				.source(States.IDLE)
				.target(States.CHARGED_INITSOLUTION)
				.event(Events.TO_CHARGED_INITSOLUTION)
				.and()
				.withExternal()
				.source(States.IDLE)
				.target(States.ERROR)
				.event(Events.STOP);


		transitions
				.withExternal()
				.source(States.CHARGED_INPUTDATA)
				.target(States.RUNNING_INIT)
				.event(Events.TO_RUNNING_INIT)
				.and()
				.withExternal()
				.source(States.CHARGED_INPUTDATA)
				.target(States.ERROR)
				.event(Events.STOP)
				.and()
				.withExternal()
				.source(States.CHARGED_INPUTDATA)
				.target(States.IDLE)
				.event(Events.RESET);

		transitions
				.withExternal()
				.source(States.RUNNING_INIT)
				.target(States.CHARGED_INITSOLUTION)
				.event(Events.TO_CHARGED_INITSOLUTION)
				.and()
				.withExternal()
				.source(States.RUNNING_INIT)
				.target(States.ERROR)
				.event(Events.STOP)
				.and()
				.withExternal()
				.source(States.RUNNING_INIT)
				.target(States.IDLE)
				.event(Events.RESET);


		transitions
				.withExternal()
				.source(States.CHARGED_INITSOLUTION)
				.target(States.EVALUATING_INIT)
				.event(Events.TO_EVALUATING_INIT)
				.and()
				.withExternal()
				.source(States.CHARGED_INITSOLUTION)
				.target(States.ERROR)
				.event(Events.STOP)
				.and()
				.withExternal()
				.source(States.CHARGED_INITSOLUTION)
				.target(States.IDLE)
				.event(Events.RESET);


		transitions
				.withExternal()
				.source(States.EVALUATING_INIT)
				.target(States.EVALUATED_INITSOLUTION)
				.event(Events.TO_EVALUATED_INITSOLUTION)
				.and()
				.withExternal()
				.source(States.EVALUATING_INIT)
				.target(States.ERROR)
				.event(Events.STOP)
				.and()
				.withExternal()
				.source(States.EVALUATING_INIT)
				.target(States.IDLE)
				.event(Events.RESET);

		transitions
				.withExternal()
				.source(States.EVALUATED_INITSOLUTION)
				.target(States.RUNNING_LS)
				.event(Events.TO_RUNNING_LS)
				.and()
				.withExternal()
				.source(States.EVALUATED_INITSOLUTION)
				.target(States.ERROR)
				.event(Events.STOP)
				.and()
				.withExternal()
				.source(States.EVALUATED_INITSOLUTION)
				.target(States.IDLE)
				.event(Events.RESET);


		transitions
				.withExternal()
				.source(States.RUNNING_LS)
				.target(States.FINISH)
				.event(Events.FINISH)
				.and()
				.withExternal()
				.source(States.RUNNING_LS)
				.target(States.ERROR)
				.event(Events.STOP)
				.and()
				.withExternal()
				.source(States.RUNNING_LS)
				.target(States.IDLE)
				.event(Events.RESET);



		transitions
				.withExternal()
				.source(States.FINISH)
				.target(States.IDLE)
				.event(Events.MIGRATE)
				.and()
				.withExternal()
				.source(States.FINISH)
				.target(States.ERROR)
				.event(Events.STOP)
				.and()
				.withExternal()
				.source(States.ERROR)
				.target(States.IDLE)
				.event(Events.MIGRATE);
	}

}
