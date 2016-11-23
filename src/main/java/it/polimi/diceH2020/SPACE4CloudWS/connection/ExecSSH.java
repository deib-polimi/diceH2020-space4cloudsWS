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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
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
			// taking streams
			OutputStream out = new ByteArrayOutputStream();
			OutputStream err = new ByteArrayOutputStream();
			channel.setOutputStream(out);
			channel.setErrStream(err);
			// connecting channel
			channel.connect();

			// reading channel while server responds something or until it
			// closes connection
			while (! channel.isClosed()) {
				try {
					Thread.sleep(1000);
				} catch (InterruptedException e) {
					// no op
				}
			}
			res.add(out.toString());
			res.add(err.toString());
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
