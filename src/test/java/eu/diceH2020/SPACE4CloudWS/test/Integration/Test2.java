package eu.diceH2020.SPACE4CloudWS.test.Integration;

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

import eu.diceH2020.SPACE4CloudWS.model.Job;
import eu.diceH2020.SPACE4CloudWS.model.Provider;
import eu.diceH2020.SPACE4CloudWS.model.TypeVM;

@RunWith(SpringJUnit4ClassRunner.class)   // 1
@SpringApplicationConfiguration(classes = eu.diceH2020.SPACE4CloudWS.app.SPACE4CloudWS.class)   // 2
@WebAppConfiguration   // 3
@IntegrationTest("server.port:8080")   // 4
@ActiveProfiles("test")
public class Test2 {

	@Value("${local.server.port}")   // 6
    int port;

    @Before
    public void setUp() {
        RestAssured.port = port;
    }
    @Test
    public void testPutJob() {
        Job jb = new Job();
        jb.setIdjob(10); 
    	RestAssured.
    	given().
    	       contentType("application/json; charset=UTF-16").
    	       body(jb, ObjectMapperType.JACKSON_2).
    	when().
    	      post("/job").then().statusCode(HttpStatus.SC_OK).body("idjob", Matchers.is(10));
    }
    
    @Test
    public void testPutTypeVM() {
        
    	Provider provider = new Provider();
    	provider.setName("Amazon");
    	TypeVM typeVM = new TypeVM();
    	typeVM.setCore(2);
    	typeVM.setDeltabar(0.8);
    	typeVM.setProvider(provider);
    	typeVM.setRhobar(2.1);
    	typeVM.setSigmabar(1.3);
    	typeVM.setType("T1");
    	RestAssured.
    	given().
    	       contentType("application/json; charset=UTF-16").
    	       body(typeVM, ObjectMapperType.JACKSON_2).
    	when().
    	      post("/typeVM").then().statusCode(HttpStatus.SC_OK).body("type", Matchers.is("T1"));
    }
	
}
