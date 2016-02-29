package it.polimi.diceH2020.SPACE4CloudWS.solvers;

import it.polimi.diceH2020.SPACE4Cloud.shared.solution.SolutionPerJob;
import it.polimi.diceH2020.SPACE4CloudWS.connection.SshConnector;
import lombok.NonNull;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import org.springframework.cache.annotation.Cacheable;

/**
 * Created by ciavotta on 11/02/16.
 */
public interface Solver {

    void setAccuracy(double accuracy);
    void setMaxDuration(Integer duration);

    void initRemoteEnvironment() throws Exception;

    List<String> pwd() throws Exception;

    SshConnector getConnector();

    Optional<BigDecimal> evaluate(@NonNull SolutionPerJob solPerJob);

}
