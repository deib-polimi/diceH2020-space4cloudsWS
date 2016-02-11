package it.polimi.diceH2020.SPACE4CloudWS.fileManagement;

import java.io.IOException;

/**
 * Created by ciavotta on 11/02/16.
 */
public interface FileBuilder {
    FileBuilder setNumberOfReduceTasks(int numberOfReduceTasks);

    FileBuilder setNumberOfMapTasks(int numberOfMapTasks);

    FileBuilder setConcurrency(int concurrency);

    String build() throws IOException;
}
