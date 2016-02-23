package it.polimi.diceH2020.SPACE4CloudWS.connection;

import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.Session;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

public class ExecSSH {



	private ConnectionCreator connector;

	public ExecSSH(ConnectionCreator connector) {
		this.connector = connector;
	}

	// main execution function
	// returns in List<Strings> all answers of the server
	public List<String> mainExec(String command) throws Exception {
		Session session = connector.createSession();
		// disabling of certificate checks
		//session.setConfig("StrictHostKeyChecking", "no");
		// creating connection
		session.connect();

		// creating channel in execution mod
		ChannelExec channel = (ChannelExec) session.openChannel("exec");
		// sending command which runs bash-script in
		// Configuration.RUN_WORKING_DIRECTORY directory
		channel.setCommand(command);
		// taking input stream
		channel.setInputStream(null);
		channel.setErrStream(System.err);
		InputStream in = channel.getInputStream();
		InputStream err = channel.getErrStream();
		// connecting channel
		channel.connect();

		// read buffer
		byte[] tmp = new byte[1024];
		List<String> res = new ArrayList<>();
		StringBuilder builder = new StringBuilder();

		// reading channel while server responses smth or until it does not
		// close connection
		while (true) {
			// Maybe in this way we get mangled up stdout and stderr
			while (err.available() > 0) {
				int i = err.read(tmp, 0, 1024);
				if (i < 0) break;
				builder.append(new String(tmp, 0, i));
			}
			while (in.available() > 0) {
				int i = in.read(tmp, 0, 1024);
				if (i < 0) break;
				builder.append(new String(tmp, 0, i));
			}
			if (channel.isClosed()) {
				res.add(builder.toString());
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

	public List<String> getPWD() throws Exception {
		return this.mainExec("pwd");
	}

}
