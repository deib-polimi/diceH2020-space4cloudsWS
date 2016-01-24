package it.polimi.diceH2020.SPACE4CloudWS.fileManagement.policy;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.io.File;

@Component
@Profile("test")
public class DeleteOnExit implements DeletionPolicy {
    @Override
    public boolean delete(File file) {
        file.deleteOnExit();
        return false;
    }
}
