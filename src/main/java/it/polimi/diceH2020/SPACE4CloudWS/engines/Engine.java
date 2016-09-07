package it.polimi.diceH2020.SPACE4CloudWS.engines;

import java.util.concurrent.Future;
import it.polimi.diceH2020.SPACE4Cloud.shared.inputData.InstanceData;
import it.polimi.diceH2020.SPACE4Cloud.shared.settings.Settings;
import it.polimi.diceH2020.SPACE4Cloud.shared.solution.Solution;

public interface Engine {

	public void setInstanceData(InstanceData inputData);
	
	public void setSolution(Solution sol);

	public Future<String> runningInitSolution();

	public void evaluatingInitSolution() ;
	
	public void localSearch();

	public void changeSettings(Settings settings);
	
	public void restoreDefaults();

	public Future<String> reduceMatrix();

	public Solution getSolution();
}
