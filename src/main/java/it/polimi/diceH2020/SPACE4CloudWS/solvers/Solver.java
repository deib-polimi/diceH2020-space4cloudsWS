package it.polimi.diceH2020.SPACE4CloudWS.solvers;

import it.polimi.diceH2020.SPACE4Cloud.shared.solution.SolutionPerJob;
import it.polimi.diceH2020.SPACE4CloudWS.connection.SshConnector;
import lombok.NonNull;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Optional;

/**
 * Created by ciavotta on 11/02/16.
 */
public interface Solver {
    static double calculateResponseTime(@NonNull double throughput, int numServers, double thinkTime) {
        return (double) numServers / throughput - thinkTime;
    }

    static BigDecimal calculateResponseTime(@NonNull BigDecimal throughput, int numServers, double thinkTime) {
        return BigDecimal.valueOf(calculateResponseTime(throughput.doubleValue(), numServers, thinkTime)).setScale(2, RoundingMode.HALF_EVEN);
    }

    void setAccuracy(double accuracy);

    void initRemoteEnvironment() throws Exception;

    List<String> pwd() throws Exception;

    SshConnector getConnector();

    Optional<BigDecimal> evaluate(@NonNull SolutionPerJob solPerJob);

}
