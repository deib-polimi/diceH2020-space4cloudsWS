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
package it.polimi.diceH2020.SPACE4CloudWS.ml;

import it.polimi.diceH2020.SPACE4Cloud.shared.inputDataMultiProvider.JobMLProfile;
import it.polimi.diceH2020.SPACE4Cloud.shared.inputDataMultiProvider.JobProfile;
import it.polimi.diceH2020.SPACE4Cloud.shared.inputDataMultiProvider.SVRFeature;
import it.polimi.diceH2020.SPACE4Cloud.shared.solution.SolutionPerJob;
import it.polimi.diceH2020.SPACE4CloudWS.main.DS4CSettings;
import it.polimi.diceH2020.SPACE4CloudWS.services.DataService;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Service
public class MLPredictor {

	private final Logger logger = Logger.getLogger(getClass());
	private final int defaultNVM = 1;

	@Autowired
	private DataService dataService;
	
	@Autowired
	private DS4CSettings settings;

	private Map<String, MLPrediction> predictedSPJ = new HashMap<>();// caches
																		// SPJ,
																		// in
																		// order
																		// to
																		// recalculate
																		// only
																		// xi
																		// TODO
																		// @Cacheable

	/**
	 * Precondition: DataService has M,V (received from JSON or retrieved from
	 * DB) SolutionPerJob has h,m,v,D and all the required parameters in
	 * JobMLProfile
	 *
	 * @param spj
	 * @return
	 */
	public Optional<BigDecimal> approximateWithSVR(SolutionPerJob spj) {
		if (predictedSPJ.containsKey(spj.getId())) {
			return retrievePrediction(spj);
		} else {
			return calculatePrediction(spj);
		}
	}

	private Optional<BigDecimal> calculatePrediction(SolutionPerJob spj) {
		JobMLProfile features = dataService.getMLProfile(spj.getId());
		JobProfile profile = spj.getProfile();

		double deadline = spj.getJob().getD();
		double chi_c = calculateChi_c(features);
		double chi_h = calculateChi_h(features);
		double chi_0 = calculateChi_0(profile, features);
		int h = spj.getNumberUsers();

		double duration = deadline; 
		double xi = calculateXi(spj);
		int c = (int) Math.ceil((double) (chi_c / (deadline - chi_h * h - chi_0)));

		spj.setXi(xi);
		spj.setDuration(duration);
		spj.updateNumberContainers(c);
		validate(spj);
		
		logVerbose("[SVR] numContainers = ceil(chi_c/(deadline - chi_h*h - chi_0)) = ceil("+chi_c+"/("+deadline+"-"+chi_h+"*"+h+"-"+chi_0+") = "+c);
		predictedSPJ.put(spj.getId(), new MLPrediction(deadline, chi_c, chi_h, chi_0));
		return Optional.of(BigDecimal.valueOf(duration));
	}

	private void validate(SolutionPerJob spj) {
		if(spj.getNumberVM()<1){
			spj.setNumberVM(defaultNVM);
			logger.info("[SVR] the #vm predicted is invalid. SolutionPerJob #VM has been updated to "+defaultNVM+".");
		}else{
			logger.info("[SVR] SolutionPerJob #VM has been updated to "+spj.getNumberVM()+".");
		}
	}

	private Optional<BigDecimal> retrievePrediction(SolutionPerJob spj) {
		MLPrediction prediction = predictedSPJ.get(spj.getId());

		double deadline = prediction.getDeadline();
		double chi_c = prediction.getChi_c();
		double chi_h = prediction.getChi_h();
		double chi_0 = prediction.getChi_0();

		int h = spj.getNumberUsers();

		double duration = deadline; 
		double xi = calculateXi(spj);
		int c = (int) Math.ceil((double) (chi_c / (deadline - chi_h * h - chi_0)));
		
		logVerbose("[SVR] numContainers = ceil(chi_c/(deadline - chi_h*h - chi_0)) = ceil("+chi_c+"/("+deadline+"-"+chi_h+"*"+h+"-"+chi_0+") = "+c);
		spj.setXi(xi);
		spj.setDuration(duration);
		spj.updateNumberContainers(c); 
		validate(spj);
		return Optional.of(BigDecimal.valueOf(duration));
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
		logVerbose(
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
		logVerbose("[SVR] Chi_c = (sigma_t/sigma_x)*w_x = (" + sigma_t + "/" + sigma_x + ")*" + w_x + " = " + result);
		return result;
	}

	private double calculateChi_h(JobMLProfile features) {
		double sigma_t = features.getSigma_t();
		double sigma_h = features.getClassFeature("h").getSigma();
		double w_h = features.getClassFeature("h").getW();
		double result = (sigma_t / sigma_h) * w_h;
		logVerbose("[SVR] Chi_h = (sigma_t/sigma_h)*w_h = (" + sigma_t + "/" + sigma_h + ")*" + w_h + " = " + result);
		return result;
	}

	private double calculateXi(SolutionPerJob spj) {
		double M = dataService.getMemory(spj.getTypeVMselected().getId());
		double m = spj.getJob().getM();
		double V = dataService.getNumCores(spj.getTypeVMselected().getId());
		double v = spj.getJob().getV();
		double xi = Math.min(M / m, V / v);
		logVerbose("[SVR] xi = min(M/m,V/v) = min(" + M + "/" + m + "," + V + "/" + v + ") = " + xi);
		return xi;
	}

	private double calculateChi_0(JobProfile profile, JobMLProfile features) {

		double defaultParametersContribution = calculateDefaultParametersContribution(features);
		double featureContribution = 0;
		String chi_0_optional_names = new String();
		String chi_0_optional_values = new String();
		try {
			for (Map.Entry<String, SVRFeature> entry : features.getMlFeatures().entrySet()) {
				if (entry.getKey().equals("h") || entry.getKey().equals("x"))
					continue;
				double valueOfEntry = profile.get(entry.getKey());
				chi_0_optional_names += "+ (sigma_t/sigma_"+entry.getKey()+")*w_"+entry.getKey()+"*("+entry.getKey()+"-"+"mu_"+entry.getKey()+")";
				chi_0_optional_values += "+("+features.getSigma_t()+" / "+entry.getValue().getSigma()+") * "+entry.getValue().getW()+"*("+valueOfEntry+ "-"+ entry.getValue().getMu()+")";
				featureContribution += (features.getSigma_t() / entry.getValue().getSigma()) * entry.getValue().getW()*(valueOfEntry - entry.getValue().getMu());
			}
		} catch (IllegalArgumentException e) {
			logger.info("[SVR] Missing a MLProfile feature parameter in Profile.");
		}
		logVerbose("[SVR] Chi_0_optional_parameters: "+chi_0_optional_names+"\n"+"="+chi_0_optional_values+"\n = "+featureContribution);
		double result = defaultParametersContribution + featureContribution; 
		logVerbose("[SVR] Chi_0 = Chi_0_mandatory_parameters + Chi_0_optional_parameters  = "+defaultParametersContribution+"+"+featureContribution +" = "+result);
		
		return result;
	}

	public void reinitialize() {
		predictedSPJ = new HashMap<>();
	}
	
	private void logVerbose(String message){//TODO define new custom Logger Level, or a wrapper class
		if(settings.isVerbose()) logger.debug(message);
	}
}
