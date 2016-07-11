/**
 *
 */
package it.polimi.diceH2020.SPACE4CloudWS.services;

import it.polimi.diceH2020.SPACE4Cloud.shared.inputData.InstanceData;
import it.polimi.diceH2020.SPACE4Cloud.shared.inputData.JobClass;
import it.polimi.diceH2020.SPACE4Cloud.shared.inputData.Profile;
import it.polimi.diceH2020.SPACE4Cloud.shared.inputData.TypeVM;
import it.polimi.diceH2020.SPACE4Cloud.shared.inputDataMultiProvider.VMConfiguration;
import it.polimi.diceH2020.SPACE4Cloud.shared.settings.CloudType;
import it.polimi.diceH2020.SPACE4Cloud.shared.settings.Scenarios;
import it.polimi.diceH2020.SPACE4Cloud.shared.solution.Solution;
import it.polimi.diceH2020.SPACE4CloudWS.model.EntityKey;
import it.polimi.diceH2020.SPACE4CloudWS.model.EntityProvider;
import it.polimi.diceH2020.SPACE4CloudWS.model.EntityTypeVM;
import lombok.Data;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

	void setInstanceData(InstanceData inputData) {
		this.data = inputData;
		this.jobNumber = data.getNumberJobs();
		this.NameProvider = data.getProvider();
		this.cloudType = data.getScenario();
		
		if(cloudType.getCloudType().equals(CloudType.Public))
			loadDataFromDB(new EntityProvider(this.NameProvider));
		else{
			loadDataFromJson();
		}
	}

	private void loadDataFromDB(EntityProvider provider) {
		this.mapTypeVM = daoService.typeVMFindAllToMap(provider);
	}
	
	private void loadDataFromJson(){
		HashMap<EntityKey, EntityTypeVM> map = new HashMap<EntityKey, EntityTypeVM>();
		
		if(data.getMapVMConfigurations().getMapVMConfigurations().size()>0){
			for (Map.Entry<String, VMConfiguration> vm : data.getMapVMConfigurations().getMapVMConfigurations().entrySet()) {
				EntityKey key = new EntityKey(vm.getKey(), vm.getValue().getProvider());
				EntityTypeVM typeVM  = new EntityTypeVM(vm.getKey());
				typeVM.setCore(vm.getValue().getCore());
				//typeVM.setMemory(vm.getValue().getMemory()); TODO settare la memoria nelle entry del DB 
				typeVM.setDeltabar(1);
				typeVM.setRhobar(0);
				typeVM.setSigmabar(0);
				typeVM.setProvider(new EntityProvider(vm.getValue().getProvider()));
				map.put(key, typeVM);
			}
		}
		
		this.mapTypeVM = map ;
			
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

	public int getGamma() {
		return data.getGamma();
	}

}
