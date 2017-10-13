/*
Copyright 2016-2017 Eugenio Gianniti
Copyright 2016 Michele Ciavotta
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
package it.polimi.diceH2020.SPACE4CloudWS.controller;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheStats;
import it.polimi.diceH2020.SPACE4Cloud.shared.inputDataMultiProvider.InstanceDataMultiProvider;
import it.polimi.diceH2020.SPACE4Cloud.shared.settings.Scenarios;
import it.polimi.diceH2020.SPACE4Cloud.shared.settings.Settings;
import it.polimi.diceH2020.SPACE4Cloud.shared.solution.Solution;
import it.polimi.diceH2020.SPACE4CloudWS.core.DataProcessor;
import it.polimi.diceH2020.SPACE4CloudWS.engines.Engine;
import it.polimi.diceH2020.SPACE4CloudWS.engines.EngineProxy;
import it.polimi.diceH2020.SPACE4CloudWS.engines.EngineTypes;
import it.polimi.diceH2020.SPACE4CloudWS.fileManagement.FileUtility;
import it.polimi.diceH2020.SPACE4CloudWS.main.CacheSettings;
import it.polimi.diceH2020.SPACE4CloudWS.main.DS4CSettings;
import it.polimi.diceH2020.SPACE4CloudWS.model.EntityProvider;
import it.polimi.diceH2020.SPACE4CloudWS.model.EntityTypeVM;
import it.polimi.diceH2020.SPACE4CloudWS.repositories.ProviderRepository;
import it.polimi.diceH2020.SPACE4CloudWS.repositories.TypeVMRepository;
import it.polimi.diceH2020.SPACE4CloudWS.services.Validator;
import it.polimi.diceH2020.SPACE4CloudWS.stateMachine.Events;
import it.polimi.diceH2020.SPACE4CloudWS.stateMachine.States;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.CacheManager;
import org.springframework.http.HttpStatus;
import org.springframework.statemachine.StateMachine;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedOutputStream;
import java.io.FileOutputStream;
import java.util.List;
import java.util.Optional;

@RestController
class Controller {

	private final Logger logger = Logger.getLogger(getClass());

	@Autowired
	private DS4CSettings settings;

	@Autowired
	private EngineProxy engineProxy;

	@Autowired
	private DataProcessor dataProcessor;

	@Autowired
	private ProviderRepository providerRepository;

	@Autowired
	private TypeVMRepository typeVMRepository;

	@Autowired
	private Validator validator;

	@Autowired
	private StateMachine<States, Events> stateHandler;

	private Engine engineService;

	@Autowired
	private CacheManager cacheManager;

	@Autowired
	private FileUtility fileUtility;

	private CacheStats cacheStats;

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
		if (getWebServiceState().equals("IDLE"))
			engineService.changeSettings(settings);
		String msg = "settings changed";
		logger.info(msg);
		return msg;
	}

	@RequestMapping(method = RequestMethod.POST, value = "/inputdata")
	@ResponseStatus(value = HttpStatus.OK)
	public String endpointInputData(@RequestBody InstanceDataMultiProvider inputData) throws Exception {
		if (getWebServiceState().equals("IDLE")) {
			logger.info("Starting simulation for " + inputData.getScenario() + " scenario...");
			refreshEngine(inputData.getScenario());
			cacheInitialization();

			validator.setInstanceData(inputData);
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
		if (state.equals("CHARGED_INITSOLUTION") || state.equals("EVALUATED_INITSOLUTION") || state.equals("FINISH")) {
			if (state.equals("FINISH")) {
				logCacheStats();
				String folderName = dataProcessor.getCurrentInputsSubFolderName ();
				if (fileUtility.destroyDir(dataProcessor.getCurrentInputsSubFolder ())) {
					logger.info (String.format ("Deleted input folder '%s'", folderName));
				}
			}
			return engineService.getSolution();
		}
		return null;
	}

	@RequestMapping(value = "/state", method = RequestMethod.GET)
	public @ResponseBody String getState() {
		return getWebServiceState();
	}

	private String getWebServiceState() {
		return stateHandler.getState().getId().toString();
	}

	private void refreshEngine(Optional<Scenarios> receivedScenario) {

		Scenarios submittedScenario = Scenarios.PublicPeakWorkload;

		if (receivedScenario.isPresent()) {
			submittedScenario = receivedScenario.get();
		}
		// TODO edit shared project in order to add EngineTypes to each case.
		if (submittedScenario.equals(Scenarios.PrivateAdmissionControl)
				|| submittedScenario.equals(Scenarios.PrivateAdmissionControlWithPhysicalAssignment)) {
			engineService = engineProxy.refreshEngine(EngineTypes.AC);
		} else if(submittedScenario.equals(Scenarios.StormPublicAvgWorkLoad)) {
			engineService = engineProxy.refreshEngine(EngineTypes.ST);
		} else {
			engineService = engineProxy.refreshEngine(EngineTypes.GENERAL);
		}
	}

	private void cacheInitialization() {
		CacheSettings cache = settings.getCache();
		logger.info(
				String.format("Cache has been initialized (maxSize:%d, max days before expiry:%d, register stats:%b).",
						cache.getSize(), cache.getDaysBeforeExpire(), cache.isRecordStats()));
		logCacheStats();
	}

	private void refreshCacheStats() {
		cacheStats = ((Cache<?, ?>) cacheManager
				.getCache(it.polimi.diceH2020.SPACE4CloudWS.main.Configurator.CACHE_NAME).getNativeCache()).stats();
	}

	private void logCacheStats() {
		if (settings.getCache().isRecordStats()) {
			refreshCacheStats();
			logger.debug(String.format(
					"Cache Statistics:\n" + "\t\t\t\t\tload time avg: %f\n" + "\t\t\t\t\teviction #: %d\n"
							+ "\t\t\t\t\thit #: %d\n" + "\t\t\t\t\thit rate: %f\n" + "\t\t\t\t\tload attempts: %d\n"
							+ "\t\t\t\t\tmiss #:%d",
					cacheStats.averageLoadPenalty(), cacheStats.evictionCount(), cacheStats.hitCount(),
					cacheStats.hitRate(), cacheStats.loadCount(), cacheStats.missCount()));
		}
	}

	@RequestMapping(value="/upload", method=RequestMethod.POST)
	public @ResponseBody String handleFileUpload(@RequestParam("name") String name,
												 @RequestParam("file") MultipartFile file) {
		if (getWebServiceState().equals("CHARGED_INPUTDATA")) {
			if (! file.isEmpty()) {
				try (BufferedOutputStream stream =
							 new BufferedOutputStream(new FileOutputStream(
									 fileUtility.provideFile(dataProcessor.getCurrentInputsSubFolderName(), name)))) {
					stream.write (file.getBytes());
					logger.info (String.format ("'%s' successfully uploaded", name));
					return "You successfully uploaded " + name + "!";
				} catch (Exception e) {
					logger.error (String.format ("'%s' failed to upload", name));
					return "You failed to upload " + name + " => " + e.getMessage();
				}
			} else {
				logger.error (String.format ("'%s' failed to upload because it is empty", name));
				return "You failed to upload " + name + " because the file was empty.";
			}
		} else {
			logger.error (String.format ("cannot receive '%s' in '%s' state", name, getWebServiceState ()));
			return "WS cannot receive " + name + " in the current state.";
		}
	}

	@RequestMapping(method = RequestMethod.GET, value = "/providers")
	public @ResponseBody List<EntityProvider> getProvider() {
		return providerRepository.findAll();
	}

	@RequestMapping(method = RequestMethod.GET, value = "/vm-types")
	public @ResponseBody List<EntityTypeVM> getTypeVM() {
		return typeVMRepository.findAll();
	}

}
