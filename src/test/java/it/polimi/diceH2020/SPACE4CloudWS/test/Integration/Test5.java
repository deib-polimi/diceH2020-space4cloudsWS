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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.jayway.restassured.internal.mapper.ObjectMapperType;
import com.jayway.restassured.module.mockmvc.RestAssuredMockMvc;
import it.polimi.diceH2020.SPACE4Cloud.shared.inputDataMultiProvider.InstanceDataMultiProvider;
import it.polimi.diceH2020.SPACE4Cloud.shared.inputDataMultiProvider.TypeVMJobClassKey;
import it.polimi.diceH2020.SPACE4Cloud.shared.solution.Solution;
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

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

import static com.jayway.restassured.module.mockmvc.RestAssuredMockMvc.*;

@RunWith(SpringJUnit4ClassRunner.class) // 1
@SpringApplicationConfiguration(classes = it.polimi.diceH2020.SPACE4CloudWS.main.SPACE4CloudWS.class) // 2
@WebAppConfiguration // 3
@ActiveProfiles("test")
@Transactional
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class Test5 {

    @Autowired
    WebApplicationContext wac;
    MockMvc mockMvc;
    private InstanceDataMultiProvider data;
    private ObjectMapper mapper;
    private Solution solution;

    @Before
    public void setUp() throws IOException {
        // RestAssured.port = port;
        mockMvc = MockMvcBuilders.webAppContextSetup(wac).build();
        RestAssuredMockMvc.mockMvc(mockMvc);

        mapper = new ObjectMapper().registerModule(new Jdk8Module());
        SimpleModule module = new SimpleModule();
        module.addKeyDeserializer(TypeVMJobClassKey.class, TypeVMJobClassKey.getDeserializer());
        mapper.registerModule(module);
        String serialized = new String(Files.readAllBytes(Paths.get("src/test/resources/myJson.json")));
//		System.out.println(serialized);
        data = mapper.readValue(serialized, InstanceDataMultiProvider.class);
        System.out.println(data.toString());
        serialized = new String(Files.readAllBytes(Paths.get("src/test/resources/sol.json")));
//		System.out.println(serialized);
        solution = mapper.readValue(serialized, Solution.class);
        System.out.println(data.toString());
    }


//	@Test
//	public void test0PutInputData() throws IOException {
//		when().get("/state").then().statusCode(HttpStatus.SC_OK).assertThat().body(Matchers.is("IDLE"));
//
//		given().contentType("application/json; charset=UTF-16").body(data).when().post("/inputdata").then()
//				.statusCode(HttpStatus.SC_OK);
//
//		when().get("/state").then().statusCode(HttpStatus.SC_OK).assertThat().body(Matchers.is("CHARGED_INPUTDATA"));
//
//		//InstanceData data = post("/debug/event").body().as(InstanceData.class);
//		
//		given().contentType("application/json; charset=UTF-16").body(Events.TO_RUNNING_INIT, ObjectMapperType.JACKSON_2).when()
//				.post("/event").then().statusCode(HttpStatus.SC_OK).assertThat().body(Matchers.is("RUNNING_INIT"));
//		String body = "RUNNING_INIT";
//		while (body.equals("RUNNING_INIT")) {
//			try {
//				Thread.sleep(5000);
//			} catch (InterruptedException e) {
//				e.printStackTrace();
//			}
//			body = get("/state").getBody().asString();
//		}
//		
//		when().get("/state").then().statusCode(HttpStatus.SC_OK).assertThat().body(Matchers.is("CHARGED_INITSOLUTION"));	
//
//		Solution sol = get("/solution").body().as(Solution.class);
//		
//		String serialized = mapper.writeValueAsString(sol);
//		System.out.println(serialized);
//		Files.write(Paths.get("src/test/resources/sol.json"), serialized.getBytes());
//
//	}

    @Test
    public void test1() {
        if (get("/state").getBody().asString().equals("IDLE")) {

            given().contentType("application/json; charset=UTF-16").body(solution).when().post("/solution").then()
                    .statusCode(HttpStatus.SC_OK);

            when().get("/state").then().statusCode(HttpStatus.SC_OK).assertThat().body(Matchers.is("CHARGED_INITSOLUTION"));
        }
    }

    @Test
    public void test2() throws IOException {
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

        Solution sol = get("/solution").body().as(Solution.class);

        String serialized = mapper.writeValueAsString(sol);
        System.out.println(serialized);
        Files.write(Paths.get("src/test/resources/solFinal.json"), serialized.getBytes());
    }

}
