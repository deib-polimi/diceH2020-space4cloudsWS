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
package it.polimi.diceH2020.SPACE4CloudWS.connection;

import java.io.IOException;
import java.io.InputStream;

class AckChecker {

    static int checkAck(InputStream in) throws IOException {
        int b = in.read();
        // b may be 0 for success,
        // 1 for error,
        // 2 for fatal error,
        // -1

        if (b == 1 || b == 2) {
            StringBuilder builder = new StringBuilder();
            int c;
            do {
                c = in.read();
                builder.append((char) c);
            } while (c != '\n');
            System.err.print(builder.toString());
        }

        return b;
    }

}
