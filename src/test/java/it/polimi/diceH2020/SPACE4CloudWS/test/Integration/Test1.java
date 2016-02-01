package it.polimi.diceH2020.SPACE4CloudWS.test.Integration;

import static com.jayway.restassured.module.jsv.JsonSchemaValidator.matchesJsonSchemaInClasspath;
import static com.jayway.restassured.module.mockmvc.RestAssuredMockMvc.get;
import static com.jayway.restassured.module.mockmvc.RestAssuredMockMvc.given;
import static com.jayway.restassured.module.mockmvc.RestAssuredMockMvc.when;

import java.io.IOException;

import org.apache.commons.httpclient.HttpStatus;
import org.hamcrest.Matchers;
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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.jayway.restassured.internal.mapper.ObjectMapperType;
import com.jayway.restassured.module.mockmvc.RestAssuredMockMvc;

import it.polimi.diceH2020.SPACE4Cloud.shared.InstanceData;
import it.polimi.diceH2020.SPACE4Cloud.shared.KeyDeserializer;
import it.polimi.diceH2020.SPACE4Cloud.shared.KeySerializer;
import it.polimi.diceH2020.SPACE4Cloud.shared.TypeVMJobClassKey;
import it.polimi.diceH2020.SPACE4CloudWS.stateMachine.Events;

@RunWith(SpringJUnit4ClassRunner.class) // 1
@SpringApplicationConfiguration(classes = it.polimi.diceH2020.SPACE4CloudWS.main.SPACE4CloudWS.class) // 2
@WebAppConfiguration // 3
@ActiveProfiles("test")
@Transactional
public class Test1 {


	@Autowired
	private InstanceData data;

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
				.body(matchesJsonSchemaInClasspath("AMPL/applData.json"));
	}

	@Test
	public void testPutInputData() {
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
			
			try {
				ObjectMapper mapper = new ObjectMapper();
				 
				SimpleModule module = new SimpleModule();
				module.addSerializer(TypeVMJobClassKey.class, new KeySerializer());
				mapper.registerModule(module);
				 
				String serialized = mapper.writeValueAsString(data);
				System.out.println(serialized);
				module.addDeserializer(TypeVMJobClassKey.class, new KeyDeserializer());
				mapper.registerModule(module);

				InstanceData myData = mapper.readValue(serialized, InstanceData.class);
			} catch (JsonProcessingException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
			
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
