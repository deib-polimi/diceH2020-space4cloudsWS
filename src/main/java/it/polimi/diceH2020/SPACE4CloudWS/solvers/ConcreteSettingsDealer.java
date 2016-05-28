package it.polimi.diceH2020.SPACE4CloudWS.solvers;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Map;

@Component
public class ConcreteSettingsDealer implements SettingsDealer {

    private final Logger logger = Logger.getLogger(getClass());

    @Autowired
    private ApplicationContext applicationContext;

    @Autowired
    private SolverSettings solverSettings;

    private Map<Class<? extends ConnectionSettings>, ConnectionSettings> connectionSettingsMap = new HashMap<>();

    @Override
    public ConnectionSettings getConnectionDefaults(Class<? extends ConnectionSettings> aClass) {
        ConnectionSettings defaultSettings;
        if (! connectionSettingsMap.containsKey(aClass)) {
            defaultSettings = applicationContext.getBean(aClass);
            connectionSettingsMap.put(aClass, defaultSettings);
        } else {
            defaultSettings = connectionSettingsMap.get(aClass);
        }
        try {
            final Constructor<? extends ConnectionSettings> constructor = aClass.getConstructor(aClass);
            return constructor.newInstance(defaultSettings);
        } catch (NoSuchMethodException e) {
            final String errorMessage = String.format(
                    "ConnectionSettings subclass <%s> does not provide copy constructor",
                    aClass.getCanonicalName());
            logger.error(errorMessage, e);
            throw new AssertionError(errorMessage, e);
        } catch (InstantiationException|IllegalAccessException|InvocationTargetException e) {
            final String errorMessage = String.format(
                    "Error when copy constructing ConnectionSettings subclass <%s>",
                    aClass.getCanonicalName());
            logger.error(errorMessage, e);
            throw new AssertionError(errorMessage, e);
        }
    }

    @Override
    public SolverSettings getSolverDefaults() {
        return new SolverSettings(solverSettings);
    }
}
