package it.polimi.diceH2020.SPACE4CloudWS.connection;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import com.jcraft.jsch.Channel;
import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.Session;

public class ExecSSH {

	private String host;
	private String user;
	private String password;
	private int port;

	public ExecSSH(String host, String user, String password, int port) {
		this.host = host;
		this.user = user;
		this.password = password;
		this.port = port;
	}

	// main execution function
	// returns in List<Strings> all answers of the server
	public List<String> mainExec(String command) throws Exception {
		List<String> res = new ArrayList<String>();

		// creating session with username, server's address and port (22 by
		// default)
		JSch jsch = new JSch();
		Session session = jsch.getSession(user, host, port);
		session.setPassword(password);

		// disabling of certificate checks
		session.setConfig("StrictHostKeyChecking", "no");
		// creating connection
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

		return res;
	}

	public List<String> localExec(String command) throws Exception {
		List<String> res = new ArrayList<String>();
		ProcessBuilder pb = new ProcessBuilder(command.split(" "));
		pb.redirectErrorStream(true);

		Process p = pb.start();
		BufferedReader stream = new BufferedReader(new InputStreamReader(p.getInputStream()));
		String line = stream.readLine();
		while (line != null) {
			res.add(line);
			line = stream.readLine();
		}
		stream.close();

		return res;
	}

	public List<String> getPWD() throws Exception{
		return this.mainExec("pwd");
	}

	public List<String> removeAllFiles() throws Exception {
		return this.mainExec("rm -rf ./*");
	}
}
