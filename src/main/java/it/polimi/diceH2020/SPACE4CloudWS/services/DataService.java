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

import it.polimi.diceH2020.SPACE4Cloud.shared.inputData.TypeVM;
import it.polimi.diceH2020.SPACE4Cloud.shared.inputDataMultiProvider.ClassParameters;
import it.polimi.diceH2020.SPACE4Cloud.shared.inputDataMultiProvider.InstanceDataMultiProvider;
import it.polimi.diceH2020.SPACE4Cloud.shared.inputDataMultiProvider.JobMLProfile;
import it.polimi.diceH2020.SPACE4Cloud.shared.inputDataMultiProvider.JobMLProfilesMap;
import it.polimi.diceH2020.SPACE4Cloud.shared.inputDataMultiProvider.JobProfile;
import it.polimi.diceH2020.SPACE4Cloud.shared.inputDataMultiProvider.PublicCloudParameters;
import it.polimi.diceH2020.SPACE4Cloud.shared.inputDataMultiProvider.VMConfiguration;
import it.polimi.diceH2020.SPACE4Cloud.shared.settings.CloudType;
import it.polimi.diceH2020.SPACE4Cloud.shared.settings.Scenarios;
import it.polimi.diceH2020.SPACE4Cloud.shared.solution.Matrix;
import it.polimi.diceH2020.SPACE4Cloud.shared.solution.Solution;
import it.polimi.diceH2020.SPACE4CloudWS.model.EntityKey;
import it.polimi.diceH2020.SPACE4CloudWS.model.EntityProvider;
import it.polimi.diceH2020.SPACE4CloudWS.model.EntityTypeVM;
import lombok.AccessLevel;
import lombok.Data;
import lombok.Getter;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.Map.Entry;

/**
 * @author ciavotta
 */
@Service
@Data
public class DataService {

	@Autowired(required = false)
	private InstanceDataMultiProvider data;

	@Autowired
	private DAOService daoService;

	private int jobNumber;

	private Solution currentSolution;

	private Scenarios scenario = Scenarios.PublicAvgWorkLoad;

	private Map<EntityKey, EntityTypeVM> mapCloudParameters;
	
	private Map<String,List<TypeVM>> mapTypeVM;

	
	private String providerName;

	private Matrix matrix;
	
	private int gamma = 1500;

	@Getter(AccessLevel.PRIVATE)
	private Optional<JobMLProfilesMap> mlProfileMap;

	public Double getDeltaBar(String tVM) {
		EntityKey key = new EntityKey(tVM, this.providerName);
		return this.mapCloudParameters.get(key).getDeltabar();
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
		return this.mapCloudParameters.get(key).getNumCores();
	}

	public Double getMemory(String tVM) {
		EntityKey key = new EntityKey(tVM, this.providerName);
		return this.mapCloudParameters.get(key).getMemory();
	}

	public void setInstanceData(InstanceDataMultiProvider inputData) {
		this.data = inputData;
		this.jobNumber = data.getNumberOfClasses();
		this.providerName = data.getProvider();
		this.scenario = data.getScenario().get();
		
		if(providerName == null || scenario == null){
			//TODO
		}
		
		if(data.getMapJobMLProfiles() != null ){
			if(data.getMapJobMLProfiles().getMapJobMLProfile()!=null){
				this.mlProfileMap = Optional.of(data.getMapJobMLProfiles());
			}
		}

		if(scenario.getCloudType().equals(CloudType.Public)){
			loadDataFromDB(new EntityProvider(this.providerName));
			if(scenario.equals(Scenarios.PublicAvgWorkLoad)){
				considerOnlyReserved();
				overrideDBLocalData();
			}
		}
		else{
			loadDataFromJson();
			considerOnlyReserved();
		}
		
		if(!scenario.equals(Scenarios.PrivateAdmissionControl)&&!scenario.equals(Scenarios.PrivateAdmissionControlWithPhysicalAssignment)){
			makeInputDataConsistent();
		}
		this.matrix = null;
	}

	private void loadDataFromDB(EntityProvider provider) {
		this.mapCloudParameters = daoService.typeVMFindAllToMap(provider);
	}

	/**
	 * Set mapTypeVm by fetching JSON data (In the Public Case this map is retrieved from DB) 
	 */
	private void loadDataFromJson(){
		HashMap<EntityKey, EntityTypeVM> map = new HashMap<EntityKey, EntityTypeVM>();
		
		if(data.getMapVMConfigurations().getMapVMConfigurations().size()>0){
			for (Map.Entry<String, VMConfiguration> vm : data.getMapVMConfigurations().getMapVMConfigurations().entrySet()) {
				EntityKey key = new EntityKey(vm.getKey(), vm.getValue().getProvider());
				EntityTypeVM typeVM  = new EntityTypeVM(vm.getKey());
				typeVM.setCore(vm.getValue().getCore());
				typeVM.setMemory(vm.getValue().getMemory());

				typeVM.setRhobar(0);
				typeVM.setSigmabar(0);
				if(vm.getValue().getCost().isPresent()) typeVM.setDeltabar(vm.getValue().getCost().get());
				else  typeVM.setDeltabar(1);

				typeVM.setProvider(new EntityProvider(vm.getValue().getProvider()));
				map.put(key, typeVM);
			}
		}
		this.mapCloudParameters = map ;
	}
	
	private void overrideDBLocalData(){
		for(Map.Entry<EntityKey, EntityTypeVM> entry : mapCloudParameters.entrySet()){
			entry.getValue().setRhobar(0);
			entry.getValue().setSigmabar(0);
		}
	}

	private void considerOnlyReserved(){
		if(data.getMapPublicCloudParameters() == null || data.getMapPublicCloudParameters().getMapPublicCloudParameters() == null) return;
		for (Map.Entry<String, Map<String, Map<String, PublicCloudParameters>>> jobIDs : this.data.getMapPublicCloudParameters().getMapPublicCloudParameters().entrySet()) {
		    for (Map.Entry<String, Map<String, PublicCloudParameters>> providers : jobIDs.getValue().entrySet()) {
		    	for (Map.Entry<String, PublicCloudParameters> typeVMs : providers.getValue().entrySet()) {
		    		PublicCloudParameters vm = typeVMs.getValue();
					vm.setEta(0);
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

	public int getGamma() {
		return gamma;
	}

}
