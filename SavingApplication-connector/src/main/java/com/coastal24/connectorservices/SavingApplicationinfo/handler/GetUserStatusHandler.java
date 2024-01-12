package com.coastal24.connectorservices.SavingApplicationinfo.handler;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.coastal24.connectorservices.SavingApplicationinfo.Config;
import com.coastal24.connectorservices.SavingApplicationinfo.adapter.LocalStorageAdapter;
import com.fasterxml.jackson.databind.JsonNode;
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
 * Handler to get user status from Db if exist.
 */
@Slf4j
public class GetUserStatusHandler extends HandlerBase {

    public static ObjectMapper mapper = Config.getObjectMapper();
    LocalStorageAdapter localStorageAdapter = new LocalStorageAdapter();
    String appId;

    @Override
    public String generateResponse(Map<String, String> params, @NonNull String userId, ConnectorMessage message)
            throws IOException, TemplateException {
        clog.warn(message, "GetUserStatusHandler start");

        String loanType = params.get("loanType");

        ObjectNode statusObject = localStorageAdapter.getLoanApplicationStatus(userId, params, message);

        ObjectNode UserStatusHandler = mapper.createObjectNode();
        if (statusObject != null) {
            String status = statusObject.get("loanStatus") != null ? statusObject.get("loanStatus").asText() : null;

            if (status == "null" || status == "") {
                try {
                    if (statusObject.get("appId") != null)
                        appId = statusObject.get("appId").asText();
                    // get the status from local db api and fill out the tile data accordingly.
                    if (appId != null) {
                        ResponseEntity<String> response = localStorageAdapter.SavingDecisionXAApplicationRequest("{}",
                                appId,
                                params, HttpMethod.GET, false);
                        ObjectMapper mapper = new ObjectMapper();
                        JsonNode dBResult = mapper.readTree(response.getBody());
                        if (dBResult == null || dBResult.isNull()) {
                            UserStatusHandler.put("appId", "");
                            UserStatusHandler.put("loanStatus", "");
                        } else {
                            UserStatusHandler = (ObjectNode) dBResult;
                            UserStatusHandler.put("appId", appId);
                        }
                    } else {
                        UserStatusHandler.put("appId", "");
                        UserStatusHandler.put("loanStatus", "");
                    }

                    UserStatusHandler.put("success", "True");
                } catch (RestClientException restEx) {

                    log.info("Could not POST application.");
                } catch (Exception ex) {
                    log.error("Exception {}", ex);
                }

            } else {
                String date = statusObject.get("loanDate").asText();
                Date loanDate;
                Date currentDate = new Date();
                try {
                    loanDate = new SimpleDateFormat("yyyy-MM-dd").parse(date);
                } catch (Exception e) {
                    throw new IOException("Date parse error");
                }
                if (loanDate != null) {
                    long time_difference = currentDate.getTime() - loanDate.getTime();
                    long days_difference = (time_difference / (1000 * 60 * 60 * 24)) % 365;
                    int declinedReApplyDays = Integer
                            .parseInt(params.get("declinedReApplyDays"));
                    int reApplyDays = Integer.parseInt(params.get("reApplyDays"));
                    if (days_difference >= reApplyDays) {
                        UserStatusHandler.put("loanStatus", "");
                        UserStatusHandler.put("appId", "");
                    } else if (status.equalsIgnoreCase("QUALIFIED")
                            || status.equalsIgnoreCase("REFERRED")) {
                        UserStatusHandler.put("loanStatus", status);
                        UserStatusHandler.put("appId", statusObject.get("appId").asText());
                    } else if (status.equalsIgnoreCase("INSTANT_DECLINED")) {
                        UserStatusHandler.put("loanStatus", "DECLINED");
                        if (days_difference >= declinedReApplyDays)
                            UserStatusHandler.put("appId", "");
                        else
                            UserStatusHandler.put("appId", statusObject.get("appId").asText());
                    } else {
                        UserStatusHandler.put("loanStatus", "PENDING");
                        UserStatusHandler.put("appId", statusObject.get("appId").asText());
                    }
                }
                UserStatusHandler.put("success", "True");
            }

        } else {
            UserStatusHandler.put("loanStatus", "");
            UserStatusHandler.put("appId", "");
            UserStatusHandler.put("success", "True");
        }

        return mapper.writeValueAsString(UserStatusHandler);
    }
}
