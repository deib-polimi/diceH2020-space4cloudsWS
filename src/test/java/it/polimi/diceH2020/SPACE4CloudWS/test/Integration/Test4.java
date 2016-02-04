package it.polimi.diceH2020.SPACE4CloudWS.test.Integration;

import static com.jayway.restassured.module.mockmvc.RestAssuredMockMvc.get;
import static com.jayway.restassured.module.mockmvc.RestAssuredMockMvc.given;
import static com.jayway.restassured.module.mockmvc.RestAssuredMockMvc.when;

import org.apache.commons.httpclient.HttpStatus;
import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.MethodSorters;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import com.jayway.restassured.internal.mapper.ObjectMapperType;
import com.jayway.restassured.module.mockmvc.RestAssuredMockMvc;

import it.polimi.diceH2020.SPACE4Cloud.shared.solution.Solution;
import it.polimi.diceH2020.SPACE4CloudWS.stateMachine.Events;

@RunWith(SpringJUnit4ClassRunner.class) // 1
@SpringApplicationConfiguration(classes = it.polimi.diceH2020.SPACE4CloudWS.main.SPACE4CloudWS.class) // 2
@WebAppConfiguration // 3
@ActiveProfiles("test")
@Transactional
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class Test4 {


	@Autowired
	private Solution solution;

	@Autowired
	WebApplicationContext wac;
	MockMvc mockMvc;

	boolean setUp = false;

	@Before
	public void setUp() {
		// RestAssured.port = port;
		mockMvc = MockMvcBuilders.webAppContextSetup(wac).build();
		RestAssuredMockMvc.mockMvc(mockMvc);
		this.setUp = true;
	}


	@Test
	public void test1() {
		if (get("/state").getBody().asString().equals("IDLE")) {
			
			given().contentType("application/json; charset=UTF-16").body(solution).when().post("/solution").then()
					.statusCode(HttpStatus.SC_OK);

			when().get("/state").then().statusCode(HttpStatus.SC_OK).assertThat().body(Matchers.is("CHARGED_INITSOLUTION"));
		}
	}
	
	@Test
	public void test2(){
		given().contentType("application/json; charset=UTF-16").body(Events.TO_RUNNING_LS, ObjectMapperType.JACKSON_2)
		.when().post("/event").then().statusCode(HttpStatus.SC_OK);
		String body = "RUNNING_LS";
		while (body.equals("RUNNING_LS")) {
			try {
				Thread.sleep(5000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			body = get("/state").getBody().asString();
		}

		when().get("/state").then().statusCode(HttpStatus.SC_OK).assertThat().body(Matchers.is("FINISH"));

	}


}
