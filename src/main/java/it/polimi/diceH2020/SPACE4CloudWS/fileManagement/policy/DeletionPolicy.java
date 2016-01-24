package it.polimi.diceH2020.SPACE4CloudWS.fileManagement.policy;

import java.io.File;

public interface DeletionPolicy {
    /**
     * Delegate file deletion
     * @param file: the file to delete
     * @return {@code true} if the file was actually deleted
     */
    boolean delete(File file);
}
