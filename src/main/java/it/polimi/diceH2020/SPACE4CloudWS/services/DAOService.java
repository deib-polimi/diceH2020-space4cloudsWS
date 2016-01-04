package it.polimi.diceH2020.SPACE4CloudWS.services;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import it.polimi.diceH2020.SPACE4CloudWS.model.Key;
import it.polimi.diceH2020.SPACE4CloudWS.model.Provider;
import it.polimi.diceH2020.SPACE4CloudWS.model.TypeVM;
import it.polimi.diceH2020.SPACE4CloudWS.repositories.JobRepository;
import it.polimi.diceH2020.SPACE4CloudWS.repositories.ProviderRepository;
import it.polimi.diceH2020.SPACE4CloudWS.repositories.TypeVMRepository;

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

	public Map<Key, TypeVM> typeVMFindAllToMap(Provider provider) {
		List<TypeVM> lstTypeVM = typeVMRepository.findByProvider(provider);
		if (lstTypeVM.size() > 0) {
			HashMap<Key, TypeVM> map = new HashMap<Key, TypeVM>();
			for (TypeVM typeVM : lstTypeVM) {
				Key key = new Key(typeVM.getType(), typeVM.getProvider());
				map.put(key, typeVM);
			}
			return map;
		}
		return null;
	}

}
