package it.polimi.diceH2020.SPACE4CloudWS.solvers.solversImpl.QNSolver;

import it.polimi.diceH2020.SPACE4CloudWS.solvers.AbstractConnectionSettings;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Created by ciavotta on 11/02/16./**
 * Configuration class for SPN solver/server
 */
@Component
@ConfigurationProperties(prefix = "QN")
public class QNSettings extends AbstractConnectionSettings {

    private Integer maxTime = Integer.MIN_VALUE;

    public Integer getMaxTime() {
        return maxTime;
    }

    public void setMaxTime(Integer maxTime) {
        this.maxTime = maxTime;
    }
}
