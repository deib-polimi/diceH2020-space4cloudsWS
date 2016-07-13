package it.polimi.diceH2020.SPACE4CloudWS.core;

import it.polimi.diceH2020.SPACE4Cloud.shared.inputData.JobClass;
import it.polimi.diceH2020.SPACE4Cloud.shared.inputData.TypeVM;
import it.polimi.diceH2020.SPACE4Cloud.shared.solution.*;
import it.polimi.diceH2020.SPACE4CloudWS.services.DataService;
import it.polimi.diceH2020.SPACE4CloudWS.solvers.solversImpl.MINLPSolver.MINLPSolver;
import it.polimi.diceH2020.SPACE4CloudWS.stateMachine.Events;
import it.polimi.diceH2020.SPACE4CloudWS.stateMachine.States;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.statemachine.StateMachine;
import org.springframework.stereotype.Service;

import javax.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class BuilderSolution {
	private static Logger logger = Logger.getLogger(BuilderSolution.class.getName());
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
		Instant first = Instant.now();
		error = false;
		String instanceId = dataService.getData().getId();
		Solution startingSol = new Solution(instanceId);
		startingSol.setGamma(dataService.getGamma());
		logger.info(String.format(
				"---------- Starting optimization for instance %s ----------", instanceId));

		// Phase 1
		// SingleClass
		dataService.getListJobClass().forEach(jobClass -> {
			Map<SolutionPerJob, Double> mapResults = new ConcurrentHashMap<>();
			dataService.getListTypeVM(jobClass).forEach(tVM -> {
				if (checkState()) {
					logger.info(String.format(
							"---------- Starting optimization jobClass %d considering VM type %s ----------",
							jobClass.getId(), tVM.getId()));
					SolutionPerJob solutionPerJob = createSolPerJob(jobClass, tVM);
					Optional<BigDecimal> result = minlpSolver.evaluate(solutionPerJob);
					// TODO: this avoids NullPointerExceptions, but MINLPSolver::evaluate should be less blind
					double cost = Double.MAX_VALUE;
					if (result.isPresent()) {
						cost = evaluator.evaluate(solutionPerJob);
					} else {
						// as in this::fallback
						solutionPerJob.setNumberUsers(solutionPerJob.getJob().getHup());
						solutionPerJob.setNumberVM(1);
					}
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
					logger.info("For job class " + jobClass.getId() + " has been selected the machine " + minTVM.getId());
					startingSol.setSolutionPerJob(s);
				});
			}
		});

		// Phase 2
		// multiClass
		if (checkState() && !error) {
			minlpSolver.evaluate(startingSol);
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

	private SolutionPerJob createSolPerJob(@NotNull JobClass jobClass, @NotNull TypeVM typeVM) {
		SolutionPerJob solPerJob = new SolutionPerJob();
		solPerJob.setChanged(Boolean.TRUE);
		solPerJob.setFeasible(Boolean.FALSE);
		solPerJob.setDuration(Double.MAX_VALUE);
		solPerJob.setJob(jobClass);
		solPerJob.setTypeVMselected(typeVM);
		solPerJob.setNumCores(dataService.getNumCores(typeVM));
		solPerJob.setDeltaBar(dataService.getDeltaBar(typeVM));
		solPerJob.setRhoBar(dataService.getRhoBar(typeVM));
		solPerJob.setSigmaBar(dataService.getSigmaBar(typeVM));
		solPerJob.setProfile(dataService.getProfile(jobClass, typeVM));
		return solPerJob;
	}

}
