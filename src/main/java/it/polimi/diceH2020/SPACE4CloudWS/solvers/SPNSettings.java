package it.polimi.diceH2020.SPACE4CloudWS.solvers;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Configuration class for SPN solver/server
 */
@Component
@ConfigurationProperties(prefix="SPN")
public class SPNSettings implements ConnectionSettings {

	private String address;

	private int port = 22; //default value

	private String username;

	private String password;

	private String solverPath;

	private String remoteWorkDir;

	private String pubKeyFile;

	private String knownHosts;

	private boolean forceClean;
	private double accuracy = 0.1; //default value

	public String getPubKeyFile() {
		return pubKeyFile;
	}

	public void setPubKeyFile(String pubKeyFile) {
		this.pubKeyFile = pubKeyFile;
	}

	public String getKnownHosts() {
		return knownHosts;
	}

	public void setKnownHosts(String knownHosts) {
		this.knownHosts = knownHosts;
	}

	/**
	 * @return the address
	 */
	public String getAddress() {
		return address;
	}

	/**
	 * @param address the address to set
	 */
	public void setAddress(String address) {
		this.address = address;
	}

	/**
	 * @return the username
	 */
	public String getUsername() {
		return username;
	}

	/**
	 * @param username the username to set
	 */
	public void setUsername(String username) {
		this.username = username;
	}

	/**
	 * @return the password
	 */
	public String getPassword() {
		return password;
	}

	/**
	 * @param password the password to set
	 */
	public void setPassword(String password) {
		this.password = password;
	}

	/**
	 * @return the port
	 */
	public int getPort() {
		return port;
	}

	/**
	 * @param port the port to set
	 */
	public void setPort(int port) {
		this.port = port;
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

	/**
	 * @return the accuracy
	 */
	public double getAccuracy() {
		return accuracy;
	}

	/**
	 * @param accuracy the accuracy to set
	 */
	public void setAccuracy(double accuracy) {
		this.accuracy = accuracy;
	}

	public boolean isForceClean() {
		return forceClean;
	}

	public void setForceClean(boolean forceClean) {
		this.forceClean = forceClean;
	}

}
