package com.coastal24.connectorservices.SavingApplicationinfo.handler;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import com.coastal24.connectorservices.SavingApplicationinfo.Config;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.xtensifi.dspco.ConnectorMessage;

import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.input.SAXBuilder;
import org.springframework.http.HttpMethod;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import lombok.NonNull;

/**
 * Handler to validate Joint Applicant Address Details.
 */
public class ValidateJointApplicantAddressDetailsHandler extends HandlerBase {

    public static ObjectMapper mapper = Config.getObjectMapper();

    @Override
    public String generateResponse(Map<String, String> params, String userId, ConnectorMessage message)
            throws IOException, TemplateException {

        ObjectNode result = mapper.createObjectNode();

        String jointuserStreetAddress1, jointuserStreetAddress2, jointuserCity, jointuserState, jointuserZipcode,
                respCity, respState, respZip5, respAddress1, respAddress2, errorMessage, uspsApiKey, uspsApiUrl = null;

        jointuserStreetAddress1 = params.get("jointuserAddressline1");
        jointuserStreetAddress2 = params.get("jointuserAddressline2");
        jointuserCity = params.get("jointuserAddressCity");
        jointuserState = params.get("jointuserAddressState");
        jointuserZipcode = params.get("jointuserAddressZip");
        uspsApiUrl = params.get("uspsApiUrl");
        uspsApiKey = params.get("uspsApiKey");

        String urlstr1 = uspsApiUrl + "?API=Verify&XML=";
        ObjectNode ValidateAddressUser = mapper.createObjectNode().put("userId", uspsApiKey);
        ValidateAddressUser.put("Revision", "1");
        ValidateAddressUser.put("id", "0");
        ValidateAddressUser.put("Address1", jointuserStreetAddress1);
        ValidateAddressUser.put("Address2", jointuserStreetAddress2);
        ValidateAddressUser.put("city", jointuserCity);
        ValidateAddressUser.put("state", jointuserState);
        ValidateAddressUser.put("zip5", jointuserZipcode);

        try {
            final String decisionLoanBody = buildDecisionLoanRequest(ValidateAddressUser, params);
            String body = URLEncoder.encode(decisionLoanBody, StandardCharsets.UTF_8);
            URI decisionLoanUrl = new URI(urlstr1 + body);
            ResponseEntity<String> decisionLoanResponseEntity = restTemplate
                    .exchange(new RequestEntity<>(HttpMethod.GET, decisionLoanUrl), String.class);

            Document decisionLoanResponse;
            ObjectNode AddressDetails = null;
            try {
                decisionLoanResponse = new SAXBuilder()
                        .build(new StringReader(decisionLoanResponseEntity.getBody()));
                Element rootElement = decisionLoanResponse.getRootElement();
                Element addressElement = rootElement.getChild("Address");
                Element errorElement = addressElement.getChild("Error");

                respAddress1 = addressElement.getChildText("Address2");
                respAddress2 = addressElement.getChildText("Address1");
                respCity = addressElement.getChildText("City");
                respState = addressElement.getChildText("State");
                respZip5 = addressElement.getChildText("Zip5");
                AddressDetails = mapper.createObjectNode().put("respAddress1", jointuserStreetAddress1);
                AddressDetails.put("respAddress2", jointuserStreetAddress2);
                AddressDetails.put("respCity", jointuserCity);
                AddressDetails.put("respState", jointuserState);
                AddressDetails.put("respZip5", jointuserZipcode);
                if (errorElement != null) {
                    errorMessage = errorElement.getChildText("Description");
                    AddressDetails.put("errorMessage", errorMessage);
                }
            } catch (JDOMException jdomEx) {
                clog.error(message,
                        "Error while parsing USPS response into a DOM :\n" + jdomEx.getMessage());
                throw new IOException("Problem parsing USPS response into a DOM", jdomEx);
            }

            // check the user address details is same.
            if (respCity == null || !respCity.equalsIgnoreCase(jointuserCity) || respState == null
                    || !respState.equalsIgnoreCase(jointuserState) || respZip5 == null
                    || !respZip5.equalsIgnoreCase(jointuserZipcode)
                    || (jointuserStreetAddress2 != null
                            && (respAddress2 != null && !respAddress2.equalsIgnoreCase(jointuserStreetAddress2)))) {

                AddressDetails = mapper.createObjectNode().put("respAddress1", respAddress1);
                AddressDetails.put("respAddress2", respAddress2);
                AddressDetails.put("respCity", respCity);
                AddressDetails.put("respState", respState);
                AddressDetails.put("respZip5", respZip5);
                AddressDetails.put("success", "false");
                return mapper.writeValueAsString(AddressDetails);
            } else {

                result.put("message", "JointApplicant address details validated");
                result.put("success", "True");
                return mapper.writeValueAsString(result);
            }

        } catch (URISyntaxException ex) {
            clog.error(message, "Problem with USPS request. \n" + ex.getMessage());
            throw new IOException("Problem with USPS request", ex);
        }

    }

    private String buildDecisionLoanRequest(@NonNull final ObjectNode localStorageApplicationNode,
            Map<String, String> params) throws TemplateException, IOException {
        Template decisionLoanTemplate = freemarkerCfg.getTemplate("MeridianLink/usps.ftlx");
        Map<String, Object> root = new HashMap<>(params);
        final Map<String, Object> localStorageApplicationMap = mapper.convertValue(localStorageApplicationNode,
                new TypeReference<Map<String, Object>>() {
                });
        root.putAll(localStorageApplicationMap);
        Writer w = new StringWriter();
        jsonHelper.processTemplateWithParams(decisionLoanTemplate, root, w);

        return w.toString();
    }

}
