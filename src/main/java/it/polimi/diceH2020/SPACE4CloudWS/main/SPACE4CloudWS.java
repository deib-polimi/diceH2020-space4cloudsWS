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
package it.polimi.diceH2020.SPACE4CloudWS.main;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.orm.jpa.EntityScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.scheduling.annotation.EnableAsync;

@ComponentScan({"it.polimi.diceH2020.SPACE4CloudWS.*" })
@EntityScan("it.polimi.diceH2020.SPACE4CloudWS.model")
@EnableJpaRepositories("it.polimi.diceH2020.SPACE4CloudWS.repositories")
@SpringBootApplication
@EnableAsync
@EnableRetry
public class SPACE4CloudWS {

	public static void main(String[] args) {
		SpringApplication.run(SPACE4CloudWS.class, args);
	}

}
