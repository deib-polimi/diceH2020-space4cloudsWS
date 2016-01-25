package it.polimi.diceH2020.SPACE4CloudWS.fileManagement.policy;

import java.io.File;

public class KeepFiles implements DeletionPolicy {
    @Override
    public boolean delete(File file) {
        return false;
    }

    @Override
    public void markForDeletion(File file) {
        // This is a no-op for choice
    }
}
