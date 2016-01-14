package it.polimi.diceH2020.SPACE4CloudWS.solvers;

public interface ConnectionSettings {
	
	

	public String getAddress() ;

	public String getPassword();

	public int getPort(); 

	public String getUsername();

	
	public String getPubKeyFile() ;
	public String getSetKnownHosts();

	public boolean isForceClean(); 

}
