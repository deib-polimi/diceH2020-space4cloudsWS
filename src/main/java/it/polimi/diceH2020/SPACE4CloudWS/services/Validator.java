/*
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

import it.polimi.diceH2020.SPACE4Cloud.shared.inputDataMultiProvider.InstanceDataMultiProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class Validator {

	@Autowired
	private DataService dataService;

	/**
	 * Set in DataService: <br>
	 * &emsp; -inputData <br>
	 * &emsp; -num job <br>
	 * &emsp; -the provider and all its available VM retrieved from DB 
	 * @param inputData
	 *            the inputData to set
	 */
	public void setInstanceData(InstanceDataMultiProvider inputData) {
		dataService.setInstanceData(inputData);
	}

}
