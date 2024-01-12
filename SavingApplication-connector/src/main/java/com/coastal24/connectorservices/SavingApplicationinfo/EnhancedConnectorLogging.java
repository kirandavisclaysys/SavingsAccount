package com.coastal24.connectorservices.SavingApplicationinfo;

import com.xtensifi.connectorservices.common.logging.ConnectorLogging;
import com.xtensifi.cufx.CustomData;
import com.xtensifi.cufx.ValuePair;
import com.xtensifi.dspco.ConnectorMessage;
import lombok.extern.slf4j.Slf4j;
import java.io.InputStream;
import java.util.List;
import java.util.Properties;

/**
 * A simple wrapper around ConnectorLogging that looks for global params and application.properties settings:
 *  eclEnabled = true in the global params will turn logging on without needing to redeploy connector
 *  connector.local=TRUE will send messages to slf4j otherwise they will go to the original ConnectorLogging
 */
@Slf4j
public class EnhancedConnectorLogging extends ConnectorLogging {

    private final boolean connectorLocal;

    /**
     *
     */
    public EnhancedConnectorLogging() {
        super();
        boolean connectorLocalFlag;
        try (InputStream stream = getClass().getClassLoader().getResourceAsStream("application.properties")) {
            Properties properties = new Properties();
            properties.load(stream);

            connectorLocalFlag = properties.getProperty("connector.local", "FALSE").equalsIgnoreCase("TRUE");
        } catch (Exception ex) {
            connectorLocalFlag = false;
        }
        connectorLocal = connectorLocalFlag;
    }

    /** This method enables the connector logging to be turned on or off with a global connector param, 'eclEnabled'
     * eclEnabled = true --> logging is turned on.  eclEnabled = false (or any other value) --> logging is turned off.
     * If the value pair is missing, logging is turned on by default
     * @param connectorMessage
     */
    public boolean isEclEnabled(ConnectorMessage connectorMessage){
        Boolean eclEnabled = false;
        try {
            CustomData params = connectorMessage.getConnectorParametersResponse().getParameters();

            List<ValuePair> list = params.getValuePair();
            ValuePair eclEnabledPair = list.stream().filter(val -> val.getName().equals("eclEnabled"))
                    .findFirst()
                    .orElse(null);
            if (eclEnabledPair != null) {
                eclEnabled = eclEnabledPair.getValue().equalsIgnoreCase("true");
            }
        }
        catch(Exception e){
            eclEnabled = true;
        }
        return eclEnabled;

    }

  /** This method enables the connector logging to be turned on or off with a global connector param for all level of logging, 'isDebugMode '
     * isDebugMode  = true --> logging is turned on for debug .  isDebugMode  = false (or any other value) --> logging is turned off.
     * If the value pair is missing, logging is turned on by default
     * @param connectorMessage
     */
    public boolean isDebugMode(ConnectorMessage connectorMessage){
        Boolean debugMode = false;
        try {
            CustomData params = connectorMessage.getConnectorParametersResponse().getParameters();

            List<ValuePair> list = params.getValuePair();
            ValuePair debugModePair = list.stream().filter(val -> val.getName().equals("isDebugMode"))
                    .findFirst()
                    .orElse(null);
            if (debugModePair != null) {
                debugMode = debugModePair.getValue().equalsIgnoreCase("true");
            }
        }
        catch(Exception e){
            debugMode = true;
        }
        return debugMode;

    }

    /** This method enables the connector logging to be turned on or off with a global connector param, 'isErrorMode '
     * isErrorMode  = true --> logging is turned on for error .  isErrorMode  = false (or any other value) --> logging is turned off.
     * If the value pair is missing, logging is turned on by default
     * @param connectorMessage
     */
    public boolean isErrorMode(ConnectorMessage connectorMessage){
        Boolean errorMode = false;
        try {
            CustomData params = connectorMessage.getConnectorParametersResponse().getParameters();

            List<ValuePair> list = params.getValuePair();
            ValuePair errorModePair = list.stream().filter(val -> val.getName().equals("isErrorMode"))
                    .findFirst()
                    .orElse(null);
            if (errorModePair != null) {
                errorMode = errorModePair.getValue().equalsIgnoreCase("true");
            }
        }
        catch(Exception e){
            errorMode = true;
        }
        return errorMode;

    }


     /** This method enables the connector logging to be turned on or off with a global connector param, 'isFatalMode '
     * isFatalMode  = true --> logging is turned on for fatal logs .  isFatalMode  = false (or any other value) --> logging is turned off.
     * If the value pair is missing, logging is turned on by default
     * @param connectorMessage
     */
    public boolean isFatalMode(ConnectorMessage connectorMessage){
        Boolean fatalMode = false;
        try {
            CustomData params = connectorMessage.getConnectorParametersResponse().getParameters();

            List<ValuePair> list = params.getValuePair();
            ValuePair fatalModePair = list.stream().filter(val -> val.getName().equals("isFatalMode"))
                    .findFirst()
                    .orElse(null);
            if (fatalModePair != null) {
                fatalMode = fatalModePair.getValue().equalsIgnoreCase("true");
            }
        }
        catch(Exception e){
            fatalMode = true;
        }
        return fatalMode;

    }

