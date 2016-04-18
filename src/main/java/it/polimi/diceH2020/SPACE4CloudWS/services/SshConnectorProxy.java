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
    private Map<String, SshConnector> connectorsMap;

    public SshConnectorProxy() {
        this.connectorsMap = new HashMap<>();
    }

    public void setConnector(SshConnector connector, String name){
        connectorsMap.put(name, connector);
    }

    @Retryable(maxAttempts = retry, backoff = @Backoff(delay = delay, multiplier = multiplier, random = true))
    public void sendFile(String localFile, String remoteFile, String className) throws JSchException, IOException {
        logger.debug("attempt to send file");
        connectorsMap.get(className).sendFile(localFile, remoteFile);
    }

    @Retryable(maxAttempts = retry, backoff = @Backoff(delay = delay, multiplier = multiplier, random = true))
    public List<String> exec(String command, String className) throws JSchException, IOException {
        logger.debug("attempt to execute command");
        return connectorsMap.get(className).exec(command);
    }

    @Retryable(maxAttempts = retry, backoff = @Backoff(delay = delay, multiplier = multiplier, random = true))
    public void receiveFile(String localFile, String remoteFile, String className) throws JSchException, IOException {
        logger.debug("attempt to receive file");
        connectorsMap.get(className).receiveFile(localFile, remoteFile);
    }

    @Retryable(maxAttempts = retry, backoff = @Backoff(delay = delay, multiplier = multiplier, random = true))
    public List<String> pwd(String className) throws JSchException, IOException {
        logger.debug("attempt to get working directory");
        return connectorsMap.get(className).pwd();
    }

}
