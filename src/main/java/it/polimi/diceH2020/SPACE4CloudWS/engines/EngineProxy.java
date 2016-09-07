package it.polimi.diceH2020.SPACE4CloudWS.engines;

import javax.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import lombok.Getter;

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
