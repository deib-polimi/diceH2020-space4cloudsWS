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

import com.jcraft.jsch.JSchException;
import it.polimi.diceH2020.SPACE4CloudWS.solvers.settings.ConnectionSettings;

import java.io.File;
import java.io.IOException;
import java.util.List;

public class SshConnector {

	private ExecSSH newExecSSH;
	private ScpTo newScpTo;
	private ScpFrom newScpFrom;

	public SshConnector(ConnectionSettings connSettings) {
		ConnectionCreator connection = new ConnectionCreator(connSettings);
		newExecSSH = new ExecSSH(connection);
		newScpTo = new ScpTo(connection);
		newScpFrom = new ScpFrom(connection);
	}

	public void sendFile(String localFile, String remoteFile) throws JSchException, IOException {
		newScpTo.sendFile(localFile, remoteFile);
		fixFile(new File(remoteFile).getParent(), new File(remoteFile).getName());
	}

	public List<String> exec(String command) throws JSchException, IOException {
		return newExecSSH.execute(command);
	}

	public void receiveFile(String localFile, String remoteFile) throws JSchException, IOException {
		newScpFrom.receiveFile(localFile, remoteFile);
	}

	private void fixFile(String folder, String file) throws JSchException, IOException {
		exec(String.format("cd %1$s && tr -d '\r' < %2$s > %2$s-bak && mv %2$s-bak %2$s", folder, file));
	}

	public List<String> pwd() throws JSchException, IOException {
		return newExecSSH.getPWD();
	}

}
