package it.polimi.diceH2020.SPACE4CloudWS.solvers.settings;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

@Component
public class ConcreteSettingsDealer implements SettingsDealer {

    @Autowired
    private ApplicationContext applicationContext;

    @Autowired
    private SolverSettings solverSettings;

    @Override
    public ConnectionSettings getConnectionDefaults(Class<? extends ConnectionSettings> aClass) {
        final ConnectionSettings defaultSettings = applicationContext.getBean(aClass);
        return defaultSettings.shallowCopy();
    }

    @Override
    public SolverSettings getSolverDefaults() {
        return new SolverSettings(solverSettings);
    }
}
