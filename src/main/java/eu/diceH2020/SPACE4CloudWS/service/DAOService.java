package eu.diceH2020.SPACE4CloudWS.service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import eu.diceH2020.SPACE4CloudWS.jpaRepositories.JobRepository;
import eu.diceH2020.SPACE4CloudWS.jpaRepositories.ProviderRepository;
import eu.diceH2020.SPACE4CloudWS.jpaRepositories.TypeVMRepository;
import eu.diceH2020.SPACE4CloudWS.model.Key;
import eu.diceH2020.SPACE4CloudWS.model.Provider;
import eu.diceH2020.SPACE4CloudWS.model.TypeVM;

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
