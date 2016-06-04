package it.polimi.diceH2020.SPACE4CloudWS.solvers.solversImpl;

import it.polimi.diceH2020.SPACE4Cloud.shared.settings.SolverType;
import it.polimi.diceH2020.SPACE4CloudWS.solvers.Solver;
import it.polimi.diceH2020.SPACE4CloudWS.solvers.settings.SettingsDealer;
import it.polimi.diceH2020.SPACE4CloudWS.solvers.solversImpl.QNSolver.QNSolver;
import it.polimi.diceH2020.SPACE4CloudWS.solvers.solversImpl.SPNSolver.SPNSolver;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;

/**
 * Created by ciavotta on 11/02/16.
 */
@Component
public class SolverFactory {

    @Autowired
    private ApplicationContext ctx;
    @Autowired
    private SettingsDealer dealer;

    @Setter
    private SolverType type;

    @PostConstruct
    public void restoreDefaults() {
        type = dealer.getSolverDefaults().getType();
    }

    public Solver create() throws RuntimeException {
        switch (type) {
            case SPNSolver:
                return ctx.getBean(SPNSolver.class);
            case QNSolver:
                return ctx.getBean(QNSolver.class);
            default:
                throw new RuntimeException("Unrecognized solver type");
        }
    }
}
