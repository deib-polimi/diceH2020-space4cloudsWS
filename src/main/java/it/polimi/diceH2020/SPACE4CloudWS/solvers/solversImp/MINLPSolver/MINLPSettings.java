package it.polimi.diceH2020.SPACE4CloudWS.solvers.solversImp.MINLPSolver;

import it.polimi.diceH2020.SPACE4CloudWS.solvers.AbstractConnectionSettings;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "minlp")
public class MINLPSettings extends AbstractConnectionSettings {

    private String amplDirectory;

    private boolean verbose = false;

    /**
     * @return the amplDirectory
     */
    public String getAmplDirectory() {
        return amplDirectory;
    }

    /**
     * @param amplDirectory the amplDirectory to set
     */
    public void setAmplDirectory(String amplDirectory) {
        this.amplDirectory = amplDirectory;
    }

    public boolean isVerbose() {
        return verbose;
    }

    public void setVerbose(boolean verbose) {
        this.verbose = verbose;
    }

}
