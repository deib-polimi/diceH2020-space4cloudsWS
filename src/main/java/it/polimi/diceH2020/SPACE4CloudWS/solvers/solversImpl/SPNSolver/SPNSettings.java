package it.polimi.diceH2020.SPACE4CloudWS.solvers.solversImpl.SPNSolver;

import it.polimi.diceH2020.SPACE4CloudWS.solvers.settings.AbstractConnectionSettings;
import it.polimi.diceH2020.SPACE4CloudWS.solvers.settings.ConnectionSettings;
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
final class SPNSettings extends AbstractConnectionSettings {

    private SPNSettings(SPNSettings that) {
        super(that);
    }

    // Used by Spring Boot
    @SuppressWarnings("unused")
    SPNSettings() {
        super();
    }

    @Override
    public ConnectionSettings shallowCopy() {
        return new SPNSettings(this);
    }
}
