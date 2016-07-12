package it.polimi.diceH2020.SPACE4CloudWS.core;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import javax.validation.constraints.NotNull;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.statemachine.StateMachine;
import org.springframework.stereotype.Service;

import it.polimi.diceH2020.SPACE4Cloud.shared.inputData.JobClass;
import it.polimi.diceH2020.SPACE4Cloud.shared.inputData.TypeVM;
import it.polimi.diceH2020.SPACE4Cloud.shared.solution.IEvaluator;
import it.polimi.diceH2020.SPACE4Cloud.shared.solution.Phase;
import it.polimi.diceH2020.SPACE4Cloud.shared.solution.PhaseID;
import it.polimi.diceH2020.SPACE4Cloud.shared.solution.Solution;
import it.polimi.diceH2020.SPACE4Cloud.shared.solution.SolutionPerJob;
import it.polimi.diceH2020.SPACE4CloudWS.services.DataService;
import it.polimi.diceH2020.SPACE4CloudWS.solvers.solversImpl.MINLPSolver.MINLPSolver;
import it.polimi.diceH2020.SPACE4CloudWS.stateMachine.Events;
import it.polimi.diceH2020.SPACE4CloudWS.stateMachine.States;

@Service
public class InitialMatrixBuilder {
	private static Logger logger = Logger.getLogger(InitialSolutionBuilder.class.getName());
	@Autowired
	private DataService dataService;
	@Autowired
	private MINLPSolver minlpSolver;
	@Autowired
	private StateMachine<States, Events> stateHandler;
	@Autowired
	private IEvaluator evaluator;
	private boolean error;

	public Solution getInitialSolution() throws Exception {
		error = false;
		String instanceId = dataService.getData().getId();
		Solution startingSol = new Solution(instanceId);
		startingSol.setGamma(dataService.getGamma());
		logger.info(String.format(
				"---------- Starting optimization for instance %s ----------", instanceId));
		dataService.getListJobClass().forEach(jobClass -> {
			SolutionPerJob solutionPerJob = createSolPerJob(jobClass);
			startingSol.setSolutionPerJob(solutionPerJob);
		});
		
		return startingSol;
	}
	
	public Matrix getInitialMatrix(Solution solution){
		Instant first = Instant.now();
		
		Matrix tmpMatrix = createTmpMatrix(solution);
		Matrix matrix = new Matrix();
		
		for (Map.Entry<String,SolutionPerJob[]> matrixRow : tmpMatrix.entrySet()) {
			SolutionPerJob[] matrixLine = new SolutionPerJob[matrixRow.getValue().length];
			int i = 0;
			for(SolutionPerJob spj : matrixRow.getValue()){
				Map<SolutionPerJob, Double> mapResults = new ConcurrentHashMap<>();
				dataService.getListTypeVM(spj.getJob()).forEach(tVM -> {
					setTypeVM(spj, tVM);
					
					Optional<BigDecimal> result = minlpSolver.evaluate(spj); //TODO: still sequential?

					// TODO: this avoids NullPointerExceptions, but MINLPSolver::evaluate should be less blind
					double cost = Double.MAX_VALUE;
					if (result.isPresent()) {
						cost = evaluator.evaluate(spj);
					} else {
						// as in this::fallback
						spj.setNumberUsers(spj.getJob().getHup());
						spj.setNumberVM(1);
					}
					mapResults.put(spj, cost);
				});
				if (checkState()) {
					Optional<SolutionPerJob> min = mapResults.entrySet().stream().min(
							Map.Entry.comparingByValue()).map(Map.Entry::getKey);
					error = true;
					min.ifPresent(s -> {
						error = false;
						TypeVM minTVM = s.getTypeVMselected();
						logger.info("For job class " + s.getJob().getId() + "with H="+s.getNumberUsers()+ " has been selected the machine " + minTVM.getId());
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
		List<SolutionPerJob> spjGivenHList = new ArrayList<SolutionPerJob>();
		Matrix matrix = new Matrix();
		
		solution.getLstSolutions().stream().forEach(spj->{
			int Hup = spj.getJob().getHup();
			int Hlow = spj.getJob().getHlow();
			SolutionPerJob[] matrixLine = new SolutionPerJob[Hup-Hlow+1];
			for(int i= Hup; i>= Hlow ; i--){
				matrixLine[i-Hlow] = null;
				
				SolutionPerJob spjGivenH = cloneSpj(spj);
				spjGivenH.setNumberUsers(i);
				spjGivenHList.add(spjGivenH);
			}
			matrix.put(spj.getJob().getId(), matrixLine);
		});
		return matrix;
	}

	//TODO
	private void fallBack(Solution sol) {
		sol.getLstSolutions().forEach(s -> {
			s.setNumberVM(1);
			s.setNumberUsers(s.getJob().getHup());
			s.setAlfa(0.0);
			s.setBeta(0.0);
		});
	}

	private boolean checkState() {
		return !stateHandler.getState().getId().equals(States.IDLE);
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
		newSpj.setJob(oldSpj.getJob());
		newSpj.setNumberContainers(oldSpj.getNumberContainers());
		newSpj.setNumberUsers(oldSpj.getNumberUsers());
		newSpj.setNumberVM(oldSpj.getNumberVM());
		newSpj.setNumCores(oldSpj.getNumCores());
		newSpj.setNumOnDemandVM(oldSpj.getNumOnDemandVM());
		newSpj.setNumReservedVM(oldSpj.getNumReservedVM());
		newSpj.setNumSpotVM(oldSpj.getNumSpotVM());
		newSpj.setParentID(oldSpj.getParentID());
		newSpj.setPos(oldSpj.getPos());
		newSpj.setProfile(oldSpj.getProfile());
		newSpj.setRhoBar(oldSpj.getRhoBar());
		newSpj.setSigmaBar(oldSpj.getSigmaBar());
		newSpj.setTypeVMselected(oldSpj.getTypeVMselected());
		return newSpj;
	}

}
