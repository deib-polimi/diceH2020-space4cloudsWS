package it.polimi.diceH2020.SPACE4CloudWS.controller;

import it.polimi.diceH2020.SPACE4Cloud.shared.inputData.InstanceData;
import it.polimi.diceH2020.SPACE4Cloud.shared.settings.Settings;
import it.polimi.diceH2020.SPACE4Cloud.shared.solution.Solution;
import it.polimi.diceH2020.SPACE4CloudWS.services.EngineService;
import it.polimi.diceH2020.SPACE4CloudWS.stateMachine.Events;
import it.polimi.diceH2020.SPACE4CloudWS.stateMachine.States;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.statemachine.StateMachine;
import org.springframework.web.bind.annotation.*;

@RestController
class Controller {

	@Autowired
	private EngineService engineService;

	@Autowired
	private StateMachine<States, Events> stateHandler;

	private final Logger logger = Logger.getLogger(getClass());

	@RequestMapping(method = RequestMethod.POST, value = "/event")
	public @ResponseBody String changeState(@RequestBody Events event) throws Exception {
		stateHandler.sendEvent(event);
		switch (stateHandler.getState().getId()) {
			case RUNNING_INIT:
				engineService.runningInitSolution();
				break;
			case EVALUATING_INIT:
				engineService.evaluatingInitSolution();
				break;
			case RUNNING_LS:
				engineService.localSearch();
				break;
			case IDLE:
				engineService.restoreDefaults();
				break;
			default:
		}

		String WSState = getWebServiceState();
		logger.info(WSState);
		return WSState;
	}

	@RequestMapping(method = RequestMethod.POST, value = "/settings")
	public @ResponseBody String changeState(@RequestBody Settings settings) {
		if (getWebServiceState().equals("IDLE")) engineService.changeSettings(settings);
		String msg = "settings changed";
		logger.info(msg);
		return msg;
	}

	@RequestMapping(method = RequestMethod.POST, value = "/inputdata")
	@ResponseStatus(value = HttpStatus.OK)
	public String endpointInputData(@RequestBody InstanceData inputData) throws Exception {
		if (getWebServiceState().equals("IDLE")) {
			engineService.setInstanceData(inputData);
			stateHandler.sendEvent(Events.TO_CHARGED_INPUTDATA);
		}
		return getWebServiceState();
	}

	@RequestMapping(method = RequestMethod.POST, value = "/solution")
	@ResponseStatus(value = HttpStatus.OK)
	public String endpointSolution(@RequestBody Solution sol) throws Exception {
		if (getWebServiceState().equals("IDLE")) {
			engineService.setSolution(sol);
			stateHandler.sendEvent(Events.TO_CHARGED_INITSOLUTION);
		}
		return getWebServiceState();
	}

	@RequestMapping(method = RequestMethod.GET, value = "/solution")
	@ResponseStatus(value = HttpStatus.OK)
	public Solution endpointSolution() throws Exception {
		String state = stateHandler.getState().getId().toString();
		if (state.equals("CHARGED_INITSOLUTION") || state.equals("EVALUATED_INITSOLUTION")  || state.equals("FINISH")) return engineService.getSolution();
		return null;
	}

	@RequestMapping(value = "/state", method = RequestMethod.GET)
	public @ResponseBody String getState() {
		return getWebServiceState();
	}

	private String getWebServiceState() {
		return stateHandler.getState().getId().toString();
	}

}
