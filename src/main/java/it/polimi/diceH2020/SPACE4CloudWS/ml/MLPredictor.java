/*
Copyright 2016-2017 Eugenio Gianniti
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
package it.polimi.diceH2020.SPACE4CloudWS.ml;

import it.polimi.diceH2020.SPACE4Cloud.shared.inputDataMultiProvider.JobMLProfile;
import it.polimi.diceH2020.SPACE4Cloud.shared.inputDataMultiProvider.JobProfile;
import it.polimi.diceH2020.SPACE4Cloud.shared.inputDataMultiProvider.SVRFeature;
import it.polimi.diceH2020.SPACE4Cloud.shared.solution.SolutionPerJob;
import it.polimi.diceH2020.SPACE4CloudWS.services.DataService;
import lombok.Setter;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class MLPredictor {

	private final Logger logger = Logger.getLogger(getClass());
	private final static int defaultNVM = 1;

	@Setter(onMethod = @__(@Autowired))
	private DataService dataService;

	/**
	 * Precondition: DataService has M,V (received from JSON or retrieved from
	 * DB) SolutionPerJob has h,m,v,D and all the required parameters in
	 * JobMLProfile
	 */
	public void approximateWithSVR(SolutionPerJob spj) {
		calculatePrediction(spj);
	}

	private void calculatePrediction(SolutionPerJob spj) {
		JobMLProfile features = dataService.getMLProfile(spj.getId());
		JobProfile profile = spj.getProfile();

		double deadline = spj.getJob().getD();
		double chi_c = calculateChi_c(features);
		double chi_h = calculateChi_h(features);
		double chi_0 = calculateChi_0(profile, features);
		int h = spj.getNumberUsers();

		double xi = calculateXi(spj);
		int c = (int) Math.ceil(chi_c / (deadline - chi_h * h - chi_0));

		logger.debug ("[SVR] numContainers = ceil(chi_c/(deadline - chi_h*h - chi_0)) = ceil("+chi_c+"/("+deadline+"-"+chi_h+"*"+h+"-"+chi_0+") = "+c);

		spj.setXi(xi);
		spj.setDuration(deadline);
		spj.updateNumberContainers(c);
		validate(spj);
	}

	private void validate(SolutionPerJob spj) {
		if(spj.getNumberVM()<1){
			spj.updateNumberVM (defaultNVM);
			logger.info("[SVR] the #vm predicted is invalid. SolutionPerJob #VM has been updated to "+defaultNVM+".");
		}else{
			logger.info("[SVR] SolutionPerJob #VM has been updated to "+spj.getNumberVM()+".");
		}
	}

	private double calculateDefaultParametersContribution(JobMLProfile features) {
		// mu_t + b*sigma_t - (sigma_t/sigma_x)*w_x*mu_x -
		// (sigma_t/sigma_h)*w_h*mu_h
		double mu_t = features.getMu_t();
		double sigma_t = features.getSigma_t();
		double b = features.getB();
		double sigma_x = features.getClassFeature("x").getSigma();
		double mu_x = features.getClassFeature("x").getMu();
		double w_x = features.getClassFeature("x").getW();
		double sigma_h = features.getClassFeature("h").getSigma();
		double mu_h = features.getClassFeature("h").getMu();
		double w_h = features.getClassFeature("h").getW();

		double result = mu_t + b * sigma_t - (sigma_t / sigma_x) * w_x * mu_x - (sigma_t / sigma_h) * w_h * mu_h;
		logger.debug (
				"[SVR] Chi_0_mandatoryParameters = mu_t + b*sigma_t - (sigma_t/sigma_x)*w_x*mu_x - (sigma_t/sigma_h)*w_h*mu_h = "
						+ mu_t + "+" + b + "*" + sigma_t + "- (" + sigma_t + "/" + sigma_x + ")*" + w_x + "*" + mu_x
						+ "- (" + sigma_t + "/" + sigma_h + ")*" + w_h + "*" + mu_h + " \n= " + result);
		return result;
	}

	private double calculateChi_c(JobMLProfile features) {
		double sigma_t = features.getSigma_t();
		double sigma_x = features.getClassFeature("x").getSigma();
		double w_x = features.getClassFeature("x").getW();

		double result = (sigma_t / sigma_x) * w_x;
		logger.debug ("[SVR] Chi_c = (sigma_t/sigma_x)*w_x = (" + sigma_t + "/" + sigma_x + ")*" + w_x + " = " + result);
		return result;
	}

	private double calculateChi_h(JobMLProfile features) {
		double sigma_t = features.getSigma_t();
		double sigma_h = features.getClassFeature("h").getSigma();
		double w_h = features.getClassFeature("h").getW();
		double result = (sigma_t / sigma_h) * w_h;
		logger.debug ("[SVR] Chi_h = (sigma_t/sigma_h)*w_h = (" + sigma_t + "/" + sigma_h + ")*" + w_h + " = " + result);
		return result;
	}

	private double calculateXi(SolutionPerJob spj) {
		double M = dataService.getMemory(spj.getTypeVMselected().getId());
		double m = spj.getJob().getM();
		double V = dataService.getNumCores(spj.getTypeVMselected().getId());
		double v = spj.getJob().getV();
		double xi = Math.min(M / m, V / v);
		logger.debug ("[SVR] xi = min(M/m,V/v) = min(" + M + "/" + m + "," + V + "/" + v + ") = " + xi);
		return xi;
	}

	private double calculateChi_0(JobProfile profile, JobMLProfile features) {
		double defaultParametersContribution = calculateDefaultParametersContribution(features);
		double featureContribution = 0;
		String chi_0_optional_names = "";
		String chi_0_optional_values = "";

		for (Map.Entry<String, SVRFeature> entry : features.getMlFeatures().entrySet()) {
			if (entry.getKey().equals("h") || entry.getKey().equals("x"))
				continue;

			try {
				double valueOfEntry = profile.get(entry.getKey());

				chi_0_optional_names += "+ (sigma_t/sigma_" + entry.getKey () + ")*w_" + entry.getKey () + "*(" + entry.getKey () + "-" + "mu_" + entry.getKey () + ")";
				chi_0_optional_values += "+(" + features.getSigma_t () + " / " + entry.getValue ().getSigma () + ") * " + entry.getValue ().getW () + "*(" + valueOfEntry + "-" + entry.getValue ().getMu () + ")";

				featureContribution += (features.getSigma_t () / entry.getValue ().getSigma ()) * entry.getValue ().getW () * (valueOfEntry - entry.getValue ().getMu ());
			} catch (IllegalArgumentException e) {
				String message = String.format (
						"[SVR] Missing a JobMLProfile feature parameter in JobProfile: '%s'.", entry.getKey ());
				logger.error (message, e);
				throw new IllegalArgumentException (message, e);
			}
		}

		logger.debug ("[SVR] Chi_0_optional_parameters: "+chi_0_optional_names+"\n"+"="+chi_0_optional_values+"\n = "+featureContribution);
		double result = defaultParametersContribution + featureContribution;
		logger.debug ("[SVR] Chi_0 = Chi_0_mandatory_parameters + Chi_0_optional_parameters  = "+defaultParametersContribution+"+"+featureContribution +" = "+result);

		return result;
	}
}
