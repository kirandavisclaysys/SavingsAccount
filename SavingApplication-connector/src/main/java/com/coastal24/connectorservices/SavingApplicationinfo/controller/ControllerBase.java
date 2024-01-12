package com.coastal24.connectorservices.SavingApplicationinfo.controller;

import com.coastal24.connectorservices.SavingApplicationinfo.EnhancedConnectorLogging;
import com.coastal24.connectorservices.SavingApplicationinfo.handler.HandlerLogic;
import com.xtensifi.cufx.CustomData;
import com.xtensifi.cufx.ValuePair;
import com.xtensifi.dspco.ConnectorMessage;
import com.xtensifi.dspco.ConnectorParametersResponse;
import com.xtensifi.dspco.ExternalServicePayload;
import com.xtensifi.dspco.MailingAddress;
import com.xtensifi.dspco.ResponseStatusMessage;
import com.xtensifi.dspco.UserData;
import java.util.HashMap;
import java.util.Map;

public class ControllerBase {

    private static EnhancedConnectorLogging clog = new EnhancedConnectorLogging();

    /**
     * Boilerplate method for handling the connector message
     *
     * @param logPrefix     A prefix for log messages and stats reasons
     * @param connectorJson The raw JSON for the request connector message
     * @param handlerLogic  The custom logic for generating a response
     * @return a response connector message
     */
    protected ConnectorMessage handleConnectorMessage(final String logPrefix, ConnectorMessage connectorMessage,
            final HandlerLogic handlerLogic) {
        ResponseStatusMessage responseStatusMessage = null;
        try {
            // the parameters in the response are the values sent by the tile to control
            // what the connector does.
            final Map<String, String> allParams = getAllParams(connectorMessage);

            // the userId is managed by the constellation platform and uniquely identifies a
            // user.
            final String userId = allParams.get("userId");
            MailingAddress mailingAddress = null;
            String userFirstName = "";
            String userLastName = "";
            String userMiddleName = "";
            String userAddressline1 = "";
            String userAddressline2 = "";
            String userAddressCity = "";
            String userAddressState = "";
            String userAddressZip = "";
            String primaryPhone = "";
            // String primaryEmail = "";
            String cdpUserId = "";
            String userHomePhone = "";
            String userWorkPhone = "";

            UserData userData = connectorMessage.getExternalServicePayload().getUserData();
            if (userData != null) {
                userFirstName = userData.getFirstName();
                userLastName = userData.getLastName();
                userMiddleName = userData.getMiddleName();
                primaryPhone = userData.getHomePhone();
                // primaryEmail = userData.getEmailAddress();
                mailingAddress = userData.getMailingAddress();
                userHomePhone = userData.getHomePhone();
                userWorkPhone = userData.getWorkPhone();

                if (mailingAddress.getLine1() != null)
                    userAddressline1 = mailingAddress.getLine1();
                else
                    userAddressline1 = "";

                if (mailingAddress.getLine2() != null)
                    userAddressline2 = mailingAddress.getLine2();
                else
                    userAddressline2 = "";

                userAddressCity = mailingAddress.getCity();

                userAddressState = mailingAddress.getState();
                userAddressZip = mailingAddress.getZipCode();
                cdpUserId = userData.getUserId();

            }
            allParams.put("userFirstName", userFirstName);
            allParams.put("userLastName", userLastName);
            allParams.put("userMiddleName", userMiddleName);
            allParams.put("primaryPhone", primaryPhone);
            // allParams.put("primaryEmail", primaryEmail);
            allParams.put("userAddressline1", userAddressline1);
            allParams.put("userAddressline2", userAddressline2);
            allParams.put("userAddressCity", userAddressCity);
            allParams.put("userAddressState", userAddressState);
            allParams.put("userAddressZip", userAddressZip);
            allParams.put("cdpUserId", cdpUserId);
            allParams.put("userId", userId);
            allParams.put("userWorkPhone", userWorkPhone);
            allParams.put("userHomePhone", userHomePhone);

            clog.warn(connectorMessage, "USER_DATA:-" + allParams);
            String response = handlerLogic.generateResponse(allParams, userId, connectorMessage);
            clog.warn(connectorMessage, "Controller base response:-" + response);
            connectorMessage.setResponse("{\"response\": " + response + "}");

            responseStatusMessage = new ResponseStatusMessage() {
                {
                    setStatus("OK");
                    setStatusCode("200");
                    setStatusDescription("Success");
                    setStatusReason(logPrefix + "Has responded.");
                }
            };

        } catch (Exception ex) {
            if (connectorMessage != null)
                clog.error(connectorMessage, "Controller base ERROR" + logPrefix + ex.getMessage());
            // response needs to be valid json even during an exception
            connectorMessage
                    .setResponse("{\"response\": {\"success\":false, \"error\": \"" + ex.getMessage() + "\"} }");
            responseStatusMessage = new ResponseStatusMessage() {
                {
                    setStatus("ERROR");
                    setStatusCode("500");
                    setStatusDescription("Failed");
                    setStatusReason(logPrefix + "Has Failed: " + ex.getMessage());
                }
            };

        } finally {
            if (connectorMessage == null) {
                connectorMessage = new ConnectorMessage();
            }
            connectorMessage.setResponseStatus(responseStatusMessage);
        }
        clog.warn(connectorMessage, ".............END............");
        return connectorMessage;
    }

    /**
     * Get all the value pairs out of the connector message. NOTE: if a name occurs
     * more than once, only the first occurrance is returned.
     *
     * @param connectorMessage the request connector message
     * @return a Map of the value pairs
     */
    private Map<String, String> getAllParams(final ConnectorMessage connectorMessage) {
        final Map<String, String> allParams = new HashMap<>();
        final ExternalServicePayload externalServicePayload = connectorMessage.getExternalServicePayload();
        final ConnectorParametersResponse connectorParametersResponse = connectorMessage
                .getConnectorParametersResponse();

        if (externalServicePayload != null) {
            // final CustomData methodParams = externalServicePayload.getPayload();
            if (connectorMessage.getConnectorParametersResponse().getMethod() != null) {
                CustomData methodParams = connectorMessage.getConnectorParametersResponse().getMethod().getParameters();

                if (methodParams != null)
                    for (ValuePair valuePair : methodParams.getValuePair()) {
                        allParams.putIfAbsent(valuePair.getName(), valuePair.getValue());
                    }
            }
        }
        if (connectorParametersResponse != null) {
            final CustomData otherParams = connectorParametersResponse.getParameters();
            if (otherParams != null) {
                for (ValuePair valuePair : otherParams.getValuePair()) {
                    allParams.putIfAbsent(valuePair.getName(), valuePair.getValue());
                }
            }
        }
        return allParams;
    }

}