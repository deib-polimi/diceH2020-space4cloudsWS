package it.polimi.diceH2020.SPACE4CloudWS.solvers.solversImpl.SPNSolver;

import it.polimi.diceH2020.SPACE4CloudWS.solvers.AbstractConnectionSettings;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Configuration class for SPN solver/server
 */
@Component
@Getter
@Setter
@ConfigurationProperties(prefix = "SPN")
public class SPNSettings extends AbstractConnectionSettings {

    public SPNSettings(SPNSettings that) {
        super(that);
    }

    SPNSettings() {
        super();
    }
}
