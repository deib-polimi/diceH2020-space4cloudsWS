package it.polimi.diceH2020.SPACE4CloudWS.solvers.solversImpl.MINLPSolver;

import it.polimi.diceH2020.SPACE4CloudWS.solvers.AbstractConnectionSettings;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@Getter
@Setter
@ConfigurationProperties(prefix = "minlp")
public class MINLPSettings extends AbstractConnectionSettings {

    private String amplDirectory;
    private boolean verbose = false;

    public MINLPSettings(MINLPSettings that) {
        super(that);
        amplDirectory = that.getAmplDirectory();
        verbose = that.isVerbose();
    }

    MINLPSettings() {
        super();
    }
}
