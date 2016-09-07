package it.polimi.diceH2020.SPACE4CloudWS.engines;

import javax.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import lombok.Setter;

/**
 * This factory has been created in order to handle the Admission Control case. 
 * For this case a different flow is applied (Matrix need to be created).
 * In order to preserve the same state Machine of the general case,
 * Matrix creation and reduction phases have to be integrated with the general flow.
 */
@Component
public class EngineFactory {
	
	@Autowired
    private ApplicationContext ctx;

    @Setter
    private EngineTypes type;

    @PostConstruct
    public void restoreDefaults() {
        type = EngineTypes.GENERAL;
    }

    public Engine create() throws RuntimeException {
        switch (type) {
            case AC:
                return ctx.getBean(EngineServiceWithACService.class);
            case GENERAL:
            default:
                return ctx.getBean(EngineService.class);
        }
    }
}
