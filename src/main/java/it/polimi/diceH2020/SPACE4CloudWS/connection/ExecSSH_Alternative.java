package it.polimi.diceH2020.SPACE4CloudWS.connection;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import com.jcraft.jsch.Channel;
import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.Session;


/**
 * @author ciavotta, desantis	
 * This class is unused, but I keep it because in the future it should replace the one based on 
 * unsername and password
 */
public class ExecSSH_Alternative {
	private String host;
	private String user;
	private int port;
	private String pubkeyfile;
	private String passphrase = "" ;

	public ExecSSH_Alternative(String pubkeyfile, String passphrase, String host, String user, int port) {
		super();
		if (pubkeyfile == "")
			this.pubkeyfile = "/home/cr/users/anand/.ssh/id_dsa";
		else
			this.pubkeyfile = pubkeyfile;
		
		this.passphrase = passphrase;
		this.host = host;
		this.user = user;
		this.port = port;
	}

	public List<String> mainExec(String command) {
		List<String> res = new ArrayList<String>();

		try {
			JSch jsch = new JSch();
			// jsch.addIdentity(pubkeyfile);
			jsch.addIdentity(pubkeyfile, passphrase);
			jsch.setKnownHosts("/home/cr/users/anand/.ssh/known_hosts");

			Session session = jsch.getSession(user, host, port);

			session.setPassword(passphrase);

			session.connect();

			// creating channel in execution mod
			Channel channel = session.openChannel("exec");
			// sending command which runs bash-script in
			// Configuration.RUN_WORKING_DIRECTORY directory
			((ChannelExec) channel).setCommand(command);
			// taking input stream
			channel.setInputStream(null);
			((ChannelExec) channel).setErrStream(System.err);
			InputStream in = channel.getInputStream();
			// connecting channel
			channel.connect();

			// read buffer
			byte[] tmp = new byte[1024];

			// reading channel while server responses smth or until it does not
			// close connection
			while (true) {
				while (in.available() > 0) {
					int i = in.read(tmp, 0, 1024);
					if (i < 0)
						break;
					res.add(new String(tmp, 0, i));
				}
				if (channel.isClosed()) {
					res.add("exit-status: " + channel.getExitStatus());
					break;
				}
				try {
					Thread.sleep(1000);
				} catch (Exception ee) {

				}

			}

			// closing connection
			channel.disconnect();
			session.disconnect();
		} catch (Exception e) {
			System.out.println(e);
			e.printStackTrace();
		}

		return res;
	}

}
