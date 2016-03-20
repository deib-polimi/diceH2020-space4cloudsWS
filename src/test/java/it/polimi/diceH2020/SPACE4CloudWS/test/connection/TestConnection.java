package it.polimi.diceH2020.SPACE4CloudWS.test.connection;

import org.aspectj.apache.bcel.util.ClassPath;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import it.polimi.diceH2020.SPACE4CloudWS.solvers.solversImpl.MINLPSolver.MINLPSolver;
import it.polimi.diceH2020.SPACE4CloudWS.solvers.solversImpl.SPNSolver.SPNSolver;

import java.util.List;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringApplicationConfiguration(classes = it.polimi.diceH2020.SPACE4CloudWS.main.SPACE4CloudWS.class)   // 2
@ActiveProfiles("test")
public class TestConnection {
	@Autowired
	MINLPSolver milpSolver;

	@Autowired
	SPNSolver spnSolver;

	@Test
	public void testAMPL() throws Exception {
		List<String> res = milpSolver.clearWorkingDir();
		Assert.assertTrue(res.contains("exit-status: 0"));
		res.clear();
		res = milpSolver.getConnector().exec("ls");
		Assert.assertTrue(res.contains("exit-status: 0"));
		String wd = milpSolver.getRemoteWorkingDirectory();
		res = milpSolver.getConnector().exec("mkdir " + wd);
		Assert.assertTrue(res.size() == 2 && res.contains("exit-status: 0"));
		res = milpSolver.getConnector().exec("cd " + wd);
		Assert.assertTrue(res.size() == 2 && res.contains("exit-status: 0"));
		System.out.println(milpSolver.pwd());
		res = milpSolver.getConnector().exec("cd " + wd + " && mkdir problems utils solve");
		Assert.assertTrue(res.size() == 2 && res.contains("exit-status: 0"));
		System.out.println(ClassPath.getClassPath());
	}

	@Test
	public void testSPN() throws Exception{
		List<String> res = spnSolver.pwd();
		Assert.assertTrue(res.size() == 2 && res.get(0).contains("/home/user") && res.contains("exit-status: 0"));
		res = spnSolver.getConnector().exec("rm -rf ./Experiments");
		Assert.assertTrue(res.size() == 2 && res.contains("exit-status: 0"));
		res = spnSolver.getConnector().exec("mkdir ./Experiments");
		Assert.assertTrue(res.size() == 2 && res.contains("exit-status: 0"));
	}

}
