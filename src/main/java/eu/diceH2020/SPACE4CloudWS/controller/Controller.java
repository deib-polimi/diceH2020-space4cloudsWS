package eu.diceH2020.SPACE4CloudWS.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.statemachine.StateMachine;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import eu.diceH2020.SPACE4CloudWS.algorithm.Solution;
import eu.diceH2020.SPACE4CloudWS.service.EngineService;
import eu.diceH2020.SPACE4CloudWS.stateMachine.Events;
import eu.diceH2020.SPACE4CloudWS.stateMachine.States;
import eu.diceH2020.SPACE4Cloud_shared.InstanceData;
import eu.diceH2020.SPACE4Cloud_shared.Settings;

@RestController
public class Controller {

	@Autowired
	EngineService engineService;

	@Autowired
	private StateMachine<States, Events> stateHandler;

	@RequestMapping(method = RequestMethod.POST, value = "/sendevent")
	public @ResponseBody String changestate(@RequestBody Events event) throws Exception {

		stateHandler.sendEvent(event); // if the state is running
		engineService.greedy();								// engine.greedy() is called
		Thread.sleep(1000000);
		return stateHandler.getState().getId().toString();
	}

	@RequestMapping(method = RequestMethod.POST, value = "/sendsettings")
	public @ResponseBody String changestate(@RequestBody Settings settings) {
		engineService.setAccuracyAncCycles(settings);
		return stateHandler.getState().getId().toString();
	}

	@RequestMapping(method = RequestMethod.GET, value = "app/debug")
	public void debug() throws Exception {
		engineService.simulation();
	}

	@RequestMapping(method = RequestMethod.POST, value = "inputdata")
	@ResponseStatus(value = HttpStatus.OK)
	public void endpointInputData(@RequestBody InstanceData inputData) throws Exception {
		engineService.setInstanceData(inputData);
		engineService.init();
		stateHandler.sendEvent(Events.MIGRATE);
		return;
	}

	@RequestMapping(method = RequestMethod.GET, value = "solution")
	@ResponseStatus(value = HttpStatus.OK)
	public Solution endpointSolution() throws Exception {
		if (stateHandler.getState().getId() == States.FINISH)
			return engineService.getSolution();
		return null;
	}


	@RequestMapping(value = "/state", method = RequestMethod.GET)
	public @ResponseBody String getstate() {
		return stateHandler.getState().getId().toString();
	}
}