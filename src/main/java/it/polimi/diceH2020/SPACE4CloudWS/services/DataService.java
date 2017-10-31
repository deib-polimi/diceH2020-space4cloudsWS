/*
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
package it.polimi.diceH2020.SPACE4CloudWS.services;

import it.polimi.diceH2020.SPACE4Cloud.shared.inputDataMultiProvider.*;
import it.polimi.diceH2020.SPACE4Cloud.shared.settings.CloudType;
import it.polimi.diceH2020.SPACE4Cloud.shared.settings.Scenario;
import it.polimi.diceH2020.SPACE4Cloud.shared.solution.Matrix;
import it.polimi.diceH2020.SPACE4Cloud.shared.solution.Solution;
import it.polimi.diceH2020.SPACE4CloudWS.fileManagement.FileUtility;
import it.polimi.diceH2020.SPACE4CloudWS.model.EntityKey;
import it.polimi.diceH2020.SPACE4CloudWS.model.EntityProvider;
import it.polimi.diceH2020.SPACE4CloudWS.model.EntityTypeVM;
import it.polimi.diceH2020.SPACE4CloudWS.stateMachine.Events;
import it.polimi.diceH2020.SPACE4CloudWS.stateMachine.States;
import lombok.AccessLevel;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.statemachine.StateMachine;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.*;
import java.util.Map.Entry;

/**
 * @author ciavotta
 */
@Service
@Data
public class DataService {

	private final Logger logger = Logger.getLogger(getClass ());

	private InstanceDataMultiProvider data;

	@Setter(onMethod = @__(@Autowired))
	private StateMachine<States, Events> stateHandler;

	@Setter(onMethod = @__(@Autowired))
	private DAOService daoService;

	@Setter(onMethod = @__(@Autowired))
	private FileUtility fileUtility;

	private int jobNumber;

	private Solution currentSolution;

	private Scenario scenario;

	private Map<EntityKey, EntityTypeVM> mapCloudParameters;

	private Map<String,List<TypeVM>> mapTypeVM;

	private String providerName;

	private Matrix matrix;

	private String simFoldersPath = "";

	private List<String> usedSimFoldersName = new ArrayList<>();

	private int gamma = 1500;

	@Getter(AccessLevel.PRIVATE)
	private Optional<JobMLProfilesMap> mlProfileMap;

	public Double getDeltaBar(String tVM) {
		EntityKey key = new EntityKey(tVM, this.providerName);
		return this.mapCloudParameters.get(key).getDeltaBar ();
	}

	public Double getRhoBar(String tVM) {
		EntityKey key = new EntityKey(tVM, this.providerName);
		return this.mapCloudParameters.get(key).getRhoBar();
	}

	public Double getSigmaBar(String tVM) {
		EntityKey key = new EntityKey(tVM, this.providerName);
		return this.mapCloudParameters.get(key).getSigmaBar();
	}

	public Double getNumCores(String tVM) {
		EntityKey key = new EntityKey(tVM, this.providerName);
		return this.mapCloudParameters.get(key).getCores ();
	}

	public Double getMemory(String tVM) {
		EntityKey key = new EntityKey(tVM, this.providerName);
		return this.mapCloudParameters.get(key).getMemory();
	}

	void setInstanceData(InstanceDataMultiProvider inputData) {
		this.data = inputData;
		this.jobNumber = data.getNumberOfClasses();
		this.providerName = data.getProvider();
		this.scenario = data.getScenario();

		if(providerName == null || scenario == null){
			//TODO
		}

		if(data.getMapJobMLProfiles() != null ){
			if(data.getMapJobMLProfiles().getMapJobMLProfile()!=null){
				this.mlProfileMap = Optional.of(data.getMapJobMLProfiles());
			}
		}

		if(scenario.getCloudType().equals(CloudType.PUBLIC)){
			loadDataFromDB(new EntityProvider(this.providerName));
			if(! scenario.getLongTermCommitment()){
				considerOnlyReserved();
				overrideDBLocalData();
			}
		}
		else{
			loadDataFromJson();
			considerOnlyReserved();
		}
		if(!scenario.getCloudType().equals(CloudType.PRIVATE)) {
			makeInputDataConsistent();
		}

		simFoldersPath = createInputSubFolder();
		this.matrix = null;
	}

	private void loadDataFromDB(EntityProvider provider) {
		this.mapCloudParameters = daoService.typeVMFindAllToMap(provider);
	}

	public String getSimFoldersName(){
		return simFoldersPath;
	}

