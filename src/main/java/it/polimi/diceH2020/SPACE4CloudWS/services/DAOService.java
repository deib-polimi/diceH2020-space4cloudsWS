package it.polimi.diceH2020.SPACE4CloudWS.services;

import it.polimi.diceH2020.SPACE4CloudWS.model.EntityKey;
import it.polimi.diceH2020.SPACE4CloudWS.model.EntityProvider;
import it.polimi.diceH2020.SPACE4CloudWS.model.EntityTypeVM;
import it.polimi.diceH2020.SPACE4CloudWS.repositories.JobRepository;
import it.polimi.diceH2020.SPACE4CloudWS.repositories.ProviderRepository;
import it.polimi.diceH2020.SPACE4CloudWS.repositories.TypeVMRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class DAOService {

	@SuppressWarnings("unused")
	@Autowired
	private JobRepository jobRepository;
	@SuppressWarnings("unused")
	@Autowired
	private ProviderRepository providerRepository;
	@Autowired
	private TypeVMRepository typeVMRepository;

	public Map<EntityKey, EntityTypeVM> typeVMFindAllToMap(EntityProvider provider) {
		List<EntityTypeVM> lstTypeVM = typeVMRepository.findByProvider(provider);
		if (lstTypeVM.size() > 0) {
			HashMap<EntityKey, EntityTypeVM> map = new HashMap<EntityKey, EntityTypeVM>();
			for (EntityTypeVM typeVM : lstTypeVM) {
				EntityKey key = new EntityKey(typeVM.getType(), typeVM.getProvider());
				map.put(key, typeVM);
			}
			return map;
		}
		return null;
	}

}
