package it.polimi.diceH2020.SPACE4CloudWS.connection;

import it.polimi.diceH2020.SPACE4CloudWS.solvers.ConnectionSettings;

import java.io.File;
import java.util.List;

public class SshConnector {


	private ExecSSH newExecSSH;
	private ScpTo newScpTo;
	private ScpFrom newScpFrom;

	public SshConnector( ConnectionSettings connSettings) {

		try {
			ConnectionCreator connection = new ConnectionCreator(connSettings);
			newExecSSH = new ExecSSH(connection);
			newScpTo = new ScpTo(connection);
			newScpFrom = new ScpFrom(connection);
		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	public void sendFile(String localFile, String remoteFile) throws Exception {
		newScpTo.sendfile(localFile, remoteFile);
		fixFile(new File(remoteFile).getParent(), new File(remoteFile).getName());
	}

	public List<String> exec(String command) throws Exception {
		return newExecSSH.mainExec(command);
	}

	public void receiveFile(String localFile, String remoteFile) throws Exception {
		newScpFrom.receivefile(localFile, remoteFile);
	}

	private void fixFile(String folder, String file) throws Exception {
		exec(String.format("cd %1$s && tr -d '\r' < %2$s > %2$s-bak && mv %2$s-bak %2$s", folder, file));
	}

	public List<String> pwd() throws Exception {
		return newExecSSH.getPWD();
	}

}
