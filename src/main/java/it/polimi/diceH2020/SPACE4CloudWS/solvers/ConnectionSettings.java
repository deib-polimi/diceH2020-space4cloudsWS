package it.polimi.diceH2020.SPACE4CloudWS.solvers;

public interface ConnectionSettings {
	String getAddress() ;
	String getPassword();
	int getPort();
	String getUsername();
	String getPubKeyFile() ;
	String getKnownHosts();
	boolean isForceClean();
}
