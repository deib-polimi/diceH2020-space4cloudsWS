/*
Copyright 2016 Jacopo Rigoli
Copyright 2016 Eugenio Gianniti

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
class MatrixBuilder extends Builder {

	@Setter(onMethod = @__(@Autowired))
	private DataService dataService;

	@Setter(onMethod = @__(@Autowired))
	private IEvaluator evaluator;

	private boolean error;

	/**
	 *
	 * @return Initialized Solution with its initialized SolutionPerJob
	 * @throws Exception
	 */
	Solution getInitialSolution() throws Exception {
		error = false;
		String instanceId = dataService.getData().getId();
		Solution startingSol = new Solution(instanceId);
		startingSol.setPrivateCloudParameters(Optional.of(dataService.getData().getPrivateCloudParameters()));
		startingSol.setProvider(dataService.getProviderName());
		startingSol.setScenario(Optional.of(dataService.getScenario()));
		logger.info(String.format(
				"---------- Starting optimization for instance %s ----------", instanceId));
		for(Entry<String, ClassParameters> jobClass : dataService.getMapJobClass().entrySet()){
			SolutionPerJob solutionPerJob = createSolPerJob(jobClass.getKey(),jobClass.getValue());

			startingSol.setSolutionPerJob(solutionPerJob);
		}
		return startingSol;
	}

	/**
	 * for each SPJ in Solution for each concurrency level create a new SPJ and put into a matrix cell
	 * for each cell calculate its duration
	 */
	Matrix getInitialMatrix(Solution solution){
		Instant first = Instant.now();
		Matrix tmpMatrix = createTmpMatrix(solution);
		Matrix matrix = new Matrix();

		for (Map.Entry<String,SolutionPerJob[]> matrixRow : tmpMatrix.entrySet()) {
			SolutionPerJob[] matrixLine = new SolutionPerJob[matrixRow.getValue().length];
			int i = 0;
			for(SolutionPerJob spj : matrixRow.getValue()){
				Map<SolutionPerJob, Double> mapResults = new ConcurrentHashMap<>();
				dataService.getLstTypeVM(spj.getId()).forEach(tVM -> {
					SolutionPerJob spj2 = createSolPerJob(spj.getId(),cloneJob(spj.getJob()));
					spj2.setParentID(dataService.getData().getId());
					setTypeVM(spj2, cloneVM(tVM));
					spj2.setNumberUsers(spj.getNumberUsers());
					approximator.approximateWithSVR(spj2);
					logger.info("Preliminary evaluation for class: "+spj2.getId()+" with "+ tVM.getId());
					double cost = evaluator.evaluate(spj2);
					logger.debug("Class"+spj2.getId()+"-> cost:"+cost+" users:"+spj2.getNumberUsers()+" #vm"+spj2.getNumberVM());
					mapResults.put(spj2, cost);
				});
				if (checkState()) {
					Optional<SolutionPerJob> min = mapResults.entrySet().stream().min(
							Map.Entry.comparingByValue()).map(Map.Entry::getKey);
					error = true;
					min.ifPresent(s -> {
						error = false;
						TypeVM minTVM = s.getTypeVMselected();
						logger.info("For job class " + s.getId() + " with H="+s.getNumberUsers()+ " has been selected the machine " + minTVM.getId());
					});
					matrixLine[i] = min.get();
				}
				i++;
			}
			matrix.put(matrixRow.getKey(), matrixLine);
		}

		if (error) {
			fallBack(solution);
		}
		else if (!checkState()) return null;

		Instant after = Instant.now();
		Phase ph = new Phase();
		ph.setId(PhaseID.INIT_SOLUTION);
		ph.setDuration(Duration.between(first, after).toMillis());
		solution.addPhase(ph);
		logger.info("---------- Initial matrix correctly created ----------");

		return matrix;
	}

	private Matrix createTmpMatrix(Solution solution){
		Matrix matrix = new Matrix();

		solution.getLstSolutions().forEach(spj -> {
			int Hup = spj.getJob().getHup();
			int Hlow = spj.getJob().getHlow();
			SolutionPerJob[] matrixLine = new SolutionPerJob[Hup-Hlow+1];
			for(int i=Hup; i >= Hlow ; i--){
				SolutionPerJob spjGivenH = createSolPerJob(spj.getId(),cloneJob(spj.getJob()));
				spjGivenH.setNumberUsers(i);
				spjGivenH.getJob().setHlow(i);
				spjGivenH.getJob().setHup(i);
				matrixLine[i-Hlow] = spjGivenH;
			}
			matrix.put(spj.getId(), matrixLine);
		});
		return matrix;
	}

	private SolutionPerJob createSolPerJob(@NotNull String classID,@NotNull ClassParameters jobClass) {
		SolutionPerJob solPerJob = new SolutionPerJob();
		solPerJob.setChanged(Boolean.TRUE);
		solPerJob.setFeasible(Boolean.FALSE);
		solPerJob.setDuration(Double.MAX_VALUE);
		solPerJob.setJob(jobClass);
		solPerJob.setId(classID);
		return solPerJob;
	}

	private void setTypeVM(@NotNull SolutionPerJob solPerJob, @NotNull TypeVM typeVM){
		solPerJob.setTypeVMselected(typeVM);
		solPerJob.setNumCores(dataService.getNumCores(typeVM.getId()));
		solPerJob.setDeltaBar(dataService.getDeltaBar(typeVM.getId()));
		solPerJob.setRhoBar(dataService.getRhoBar(typeVM.getId()));
		solPerJob.setSigmaBar(dataService.getSigmaBar(typeVM.getId()));
		solPerJob.setProfile(dataService.getProfile(solPerJob.getId(), solPerJob.getTypeVMselected().getId()));
	}

	private ClassParameters cloneJob(ClassParameters oldJob){
		ClassParameters job = new ClassParameters();
		job.setD(oldJob.getD());
		job.setHlow(oldJob.getHlow());
		job.setHup(oldJob.getHup());
		job.setPenalty(oldJob.getPenalty());
		job.setM(oldJob.getM());
		job.setThink(oldJob.getThink());
		job.setV(oldJob.getV());
		return job;
	}

	private TypeVM cloneVM(TypeVM oldTypeVM){
		TypeVM typeVM = new TypeVM();
		typeVM.setEta(oldTypeVM.getEta());
		typeVM.setId(oldTypeVM.getId());
		typeVM.setR(oldTypeVM.getR());
		return typeVM;
	}

}
