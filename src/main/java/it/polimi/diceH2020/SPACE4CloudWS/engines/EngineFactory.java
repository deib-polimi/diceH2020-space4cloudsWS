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

import lombok.Setter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import it.polimi.diceH2020.SPACE4CloudWS.core.EngineService;
import it.polimi.diceH2020.SPACE4CloudWS.core.EngineServiceWithACService;

import javax.annotation.PostConstruct;

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
