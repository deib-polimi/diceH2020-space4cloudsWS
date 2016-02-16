package it.polimi.diceH2020.SPACE4CloudWS.controller;

import it.polimi.diceH2020.SPACE4Cloud.shared.inputData.InstanceData;
import it.polimi.diceH2020.SPACE4Cloud.shared.solution.Solution;
import it.polimi.diceH2020.SPACE4CloudWS.fileManagement.FileUtility;
import it.polimi.diceH2020.SPACE4CloudWS.model.EntityJobClass;
import it.polimi.diceH2020.SPACE4CloudWS.model.EntityKey;
import it.polimi.diceH2020.SPACE4CloudWS.model.EntityProvider;
import it.polimi.diceH2020.SPACE4CloudWS.model.EntityTypeVM;
import it.polimi.diceH2020.SPACE4CloudWS.repositories.JobRepository;
import it.polimi.diceH2020.SPACE4CloudWS.repositories.ProviderRepository;
import it.polimi.diceH2020.SPACE4CloudWS.repositories.TypeVMRepository;
import it.polimi.diceH2020.SPACE4CloudWS.services.DataService;
import it.polimi.diceH2020.SPACE4CloudWS.services.EngineService;
import it.polimi.diceH2020.SPACE4CloudWS.stateMachine.Events;
import it.polimi.diceH2020.SPACE4CloudWS.stateMachine.States;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.statemachine.StateMachine;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
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
	InstanceData inputData;
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
	public @ResponseBody InstanceData applData(){
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
		Optional<Solution> sol =engineService.generateInitialSolution();
		return stateHandler.getState().getId().toString();
	}
	
	@RequestMapping(method = RequestMethod.GET, value = "debug/solution")
	@Profile("test")
	public Solution getSolutionDebug() {
		engineService.setInstanceData(inputData);
		//stateHandler.sendEvent(Events.MIGRATE);
		Optional<Solution> sol = engineService.generateInitialSolution(); 
		return sol.isPresent()? sol.get(): null;
	}
	
    @RequestMapping(value="/upload", method=RequestMethod.POST)
    public @ResponseBody String handleFileUpload(@RequestParam("name") String name,
            @RequestParam("file") MultipartFile file){
        if (!file.isEmpty()) {
            try {
                byte[] bytes = file.getBytes();
                BufferedOutputStream stream =
                        new BufferedOutputStream(new FileOutputStream(fileUtility.provideFile(name)));
                stream.write(bytes);
                stream.close();
                return "You successfully uploaded " + name + "!";
            } catch (Exception e) {
                return "You failed to upload " + name + " => " + e.getMessage();
            }
        } else {
            return "You failed to upload " + name + " because the file was empty.";
        }
    }

	
}