	/**
	 * Set mapTypeVm by fetching JSON data (In the Public Case this map is retrieved from DB)
	 */
	private void loadDataFromJson(){
		HashMap<EntityKey, EntityTypeVM> map = new HashMap<>();

		if(data.getMapVMConfigurations().getMapVMConfigurations().size()>0){
			for (Map.Entry<String, VMConfiguration> vm : data.getMapVMConfigurations().getMapVMConfigurations().entrySet()) {
				EntityKey key = new EntityKey(vm.getKey(), vm.getValue().getProvider());
				EntityTypeVM typeVM  = new EntityTypeVM(vm.getKey());
				typeVM.setCores (vm.getValue().getCore());
				typeVM.setMemory(vm.getValue().getMemory());

				typeVM.setRhoBar (0.0);
				typeVM.setSigmaBar (0.0);
				if(vm.getValue().getCost().isPresent()) typeVM.setDeltaBar (vm.getValue().getCost().get());
				else  typeVM.setDeltaBar (1);

				typeVM.setProvider(new EntityProvider(vm.getValue().getProvider()));
				map.put(key, typeVM);
			}
		}
		this.mapCloudParameters = map ;
	}

	private void overrideDBLocalData(){
		//TODO if empty throw exception
		for(Map.Entry<EntityKey, EntityTypeVM> entry : mapCloudParameters.entrySet()){
			entry.getValue().setRhoBar (0.0);
			entry.getValue().setSigmaBar (0.0);
		}
	}

	private void considerOnlyReserved(){
		if(data.getMapPublicCloudParameters() == null || data.getMapPublicCloudParameters().getMapPublicCloudParameters() == null) return;
		for (Map.Entry<String, Map<String, Map<String, PublicCloudParameters>>> jobIDs : this.data.getMapPublicCloudParameters().getMapPublicCloudParameters().entrySet()) {
			for (Map.Entry<String, Map<String, PublicCloudParameters>> providers : jobIDs.getValue().entrySet()) {
				for (Map.Entry<String, PublicCloudParameters> typeVMs : providers.getValue().entrySet()) {
					PublicCloudParameters vm = typeVMs.getValue();
					vm.setEta(0.0);
					vm.setR(0);
				}
			}
		}
	}

	private void makeInputDataConsistent(){
		for(ClassParameters jobClass : this.data.getMapClassParameters().getMapClassParameters().values()){
			jobClass.setHlow(jobClass.getHup());
			jobClass.setPenalty(0);
		}
	}


	public Map<String, ClassParameters> getMapJobClass() {
		return data.getMapClassParameters().getMapClassParameters();
	}

	public List<TypeVM> getLstTypeVM(String jobID) {
		List<TypeVM> lst = new ArrayList<>();
		if(data.getMapPublicCloudParameters()==null || data.getMapPublicCloudParameters().getMapPublicCloudParameters()==null || data.getMapPublicCloudParameters().getMapPublicCloudParameters().isEmpty()){
			for (Entry<String, JobProfile> entry : data.getMapJobProfiles().getMapJobProfile().get(jobID).get(providerName).entrySet()) {
				TypeVM vm = new TypeVM();
				vm.setId(entry.getKey());
				vm.setEta(0);
				vm.setR(0);
				lst.add(vm);
			}
		}
		else{
			for (Entry<String, PublicCloudParameters> entry : data.getMapPublicCloudParameters().getMapPublicCloudParameters().get(jobID).get(providerName).entrySet()) {
				TypeVM vm = new TypeVM();
				vm.setId(entry.getKey());
				vm.setEta(entry.getValue().getEta());
				vm.setR(entry.getValue().getR());
				lst.add(vm);
			}
		}
		return lst;
	}

	public JobProfile getProfile(String classID, String tVM) {
		return data.getProfile(classID, providerName, tVM);
	}

	public JobMLProfile getMLProfile(String id) throws NullPointerException{
		if(!mlProfileMap.isPresent()) throw new NullPointerException();
		return mlProfileMap.get().getMapJobMLProfile().get(id);
	}

	private synchronized String createInputSubFolder(){
		String folderNameStaticPart = fileUtility.generateUniqueString();
		if(usedSimFoldersName.stream().anyMatch(f->f.equals(folderNameStaticPart))){
			return addFolder(folderNameStaticPart);
		}

		return addFolder(incrementFolderName(folderNameStaticPart));
	}

	private String incrementFolderName(String name){
		String fol = name;
		int i = 1;
		while(usedSimFoldersName.stream().anyMatch(f->f.equals(name))){
			fol = name+i;
			i++;
		}
		return fol;
	}

	private synchronized String addFolder(String folder){
		String folderAbsolutePath = "";
		try {
			folderAbsolutePath = fileUtility.createInputSubFolder(folder);
		} catch (IOException e) {
			logger.error("Error while performing optimization", e);
			stateHandler.sendEvent(Events.STOP);
		}

		usedSimFoldersName.add(0, folder);
		if(usedSimFoldersName.size()==100){
			usedSimFoldersName.remove(usedSimFoldersName.size()-1);
		}

		if(usedSimFoldersName.size()>=100){
			System.out.println("FolderList dimension doesn't respect the upper bound (99)");
		}
		return folderAbsolutePath;
	}

}
