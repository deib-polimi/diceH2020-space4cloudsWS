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
package it.polimi.diceH2020.SPACE4CloudWS.test.Integration;

import com.jayway.restassured.internal.mapper.ObjectMapperType;
import com.jayway.restassured.module.mockmvc.RestAssuredMockMvc;

import it.polimi.diceH2020.SPACE4Cloud.shared.inputDataMultiProvider.InstanceDataMultiProvider;
import it.polimi.diceH2020.SPACE4CloudWS.stateMachine.Events;
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

import static com.jayway.restassured.module.jsv.JsonSchemaValidator.matchesJsonSchemaInClasspath;
import static com.jayway.restassured.module.mockmvc.RestAssuredMockMvc.*;

@RunWith(SpringJUnit4ClassRunner.class) // 1
@SpringApplicationConfiguration(classes = it.polimi.diceH2020.SPACE4CloudWS.main.SPACE4CloudWS.class) // 2
@WebAppConfiguration // 3
@ActiveProfiles("test")
@Transactional
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class Test1 {

	@Autowired
	WebApplicationContext wac;
	MockMvc mockMvc;
	boolean setUp = false;
	@Autowired
	private InstanceDataMultiProvider data;

	@Before
	public void setUp() {
		// RestAssured.port = port;
		mockMvc = MockMvcBuilders.webAppContextSetup(wac).build();
		RestAssuredMockMvc.mockMvc(mockMvc);
		this.setUp = true;
	}

	@Test
	public void test1ApplDataFormat() {

		when().get("/appldata").then().statusCode(HttpStatus.SC_OK)
				.body(matchesJsonSchemaInClasspath("AMPL/applData.json"));
	}

	// @Test
	// public void test2PutInputData() {
	// when().get("/state").then().statusCode(HttpStatus.SC_OK).assertThat().body(Matchers.is("IDLE"));
	//
	// given().contentType("application/json;
	// charset=UTF-16").body(data).when().post("/inputdata").then()
	// .statusCode(HttpStatus.SC_OK);
	//
	// when().get("/state").then().statusCode(HttpStatus.SC_OK).assertThat().body(Matchers.is("CHARGED_INPUTDATA"));
	//
	// given().contentType("application/json;
	// charset=UTF-16").body(Events.RESET, ObjectMapperType.JACKSON_2).when()
	// .post("/event").then().statusCode(HttpStatus.SC_OK).assertThat().body(Matchers.is("IDLE"));
	//
	// }

	@Test
	public void test3OptimizationAlgorithm() {
		if (get("/state").getBody().asString().equals("IDLE")) {

			given().contentType("application/json; charset=UTF-16").body(data).when().post("/inputdata").then()
					.statusCode(HttpStatus.SC_OK);

			when().get("/state").then().statusCode(HttpStatus.SC_OK).assertThat()
					.body(Matchers.is("CHARGED_INPUTDATA"));
		}

		given().contentType("application/json; charset=UTF-16").body(Events.TO_RUNNING_INIT, ObjectMapperType.JACKSON_2)
				.when().post("/event").then().statusCode(HttpStatus.SC_OK);

		String body = "RUNNING_INIT";
		while (body.equals("RUNNING_INIT")) {
			try {
				Thread.sleep(5000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			body = get("/state").getBody().asString();
		}

		when().get("/state").then().statusCode(HttpStatus.SC_OK).assertThat().body(Matchers.is("CHARGED_INITSOLUTION"));
	}

	// @BeforeTransaction
	// public void possiblyRecover() {
	// if (!setUp) {
	// setUp();
	// }
	// while (!get("/state").getBody().asString().equals("IDLE")) {
	//
	// given().contentType("application/json;
	// charset=UTF-16").body(Events.MIGRATE, ObjectMapperType.JACKSON_2)
	// .when().post("/event").then().statusCode(HttpStatus.SC_OK);
	// }
	// }

}
