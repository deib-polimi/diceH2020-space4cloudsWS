package it.polimi.diceH2020.SPACE4CloudWS.solvers;

public interface ConnectionSettings {
    String getAddress();

    String getPassword();

    Integer getPort();

    String getUsername();

    String getPrivateKeyFile();

    String getKnownHosts();

    boolean isForceClean();

    String getRemoteWorkDir();

    double getAccuracy();

    String getSolverPath();

    Integer getMaxDuration();

    void setAccuracy(double accuracy);

    void setMaxDuration(Integer duration);
}
