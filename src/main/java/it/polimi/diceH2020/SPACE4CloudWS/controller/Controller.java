package it.polimi.diceH2020.SPACE4CloudWS.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.statemachine.StateMachine;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import it.polimi.diceH2020.SPACE4Cloud.shared.InstanceData;
import it.polimi.diceH2020.SPACE4Cloud.shared.Settings;
import it.polimi.diceH2020.SPACE4Cloud.shared.Solution;
import it.polimi.diceH2020.SPACE4CloudWS.services.EngineService;
import it.polimi.diceH2020.SPACE4CloudWS.stateMachine.Events;
import it.polimi.diceH2020.SPACE4CloudWS.stateMachine.States;

@RestController
public class Controller {

	@Autowired
	EngineService engineService;

	@Autowired
	private StateMachine<States, Events> stateHandler;

	@RequestMapping(method = RequestMethod.POST, value = "/sendevent")
	public @ResponseBody String changeState(@RequestBody Events event) throws Exception {

		stateHandler.sendEvent(event);
		// if the state transitions to RUNNING Spring will call EngineService.optimizationPublicCloud
		return getWebServiceState();
	}

	@RequestMapping(method = RequestMethod.POST, value = "/sendsettings")
	public @ResponseBody String changeState(@RequestBody Settings settings) {
		engineService.setAccuracyAndCycles(settings);
		return getWebServiceState();
	}

	@RequestMapping(method = RequestMethod.POST, value = "inputdata")
	@ResponseStatus(value = HttpStatus.OK)
	public String endpointInputData(@RequestBody InstanceData inputData) throws Exception {
		engineService.setInstanceData(inputData);
		stateHandler.sendEvent(Events.MIGRATE);
		return getWebServiceState();
	}

	@RequestMapping(method = RequestMethod.GET, value = "solution")
	@ResponseStatus(value = HttpStatus.OK)
	public Solution endpointSolution() throws Exception {
		if (stateHandler.getState().getId() == States.FINISH)
			return engineService.getSolution();
		return null;
	}

	@RequestMapping(value = "/state", method = RequestMethod.GET)
	public @ResponseBody String getState() {
		return getWebServiceState();
	}
	
	private String getWebServiceState(){
		return stateHandler.getState().getId().toString();
	}

}
