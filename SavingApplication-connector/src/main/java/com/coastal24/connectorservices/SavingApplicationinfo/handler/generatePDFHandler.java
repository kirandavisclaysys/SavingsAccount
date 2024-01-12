package com.coastal24.connectorservices.SavingApplicationinfo.handler;

import static org.springframework.util.MimeTypeUtils.APPLICATION_XML_VALUE;
import java.io.IOException;
import java.io.StringWriter;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import com.coastal24.connectorservices.SavingApplicationinfo.Config;
import com.coastal24.connectorservices.SavingApplicationinfo.adapter.LocalStorageAdapter;
import com.coastal24.connectorservices.SavingApplicationinfo.util.XMLHelper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.jdom2.Document;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.Base64;
import org.jdom2.JDOMException;
import com.xtensifi.dspco.ConnectorMessage;
import com.fasterxml.jackson.core.type.TypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestClientException;
import java.io.Writer;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpHeaders;
import org.jdom2.input.SAXBuilder;
import java.io.StringReader;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import lombok.NonNull;

/**
 * Handler to Save the Details to the storage DB.
 */

public class generatePDFHandler extends HandlerBase {

    public static ObjectMapper mapper = Config.getObjectMapper();
    private ExecutorService executor = Executors.newSingleThreadExecutor();

    private static final HttpHeaders meridianLinkHeaders = new HttpHeaders();
    static {
        meridianLinkHeaders.set("Content-Type", APPLICATION_XML_VALUE);
    }

    @Override
    public String generateResponse(Map<String, String> params, String userId, ConnectorMessage message)
            throws IOException, TemplateException {

        ObjectNode result = mapper.createObjectNode();

        String loanUId = params.get("loanUid");
        String localDBdata = "";
        String expressAppid = "";
        String decodedPassword, decodedUserId;

        final String generatePDFApiPasswordBase64 = params.get("generatePDFApiPasswordBase64");
        final String generatePDFApiUserIdBase64 = params.get("generatePDFApiUserIdBase64");

        decodedPassword = new String(Base64.getDecoder().decode(generatePDFApiPasswordBase64));
        decodedUserId = new String(Base64.getDecoder().decode(generatePDFApiUserIdBase64));
        params.put("generatePDFApiUserId", decodedUserId);
        params.put("generatePDFApiPassword", decodedPassword);

        LocalStorageAdapter localStorageAdapter = new LocalStorageAdapter();
        clog.warn(message, "LoanUId" + loanUId + " Before retriving data from costal DB");
        try {
            // methods to retrieving Data from Storage DB.
            ResponseEntity<String> response = localStorageAdapter.SavingDecisionXAApplicationRequest("{}", loanUId,
                    params,
                    HttpMethod.GET, false);

            localDBdata = response.getBody();
        } catch (Exception ex) {
            clog.error(message, "LoanUId" + loanUId + " Error in costal DB:-" + ex.getMessage());
            throw new IOException("Could not retrive value from DB.");
        }
        JsonNode savingsaccountdata = mapper.readTree(localDBdata);
        expressAppid = savingsaccountdata.get("loanId").asText();
        clog.warn(message, "LoanUId" + loanUId + " Got expressAppId from costal DB:-" + expressAppid);
        params.put("expressAppid", expressAppid);

        try {
            // Meridianlink Process
            final String decisionLoanBody = buildDecisionLoanRequest(result, params);
            clog.warn(message, "LoanUId" + loanUId + " generatePDF dicision body:-" + decisionLoanBody);

            if (decisionLoanBody != "") {
                Future<String> future = this.submitApplication(params, decisionLoanBody, message, loanUId);
                result.put("success", true);
                result.put("message", "generatePDF submitted");
                result.put("loanUId", loanUId);
                clog.warn(message, "LoanUId" + loanUId + "Initiated generatePDF");
                return mapper.writeValueAsString(result);
            } else {
                result.put("Success", "false");
                result.put("message", "xml body not generated correctly");
                clog.error(message, "LoanUId" + loanUId + "generatePDF xml body not generated correctly.");

            }

        } catch (

        Exception serverEx) {
            clog.error(message, "LoanUId" + loanUId + " generatePDF Error :- " + serverEx.getMessage());
            throw new RestClientException("Error in generatePDF API");
        }

        return mapper.writeValueAsString(result);
    }

    public Future<String> submitApplication(Map<String, String> params, @NonNull String decisionLoanBody,
            ConnectorMessage message, String loanUId) throws IOException, TemplateException {

        return executor.submit(() -> {

            URI decisionLoanUrl = new URI(params.get("generatePDFApiUrl"));

            ResponseEntity<String> decisionLoanResponseEntity = restTemplate
                    .exchange(new RequestEntity<>(decisionLoanBody, meridianLinkHeaders, HttpMethod.POST,
                            decisionLoanUrl, String.class), String.class);

            if (!decisionLoanResponseEntity.getStatusCode().equals(HttpStatus.OK)) {

                throw new IOException("There was a problem sending generatePDF request to meridianlink");
            }

            clog.warn(message, "LoanUId" + loanUId + " Response from generatePDF API" + decisionLoanResponseEntity);

            return "";

        });
    }

    public String buildDecisionLoanRequest(@NonNull final ObjectNode localStorageApplicationNode,
            @NonNull final Map<String, String> params) throws TemplateException, IOException {

        // get the template for making
        Template decisionLoanTemplate = freemarkerCfg.getTemplate("MeridianLink/generatePDF.ftlx");
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