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
package it.polimi.diceH2020.SPACE4CloudWS.core;

import it.polimi.diceH2020.SPACE4Cloud.shared.solution.Solution;
import it.polimi.diceH2020.SPACE4CloudWS.main.DS4CSettings;
import it.polimi.diceH2020.SPACE4CloudWS.ml.MLPredictor;
import it.polimi.diceH2020.SPACE4CloudWS.stateMachine.Events;
import it.polimi.diceH2020.SPACE4CloudWS.stateMachine.States;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.statemachine.StateMachine;

abstract class Builder {
	protected final Logger logger = Logger.getLogger(getClass());

	@Autowired
	private StateMachine<States, Events> stateHandler;

	@Autowired
	protected DS4CSettings settings;

	@Autowired
	protected MLPredictor approximator;

	void fallBack(Solution sol) {
		sol.getLstSolutions().forEach(s -> {
			s.updateNumberVM(1);
			s.setNumberUsers(s.getJob().getHup());
		});
	}

	boolean checkState() {
		return !stateHandler.getState().getId().equals(States.IDLE);
	}
}
