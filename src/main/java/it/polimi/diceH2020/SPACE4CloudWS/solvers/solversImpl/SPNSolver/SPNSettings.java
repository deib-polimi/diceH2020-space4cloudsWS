/*
Copyright 2016 Michele Ciavotta
Copyright 2016 Eugenio Gianniti

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
package it.polimi.diceH2020.SPACE4CloudWS.solvers.solversImpl.SPNSolver;

import it.polimi.diceH2020.SPACE4Cloud.shared.settings.Technology;
import it.polimi.diceH2020.SPACE4CloudWS.solvers.settings.AbstractConnectionSettings;
import it.polimi.diceH2020.SPACE4CloudWS.solvers.settings.ConnectionSettings;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Configuration class for SPN solver/server
 */
@Component
@Getter
@Setter
@ConfigurationProperties(prefix = "SPN")
final class SPNSettings extends AbstractConnectionSettings {

    private SPNConfidence confidence;

    private SPNSettings(SPNSettings that) {
        super(that);
        confidence = that.confidence;
    }

    // Used by Spring Boot
    @SuppressWarnings("unused")
    SPNSettings() {
        super();
    }

    @Override
    public ConnectionSettings shallowCopy() {
        return new SPNSettings(this);
    }
}
