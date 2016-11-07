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
package it.polimi.diceH2020.SPACE4CloudWS.controller;

import it.polimi.diceH2020.SPACE4Cloud.shared.inputDataMultiProvider.InstanceDataMultiProvider;
import it.polimi.diceH2020.SPACE4Cloud.shared.solution.Solution;
import it.polimi.diceH2020.SPACE4CloudWS.core.EngineService;
import it.polimi.diceH2020.SPACE4CloudWS.fileManagement.FileUtility;
import it.polimi.diceH2020.SPACE4CloudWS.model.EntityJobClass;
import it.polimi.diceH2020.SPACE4CloudWS.model.EntityKey;
import it.polimi.diceH2020.SPACE4CloudWS.model.EntityProvider;
import it.polimi.diceH2020.SPACE4CloudWS.model.EntityTypeVM;
import it.polimi.diceH2020.SPACE4CloudWS.repositories.JobRepository;
import it.polimi.diceH2020.SPACE4CloudWS.repositories.ProviderRepository;
import it.polimi.diceH2020.SPACE4CloudWS.repositories.TypeVMRepository;
import it.polimi.diceH2020.SPACE4CloudWS.services.DataService;
import it.polimi.diceH2020.SPACE4CloudWS.services.Validator;
import it.polimi.diceH2020.SPACE4CloudWS.stateMachine.Events;
import it.polimi.diceH2020.SPACE4CloudWS.stateMachine.States;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.statemachine.StateMachine;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Optional;


@RestController
@Profile("test")
public class ControllerTest {

	@Autowired
	DataService dataService;
	
	@Autowired
	EngineService engineService;
	
	@Autowired
	FileUtility fileUtility;
	
	@Autowired
	Validator validator;

	// I could use the daoService, but this class is only for testing purposes
	@Autowired
	JobRepository jobRepository;
	@Autowired
	TypeVMRepository typeVMRepository;
	@Autowired
	ProviderRepository providerRepository;
	@Autowired
	private StateMachine<States, Events> stateHandler;

	@RequestMapping(method = RequestMethod.GET, value = "appldata")
	@Profile("test")
	public @ResponseBody InstanceDataMultiProvider applData(){
		return dataService.getData();
	}
	
	@RequestMapping(method = RequestMethod.POST, value = "job")
	@Profile("test")
	public @ResponseBody EntityJobClass postJob(@RequestBody EntityJobClass jb){
		jobRepository.saveAndFlush(jb);
		EntityJobClass job = jobRepository.findOne(jb.getIdJob());
		return job;
	}
	@RequestMapping(method = RequestMethod.POST, value = "typeVM")
	@Profile("test")
	public @ResponseBody EntityTypeVM postTypeVM(@RequestBody EntityTypeVM typeVM){
		typeVMRepository.saveAndFlush(typeVM);
		EntityProvider provider = new EntityProvider();
		provider.setName("Amazon");
		EntityKey key = new EntityKey("T1", provider);
		EntityTypeVM tVM = typeVMRepository.findOne(key);
		return tVM;
	}
	@RequestMapping(method = RequestMethod.GET, value = "providers")
	@Profile("test")
	public @ResponseBody List<EntityProvider> getProvider(){
		return providerRepository.findAll();
	}
	@RequestMapping(method = RequestMethod.GET, value = "typeVM")
	@Profile("test")
	public @ResponseBody List<EntityTypeVM> getTypeVM(){
		return typeVMRepository.findAll();
	}
	
	
	@RequestMapping(method = RequestMethod.POST, value = "debug/event")
	@Profile("test")
	public String debug() throws Exception {
		engineService.generateInitialSolution();
		return stateHandler.getState().getId().toString();
	}
	
	@RequestMapping(method = RequestMethod.GET, value = "debug/solution")
	@Profile("test")
	public Solution getSolutionDebug() {
		InstanceDataMultiProvider inputData = new InstanceDataMultiProvider();
		validator.setInstanceData(inputData);
		//stateHandler.sendEvent(Events.MIGRATE);
		Optional<Solution> sol = engineService.generateInitialSolution(); 
		return sol.isPresent()? sol.get(): null;
	}
}
