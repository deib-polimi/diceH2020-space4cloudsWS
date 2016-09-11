/*
Copyright 2016 Michele Ciavotta

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
*/
package it.polimi.diceH2020.SPACE4CloudWS.test.connection;

import it.polimi.diceH2020.SPACE4CloudWS.solvers.solversImpl.MINLPSolver.MINLPSolver;
import it.polimi.diceH2020.SPACE4CloudWS.solvers.solversImpl.SPNSolver.SPNSolver;
import org.aspectj.apache.bcel.util.ClassPath;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

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
		res = milpSolver.getConnector().exec("ls", milpSolver.getClass());
		Assert.assertTrue(res.contains("exit-status: 0"));
		String wd = milpSolver.getRemoteWorkingDirectory();
		res = milpSolver.getConnector().exec("mkdir " + wd, milpSolver.getClass());
		Assert.assertTrue(res.size() == 2 && res.contains("exit-status: 0"));
		res = milpSolver.getConnector().exec("cd " + wd, milpSolver.getClass());
		Assert.assertTrue(res.size() == 2 && res.contains("exit-status: 0"));
		System.out.println(milpSolver.pwd());
		res = milpSolver.getConnector().exec("cd " + wd + " && mkdir problems utils solve", milpSolver.getClass());
		Assert.assertTrue(res.size() == 2 && res.contains("exit-status: 0"));
		System.out.println(ClassPath.getClassPath());
	}

	@Test
	public void testSPN() throws Exception{
		List<String> res = spnSolver.pwd();
		Assert.assertTrue(res.size() == 2 && res.contains("exit-status: 0"));
		res = spnSolver.getConnector().exec("rm -rf ./Experiments", spnSolver.getClass());
		Assert.assertTrue(res.size() == 2 && res.contains("exit-status: 0"));
		res = spnSolver.getConnector().exec("mkdir ./Experiments", spnSolver.getClass());
		Assert.assertTrue(res.size() == 2 && res.contains("exit-status: 0"));
	}

}
