package it.polimi.diceH2020.SPACE4CloudWS.services;

import com.jcraft.jsch.JSchException;
import it.polimi.diceH2020.SPACE4CloudWS.connection.SshConnector;
import lombok.Setter;
import org.apache.log4j.Logger;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.List;

@Service
public class SshConnectorProxy {

    private final Logger logger = Logger.getLogger(this.getClass());

    private final int retry = 3;
    private final long delay = 200L; // [ms]
    private final long maxDelay = 1000L;

    @Setter
    private SshConnector connector;

    @Retryable(maxAttempts = retry, backoff = @Backoff(delay = delay, maxDelay = maxDelay))
    public void sendFile(String localFile, String remoteFile) throws JSchException, IOException {
        logger.debug("attempt to send file");
        connector.sendFile(localFile, remoteFile);
    }

    @Retryable(maxAttempts = retry, backoff = @Backoff(delay = delay, maxDelay = maxDelay))
    public List<String> exec(String command) throws JSchException, IOException {
        logger.debug("attempt to execute command");
        return connector.exec(command);
    }

    @Retryable(maxAttempts = retry, backoff = @Backoff(delay = delay, maxDelay = maxDelay))
    public void receiveFile(String localFile, String remoteFile) throws JSchException, IOException {
        logger.debug("attempt to receive file");
        connector.receiveFile(localFile, remoteFile);
    }

    @Retryable(maxAttempts = retry, backoff = @Backoff(delay = delay, maxDelay = maxDelay))
    public List<String> pwd() throws JSchException, IOException {
        logger.debug("attempt to get working directory");
        return connector.pwd();
    }

}
