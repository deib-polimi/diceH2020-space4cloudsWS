package it.polimi.diceH2020.SPACE4CloudWS.core;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.statemachine.StateMachine;

import it.polimi.diceH2020.SPACE4Cloud.shared.solution.Solution;
import it.polimi.diceH2020.SPACE4CloudWS.main.DS4CSettings;
import it.polimi.diceH2020.SPACE4CloudWS.ml.MLPredictor;
import it.polimi.diceH2020.SPACE4CloudWS.stateMachine.Events;
import it.polimi.diceH2020.SPACE4CloudWS.stateMachine.States;

public abstract class Builder {
	@Autowired
	private StateMachine<States, Events> stateHandler;
	
	@Autowired
	protected DS4CSettings settings;
	
	@Autowired
	protected MLPredictor approximator;
	
	protected void fallBack(Solution sol) {
		sol.getLstSolutions().forEach(s -> {
			s.setNumberVM(1);
			s.setNumberUsers(s.getJob().getHup());
			s.setAlfa(0.0);
			s.setBeta(0.0);
		});
	}
	
	protected boolean checkState() {
		return !stateHandler.getState().getId().equals(States.IDLE);
	}
}
