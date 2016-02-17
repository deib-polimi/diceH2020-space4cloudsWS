package it.polimi.diceH2020.SPACE4CloudWS.core;

import it.polimi.diceH2020.SPACE4Cloud.shared.inputData.JobClass;
import it.polimi.diceH2020.SPACE4Cloud.shared.inputData.TypeVM;
import it.polimi.diceH2020.SPACE4Cloud.shared.solution.Phase;
import it.polimi.diceH2020.SPACE4Cloud.shared.solution.PhaseID;
import it.polimi.diceH2020.SPACE4Cloud.shared.solution.Solution;
import it.polimi.diceH2020.SPACE4Cloud.shared.solution.SolutionPerJob;
import it.polimi.diceH2020.SPACE4CloudWS.services.DataService;
import it.polimi.diceH2020.SPACE4CloudWS.solvers.solversImpl.MINLPSolver.MINLPSolver;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.Comparator;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class InitialSolutionBuilder {
    private static final AbsentLast<BigDecimal> ABSENT_LAST = new AbsentLast<>();
    private static Logger logger = Logger.getLogger(InitialSolutionBuilder.class.getName());
    @Autowired
    private DataService dataService;
    @Autowired
    private MINLPSolver minlpSolver;


    public Solution getInitialSolution() throws Exception {
    	Instant first = Instant.now();
        Solution startingSol = new Solution(dataService.getData().getId());
        startingSol.setGamma(dataService.getGamma());
        // Phase 1
        //SingleClass
        dataService.getListJobClass().forEach(jobClass -> {
            Map<TypeVM, Optional<BigDecimal>> mapResults = new ConcurrentHashMap<TypeVM, Optional<BigDecimal>>();
            dataService.getListTypeVM(jobClass).forEach(tVM -> {
                logger.info(String.format("---------- Starting optimization jobClass %d considering VM type %s ----------", jobClass.getId(), tVM.getId()));
                SolutionPerJob solutionPerJob = createSolPerJob(jobClass, tVM);
                mapResults.put(tVM, minlpSolver.evaluate(solutionPerJob));
            });

            Map.Entry<TypeVM, Optional<BigDecimal>> min = mapResults.entrySet().stream()
                    .min(Map.Entry.comparingByValue((o1, o2) -> ABSENT_LAST.compare(o1, o2))).get();

            TypeVM minTVM = min.getKey();
            logger.info("For job class " + jobClass.getId() + " has been selected the machine " + minTVM.getId());
            startingSol.setSolutionPerJob(createSolPerJob(jobClass, minTVM));
        });

        // Phase 2
        //multiClass
        minlpSolver.evaluate(startingSol);
        Evaluator.evaluate(startingSol);
        Instant after = Instant.now();
		Phase ph = new Phase();
		ph.setId(PhaseID.OPTIMIZATION);
		ph.setDuration(Duration.between(first, after).toMillis());
        startingSol.addPhase(ph);
        logger.info("---------- Initial solution correctly created ----------");
        return startingSol;
    }


    private SolutionPerJob createSolPerJob(@NotNull JobClass jobClass, @NotNull TypeVM minTVM) {
        SolutionPerJob solPerJob = new SolutionPerJob();
        solPerJob.setChanged(Boolean.TRUE);
        solPerJob.setFeasible(Boolean.FALSE);
        solPerJob.setDuration(Double.MAX_VALUE);
        solPerJob.setJob(jobClass);
        solPerJob.setTypeVMselected(minTVM);
        solPerJob.setNumCores(dataService.getNumCores(minTVM));
        solPerJob.setDeltaBar(dataService.getDeltaBar(minTVM));
        solPerJob.setRhoBar(dataService.getRhoBar(minTVM));
        solPerJob.setSigmaBar(dataService.getSigmaBar(minTVM));
        solPerJob.setProfile(dataService.getProfile(jobClass, minTVM));
        return solPerJob;

    }

    private interface OptionalComparator<T extends Comparable<T>> extends Comparator<Optional<T>> {
    }

    private static class AbsentLast<BigDecimal extends Comparable<BigDecimal>> implements OptionalComparator<BigDecimal> {
        @Override
        public int compare(Optional<BigDecimal> obj1, Optional<BigDecimal> obj2) {
            if (obj1.isPresent() && obj2.isPresent()) return obj1.get().compareTo(obj2.get());
            else if (obj1.isPresent()) return 1;
            else if (obj2.isPresent()) return -1;
            else return 0;
        }

    }

}
