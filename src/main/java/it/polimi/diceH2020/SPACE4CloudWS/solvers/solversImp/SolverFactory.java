package it.polimi.diceH2020.SPACE4CloudWS.solvers.solversImp;

import it.polimi.diceH2020.SPACE4CloudWS.solvers.Solver;
import it.polimi.diceH2020.SPACE4CloudWS.solvers.solversImp.QNSolver.QNSolver;
import it.polimi.diceH2020.SPACE4CloudWS.solvers.solversImp.SPNSolver.SPNSolver;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

/**
 * Created by ciavotta on 11/02/16.
 */
@Component
@ConfigurationProperties(prefix = "solver")
public class SolverFactory {
    @Autowired
    private ApplicationContext ctx;
    private Type type = Type.SPNSolver;

    public Type getType() {
        return this.type;
    }

    public void setType(Type type) {
        this.type = type;
    }

    public Solver create() throws RuntimeException {

        switch (getType()) {
            case SPNSolver:
                return ctx.getBean(SPNSolver.class);
            case QNSolver:
                return ctx.getBean(QNSolver.class);
            default:
                throw new RuntimeException("Mis-configured deletion policy");
        }
    }

    public enum Type {SPNSolver, QNSolver}

    public enum SolverType {SPNSolver, QNSolver}

}
