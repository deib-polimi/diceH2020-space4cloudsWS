/*
Copyright 2016-2017 Eugenio Gianniti
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
package it.polimi.diceH2020.SPACE4CloudWS.core;

import it.polimi.diceH2020.SPACE4Cloud.shared.inputDataMultiProvider.ClassParameters;
import it.polimi.diceH2020.SPACE4Cloud.shared.inputDataMultiProvider.TypeVM;
import it.polimi.diceH2020.SPACE4Cloud.shared.settings.SPNModel;
import it.polimi.diceH2020.SPACE4Cloud.shared.solution.*;
import it.polimi.diceH2020.SPACE4CloudWS.services.DataService;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.validation.constraints.NotNull;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Service
class SolutionBuilder extends Builder {

	@Setter(onMethod = @__(@Autowired))
	private DataService dataService;

	@Setter(onMethod = @__(@Autowired))
	private IEvaluator evaluator;

	private boolean error;

	Solution getInitialSolution() {
		Instant first = Instant.now();
		error = false;
		String instanceId = dataService.getData().getId();
		Solution startingSol = new Solution(instanceId);
		startingSol.setProvider(dataService.getProviderName());
		startingSol.setScenario(Optional.of(dataService.getScenario()));
		logger.info(String.format(
				"---------- Starting optimization for instance %s ----------", instanceId));

		for(Entry<String, ClassParameters> jobClass : dataService.getMapJobClass().entrySet()){
			Map<SolutionPerJob, Double> mapResults = new ConcurrentHashMap<>();
			dataService.getLstTypeVM(jobClass.getKey()).forEach(tVM -> {
				if (checkState()) {
					logger.info(String.format(
							"---------- Starting optimization jobClass %s considering VM type %s ----------",
							jobClass.getKey(), tVM.getId()));
					SolutionPerJob solutionPerJob = createSolPerJob(jobClass.getValue(), tVM, jobClass.getKey());
					solutionPerJob.setNumberUsers(solutionPerJob.getJob().getHup());
					solutionPerJob.setXi(approximator.calculateXi(solutionPerJob));
					logger.trace("Computed xi is " + solutionPerJob.getXi());
					if(dataService.getScenario().getSwn() == SPNModel.STORM) {
						solutionPerJob.updateNumberVM(1);
					} else {
						approximator.approximateWithSVR(solutionPerJob);
					}
					double cost = evaluator.evaluate(solutionPerJob);
					logger.debug("Class"+solutionPerJob.getId()+"-> cost:"+cost+" users:"+solutionPerJob.getNumberUsers()+" #vm"+solutionPerJob.getNumberVM());
					mapResults.put(solutionPerJob, cost);
				}
			});
			if (checkState()) {
				Optional<SolutionPerJob> min = mapResults.entrySet().stream().min(
						Map.Entry.comparingByValue()).map(Map.Entry::getKey);
				error = true;
				min.ifPresent(s -> {
					error = false;
					TypeVM minTVM = s.getTypeVMselected();
					logger.info("For job class " + s.getId() + " has been selected the machine " + minTVM.getId());
					startingSol.setSolutionPerJob(s);
				});
			}
		}

		if (checkState() && !error) {
			evaluator.evaluate(startingSol);
		} else if (error) {
			fallBack(startingSol);
		}
		else if (!checkState()) return null;

		Instant after = Instant.now();
		Phase ph = new Phase();
		ph.setId(PhaseID.INIT_SOLUTION);
		ph.setDuration(Duration.between(first, after).toMillis());
		startingSol.addPhase(ph);
		logger.info("---------- Initial solution correctly created ----------");
		return startingSol;
	}

	private SolutionPerJob createSolPerJob(@NotNull ClassParameters jobClass, @NotNull TypeVM typeVM, String id) {
		String vmName = typeVM.getId();
		SolutionPerJob solPerJob = new SolutionPerJob();
		solPerJob.setId(id);
		solPerJob.setChanged(Boolean.TRUE);
		solPerJob.setFeasible(Boolean.FALSE);
		solPerJob.setDuration(Double.MAX_VALUE);
		solPerJob.setJob(jobClass);
		solPerJob.setTypeVMselected(typeVM);
		solPerJob.setNumCores(dataService.getNumCores(vmName));
		solPerJob.setDeltaBar(dataService.getDeltaBar(vmName));
		solPerJob.setRhoBar(dataService.getRhoBar(vmName));
		solPerJob.setSigmaBar(dataService.getSigmaBar(vmName));
		solPerJob.setProfile(dataService.getProfile(id, vmName));
		solPerJob.setParentID(dataService.getData().getId());
		return solPerJob;
	}

}
