/*
Copyright 2016 Michele Ciavotta
Copyright 2016 Eugenio Gianniti

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
*/
package it.polimi.diceH2020.SPACE4CloudWS.connection;

import com.jcraft.jsch.*;
import it.polimi.diceH2020.SPACE4CloudWS.solvers.settings.ConnectionSettings;

class ConnectionCreator {
    private final ConnectionSettings settings;

    ConnectionCreator(ConnectionSettings settings) {
        this.settings = settings;
    }

    Session createSession() throws JSchException {
        JSch jsch = new JSch();
        jsch.setKnownHosts(settings.getKnownHosts());

        String key = settings.getPrivateKeyFile();
        if (key != null) {
            jsch.addIdentity(key, settings.getPassword());
        }

        Session session = jsch.getSession(settings.getUsername(), settings.getAddress(), settings.getPort());

        if (key == null) {
            session.setPassword(settings.getPassword());
        }

        session.setServerAliveCountMax(3);
        session.setServerAliveInterval(20000);

        /* Jsch 0.1.53 supports ecdsa-sha2-nistp256 keys, but the default
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
