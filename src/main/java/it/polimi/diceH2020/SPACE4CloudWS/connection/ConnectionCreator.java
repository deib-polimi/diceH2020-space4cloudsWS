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
		jsch.addIdentity(settings.getPubKeyFile(), settings.getPassword());
		jsch.setKnownHosts(settings.getSetKnownHosts());

		Session session = jsch.getSession(settings.getUsername(), settings.getAddress(), settings.getPort());

		// Jsch 0.1.53 supports ecdsa-sha2-nistp256 key but default
		// configuration look for RSA key
		HostKeyRepository hkr = jsch.getHostKeyRepository();
		for (HostKey hk : hkr.getHostKey()) {
			if (hk.getHost().contains(settings.getAddress())) { // So the variable host inserted
												// by the user must be contained
												// in setKnownHosts
				String type = hk.getType();
				session.setConfig("server_host_key", type); // set the real key
															// type instead of
															// using the default
															// one
			}
		}
		return session;
	}
	
	
}