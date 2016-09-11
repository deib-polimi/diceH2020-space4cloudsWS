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
package it.polimi.diceH2020.SPACE4CloudWS.core;

import it.polimi.diceH2020.SPACE4Cloud.shared.inputData.JobClass;
import it.polimi.diceH2020.SPACE4Cloud.shared.inputData.Profile;
import it.polimi.diceH2020.SPACE4Cloud.shared.inputData.TypeVM;
import it.polimi.diceH2020.SPACE4Cloud.shared.solution.*;
import it.polimi.diceH2020.SPACE4CloudWS.services.DataService;
import it.polimi.diceH2020.SPACE4CloudWS.solvers.solversImpl.MINLPSolver.AMPLModelType;
import it.polimi.diceH2020.SPACE4CloudWS.solvers.solversImpl.MINLPSolver.MINLPSolver;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class BuilderMatrix extends Builder{
	private static Logger logger = Logger.getLogger(BuilderSolution.class.getName());
	@Autowired
	private DataService dataService;
	@Autowired
	private MINLPSolver minlpSolver;
	@Autowired
	private IEvaluator evaluator;
	private boolean error;

	/**
	 *
	 * @return Initialized Solution with its initialized SolutionPerJob
	 * @throws Exception
	 */
	public Solution getInitialSolution() throws Exception {
		error = false;
		String instanceId = dataService.getData().getId();
		Solution startingSol = new Solution(instanceId);
		startingSol.setPrivateCloudParameters(dataService.getData().getPrivateCloudParameters());
		startingSol.setGamma(dataService.getGamma());
		logger.info(String.format(
				"---------- Starting optimization for instance %s ----------", instanceId));
		dataService.getListJobClass().forEach(jobClass -> {
			SolutionPerJob solutionPerJob = createSolPerJob(jobClass);
			startingSol.setSolutionPerJob(solutionPerJob);
		});
		return startingSol;
	}

	/**
	 * for each SPJ in Solution for each concurrency level create a new SPJ and put into a matrix cell
	 * for each cell calculate its duration
	 */
	public Matrix getInitialMatrix(Solution solution){
		Instant first = Instant.now();
		minlpSolver.reinitialize(); //TODO no more used here?
		approximator.reinitialize();
		Matrix tmpMatrix = createTmpMatrix(solution);
		Matrix matrix = new Matrix();

		for (Map.Entry<String,SolutionPerJob[]> matrixRow : tmpMatrix.entrySet()) {
			SolutionPerJob[] matrixLine = new SolutionPerJob[matrixRow.getValue().length];
			int i = 0;
			for(SolutionPerJob spj : matrixRow.getValue()){
				Map<SolutionPerJob, Double> mapResults = new ConcurrentHashMap<>();
				dataService.getListTypeVM(spj.getJob()).forEach(tVM -> {
					SolutionPerJob spj2 = createSolPerJob(cloneJob(spj.getJob()));
					spj2.setParentID(dataService.getData().getId());
					setTypeVM(spj2, cloneVM(tVM));
					spj2.setNumberUsers(spj.getNumberUsers());
					Optional<BigDecimal> result = null;
					if(!settings.isSvr()){ //exploit SVR
						result = minlpSolver.evaluate(spj2); //TODO: still sequential?
					}else{
						result = approximator.approximate(spj2);
					}
					// TODO: this avoids NullPointerExceptions, but MINLPSolver::evaluate should be less blind
					double cost = Double.MAX_VALUE;
					if (result.isPresent()) {
						cost = evaluator.evaluate(spj2);
						logger.debug("Class"+spj2.getJob().getId()+"-> cost:"+cost+" users:"+spj2.getNumberUsers()+" #vm"+spj2.getNumberVM());
					} else {
						// as in this::fallback
						spj2.setNumberUsers(spj2.getJob().getHup());
						spj2.setXi(1); //TODO
						spj2.setNumberVM(1);
						logger.info("No result from solver for evaluating SPJ");
					}
					mapResults.put(spj2, cost);
				});
				if (checkState()) {
					Optional<SolutionPerJob> min = mapResults.entrySet().stream().min(
							Map.Entry.comparingByValue()).map(Map.Entry::getKey);
					error = true;
					min.ifPresent(s -> {
						error = false;
						TypeVM minTVM = s.getTypeVMselected();
						logger.info("For job class " + s.getJob().getId() + " with H="+s.getNumberUsers()+ " has been selected the machine " + minTVM.getId());
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

	/**
	 * Selection of matrix cells to retrieve the best combination.
	 * One and only one cell per row (one H for each Job).
	 */
	public void cellsSelectionWithKnapsack(Matrix matrix, Solution solution){
		minlpSolver.setModelType(AMPLModelType.KNAPSACK);
		minlpSolver.evaluate(matrix,solution);
	}

	public void cellsSelectionWithBinPacking(Matrix matrix, Solution solution){
		minlpSolver.setModelType(AMPLModelType.BIN_PACKING);
		minlpSolver.evaluate(matrix,solution);
	}

	private Matrix createTmpMatrix(Solution solution){
		Matrix matrix = new Matrix();

		solution.getLstSolutions().stream().forEach(spj->{
			int Hup = spj.getJob().getHup();
			int Hlow = spj.getJob().getHlow();
			SolutionPerJob[] matrixLine = new SolutionPerJob[Hup-Hlow+1];
			for(int i=Hup; i >= Hlow ; i--){
				SolutionPerJob spjGivenH = createSolPerJob(cloneJob(spj.getJob()));
				spjGivenH.setNumberUsers(i);
				spjGivenH.getJob().setHlow(i);
				spjGivenH.getJob().setHup(i);
				matrixLine[i-Hlow] = spjGivenH;
			}
			matrix.put(spj.getJob().getId(), matrixLine);
		});
		return matrix;
	}

	private SolutionPerJob createSolPerJob(@NotNull JobClass jobClass) {
		SolutionPerJob solPerJob = new SolutionPerJob();
		solPerJob.setChanged(Boolean.TRUE);
		solPerJob.setFeasible(Boolean.FALSE);
		solPerJob.setDuration(Double.MAX_VALUE);
		solPerJob.setJob(jobClass);

		return solPerJob;
	}

	private void setTypeVM(SolutionPerJob solPerJob, @NotNull TypeVM typeVM){
		solPerJob.setTypeVMselected(typeVM);
		solPerJob.setNumCores(dataService.getNumCores(typeVM));
		solPerJob.setDeltaBar(dataService.getDeltaBar(typeVM));
		solPerJob.setRhoBar(dataService.getRhoBar(typeVM));
		solPerJob.setSigmaBar(dataService.getSigmaBar(typeVM));
		solPerJob.setProfile(dataService.getProfile(solPerJob.getJob(), typeVM));
	}


	public SolutionPerJob cloneSpj(SolutionPerJob oldSpj){
		SolutionPerJob newSpj = new SolutionPerJob();

		newSpj.setAlfa(oldSpj.getAlfa());
		newSpj.setBeta(oldSpj.getBeta());
		newSpj.setChanged(oldSpj.getChanged());
		newSpj.setCost(oldSpj.getCost());
		newSpj.setDeltaBar(oldSpj.getDeltaBar());
		newSpj.setDuration(oldSpj.getDuration());
		newSpj.setError(oldSpj.getError());
		newSpj.setFeasible(oldSpj.getFeasible());
		newSpj.setNumberContainers(oldSpj.getNumberContainers());
		newSpj.setNumberUsers(oldSpj.getNumberUsers());
		//newSpj.setNumberVM(oldSpj.getNumberVM());
		newSpj.setNumCores(oldSpj.getNumCores());
		//newSpj.setNumOnDemandVM(oldSpj.getNumOnDemandVM());
		//newSpj.setNumReservedVM(oldSpj.getNumReservedVM());
		//newSpj.setNumSpotVM(oldSpj.getNumSpotVM());
		newSpj.setParentID(oldSpj.getParentID());
		newSpj.setPos(oldSpj.getPos());
		newSpj.setRhoBar(oldSpj.getRhoBar());
		newSpj.setSigmaBar(oldSpj.getSigmaBar());
		newSpj.setXi(oldSpj.getXi());

		newSpj.setJob(cloneJob(oldSpj.getJob()));
		//newSpj.setTypeVMselected(cloneVM(oldSpj.getTypeVMselected()));
		newSpj.setProfile(cloneProfile(oldSpj.getProfile()));

		return newSpj;
	}

	private Profile cloneProfile(Profile oldProfile){
		Profile profile = new Profile();
		profile.setCm(oldProfile.getCm());
		profile.setCr(oldProfile.getCr());
		profile.setMavg(oldProfile.getMavg());
		profile.setMmax(oldProfile.getMmax());
		profile.setNm(oldProfile.getNm());
		profile.setNr(oldProfile.getNr());
		profile.setRavg(oldProfile.getRavg());
		profile.setRmax(oldProfile.getRmax());
		profile.setSh1max(oldProfile.getSh1max());
		profile.setShtypavg(oldProfile.getShtypavg());
		profile.setShtypmax(oldProfile.getShtypmax());
		return profile;
	}

	private JobClass cloneJob(JobClass oldJob){
		JobClass job = new JobClass();
		job.setD(oldJob.getD());
		job.setHlow(oldJob.getHlow());
		job.setHup(oldJob.getHup());
		job.setId(oldJob.getId());
		job.setJob_penalty(oldJob.getJob_penalty());
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
