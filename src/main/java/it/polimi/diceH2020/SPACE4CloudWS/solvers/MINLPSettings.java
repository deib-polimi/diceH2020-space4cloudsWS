package it.polimi.diceH2020.SPACE4CloudWS.solvers;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "minlp")
public class MINLPSettings {
	private String address;

	private String password;

	private int port = 22;

	private String username;
	
	private String pubkeyfile;
	
	private String remoteWorkDir;
	
	private String amplDirectory;
	
	private String solverPath;

	public String getAddress() {
		return address;
	}

	public String getPassword() {
		return password;
	}

	/**
	 * @return the port
	 */
	public int getPort() {
		return port;
	}

	public String getUsername() {
		return username;
	}

	public void setAddress(String address) {
		this.address = address;
	}

	public void setPassword(String password) {
		this.password = password;
	}
	/**
	 * @param port the port to set
	 */
	public void setPort(int port) {
		this.port = port;
	}

	public void setUsername(String username) {
		this.username = username;
	}

	/**
	 * @return the pubkeyfile
	 */
	public String getPubkeyfile() {
		return pubkeyfile;
	}

	/**
	 * @param pubkeyfile the pubkeyfile to set
	 */
	public void setPubkeyfile(String pubkeyfile) {
		this.pubkeyfile = pubkeyfile;
	}


	/**
	 * @return the amplDirectory
	 */
	public String getAmplDirectory() {
		return amplDirectory;
	}

	/**
	 * @param amplDirectory the amplDirectory to set
	 */
	public void setAmplDirectory(String amplDirectory) {
		this.amplDirectory = amplDirectory;
	}

	/**
	 * @return the solverPath
	 */
	public String getSolverPath() {
		return solverPath;
	}

	/**
	 * @param solverPath the solverPath to set
	 */
	public void setSolverPath(String solverPath) {
		this.solverPath = solverPath;
	}

	/**
	 * @return the remoteWorkDir
	 */
	public String getRemoteWorkDir() {
		return remoteWorkDir;
	}

	/**
	 * @param remoteWorkDir the remoteWorkDir to set
	 */
	public void setRemoteWorkDir(String remoteWorkDir) {
		this.remoteWorkDir = remoteWorkDir;
	}

}
