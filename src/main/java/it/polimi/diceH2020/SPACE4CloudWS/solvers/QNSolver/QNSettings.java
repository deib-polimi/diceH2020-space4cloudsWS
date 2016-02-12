package it.polimi.diceH2020.SPACE4CloudWS.solvers.QNSolver;

import it.polimi.diceH2020.SPACE4CloudWS.solvers.ConnectionSettings;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Created by ciavotta on 11/02/16./**
 * Configuration class for SPN solver/server
 */
@Component
@ConfigurationProperties(prefix = "QN")
public class QNSettings implements ConnectionSettings {

    private String address;

    private int port = 22; //default value

    private String username;

    private String password;

    private String solverPath;

    private String remoteWorkDir;

    private String pubKeyFile;

    private String knownHosts;

    private Integer maxTime = Integer.MIN_VALUE;

    private boolean forceClean;

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
    public int getPort() {
        return port;
    }

    public void setPort(int port) {
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
        return 0;
    }

    @Override
    public void setAccuracy(double accuracy) {

    }

    @Override
    public String getSolverPath() {
        return solverPath;
    }

    public void setSolverPath(String solverPath) {
        this.solverPath = solverPath;
    }

    public Integer getMaxTime() {
        return maxTime;
    }

    public void setMaxTime(Integer maxTime) {
        this.maxTime = maxTime;
    }
}
