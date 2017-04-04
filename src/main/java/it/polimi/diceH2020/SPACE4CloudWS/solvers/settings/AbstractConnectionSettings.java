/*
Copyright 2016-2017 Eugenio Gianniti
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
package it.polimi.diceH2020.SPACE4CloudWS.solvers.settings;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public abstract class AbstractConnectionSettings implements ConnectionSettings {
    private String address;

    private Integer port = 22; //default value

    private String username;

    private String password;

    private String privateKeyFile;

    private String knownHosts;

    private boolean forceClean;

    private String remoteWorkDir;

    private double accuracy = 0.1; //default value

    private Integer maxDuration = Integer.MIN_VALUE;

    private String solverPath;

    private boolean cleanRemote;

    protected AbstractConnectionSettings(AbstractConnectionSettings that) {
        super();
        address = that.address;
        port = that.port;
        username = that.username;
        password = that.password;
        privateKeyFile = that.privateKeyFile;
        knownHosts = that.knownHosts;
        forceClean = that.forceClean;
        remoteWorkDir = that.remoteWorkDir;
        accuracy = that.accuracy;
        maxDuration = that.maxDuration;
        solverPath = that.solverPath;
        cleanRemote = that.cleanRemote;
    }
}
