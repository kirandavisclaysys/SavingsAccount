package com.coastal24.connectorservices.SavingApplicationinfo.handler;

import static org.springframework.util.MimeTypeUtils.APPLICATION_XML_VALUE;
import java.io.IOException;
import java.io.StringWriter;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import com.coastal24.connectorservices.SavingApplicationinfo.Config;
import com.coastal24.connectorservices.SavingApplicationinfo.adapter.LocalStorageAdapter;
import com.coastal24.connectorservices.SavingApplicationinfo.util.XMLHelper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.jdom2.Document;
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

public class bookSavingsAccountHandler extends HandlerBase {

    private ExecutorService executor = Executors.newSingleThreadExecutor();

    String loanType, xpressAppid = null, xpressAppnumber = null, status = null, error, loanUId;

    public static ObjectMapper mapper = Config.getObjectMapper();

    private static final HttpHeaders meridianLinkHeaders = new HttpHeaders();
    static {
        meridianLinkHeaders.set("Content-Type", APPLICATION_XML_VALUE);
    }

    @Override
    public String generateResponse(Map<String, String> params, String userId, ConnectorMessage message)
            throws IOException, TemplateException {

        ObjectNode result = mapper.createObjectNode();
        String EnableBooking = params.get("EnableBooking");
        String loanUId = params.get("loanUid");
        String localDBdata = "";
        String expressAppid = "";
        String status;
        String decodedPassword, decodedUserId;

        final String bookappApiPasswordBase64 = params.get("bookappApiPasswordBase64");
        final String bookappApiUserIdBase64 = params.get("bookappApiUserIdBase64");

        decodedPassword = new String(Base64.getDecoder().decode(bookappApiPasswordBase64));
        decodedUserId = new String(Base64.getDecoder().decode(bookappApiUserIdBase64));
        params.put("bookappApiUserId", decodedUserId);
        params.put("bookappApiPassword", decodedPassword);
        if (!EnableBooking.isBlank() && EnableBooking.equalsIgnoreCase("true")) {
            LocalStorageAdapter localStorageAdapter = new LocalStorageAdapter();

            clog.warn(message, "LoanUId" + loanUId + " Before emptying status from coastal DB for booking");
            ObjectNode savingDecisionXAObject = mapper.createObjectNode().put("loanStatus", "");

            try {
                ResponseEntity<String> response = localStorageAdapter
                        .SavingDecisionXAApplicationRequest(mapper.writeValueAsString(savingDecisionXAObject),
                                loanUId,
                                params, HttpMethod.PUT, false);

                // methods to retrieving Data from Storage DB.
                ResponseEntity<String> response1 = localStorageAdapter.SavingDecisionXAApplicationRequest("{}", loanUId,
                        params,
                        HttpMethod.GET, false);

                localDBdata = response1.getBody();
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

                if (decisionLoanBody != "") {
                    clog.warn(message, "LoanUId" + loanUId + " Book savings Loan Body:-" + decisionLoanBody);
                    Future<String> future = this.submitApplication(params, userId, decisionLoanBody, message);
                    result.put("success", true);
                    result.put("message", "Book App Application submitted");
                    result.put("loanUId", loanUId);
                    return mapper.writeValueAsString(result);

                }
                clog.error(message, "LoanUId" + loanUId + " Decision Loan body could not be framed.");
                result = mapper.createObjectNode().put("success", "false");
                result.put("Error", "Disition body could not be framed");
                return mapper.writeValueAsString(result);

            } catch (Exception serverEx) {
                clog.error(message, "LoanUId" + loanUId + " Book App Error :- " + serverEx.getMessage());
                throw new RestClientException("Error in Book-app API");
            }
        } else {
            result.put("Success", "true");
            result.put("EnableBooking", EnableBooking);
        }
        return mapper.writeValueAsString(result);
    }

    public Future<String> submitApplication(Map<String, String> params, @NonNull String userId, String decisionLoanBody,
            ConnectorMessage message) throws IOException, TemplateException {

        return executor.submit(() -> {

            try {
                loanUId = params.get("loanUid");
                clog.warn(message, "LoanUId" + loanUId + " BookApp dicision body:-" + decisionLoanBody);
                URI decisionLoanUrl = new URI(params.get("bookappApiUrl"));
                ResponseEntity<String> decisionLoanResponseEntity = restTemplate
                        .exchange(new RequestEntity<>(decisionLoanBody, meridianLinkHeaders, HttpMethod.POST,
                                decisionLoanUrl, String.class), String.class);

                if (!decisionLoanResponseEntity.getStatusCode().equals(HttpStatus.OK)) {

                    clog.error(message, "LoanUId" + loanUId +
                            "Problem making meridian link request. Request:\n" + decisionLoanBody
                            + "\nResponse status :\n" + decisionLoanResponseEntity.getStatusCode()
                            + "\nResponse body: \n" + decisionLoanResponseEntity.getBody());
                    throw new IOException("There was a problem sending that request to meridian link");
                }

                clog.info(message, "LoanUId" + loanUId + " Response from book app API" + decisionLoanResponseEntity);

                Document decisionLoanResponse;
                try {

                    decisionLoanResponse = new SAXBuilder()
                            .build(new StringReader(decisionLoanResponseEntity.getBody()));

                    status = XMLHelper.getElementAttributeValue(decisionLoanResponse, "RESPONSE", "status");

                    if (status != null && status.equalsIgnoreCase("success")) {
                        ObjectNode savingDecisionXAObject = mapper.createObjectNode().put("loanStatus", status);

                        clog.warn(message,
                                "Response Object body send to costal DB:- "
                                        + mapper.writeValueAsString(savingDecisionXAObject));
                        LocalStorageAdapter localStorageAdapter = new LocalStorageAdapter();
                        try {

                            ResponseEntity<String> response = localStorageAdapter
                                    .SavingDecisionXAApplicationRequest(
                                            mapper.writeValueAsString(savingDecisionXAObject),
                                            loanUId,
                                            params, HttpMethod.PUT, false);
                            ObjectMapper mapper = new ObjectMapper();
                            JsonNode dBResult = mapper.readTree(response.getBody());
                            clog.warn(message, "LoanUId" + loanUId + " Saved Book App response to DB");

                        } catch (RestClientException serverEx) {
                            clog.error(message, "LoanUId" + loanUId +
                                    " Error in saving Book App response to Costal DB :-" + serverEx.getMessage());
                            throw new RestClientException(
                                    "Local storage service could not save Book App response Data.");
                        }

                    } else {
                        clog.error(message, "LoanUId" + loanUId +
                                " Booking error, status failed");
                    }
                    return "";
                } catch (JDOMException jdomEx) {
                    clog.error(message, "LoanUId" + loanUId +
                            " Error while parsing Book App response into a DOM :\n" + jdomEx.getMessage());
                    throw new IOException("Problem parsing Book App response into a DOM", jdomEx);
                }

            } catch (URISyntaxException ex) {
                clog.error(message, "LoanUId" + loanUId + "Problem with Book App request. Returned Error \n"
                        + ex.getMessage());
                throw new IOException("Problem with Book App request", ex);
            }
        });
    }

    public String buildDecisionLoanRequest(@NonNull final ObjectNode localStorageApplicationNode,
            @NonNull final Map<String, String> params) throws TemplateException, IOException {

        Template decisionLoanTemplate = freemarkerCfg.getTemplate("MeridianLink/bookApp.ftlx");
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