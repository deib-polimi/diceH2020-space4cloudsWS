package it.polimi.diceH2020.SPACE4CloudWS.fileManagement.policy;

import java.io.File;

public class DeleteOnExit implements DeletionPolicy {
    @Override
    public boolean delete(File file) {
        file.deleteOnExit();
        return false;
    }

    @Override
    public void markForDeletion(File file) {
        file.deleteOnExit();
    }
}
