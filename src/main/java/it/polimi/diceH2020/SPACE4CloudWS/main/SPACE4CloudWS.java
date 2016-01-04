package it.polimi.diceH2020.SPACE4CloudWS.main;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.orm.jpa.EntityScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableAsync;

@ComponentScan({"it.polimi.diceH2020.SPACE4CloudWS.*" })
@EntityScan("it.polimi.diceH2020.SPACE4CloudWS.model")
@EnableJpaRepositories("it.polimi.diceH2020.SPACE4CloudWS.repositories")
@SpringBootApplication
@EnableAsync
public class SPACE4CloudWS {

	
	public static void main(String[] args) {

		SpringApplication.run(SPACE4CloudWS.class, args);

	}

}