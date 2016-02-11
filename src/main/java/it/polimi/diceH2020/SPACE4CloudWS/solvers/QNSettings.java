package it.polimi.diceH2020.SPACE4CloudWS.solvers;

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

    private boolean forceClean;

    @Override
    public String getAddress() {
        return address;
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public int getPort() {
        return port;
    }

    @Override
    public String getUsername() {
        return username;
    }

    @Override
    public String getPubKeyFile() {
        return pubKeyFile;
    }

    @Override
    public String getKnownHosts() {
        return knownHosts;
    }

    @Override
    public boolean isForceClean() {
        return forceClean;
    }
}
