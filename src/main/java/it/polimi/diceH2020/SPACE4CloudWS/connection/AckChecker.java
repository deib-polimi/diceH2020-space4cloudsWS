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
