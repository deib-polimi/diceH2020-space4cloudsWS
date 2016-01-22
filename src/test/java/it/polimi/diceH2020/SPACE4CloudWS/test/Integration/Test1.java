package it.polimi.diceH2020.SPACE4CloudWS.test.Integration;

import static com.jayway.restassured.module.jsv.JsonSchemaValidator.matchesJsonSchemaInClasspath;

import java.util.Arrays;
import java.util.List;

import org.apache.commons.httpclient.HttpStatus;
import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.IntegrationTest;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;

import com.jayway.restassured.RestAssured;
import com.jayway.restassured.internal.mapper.ObjectMapperType;

import it.polimi.diceH2020.SPACE4Cloud.shared.InstanceData;
import it.polimi.diceH2020.SPACE4CloudWS.stateMachine.Events;

@RunWith(SpringJUnit4ClassRunner.class)   // 1
@SpringApplicationConfiguration(classes = it.polimi.diceH2020.SPACE4CloudWS.main.SPACE4CloudWS.class)   // 2
@WebAppConfiguration   // 3
@IntegrationTest("server.port:8080")   // 4
@ActiveProfiles("test")
public class Test1 {

    @Value("${local.server.port}")   // 6
    int port;

    @Before
    public void setUp() {
        RestAssured.port = port;
    }

    @Test
    public void testApplDataFormat() {
		possiblyRecover();

        RestAssured.	
        when().
                get("/appldata").
        then().
                statusCode(HttpStatus.SC_OK).
                body(matchesJsonSchemaInClasspath("myFiles/applData.json"));
    }

    @Test
    public void testPutInputData() {
		possiblyRecover();

		InstanceData data = createTestInstanceData();
    	
	   	 RestAssured.	
	     when().
	             get("/state").
	     then().
	             statusCode(HttpStatus.SC_OK).
	             assertThat().body(Matchers.is("IDLE"));
		
    	RestAssured.
    	given().
    	       contentType("application/json; charset=UTF-16").
    	       body(data).
    	when().
    	      post("/inputdata").then().statusCode(HttpStatus.SC_OK);

	   	 RestAssured.	
	     when().
	             get("/state").
	     then().
	             statusCode(HttpStatus.SC_OK).
	             assertThat().body(Matchers.is("CHARGED"));
	   	
    	 RestAssured.
    	 given().
	       contentType("application/json; charset=UTF-16").
	       body(Events.MIGRATE, ObjectMapperType.JACKSON_2).
         when().
                 post("/sendevent").
         then().
                 statusCode(HttpStatus.SC_OK)
                 .assertThat().body(Matchers.is("RUNNING"));
   }

	@Test
	public void testOptimizationAlgorithm() {
		possiblyRecover();

		if (RestAssured.get("/state").getBody().asString().equals("IDLE")) {
			InstanceData data = createTestInstanceData();

			RestAssured.
					given().
					contentType("application/json; charset=UTF-16").
					body(data).
					when().
					post("/inputdata").then().statusCode(HttpStatus.SC_OK);

			RestAssured.
					when().
					get("/state").
					then().
					statusCode(HttpStatus.SC_OK).
					assertThat().body(Matchers.is("CHARGED"));
		}

		if (! RestAssured.get("/state").getBody().asString().equals("RUNNING")) {
			RestAssured.
					given().
					contentType("application/json; charset=UTF-16").
					body(Events.MIGRATE, ObjectMapperType.JACKSON_2).
					when().
					post("/sendevent").
					then().
					statusCode(HttpStatus.SC_OK);
		}

		String body = "RUNNING";
		while (body.equals("RUNNING")) {
			try {
				Thread.sleep(5000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			body = RestAssured.get("/state").getBody().asString();
		}

		RestAssured.
				when().
				get("/state").
				then().
				statusCode(HttpStatus.SC_OK).
				assertThat().body(Matchers.is("FINISH"));
	}

	private InstanceData createTestInstanceData() {
		int gamma = 2400; // num cores cluster
		List<String> typeVm = Arrays.asList( "T1", "T2" );
		String provider = "Amazon";
		List<Integer> id_job = Arrays.asList( 10, 11 ); // numJobs = 2
		double[] think = { 15, 5 }; // check
		int[][] cM = { { 8, 8 }, { 8, 8 } };
		int[][] cR = { { 8, 8 }, { 8, 8 } };
		double[] eta = { 0.1, 0.3 };
		int[] hUp = { 10, 10 };
		int[] hLow = { 5, 5 };
		int[] nM = { 495, 65 };
		int[] nR = { 575, 5 };
		double[] mmax = { 36.016, 17.541 }; // maximum time to execute a single map
		double[] rmax = {  4.797, 0.499  };
		double[] mavg = { 17.196,  8.235 };
		double[] ravg = { 0.605, 0.297 };
		double[] d = { 300, 240 };
		double[] sH1max = { 0, 0 };
		double[] sHtypmax = { 18.058, 20.141 };
		double[] sHtypavg = { 2.024, 14.721 };
		double[] job_penalty = { 25.0, 14.99 };
		double[] r = { 200, 200 };
		return new InstanceData(gamma, typeVm, provider, id_job, think, cM, cR,
				eta, hUp, hLow, nM, nR, mmax, rmax, mavg, ravg,
				d, sH1max, sHtypmax, sHtypavg, job_penalty, r);
	}

	private void possiblyRecover() {
		while (! RestAssured.get("/state").getBody().asString().equals("IDLE")) {
			RestAssured.
					given().
					contentType("application/json; charset=UTF-16").
					body(Events.MIGRATE, ObjectMapperType.JACKSON_2).
					when().
					post("/sendevent").
					then().
					statusCode(HttpStatus.SC_OK);
		}
		RestAssured.
				when().
				get("/state").
				then().
				statusCode(HttpStatus.SC_OK).
				assertThat().body(Matchers.is("IDLE"));
	}

}
