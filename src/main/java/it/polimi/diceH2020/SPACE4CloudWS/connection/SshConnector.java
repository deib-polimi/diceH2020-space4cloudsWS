package it.polimi.diceH2020.SPACE4CloudWS.connection;

import java.io.File;
import java.util.List;

public class SshConnector {

	// this object runs bash-script on AMPL server
	private ExecSSH newExecSSH;

	// this object uploads files on AMPL server
	private ScpTo newScpTo;

	// this block downloads logs and results of AMPL
	private ScpFrom newScpFrom;

	public SshConnector(String host, String user, String password, int port) {
		newExecSSH = new ExecSSH(host, user, password, port);
		newScpTo = new ScpTo(host, user, password, port);
		newScpFrom = new ScpFrom(host, user, password, port);
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
	
	public List<String> pwd() throws Exception{
		return newExecSSH.getPWD();
	}

	public List<String> clear() throws Exception {
		return newExecSSH.removeAllFiles();
	}

	public void createRemoteDirs() throws Exception {
				
	}

}