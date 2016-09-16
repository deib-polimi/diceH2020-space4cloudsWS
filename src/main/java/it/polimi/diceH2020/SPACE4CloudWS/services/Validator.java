package it.polimi.diceH2020.SPACE4CloudWS.services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import it.polimi.diceH2020.SPACE4Cloud.shared.inputData.InstanceData;

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
	public void setInstanceData(InstanceData inputData) {
		dataService.setInstanceData(inputData);
	}

}
