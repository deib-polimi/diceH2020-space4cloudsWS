package it.polimi.diceH2020.SPACE4CloudWS.solvers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

/**
 * Created by ciavotta on 11/02/16.
 */
@Component
@ConfigurationProperties(prefix = "solverType")
public class SolverFactory {

    @Autowired
    private ApplicationContext ctx;
    private SolverType solverType = SolverType.SPNSolver;

    public SolverType getSolverType() {
        return this.solverType;
    }

    public void setSolverType(SolverType solverType) {
        this.solverType = solverType;
    }

    public Solver create() throws RuntimeException {

        switch (getSolverType()) {
            case SPNSolver:
                return ctx.getBean(SPNSolver.class);
            case QNSolver:
                return ctx.getBean(QNSolver.class);
            default:
                throw new RuntimeException("Misconfigured deletion policy");
        }
    }

    public enum SolverType {SPNSolver, QNSolver}

}
