/*
Copyright 2016 Jacopo Rigoli

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
package it.polimi.diceH2020.SPACE4CloudWS.engines;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;

@Service
public class EngineProxy {

	@Autowired
	private EngineFactory engineFactory;

	@Getter
	private Engine engine;

	@PostConstruct
	private void setEngine() {
		engine = engineFactory.create();
	}

	public void restoreDefaults() {
		engineFactory.restoreDefaults();
		initializeEngine();
	}

	private void initializeEngine() {
		engine = engineFactory.create();
		engine.restoreDefaults();
	}

	public Engine refreshEngine(EngineTypes type){
		engineFactory.setType(type);
		engine = engineFactory.create();
		return engine;
	}
}
