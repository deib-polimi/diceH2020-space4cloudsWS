package it.polimi.diceH2020.SPACE4CloudWS.solvers;

import it.polimi.diceH2020.SPACE4Cloud.shared.solution.SolutionPerJob;
import it.polimi.diceH2020.SPACE4CloudWS.services.SshConnectorProxy;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

/**
 * Created by ciavotta on 11/02/16.
 */
public interface Solver {

    void setAccuracy(double accuracy);

    void setMaxDuration(Integer duration);

    void initRemoteEnvironment() throws Exception;

    List<String> pwd() throws Exception;

    SshConnectorProxy getConnector();

    Optional<BigDecimal> evaluate(SolutionPerJob solPerJob);

    void restoreDefaults();

}
