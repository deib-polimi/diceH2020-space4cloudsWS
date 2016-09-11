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
