package it.polimi.diceH2020.SPACE4CloudWS.solvers.solversImp.SPNSolver;

import it.polimi.diceH2020.SPACE4CloudWS.solvers.AbstractConnectionSettings;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Configuration class for SPN solver/server
 */
@Component
@ConfigurationProperties(prefix = "SPN")
public class SPNSettings extends AbstractConnectionSettings {


}
