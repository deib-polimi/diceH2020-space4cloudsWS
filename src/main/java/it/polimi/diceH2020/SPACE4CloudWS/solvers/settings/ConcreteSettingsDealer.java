/*
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
package it.polimi.diceH2020.SPACE4CloudWS.solvers.settings;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

@Component
public class ConcreteSettingsDealer implements SettingsDealer {

    @Autowired
    private ApplicationContext applicationContext;

    @Autowired
    private PerformanceSolverSettings performanceSolverSettings;

    @Override
    public ConnectionSettings getConnectionDefaults(Class<? extends ConnectionSettings> aClass) {
        final ConnectionSettings defaultSettings = applicationContext.getBean(aClass);
        return defaultSettings.shallowCopy();
    }

    @Override
    public PerformanceSolverSettings getPerformanceSolverDefaults() {
        return new PerformanceSolverSettings(performanceSolverSettings);
    }
}
