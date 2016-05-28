package it.polimi.diceH2020.SPACE4CloudWS.solvers;

import lombok.Getter;
import lombok.Setter;

/**
 * Created by ciavotta on 13/02/16.
 */
@Getter
@Setter
public abstract class AbstractConnectionSettings implements ConnectionSettings {
    private String address;

    private Integer port = 22; //default value

    private String username;

    private String password;

    private String privateKeyFile;

    private String knownHosts;

    private boolean forceClean;

    private String remoteWorkDir;

    private double accuracy = 0.1; //default value

    private Integer maxDuration = Integer.MIN_VALUE;

    private String solverPath;

    public AbstractConnectionSettings(AbstractConnectionSettings that) {
        address = that.getAddress();
        port = that.getPort();
        username = that.getUsername();
        password = that.getPassword();
        privateKeyFile = that.getPrivateKeyFile();
        knownHosts = that.getKnownHosts();
        forceClean = that.isForceClean();
        remoteWorkDir = that.getRemoteWorkDir();
        accuracy = that.getAccuracy();
        maxDuration = that.getMaxDuration();
        solverPath = that.getSolverPath();
    }

    public AbstractConnectionSettings() {
        super();
    }
}