    /** This method enables the connector logging to be turned on or off with a global connector param, 'isInfoMode '
     * isInfoMode  = true --> logging is turned on for info logs .  isInfoMode  = false (or any other value) --> logging is turned off.
     * If the value pair is missing, logging is turned on by default
     * @param connectorMessage
     */
    public boolean isInfoMode(ConnectorMessage connectorMessage){
        Boolean infoMode = false;
        try {
            CustomData params = connectorMessage.getConnectorParametersResponse().getParameters();

            List<ValuePair> list = params.getValuePair();
            ValuePair infoModePair = list.stream().filter(val -> val.getName().equals("isInfoMode"))
                    .findFirst()
                    .orElse(null);
            if (infoModePair != null) {
                infoMode = infoModePair.getValue().equalsIgnoreCase("true");
            }
        }
        catch(Exception e){
            infoMode = true;
        }
        return infoMode;

    }

     /** This method enables the connector logging to be turned on or off with a global connector param, 'isTraceMode '
     * isTraceMode  = true --> logging is turned on for trace logs .  isTraceMode  = false (or any other value) --> logging is turned off.
     * If the value pair is missing, logging is turned on by default
     * @param connectorMessage
     */
    public boolean isTraceMode(ConnectorMessage connectorMessage){
        Boolean traceMode = false;
        try {
            CustomData params = connectorMessage.getConnectorParametersResponse().getParameters();

            List<ValuePair> list = params.getValuePair();
            ValuePair traceModePair = list.stream().filter(val -> val.getName().equals("isTraceMode"))
                    .findFirst()
                    .orElse(null);
            if (traceModePair != null) {
                traceMode = traceModePair.getValue().equalsIgnoreCase("true");
            }
        }
        catch(Exception e){
            traceMode = true;
        }
        return traceMode;

    }


     /** This method enables the connector logging to be turned on or off with a global connector param, 'isWarnMode '
     * isWarnMode  = true --> logging is turned on for warn logs .  isWarnMode  = false (or any other value) --> logging is turned off.
     * If the value pair is missing, logging is turned on by default
     * @param connectorMessage
     */
    public boolean isWarnMode(ConnectorMessage connectorMessage){
        Boolean warnMode = false;
        try {
            CustomData params = connectorMessage.getConnectorParametersResponse().getParameters();

            List<ValuePair> list = params.getValuePair();
            ValuePair warnModePair = list.stream().filter(val -> val.getName().equals("isWarnMode"))
                    .findFirst()
                    .orElse(null);
            if (warnModePair != null) {
                warnMode = warnModePair.getValue().equalsIgnoreCase("true");
            }
        }
        catch(Exception e){
            warnMode = true;
        }
        return warnMode;

    }

    @Override
    public void debug(ConnectorMessage connectorMessage, String message) {
        if (isEclEnabled(connectorMessage) && isDebugMode(connectorMessage)) {
            if (!connectorLocal) {
                super.debug(connectorMessage, "DEBUG: " + message);
            } else {
                log.debug(message);
            }
        }
    }

    /**
     *
     * @param connectorMessage
     * @param message
     */
    @Override
    public void error(ConnectorMessage connectorMessage, String message) {
        if (isEclEnabled(connectorMessage) && isErrorMode(connectorMessage)) {
            if (!connectorLocal) {
                super.error(connectorMessage, message);
            } else {
                log.error(message);
            }
        }
    }

    /**
     *
     * @param connectorMessage
     * @param message
     */
    @Override
    public void fatal(ConnectorMessage connectorMessage, String message) {
        if (isEclEnabled(connectorMessage) && isFatalMode(connectorMessage)) {
            if (!connectorLocal) {
                super.fatal(connectorMessage, message);
            } else {
                log.error(message);
            }
        }
    }

    /**
     *
     * @param connectorMessage
     * @param message
     */
    @Override
    public void info(ConnectorMessage connectorMessage, String message) {
        if (isEclEnabled(connectorMessage) && isInfoMode(connectorMessage)) {
            if (!connectorLocal) {
                super.info(connectorMessage, message);
            } else {
                log.info(message);
            }
        }
    }

    /**
     *
     * @param connectorMessage
     * @param message
     */
    @Override
    public void trace(ConnectorMessage connectorMessage, String message) {
        if (isEclEnabled(connectorMessage) && isTraceMode(connectorMessage)) {
            if (!connectorLocal) {
                super.trace(connectorMessage, message);
            } else {
                log.trace(message);
            }
        }
    }

    /**
     *
     * @param connectorMessage
     * @param message
     */
    @Override
    public void warn(ConnectorMessage connectorMessage, String message) {
        if (isEclEnabled(connectorMessage) && isWarnMode(connectorMessage)) {
            if (!connectorLocal) {
                super.warn(connectorMessage, message);
            } else {
                log.warn(message);
            }
        }
    }
}