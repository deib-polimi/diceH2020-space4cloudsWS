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

import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

class ExecSSH {

	private ConnectionCreator connector;

	ExecSSH(ConnectionCreator connector) {
		this.connector = connector;
	}

	// returns in List<Strings> all answers of the server
	List<String> execute(String command) throws JSchException, IOException {
		Session session = connector.createSession();
		ChannelExec channel = null;
		List<String> res = new ArrayList<>();

		try {
			// creating connection
			session.connect();

			// creating channel in execution mod
			channel = (ChannelExec) session.openChannel("exec");
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
			StringBuilder builder = new StringBuilder();

			// reading channel while server responds something or until it
			// closes connection
			while (! channel.isClosed()) {
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
				try {
					Thread.sleep(1000);
				} catch (InterruptedException e) {
					// no op
				}
			}
			res.add(builder.toString());
			res.add("exit-status: " + channel.getExitStatus());
		} finally {
			// closing connection
			if (channel != null)
				channel.disconnect();
			session.disconnect();
		}

		return res;
	}

	List<String> getPWD() throws JSchException, IOException {
		return this.execute("pwd");
	}

}
