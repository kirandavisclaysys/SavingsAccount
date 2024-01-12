package com.coastal24.connectorservices.SavingApplicationinfo.handler;

import com.coastal24.connectorservices.SavingApplicationinfo.Config;
import com.coastal24.connectorservices.SavingApplicationinfo.EnhancedConnectorLogging;
import com.coastal24.connectorservices.SavingApplicationinfo.JsonHelper;
import com.xtensifi.dspco.ConnectorMessage;
import freemarker.template.Configuration;
import freemarker.template.TemplateException;
import org.springframework.web.client.RestTemplate;
import java.io.IOException;
import java.util.Map;

public abstract class HandlerBase implements HandlerLogic{

    EnhancedConnectorLogging clog = new EnhancedConnectorLogging();

    static Configuration freemarkerCfg = Config.getFreemarkerConfiguration();
    static JsonHelper jsonHelper = new JsonHelper();
   // static SubmitApplicationHandler submitApplicationHandler = new SubmitApplicationHandler();
    static RestTemplate restTemplate = Config.getRestTemplate();

    //public abstract String generateResponse(Map<String, String> params, String userId) throws IOException, TemplateException;
    public abstract String generateResponse(Map<String, String> params, String userId,ConnectorMessage message) throws IOException, TemplateException;

}