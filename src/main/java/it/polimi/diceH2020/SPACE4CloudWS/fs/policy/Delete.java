package it.polimi.diceH2020.SPACE4CloudWS.fs.policy;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.io.File;

@Component
@Profile("default")
public class Delete implements DeletionPolicy {
    @Override
    public boolean delete(File file) {
        return file.delete();
    }
}
