package it.polimi.diceH2020.SPACE4CloudWS.solvers.solversImpl.MINLPSolver;

import it.polimi.diceH2020.SPACE4CloudWS.solvers.settings.AbstractConnectionSettings;
import it.polimi.diceH2020.SPACE4CloudWS.solvers.settings.ConnectionSettings;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@Getter
@Setter
@ConfigurationProperties(prefix = "minlp")
final class MINLPSettings extends AbstractConnectionSettings {

    private String amplDirectory;
    private boolean verbose = false;

    private MINLPSettings(MINLPSettings that) {
        super(that);
        amplDirectory = that.getAmplDirectory();
        verbose = that.isVerbose();
    }

    // Used by Spring Boot
    @SuppressWarnings("unused")
    MINLPSettings() {
        super();
    }

    @Override
    public ConnectionSettings shallowCopy() {
        return new MINLPSettings(this);
    }
}
