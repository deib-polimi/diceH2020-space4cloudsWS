package it.polimi.diceH2020.SPACE4CloudWS.connection;

import com.jcraft.jsch.HostKey;
import com.jcraft.jsch.HostKeyRepository;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.Session;
import it.polimi.diceH2020.SPACE4CloudWS.solvers.ConnectionSettings;

public class ConnectionCreator {
    private final ConnectionSettings settings;

    public ConnectionCreator(ConnectionSettings settings) {
        this.settings = settings;
    }

    public Session createSession() throws Exception {
        JSch jsch = new JSch();
        jsch.setKnownHosts(settings.getKnownHosts());

        String key = settings.getPubKeyFile();
        if (key != null) {
            jsch.addIdentity(key, settings.getPassword());
        }

        Session session = jsch.getSession(settings.getUsername(), settings.getAddress(), settings.getPort());

        if (key == null) {
            session.setPassword(settings.getPassword());
        }

        session.setServerAliveCountMax(3);
        session.setServerAliveInterval(20000);

        /* Jsch 0.1.53 supports ecdsa-sha2-nistp256 key, but default
           configuration looks for RSA key. Here we override with the
           correct key type. */
        HostKeyRepository hkr = jsch.getHostKeyRepository();
        for (HostKey hk : hkr.getHostKey()) {
            if (hk.getHost().contains(settings.getAddress())) {
                String type = hk.getType();
                session.setConfig("server_host_key", type);
            }
        }
        return session;
    }

}
