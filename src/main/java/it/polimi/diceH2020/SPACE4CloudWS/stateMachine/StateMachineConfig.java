package it.polimi.diceH2020.SPACE4CloudWS.stateMachine;

import java.util.EnumSet;

import org.springframework.context.annotation.Configuration;
import org.springframework.statemachine.config.EnableStateMachine;
import org.springframework.statemachine.config.EnumStateMachineConfigurerAdapter;
import org.springframework.statemachine.config.builders.StateMachineStateConfigurer;
import org.springframework.statemachine.config.builders.StateMachineTransitionConfigurer;

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
		        .event(Events.STOP)
		        .and()
            .withExternal()
                .source(States.IDLE)
                .target(States.CHARGED)
                .event(Events.MIGRATE)
                .and()
            .withExternal()
                .source(States.IDLE)
                .target(States.ERROR)
                .event(Events.STOP)
                .and()
            .withExternal()
                .source(States.CHARGED)
                .target(States.RUNNING)
                .event(Events.MIGRATE)
                .and()
            .withExternal()
                .source(States.CHARGED)
                .target(States.ERROR)
                .event(Events.STOP)
                .and()           
            .withExternal()
                .source(States.CHARGED)
                .target(States.IDLE)
                .event(Events.RESET)
                .and()
            .withExternal()
                .source(States.RUNNING)
                .target(States.FINISH)
                .event(Events.MIGRATE)
                .and()
            .withExternal()
                .source(States.RUNNING)
                .target(States.ERROR)
                .event(Events.STOP)
                .and()
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
