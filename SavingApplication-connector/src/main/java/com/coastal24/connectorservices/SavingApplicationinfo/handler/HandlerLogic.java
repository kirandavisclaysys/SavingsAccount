package com.coastal24.connectorservices.SavingApplicationinfo.handler;

import freemarker.template.TemplateException;
import java.io.IOException;
import java.text.ParseException;
import java.util.Map;
import com.xtensifi.dspco.ConnectorMessage;

/**
 * Interface for the custom logic to generate a response
 */
@FunctionalInterface
public interface HandlerLogic {
    String generateResponse(final Map<String, String> params, String userId, ConnectorMessage message)
            throws IOException, TemplateException, ParseException;
}