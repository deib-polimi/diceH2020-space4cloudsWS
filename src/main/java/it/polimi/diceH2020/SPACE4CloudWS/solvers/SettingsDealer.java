package it.polimi.diceH2020.SPACE4CloudWS.solvers;

public interface SettingsDealer {

    ConnectionSettings getConnectionDefaults(Class<? extends ConnectionSettings> aClass);
    SolverSettings getSolverDefaults();
}
