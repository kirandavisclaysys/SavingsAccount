package com.coastal24.connectorservices.SavingApplicationinfo.adapter;

import java.io.IOException;
import java.util.Date;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import com.coastal24.connectorservices.SavingApplicationinfo.Config;
import com.coastal24.connectorservices.SavingApplicationinfo.EnhancedConnectorLogging;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.xtensifi.dspco.ConnectorMessage;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;
import lombok.NonNull;
import org.json.JSONArray;

public class LocalStorageAdapter {

    private EnhancedConnectorLogging clog = new EnhancedConnectorLogging();
    private EnhancedConnectorLogging log = new EnhancedConnectorLogging();

    /**
     * Make a request to the applications URL
     * 
     * @param body
     * @param userId
     * @param params
     * @param method
     * @return
     */

    public ResponseEntity<String> SavingDecisionXAApplicationRequest(String body, @NonNull String userId,
            @NonNull Map<String, String> params, @NonNull HttpMethod method, Boolean getloanStatus) {

        String storageUrl = params.get("localStorageUrl");
        String apiKey = params.get("localStorageApiKey");
        String savingDecisionXAUrlTemplate;
        // String loanType = params.get("loanType");
        if (method == HttpMethod.POST) {
            savingDecisionXAUrlTemplate = Config.savingDecisionXAUrlTemplate;

        } else if (getloanStatus) {

            savingDecisionXAUrlTemplate = Config.getallsavingDecisionXAUrlTemplate;

        }

        else {
            savingDecisionXAUrlTemplate = Config.getsavingDecisionXAUrlTemplate;

            if (savingDecisionXAUrlTemplate != null) {
                savingDecisionXAUrlTemplate = savingDecisionXAUrlTemplate.replaceAll("\\(", "\\{");
                savingDecisionXAUrlTemplate = savingDecisionXAUrlTemplate.replaceAll("\\)", "\\}");
            }

        }
        // map for URL template
        Map<String, String> applicationMap = new HashMap<>();
        applicationMap.put("userId", userId);

        RestTemplate restTemplate = Config.getRestTemplate();
        ResponseEntity<String> response = restTemplate.exchange(storageUrl + savingDecisionXAUrlTemplate, method,
                buildLocalStorageRequest(body, apiKey), // for headers
                String.class, applicationMap);

        return response;

    }

    /**
     * Prepares an http entity with headers.
     * 
     * @param requestString
     * @param apiKey
     * @return
     */
    public HttpEntity<String> buildLocalStorageRequest(@NonNull final String requestString,
            @NonNull final String apiKey) {
        return new HttpEntity<String>(requestString, Config.getJsonHeaders(apiKey));
    }

    public void SavingDecisionXAApplicationRequest(String writeValueAsString, String writeValueAsString2,
            @NonNull String userId,
            Map<String, String> params, HttpMethod put) {
    }

    public ObjectNode getLoanApplicationStatus(@NonNull final String userId, @NonNull final Map<String, String> params,
            ConnectorMessage message)
            throws IOException {
        String preFix = "getLoanApplicationStatus: ";
        clog.debug(message, preFix + "Start");
        ObjectMapper mapper = new ObjectMapper();
        ObjectNode Result = null;
        ResponseEntity<String> responseEntity = this.SavingDecisionXAApplicationRequest("", userId, params,
                HttpMethod.GET, true);
        clog.debug(message, preFix + "Response entity retrieved");
        clog.debug(message, preFix + "response entity is: " + responseEntity.getBody());
        if (responseEntity.getStatusCode() == HttpStatus.OK) {
            clog.debug(message, preFix + "Response entity http status: " + responseEntity.getStatusCode().toString());
            ArrayNode arrayNode = (ArrayNode) mapper.readTree(responseEntity.getBody());
            clog.debug(message, preFix + "Response body converted to array node.:" + arrayNode.toPrettyString());
            ArrayNode finalDateNode = (ArrayNode) new ObjectMapper().createArrayNode();
            clog.debug(message, preFix + "array node. converted to final date node:" + finalDateNode.toPrettyString());
            String loanType = params.get("loanType");

            if (arrayNode != null) {

                try {
                    SimpleDateFormat datee = new SimpleDateFormat("yyyy-MM-dd");
                    List<Date> sDates = new ArrayList<>();
                    if (arrayNode.size() > 0) {
                        clog.debug(message, preFix + "Array node size is greater than 0");
                        for (int i = arrayNode.size() - 1; i >= 0; i--) {
                            String itemNode = arrayNode.get(i).get("loanType").asText();
                            if (!itemNode.equalsIgnoreCase(loanType)) {
                                arrayNode.remove(i);
                            }
                        }
                        if (arrayNode.size() > 0) {
                            for (int j = arrayNode.size() - 1; j >= 0; j--) {
                                String jsonNode = arrayNode.get(j).get("loanDate").asText();
                                Date date1 = datee.parse(jsonNode);
                                sDates.add(date1);
                            }
                            Date date2 = Collections.max(sDates);
                            for (int k = arrayNode.size() - 1; k >= 0; k--) {
                                String jsonNode = arrayNode.get(k).get("loanDate").asText();
                                Date date1 = datee.parse(jsonNode);
                                if (date2.compareTo(date1) == 0) {
                                    finalDateNode.add(arrayNode.get(k));
                                }
                            }
                        }

                        String objectNode = mapper.writeValueAsString(finalDateNode);
                        JSONArray array = new JSONArray(objectNode);
                        Result = (ObjectNode) new ObjectMapper().createObjectNode();
                        if (array.length() > 0) {
                            Result.put("appId", array.getJSONObject(0).get("appId").toString());
                            Result.put("loanType", array.getJSONObject(0).get("loanType").toString());
                            Result.put("loanStatus", array.getJSONObject(0).get("loanStatus").toString());
                            Result.put("loanDate", array.getJSONObject(0).get("loanDate").toString());
                        } else
                            Result.put("loanStatus", "");
                    } else {
                        clog.debug(message, preFix + "Array node size is 0, and we aren't doing anything.");
                    }
                } catch (Exception ex) {
                    throw new IOException("Fetching status failed");
                }
            }
        }
        clog.debug(message, preFix + "Local storage adapter finished");
        return Result;
    }

}