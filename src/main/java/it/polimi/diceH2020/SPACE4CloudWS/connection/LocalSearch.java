package it.polimi.diceH2020.SPACE4CloudWS.connection;

import java.util.concurrent.Future;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.AsyncResult;
import org.springframework.stereotype.Component;

import it.polimi.diceH2020.SPACE4Cloud.shared.SolutionPerJob;
import it.polimi.diceH2020.SPACE4CloudWS.core.FileMaster;
import it.polimi.diceH2020.SPACE4CloudWS.core.Optimizer;
import it.polimi.diceH2020.SPACE4CloudWS.services.DataService;
import it.polimi.diceH2020.SPACE4CloudWS.solvers.SPNSolver;

@Component
public class LocalSearch {
	@Autowired
	private DataService dataService;
	
	@Autowired
	private SPNSolver SPNSolver;
	
	
	@Async
	public Future<Float> execute(SolutionPerJob solPerJob, int cycles) throws Exception {
		double tempResponseTime = 0;
		double throughput = 0;
		int maxIter = (int) Math.ceil((double) cycles / 2.0);
		int i = solPerJob.getPos();
		double deadline = dataService.getData().getD(i);
		int index = solPerJob.getIdxVmTypeSelected();
		double mAvg = dataService.getData().getMavg(index);
		double rAvg = dataService.getData().getRavg(index);
		double shTypAvg = dataService.getData().getSHtypavg(index);
		double think = dataService.getData().getThink(i);

		int nUsers = solPerJob.getNumberUsers();
		int NM = dataService.getData().getNM(i);
		int NR = dataService.getData().getNR(i);
		int nContainers = solPerJob.getNumberContainers();
		int hUp = dataService.getData().getHUp(i);

		FileMaster.create_SWN_ProfileR_Nef_File(nContainers, 1 / mAvg, 1 / (rAvg + shTypAvg), 1 / think, i);
		FileMaster.create_SWN_ProfileR_Def_File(nUsers, NM, NR, i);

		throughput = SPNSolver.run("SWN_ProfileR" + i, "fil" + i + ".sta");

		double responseTime = Optimizer.calculateResponseTime(throughput, nUsers, think);

		if (responseTime < deadline) {
			int iter = 0;
			while (responseTime < deadline && nUsers < hUp && iter <= maxIter) {
				tempResponseTime = responseTime;
				nUsers++;
				FileMaster.create_SWN_ProfileR_Nef_File(nContainers, 1 / mAvg, 1 / (rAvg + shTypAvg), 1 / think, i);
				FileMaster.create_SWN_ProfileR_Def_File(nUsers, NM, NR, i);
				throughput = SPNSolver.run("SWN_ProfileR" + i, "fil" + i + ".sta");
				responseTime = Optimizer.calculateResponseTime(throughput, nUsers, think);
				iter++;
			}
			iter = 0;
			while (responseTime < deadline && iter <= maxIter && nContainers > 1) {
				tempResponseTime = responseTime;
				// TODO aggiungere vm invece che gli slot.

				nContainers++;
				FileMaster.create_SWN_ProfileR_Nef_File(nContainers, 1 / mAvg, 1 / (rAvg + shTypAvg), 1 / think, i);
				FileMaster.create_SWN_ProfileR_Def_File(nUsers, NM, NR, i);

				throughput = SPNSolver.run("SWN_ProfileR" + i, "fil" + i + ".sta");
				responseTime = Optimizer.calculateResponseTime(throughput, nUsers, think);
				iter++;
			}

		} else {
			// TODO: check on this.
			while (responseTime > deadline) {

				nContainers++;
				FileMaster.create_SWN_ProfileR_Nef_File(nContainers, 1 / mAvg, 1 / (rAvg + shTypAvg), 1 / think, i);
				FileMaster.create_SWN_ProfileR_Def_File(nUsers, NM, NR, i);

				throughput = SPNSolver.run("SWN_ProfileR" + i, "fil" + i + ".sta");

				responseTime = Optimizer.calculateResponseTime(throughput, nUsers, think);
				tempResponseTime = responseTime;
			}
		}
		Thread.sleep(1000L);
		solPerJob.setSimulatedTime(tempResponseTime);
		solPerJob.setNumberUsers(nUsers);
		solPerJob.setNumberContainers(nContainers);
		solPerJob.setNumberVM(nContainers / solPerJob.getNumCores()); // TODO
																		// check.
		return new AsyncResult<Float>((float) tempResponseTime);
	}
	
	
}
