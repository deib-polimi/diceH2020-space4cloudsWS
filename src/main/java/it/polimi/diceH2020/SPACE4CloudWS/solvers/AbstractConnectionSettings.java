package it.polimi.diceH2020.SPACE4CloudWS.solvers;

/**
 * Created by ciavotta on 13/02/16.
 */
public abstract class AbstractConnectionSettings implements ConnectionSettings {
    protected String address;

    protected Integer port = 22; //default value

    protected String username;

    protected String password;

    private String pubKeyFile;

    private String knownHosts;

    private boolean forceClean;

    private String remoteWorkDir;

    private double accuracy = 0.1; //default value
    
    private Integer maxDuration = Integer.MIN_VALUE;

    public Integer getMaxDuration() {
        return maxDuration;
    }

    public void setMaxDuration(Integer maxTime) {
        this.maxDuration = maxTime;
    }

    private String solverPath;


    @Override
    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    @Override
    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    @Override
    public Integer getPort() {
        return port;
    }

    public void setPort(Integer port) {
        this.port = port;
    }

    @Override
    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    @Override
    public String getPubKeyFile() {
        return pubKeyFile;
    }

    public void setPubKeyFile(String pubKeyFile) {
        this.pubKeyFile = pubKeyFile;
    }

    @Override
    public String getKnownHosts() {
        return knownHosts;
    }

    public void setKnownHosts(String knownHosts) {
        this.knownHosts = knownHosts;
    }

    @Override
    public boolean isForceClean() {
        return forceClean;
    }

    public void setForceClean(boolean forceClean) {
        this.forceClean = forceClean;
    }

    @Override
    public String getRemoteWorkDir() {
        return remoteWorkDir;
    }

    public void setRemoteWorkDir(String remoteWorkDir) {
        this.remoteWorkDir = remoteWorkDir;
    }

    @Override
    public double getAccuracy() {
        return accuracy;
    }

    @Override
    public void setAccuracy(double accuracy) {
        this.accuracy = accuracy;
    }

    @Override
    public String getSolverPath() {
        return solverPath;
    }

    public void setSolverPath(String solverPath) {
        this.solverPath = solverPath;
    }
}
