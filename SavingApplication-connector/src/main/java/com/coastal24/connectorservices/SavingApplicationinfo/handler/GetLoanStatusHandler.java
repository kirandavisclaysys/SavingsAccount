package com.coastal24.connectorservices.SavingApplicationinfo.handler;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import com.amazonaws.Response;
import com.coastal24.connectorservices.SavingApplicationinfo.Config;
import com.coastal24.connectorservices.SavingApplicationinfo.EnhancedConnectorLogging;
import com.coastal24.connectorservices.SavingApplicationinfo.adapter.LocalStorageAdapter;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.xtensifi.dspco.ConnectorMessage;

import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestClientException;
import freemarker.template.TemplateException;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

/**
 * Handler for retrieving whole details from DB.
 */
@Slf4j

public class GetLoanStatusHandler extends HandlerBase {
    public static ObjectMapper mapper = Config.getObjectMapper();
    LocalStorageAdapter localStorageAdapter = new LocalStorageAdapter();

    // Make synchronous
    @Override
    public String generateResponse(Map<String, String> params, @NonNull String userId, ConnectorMessage message)
            throws IOException, TemplateException {
        // ResponseEntity<String> response =
        // localStorageAdapter.LoanApplicationRequest("{}", appId,
        // params, HttpMethod.GET);
        String loanUId = params.get("loanUId");
        ResponseEntity<String> statusResponse = localStorageAdapter.SavingDecisionXAApplicationRequest("{}", loanUId,
                params,
                HttpMethod.GET, false);
        ObjectNode statusObject = (ObjectNode) mapper.readTree(statusResponse.getBody());

        // ObjectNode statusObject = localStorageAdapter.LoanApplicationRequest("{}",
        // userId, params, HttpMethod.GET, appId);
        clog.warn(message, "LoanUId" + loanUId + "Loan status Data: " + statusObject.toString());
        ObjectNode UserStatusHandler = mapper.createObjectNode();

        if (statusObject != null) {
            // move mapping logic here
            String status = statusObject.get("loanStatus").asText();
            if (status != "null" && status != "") {

                UserStatusHandler.put("status", status);
                UserStatusHandler.put("applicationNumber", statusObject.get("loanNumber").asText());
                UserStatusHandler.put("xpressAppid", statusObject.get("loanId").asText());
                UserStatusHandler.put("success", "true");
            } else {

                UserStatusHandler.put("success", "false");
            }
        }
        return mapper.writeValueAsString(UserStatusHandler);
    }
}