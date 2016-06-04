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
