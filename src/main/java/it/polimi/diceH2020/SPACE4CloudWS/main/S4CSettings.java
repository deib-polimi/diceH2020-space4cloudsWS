package it.polimi.diceH2020.SPACE4CloudWS.main;


import javax.validation.constraints.Min;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import lombok.Data;
@Component
@ConfigurationProperties(prefix="s4c")
@Data
public class S4CSettings {
	private boolean parallel= false;
	@Min(1)
	private int availableCores = 1; //for fine Grained Optimization
}
