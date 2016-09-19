/*
Copyright 2016 Michele Ciavotta
Copyright 2016 Jacopo Rigoli
Copyright 2016 Eugenio Gianniti

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
package it.polimi.diceH2020.SPACE4CloudWS.controller;

import it.polimi.diceH2020.SPACE4Cloud.shared.inputDataMultiProvider.InstanceDataMultiProvider;
import it.polimi.diceH2020.SPACE4Cloud.shared.settings.Scenarios;
import it.polimi.diceH2020.SPACE4Cloud.shared.settings.Settings;
import it.polimi.diceH2020.SPACE4Cloud.shared.solution.Solution;
import it.polimi.diceH2020.SPACE4CloudWS.engines.Engine;
import it.polimi.diceH2020.SPACE4CloudWS.engines.EngineProxy;
import it.polimi.diceH2020.SPACE4CloudWS.engines.EngineTypes;
import it.polimi.diceH2020.SPACE4CloudWS.services.Validator;
import it.polimi.diceH2020.SPACE4CloudWS.stateMachine.Events;
import it.polimi.diceH2020.SPACE4CloudWS.stateMachine.States;

import java.io.File;
import java.util.Optional;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.statemachine.StateMachine;
import org.springframework.web.bind.annotation.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;

@RestController
class Controller {

	@Autowired
	private EngineProxy engineProxy;
	
	@Autowired
	private Validator validator;
	
	@Autowired
	private StateMachine<States, Events> stateHandler;

	private final Logger logger = Logger.getLogger(getClass());
	
	@Autowired
	private Engine engineService;
	
	@RequestMapping(method = RequestMethod.POST, value = "/event")
	public @ResponseBody String changeState(@RequestBody Events event) throws Exception {
		stateHandler.sendEvent(event);
		switch (stateHandler.getState().getId()) {
			case RUNNING_INIT:
				engineService.runningInitSolution();
				break;
			case EVALUATING_INIT:
				
				ObjectMapper mapper = new ObjectMapper(); //TODO delete
				mapper.registerModule(new Jdk8Module());
				mapper.writeValue(new File("/Users/jacoporigoli/Desktop/PROVA/test/"+engineService.getSolution().getId()+"_"+engineService.getSolution().getScenario()+".json"), engineService.getSolution());
				if(engineService.getMatrix()!= null){
					mapper.writeValue(new File("/Users/jacoporigoli/Desktop/PROVA/test/"+engineService.getSolution().getId()+"_"+engineService.getSolution().getScenario()+"_MATRIX.json"), engineService.getMatrix());
				}
				
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
	public String endpointInputData(@RequestBody InstanceDataMultiProvider inputData) throws Exception {
		ObjectMapper mapper = new ObjectMapper(); //TODO delete
		mapper.registerModule(new Jdk8Module());
		mapper.writeValue(new File("/Users/jacoporigoli/Desktop/PROVA/test/input.json"), inputData);
		if (getWebServiceState().equals("IDLE")) {
			logger.info("Starting simulation for "+inputData.getScenario()+" scenario...");
			refreshEngine(inputData.getScenario());
			
			validator.setInstanceData(inputData);
			stateHandler.sendEvent(Events.TO_CHARGED_INPUTDATA);
		}
		return getWebServiceState();
	}

	@RequestMapping(method = RequestMethod.POST, value = "/solution")
	@ResponseStatus(value = HttpStatus.OK)
	public String endpointSolution(@RequestBody Solution sol) throws Exception {
		if(getWebServiceState().equals("IDLE")) {
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
	
	private void refreshEngine(Optional<Scenarios> receivedScenario){
		
		Scenarios submittedScenario = Scenarios.PublicPeakWorkload;
		
		if(receivedScenario.isPresent()) {
			submittedScenario = receivedScenario.get();
		}
		//TODO edit shared project in order to add EngineTypes to each case.
		if(submittedScenario.equals(Scenarios.PrivateAdmissionControl) || submittedScenario.equals(Scenarios.PrivateAdmissionControlWithPhysicalAssignment)){
			engineService = engineProxy.refreshEngine(EngineTypes.AC);
		}else{
			engineService = engineProxy.refreshEngine(EngineTypes.GENERAL);
		}
	}

}
