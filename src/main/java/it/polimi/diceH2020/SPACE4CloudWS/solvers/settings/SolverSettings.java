package it.polimi.diceH2020.SPACE4CloudWS.solvers.settings;

import it.polimi.diceH2020.SPACE4Cloud.shared.settings.SolverType;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@Getter
@Setter
@ConfigurationProperties(prefix = "solver")
public class SolverSettings {

    private SolverType type = SolverType.SPNSolver;

    SolverSettings(SolverSettings that) {
        type = that.getType();
    }

    SolverSettings() {
        super();
    }
}
