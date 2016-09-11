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

import it.polimi.diceH2020.SPACE4Cloud.shared.inputData.*;
import it.polimi.diceH2020.SPACE4Cloud.shared.inputDataMultiProvider.JobMLProfile;
import it.polimi.diceH2020.SPACE4Cloud.shared.inputDataMultiProvider.JobMLProfilesMap;
import it.polimi.diceH2020.SPACE4Cloud.shared.inputDataMultiProvider.VMConfiguration;
import it.polimi.diceH2020.SPACE4Cloud.shared.settings.CloudType;
import it.polimi.diceH2020.SPACE4Cloud.shared.settings.Scenarios;
import it.polimi.diceH2020.SPACE4Cloud.shared.solution.Solution;
import it.polimi.diceH2020.SPACE4CloudWS.core.Matrix;
import it.polimi.diceH2020.SPACE4CloudWS.model.EntityKey;
import it.polimi.diceH2020.SPACE4CloudWS.model.EntityProvider;
import it.polimi.diceH2020.SPACE4CloudWS.model.EntityTypeVM;
import lombok.AccessLevel;
import lombok.Data;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * @author ciavotta
 */
@Service
@Data
public class DataService {

	@Autowired(required = false)
	private InstanceData data;

	@Autowired
	private DAOService daoService;

	private int jobNumber;

	private Solution currentSolution;

	private Scenarios cloudType = Scenarios.PublicAvgWorkLoad;

	private Map<EntityKey, EntityTypeVM> mapTypeVM;

	private String NameProvider;

	private Matrix matrix;

	@Getter(AccessLevel.PRIVATE)
	private Optional<JobMLProfilesMap> mlProfileMap;

	public Double getDeltaBar(TypeVM tVM) {
		EntityKey key = new EntityKey(tVM.getId(), this.NameProvider);
		return this.mapTypeVM.get(key).getDeltabar();
	}

	public Double getRhoBar(TypeVM tVM) {
		EntityKey key = new EntityKey(tVM.getId(), this.NameProvider);
		return this.mapTypeVM.get(key).getRhoBar();
	}

	public Double getSigmaBar(TypeVM tVM) {
		EntityKey key = new EntityKey(tVM.getId(), this.NameProvider);
		return this.mapTypeVM.get(key).getSigmaBar();
	}

	public Double getNumCores(TypeVM tVM) {
		EntityKey key = new EntityKey(tVM.getId(), this.NameProvider);
		return this.mapTypeVM.get(key).getNumCores();
	}

	public Double getMemory(TypeVM tVM) {
		EntityKey key = new EntityKey(tVM.getId(), this.NameProvider);
		return this.mapTypeVM.get(key).getMemory();
	}

	public void setInstanceData(InstanceData inputData) {
		this.data = inputData;
		this.jobNumber = data.getNumberJobs();
		this.NameProvider = data.getProvider();
		this.cloudType = data.getScenario().get();

		if(data.getMapJobMLProfiles().getMapJobMLProfile()!=null){
			this.mlProfileMap = Optional.of(data.getMapJobMLProfiles());
		}

		if(cloudType.getCloudType().equals(CloudType.Public))
			loadDataFromDB(new EntityProvider(this.NameProvider));
		else{
			loadDataFromJson();
			considerOnlyReserved();
		} //TODO other scenario
		this.matrix = null;
	}

	private void loadDataFromDB(EntityProvider provider) {
		this.mapTypeVM = daoService.typeVMFindAllToMap(provider);
	}

	private void loadDataFromJson(){
		HashMap<EntityKey, EntityTypeVM> map = new HashMap<EntityKey, EntityTypeVM>();

		if(data.getMapVMConfigurations().get().getMapVMConfigurations().size()>0){
			for (Map.Entry<String, VMConfiguration> vm : data.getMapVMConfigurations().get().getMapVMConfigurations().entrySet()) {
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
		this.mapTypeVM = map ;
	}

	private void considerOnlyReserved(){
		this.data.setMapTypeVMs(Optional.of(initializeMapTypeVMs(data.getMapProfiles())));
	}

	/**
	 *
	 * @param mapProfiles is used to get all couples (typeVM_id, job_id) (unique json common parameter that have this list)
	 * @return
	 */
	private Map<String,List<TypeVM>> initializeMapTypeVMs(Map<TypeVMJobClassKey, Profile> mapProfiles){
		Map<String,List<TypeVM>> map = new HashMap<String,List<TypeVM>>();

		Set<TypeVMJobClassKey> set = mapProfiles.keySet();

		List<String> jobIDs = set.stream().map(TypeVMJobClassKey::getJob).distinct().collect(Collectors.toList());

		for (String jobID : jobIDs) {
			List<TypeVMJobClassKey> lst = set.stream().filter(t->t.getJob().equals(jobID)).collect(Collectors.toList());
			List<TypeVM> lst2 = new ArrayList<>();

			for (TypeVMJobClassKey key : lst) {
				TypeVM vm = new TypeVM();
				vm.setId(key.getTypeVM());

				vm.setEta(0);
				vm.setR(0);

				lst2.add(vm);
			}
			map.put(jobID,lst2);
		}
		return map;
	}

	public List<JobClass> getListJobClass() {
		return data.getLstClass();
	}

	public List<TypeVM> getListTypeVM(JobClass jobClass) {
		return data.getLstTypeVM(jobClass);
	}

	public Profile getProfile(JobClass jobClass, TypeVM tVM) {
		return data.getProfile(jobClass, tVM);
	}

	public JobMLProfile getMLProfile(String id) throws NullPointerException{
		if(!mlProfileMap.isPresent()) throw new NullPointerException();
		return mlProfileMap.get().getMapJobMLProfile().get(id);
	}

	public int getGamma() {
		return data.getGamma();
	}

}
