package it.polimi.diceH2020.SPACE4CloudWS.test.Integration;

import com.jayway.restassured.internal.mapper.ObjectMapperType;
import com.jayway.restassured.module.mockmvc.RestAssuredMockMvc;

import static com.jayway.restassured.module.mockmvc.RestAssuredMockMvc.*;

import it.polimi.diceH2020.SPACE4Cloud.shared.InstanceData;
import it.polimi.diceH2020.SPACE4CloudWS.stateMachine.Events;
import org.apache.commons.httpclient.HttpStatus;
import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.transaction.BeforeTransaction;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import java.util.Arrays;
import java.util.List;

import static com.jayway.restassured.module.jsv.JsonSchemaValidator.matchesJsonSchemaInClasspath;

@RunWith(SpringJUnit4ClassRunner.class) // 1
@SpringApplicationConfiguration(classes = it.polimi.diceH2020.SPACE4CloudWS.main.SPACE4CloudWS.class) // 2
@WebAppConfiguration // 3
// @IntegrationTest("server.port:8080") // 4
//@ActiveProfiles("test")
@Transactional
public class Test1 {

	// @Value("${local.server.port}") // 6
	// int port;

	@Autowired
	WebApplicationContext wac;
	MockMvc mockMvc;
	
	boolean setUp = false;
	
	
	public void setUp() {
		// RestAssured.port = port;
		mockMvc = MockMvcBuilders.webAppContextSetup(wac).build();
		RestAssuredMockMvc.mockMvc(mockMvc);	
		this.setUp = true;
	}

	@Test
	public void testApplDataFormat() {

		when().get("/appldata").then().statusCode(HttpStatus.SC_OK)
				.body(matchesJsonSchemaInClasspath("myFiles/applData.json"));
	}

	@Test
	public void testPutInputData() {
		InstanceData data = createTestInstanceData();

		when().get("/state").then().statusCode(HttpStatus.SC_OK).assertThat().body(Matchers.is("IDLE"));

		given().contentType("application/json; charset=UTF-16").body(data).when().post("/inputdata").then()
				.statusCode(HttpStatus.SC_OK);

		when().get("/state").then().statusCode(HttpStatus.SC_OK).assertThat().body(Matchers.is("CHARGED"));

		given().contentType("application/json; charset=UTF-16").body(Events.MIGRATE, ObjectMapperType.JACKSON_2).when()
				.post("/sendevent").then().statusCode(HttpStatus.SC_OK).assertThat().body(Matchers.is("RUNNING"));
	}

	@Test
	public void testOptimizationAlgorithm() {
		if (get("/state").getBody().asString().equals("IDLE")) {
			InstanceData data = createTestInstanceData();

			given().contentType("application/json; charset=UTF-16").body(data).when().post("/inputdata").then()
					.statusCode(HttpStatus.SC_OK);

			when().get("/state").then().statusCode(HttpStatus.SC_OK).assertThat().body(Matchers.is("CHARGED"));
		}

		if (!get("/state").getBody().asString().equals("RUNNING")) {

			given().contentType("application/json; charset=UTF-16").body(Events.MIGRATE, ObjectMapperType.JACKSON_2)
					.when().post("/sendevent").then().statusCode(HttpStatus.SC_OK);
		}

		String body = "RUNNING";
		while (body.equals("RUNNING")) {
			try {
				Thread.sleep(5000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			body = get("/state").getBody().asString();
		}

		when().get("/state").then().statusCode(HttpStatus.SC_OK).assertThat().body(Matchers.is("FINISH"));
	}
//
	private InstanceData createTestInstanceData() {
		int gamma = 2400; // num cores cluster
		List<String> typeVm = Arrays.asList("T1", "T2");
		String provider = "Amazon";
		List<Integer> id_job = Arrays.asList(10, 11); // numJobs = 2
		double[] think = { 15, 5 }; // check
		int[][] cM = { { 8, 8 }, { 8, 8 } };
		int[][] cR = { { 8, 8 }, { 8, 8 } };
		double[] eta = { 0.1, 0.3 };
		int[] hUp = { 10, 10 };
		int[] hLow = { 5, 5 };
		int[] nM = { 495, 65 };
		int[] nR = { 575, 5 };
		double[] mmax = { 36.016, 17.541 }; // maximum time to execute a single
											// map
		double[] rmax = { 4.797, 0.499 };
		double[] mavg = { 17.196, 8.235 };
		double[] ravg = { 0.605, 0.297 };
		double[] d = { 300, 240 };
		double[] sH1max = { 0, 0 };
		double[] sHtypmax = { 18.058, 20.141 };
		double[] sHtypavg = { 2.024, 14.721 };
		double[] job_penalty = { 25.0, 14.99 };
		int[] r = { 200, 200 };
		return new InstanceData(gamma, typeVm, provider, id_job, think, cM, cR, eta, hUp, hLow, nM, nR, mmax, rmax,
				mavg, ravg, d, sH1max, sHtypmax, sHtypavg, job_penalty, r);
	}

	@BeforeTransaction
	public void possiblyRecover() {
		if (!setUp) {
			setUp();
		}
		while (!get("/state").getBody().asString().equals("IDLE")) {

			given().contentType("application/json; charset=UTF-16").body(Events.MIGRATE, ObjectMapperType.JACKSON_2)
					.when().post("/sendevent").then().statusCode(HttpStatus.SC_OK);
		}
	}

}
