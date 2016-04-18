package it.polimi.diceH2020.SPACE4CloudWS.services;

import com.jcraft.jsch.JSchException;
import it.polimi.diceH2020.SPACE4CloudWS.connection.SshConnector;
import it.polimi.diceH2020.SPACE4CloudWS.solvers.ConnectionSettings;
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

import javax.annotation.PostConstruct;

@Service
public class SshConnectorProxy {

    private final Logger logger = Logger.getLogger(this.getClass());

    private final int retry = 3;
    private final long delay = 200L; // [ms]
    private final long maxDelay = 1000L;
    
    @Setter
    @Getter
    private Map<String,SshConnector> connectorsMap;
    
    @Setter
    private SshConnector connector;
    
    public SshConnectorProxy() {
		this.connectorsMap = new HashMap<String,SshConnector>();
	}
    
    public void setConnector(SshConnector connector,String name){
    	connectorsMap.put(name, connector);
    }
    
    @Retryable(maxAttempts = retry, backoff = @Backoff(delay = delay, maxDelay = maxDelay))
    public void sendFile(String localFile, String remoteFile, String className) throws JSchException, IOException {
        logger.debug("attempt to send file");
        connectorsMap.get(className).sendFile(localFile, remoteFile);
    }

    @Retryable(maxAttempts = retry, backoff = @Backoff(delay = delay, maxDelay = maxDelay))
    public List<String> exec(String command, String className) throws JSchException, IOException {
        logger.debug("attempt to execute command");
        return connectorsMap.get(className).exec(command);
    }

    @Retryable(maxAttempts = retry, backoff = @Backoff(delay = delay, maxDelay = maxDelay))
    public void receiveFile(String localFile, String remoteFile, String className) throws JSchException, IOException {
        logger.debug("attempt to receive file");
        connectorsMap.get(className).receiveFile(localFile, remoteFile);
    }

    @Retryable(maxAttempts = retry, backoff = @Backoff(delay = delay, maxDelay = maxDelay))
    public List<String> pwd(String className) throws JSchException, IOException {
        logger.debug("attempt to get working directory");
        return connectorsMap.get(className).pwd();
    }

}
