package eu.diceH2020.SPACE4CloudWS.test.connection;

import java.util.List;

import org.aspectj.apache.bcel.util.ClassPath;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import eu.diceH2020.SPACE4CloudWS.solvers.MINLPSolver;

@RunWith(SpringJUnit4ClassRunner.class)  
@SpringApplicationConfiguration(classes = eu.diceH2020.SPACE4CloudWS.app.SPACE4CloudWS.class)   // 2
@ActiveProfiles("test")
public class TestConnection {
		@Autowired
		MINLPSolver milpSolver;	

	    @Test
	    public void testApplDataFormat() {
	    	try {
				//System.out.println(milpSolver.pwd());
				List<String> res = milpSolver.clear();
				Assert.assertTrue(res.contains("exit-status: 0"));
				res.clear();
				res = milpSolver.getConnector().exec("ls");
				Assert.assertTrue(res.size() == 1 && res.contains("exit-status: 0"));
				res = milpSolver.getConnector().exec("mkdir AMPL");
				Assert.assertTrue(res.size() == 1 && res.contains("exit-status: 0"));
				res = milpSolver.getConnector().exec("cd AMPL");
				Assert.assertTrue(res.size() == 1 && res.contains("exit-status: 0"));
				System.out.println(milpSolver.pwd());
				res = milpSolver.getConnector().exec("cd AMPL && mkdir problems utils solve");
				Assert.assertTrue(res.size() == 1 && res.contains("exit-status: 0"));
				System.out.println(ClassPath.getClassPath());
				milpSolver.getConnector().sendFile("src/main/resources/static/initFiles/MILPSolver/problems/AM.run", "/home/tueguem/AMPL/AM.run");
				
				
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
	    }
}
