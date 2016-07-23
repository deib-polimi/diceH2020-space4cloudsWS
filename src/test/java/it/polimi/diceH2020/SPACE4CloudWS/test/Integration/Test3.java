package it.polimi.diceH2020.SPACE4CloudWS.test.Integration;

import com.jayway.restassured.internal.mapper.ObjectMapperType;
import com.jayway.restassured.module.mockmvc.RestAssuredMockMvc;
import it.polimi.diceH2020.SPACE4Cloud.shared.inputData.InstanceData;
import it.polimi.diceH2020.SPACE4CloudWS.stateMachine.Events;
import org.apache.commons.httpclient.HttpStatus;
import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import static com.jayway.restassured.module.mockmvc.RestAssuredMockMvc.*;


@RunWith(SpringJUnit4ClassRunner.class) // 1
@SpringApplicationConfiguration(classes = it.polimi.diceH2020.SPACE4CloudWS.main.SPACE4CloudWS.class) // 2
@WebAppConfiguration // 3
@ActiveProfiles("test")
@Transactional
public class Test3 {


	@Autowired
	WebApplicationContext wac;
	MockMvc mockMvc;
	@Autowired
	private InstanceData data;

	@Before
	public void setUp() {
		// RestAssured.port = port;
		mockMvc = MockMvcBuilders.webAppContextSetup(wac).build();
		RestAssuredMockMvc.mockMvc(mockMvc);
	}
	
	
	@Test
	public void testPutInputData() {
		when().get("/state").then().statusCode(HttpStatus.SC_OK).assertThat().body(Matchers.is("IDLE"));

		given().contentType("application/json; charset=UTF-16").body(data).when().post("/inputdata").then()
				.statusCode(HttpStatus.SC_OK);

		when().get("/state").then().statusCode(HttpStatus.SC_OK).assertThat().body(Matchers.is("CHARGED"));

		post("/debug/event").body().as(InstanceData.class);
		
		given().contentType("application/json; charset=UTF-16").body(Events.MIGRATE, ObjectMapperType.JACKSON_2).when()
				.post("/debug/sendevent").then().statusCode(HttpStatus.SC_OK).assertThat().body(Matchers.is("RUNNING"));
	}

}
