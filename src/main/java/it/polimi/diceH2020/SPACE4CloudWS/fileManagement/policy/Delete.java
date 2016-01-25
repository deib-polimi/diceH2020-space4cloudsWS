package it.polimi.diceH2020.SPACE4CloudWS.fileManagement.policy;

import java.io.File;

public class Delete implements DeletionPolicy {
    @Override
    public boolean delete(File file) {
        return file.delete();
    }

    @Override
    public void markForDeletion(File file) {
        file.deleteOnExit();
    }
}
