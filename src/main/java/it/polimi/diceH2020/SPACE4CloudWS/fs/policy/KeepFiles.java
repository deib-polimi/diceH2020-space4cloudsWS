package it.polimi.diceH2020.SPACE4CloudWS.fs.policy;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.io.File;

@Component
@Profile("dev")
public class KeepFiles implements DeletionPolicy {
    @Override
    public boolean delete(File file) {
        return false;
    }
}
