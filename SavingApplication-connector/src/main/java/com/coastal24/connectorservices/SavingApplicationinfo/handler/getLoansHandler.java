package com.coastal24.connectorservices.SavingApplicationinfo.handler;

import static org.springframework.util.MimeTypeUtils.APPLICATION_XML_VALUE;
import java.io.IOException;
import java.io.StringWriter;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.jdom2.JDOMException;
import com.coastal24.connectorservices.SavingApplicationinfo.Config;
import com.coastal24.connectorservices.SavingApplicationinfo.adapter.LocalStorageAdapter;
import com.coastal24.connectorservices.SavingApplicationinfo.util.XMLHelper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.jdom2.Document;
import java.util.Base64;
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

public class getLoansHandler extends HandlerBase {

    public static ObjectMapper mapper = Config.getObjectMapper();

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
        String respData;
        String suffix = "";
        String decodedPassword, decodedUserId;

        final String getLoansApiPasswordBase64 = params.get("getLoansApiPasswordBase64");
        final String getLoansApiUserIdBase64 = params.get("getLoansApiUserIdBase64");

        decodedPassword = new String(Base64.getDecoder().decode(getLoansApiPasswordBase64));
        decodedUserId = new String(Base64.getDecoder().decode(getLoansApiUserIdBase64));
        params.put("getLoansApiUserId", decodedUserId);
        params.put("getLoansApiPassword", decodedPassword);

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
            // get Loans Process
            final String decisionLoanBody = buildDecisionLoanRequest(result, params);
            clog.warn(message, "LoanUId" + loanUId + " Get Loans dicision body:-" + decisionLoanBody);
            URI decisionLoanUrl = new URI(params.get("GetLoansApiUrl"));
            ResponseEntity<String> decisionLoanResponseEntity = restTemplate
                    .exchange(new RequestEntity<>(decisionLoanBody, meridianLinkHeaders, HttpMethod.POST,
                            decisionLoanUrl, String.class), String.class);

            if (!decisionLoanResponseEntity.getStatusCode().equals(HttpStatus.OK)) {

                throw new IOException("There was a problem sending request to get loans");
            }

            clog.warn(message, "LoanUId" + loanUId + " Response from Get Loans API" + decisionLoanResponseEntity);
            Document getLoansLoanResponse;
            getLoansLoanResponse = new SAXBuilder()
                    .build(new StringReader(decisionLoanResponseEntity.getBody()));

            respData = XMLHelper.getElementValue(getLoansLoanResponse, "LOAN_DATA");
            Pattern pattern = Pattern.compile("suffix=\"(.*?)\"");
            Matcher matcher = pattern.matcher(respData);
            if (matcher.find()) {
                suffix = matcher.group(0);
                if (suffix.length() > 12)
                    suffix = suffix.substring(8, 12);
                else
                    suffix = suffix.substring(8, 11);
            }

            if (!suffix.isEmpty()) {
                result.put("Success", "true");
                result.put("suffix", suffix);
            } else {
                throw new JDOMException(
                        "Error in get loans, Cannot find suffix:-"
                                + decisionLoanResponseEntity);
            }
        } catch (Exception serverEx) {
            clog.error(message, "LoanUId" + loanUId + " Get Loans Error :- " + serverEx.getMessage());
            throw new RestClientException("Error in Get-Loans API");
        }

        return mapper.writeValueAsString(result);
    }

    public String buildDecisionLoanRequest(@NonNull final ObjectNode localStorageApplicationNode,
            @NonNull final Map<String, String> params) throws TemplateException, IOException {

        // get the template for making API body
        Template decisionLoanTemplate = freemarkerCfg.getTemplate("MeridianLink/getLoans.ftlx");
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
