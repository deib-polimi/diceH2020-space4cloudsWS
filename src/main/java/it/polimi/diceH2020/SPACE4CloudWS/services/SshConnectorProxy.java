package it.polimi.diceH2020.SPACE4CloudWS.services;

import com.jcraft.jsch.JSchException;
import it.polimi.diceH2020.SPACE4CloudWS.connection.SshConnector;
import org.apache.log4j.Logger;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class SshConnectorProxy {

    private final Logger logger = Logger.getLogger(getClass());

    private final int retry = 5;
    private final long delay = 2000L; // [ms]
    private final double multiplier = 2.;

    private Map<Class<?>, SshConnector> connectorsMap;

    public SshConnectorProxy() {
        this.connectorsMap = new HashMap<>();
    }

    public void registerConnector(SshConnector connector, Class<?> aClass){
        connectorsMap.put(aClass, connector);
    }

    @Retryable(maxAttempts = retry, backoff = @Backoff(delay = delay, multiplier = multiplier, random = true))
    public void sendFile(String localFile, String remoteFile, Class<?> aClass) throws JSchException, IOException {
        logger.debug(String.format("attempt to send file: %s -> %s", localFile, remoteFile));
        connectorsMap.get(aClass).sendFile(localFile, remoteFile);
    }

    @Retryable(maxAttempts = retry, backoff = @Backoff(delay = delay, multiplier = multiplier, random = true))
    public List<String> exec(String command, Class<?> aClass) throws JSchException, IOException {
        logger.debug(String.format("attempt to execute command: %s", command));
        return connectorsMap.get(aClass).exec(command);
    }

    @Retryable(maxAttempts = retry, backoff = @Backoff(delay = delay, multiplier = multiplier, random = true))
    public void receiveFile(String localFile, String remoteFile, Class<?> aClass) throws JSchException, IOException {
        logger.debug(String.format("attempt to receive file: %s <- %s", localFile, remoteFile));
        connectorsMap.get(aClass).receiveFile(localFile, remoteFile);
    }

    @Retryable(maxAttempts = retry, backoff = @Backoff(delay = delay, multiplier = multiplier, random = true))
    public List<String> pwd(Class<?> aClass) throws JSchException, IOException {
        logger.debug("attempt to get working directory");
        return connectorsMap.get(aClass).pwd();
    }

}
