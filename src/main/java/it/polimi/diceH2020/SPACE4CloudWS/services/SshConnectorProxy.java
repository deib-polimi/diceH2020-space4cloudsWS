package it.polimi.diceH2020.SPACE4CloudWS.services;

import com.jcraft.jsch.JSchException;
import it.polimi.diceH2020.SPACE4CloudWS.connection.SshConnector;
import lombok.Getter;
import lombok.Setter;
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

    private final Logger logger = Logger.getLogger(this.getClass());

    private final int retry = 3;
    private final long delay = 500L; // [ms]
    private final double multiplier = 2.;

    @Setter
    @Getter
    private Map<Class<?>, SshConnector> connectorsMap;

    public SshConnectorProxy() {
        this.connectorsMap = new HashMap<>();
    }

    public void registerConnector(SshConnector connector, Class<?> aClass){
        connectorsMap.put(aClass, connector);
    }

    @Retryable(maxAttempts = retry, backoff = @Backoff(delay = delay, multiplier = multiplier, random = true))
    public void sendFile(String localFile, String remoteFile, Class<?> aClass) throws JSchException, IOException {
        logger.debug("attempt to send file");
        connectorsMap.get(aClass).sendFile(localFile, remoteFile);
    }

    @Retryable(maxAttempts = retry, backoff = @Backoff(delay = delay, multiplier = multiplier, random = true))
    public List<String> exec(String command, Class<?> aClass) throws JSchException, IOException {
        logger.debug("attempt to execute command");
        return connectorsMap.get(aClass).exec(command);
    }

    @Retryable(maxAttempts = retry, backoff = @Backoff(delay = delay, multiplier = multiplier, random = true))
    public void receiveFile(String localFile, String remoteFile, Class<?> aClass) throws JSchException, IOException {
        logger.debug("attempt to receive file");
        connectorsMap.get(aClass).receiveFile(localFile, remoteFile);
    }

    @Retryable(maxAttempts = retry, backoff = @Backoff(delay = delay, multiplier = multiplier, random = true))
    public List<String> pwd(Class<?> aClass) throws JSchException, IOException {
        logger.debug("attempt to get working directory");
        return connectorsMap.get(aClass).pwd();
    }

}
