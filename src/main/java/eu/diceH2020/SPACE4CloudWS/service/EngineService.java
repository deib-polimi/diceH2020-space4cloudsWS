package eu.diceH2020.SPACE4CloudWS.service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Future;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.statemachine.StateMachine;
import org.springframework.statemachine.annotation.OnTransition;
import org.springframework.statemachine.annotation.WithStateMachine;
import org.springframework.stereotype.Service;

import eu.diceH2020.SPACE4CloudWS.algorithm.Algorithm;
import eu.diceH2020.SPACE4CloudWS.algorithm.Solution;
import eu.diceH2020.SPACE4CloudWS.messages.InstanceData;
import eu.diceH2020.SPACE4CloudWS.messages.Settings;
import eu.diceH2020.SPACE4CloudWS.stateMachine.Events;
import eu.diceH2020.SPACE4CloudWS.stateMachine.States;

@Service
@WithStateMachine
public class EngineService {

	@Autowired
	private Algorithm algorithm;

	@Autowired
	private DataService dataService;

	@Autowired
	private StateMachine<States, Events> stateHandler;

	public EngineService() {
	}

	public Solution getSolution() {
		return algorithm.getSolution();

	}

	@Async("workExecutor")
	@OnTransition(target = "RUNNING")
	public void greedy() throws Exception {
		int i = 0;
		List<Future<Float>> objective = new ArrayList<Future<Float>>();
		do {
			algorithm.greedy_cal();

			for (int j = 0; j < dataService.getNumberJobs(); j++)
				objective.add(algorithm.localSearch(j));

			for (int j = 0; j < dataService.getNumberJobs(); j++) {
				while (!(objective.get(j).isDone())) {
					Thread.sleep(100);
				}
				System.out.println("parall" + j);
			}
			objective.clear();
			System.out.println("paralleis3" + i);

			if (dataService.getNumberJobs() > 1)
				algorithm.sharedCluster();

			for (int j = 0; j < dataService.getNumberJobs(); j++) {
				dataService.getData().incHUp(j, 1);
				dataService.getData().setHLow(j, (int) Math.ceil(0.9 * dataService.getData().getHUp(j)));

			}
			i = i + 1;
		} while (i < 100);
		stateHandler.sendEvent(Events.MIGRATE);
		System.out.println(stateHandler.getState().getId());

	}

	public void init() {
		try {
			algorithm.init();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	//nobody calls this function
	public void sequence() throws Exception {
		algorithm.sequentialGreedy();
	}

	public void setAccuracyAncCycles(Settings settings) {
		algorithm.extractAccuracyAndCycle(settings);
	}

	/**
	 * @param inputData
	 *            the inputData to set
	 */
	public void setInstanceData(InstanceData inputData) {
		this.dataService.setInstanceData(inputData);
	}

	//this is called from a debug endpoint
	public void simulation() throws Exception {
		List<Double> lstResults = algorithm.sharedCluster();
		System.out.println(lstResults.get(1));
	}

}
