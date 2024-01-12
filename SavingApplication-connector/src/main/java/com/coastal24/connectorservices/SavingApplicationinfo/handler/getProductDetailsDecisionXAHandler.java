package com.coastal24.connectorservices.SavingApplicationinfo.handler;

import static org.springframework.util.MimeTypeUtils.APPLICATION_XML_VALUE;
import java.io.IOException;
import java.io.StringWriter;
import java.net.URI;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import com.coastal24.connectorservices.SavingApplicationinfo.Config;
import com.coastal24.connectorservices.SavingApplicationinfo.adapter.LocalStorageAdapter;
import com.coastal24.connectorservices.SavingApplicationinfo.util.XMLHelper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import org.jdom2.Document;
import java.util.Base64;
import java.util.Date;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.jdom2.JDOMException;
import java.net.URISyntaxException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;

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
public class getProductDetailsDecisionXAHandler extends HandlerBase {

    public static ObjectMapper mapper = Config.getObjectMapper();

    private static final HttpHeaders meridianLinkHeaders = new HttpHeaders();
    static {
        meridianLinkHeaders.set("Content-Type", APPLICATION_XML_VALUE);
    }

    private ExecutorService executor = Executors.newSingleThreadExecutor();
    ObjectNode meridianLinkDecision = mapper.createObjectNode();

    String loanType, xpressAppid = null, xpressAppnumber = null, status = null, error, loanUId;

    @Override
    public String generateResponse(Map<String, String> params, @NonNull String userId, ConnectorMessage message)
            throws IOException, TemplateException {

        ObjectNode result = mapper.createObjectNode();
        ObjectNode finalResult = mapper.createObjectNode();
        loanType = params.get("loanType");
        String accountName = params.get("accountName");
        String productCode = params.get("productCode");
        String loanCategoryString = params.get("loanCat");
        String loancdpUserId = params.get("cdpUserId");
        String loanDate = params.get("loanDate");
        String decodedPassword, decodedUserId;
        String decisionLoanBody = "";
        Boolean IsJoint = false;
        Boolean isJointApplicantAddressSame = false;
        String jointuseremployedMonths = params.get("jointuseremployedMonths");
        String citizenshipStatus1 = params.get("citizenshipStatus1");
        String citizenshipStatus2 = params.get("citizenshipStatus2");
        String citizenshipStatus3 = params.get("citizenshipStatus3");
        Boolean JointIsEmployee = false;

        if (params.get("isJointApplicant") != null && !params.get("isJointApplicant").toString().isEmpty()) {
            IsJoint = Boolean.parseBoolean(params.get("isJointApplicant"));
        }

        final String meridianLinkApiPasswordBase64 = params.get("meridianLinkApiPasswordBase64");
        final String meridianLinkApiUserIdBase64 = params.get("meridianLinkApiUserIdBase64");

        decodedPassword = new String(Base64.getDecoder().decode(meridianLinkApiPasswordBase64));
        decodedUserId = new String(Base64.getDecoder().decode(meridianLinkApiUserIdBase64));
        params.put("meridianLinkApiUserId", decodedUserId);
        params.put("meridianLinkApiPassword", decodedPassword);

        ObjectNode savingObject = mapper.createObjectNode().put("userId", userId);
        savingObject.put("loanType", loanType);
        savingObject.put("accountName", accountName);
        savingObject.put("productCode", productCode);
        savingObject.put("cdpUserId", loancdpUserId);
        savingObject.put("loanCategory", loanCategoryString);
        savingObject.put("loanDate", loanDate);

        LocalStorageAdapter localStorageAdapter = new LocalStorageAdapter();
        try {
            clog.info(message, "DB Body:- " + mapper.writeValueAsString(savingObject));
            ResponseEntity<String> response = localStorageAdapter
                    .SavingDecisionXAApplicationRequest(mapper.writeValueAsString(savingObject), userId, params,
                            HttpMethod.POST, false);
            ObjectMapper mapper = new ObjectMapper();
            JsonNode dBResult = mapper.readTree(response.getBody());
            loanUId = dBResult.get("loanApplication").asText();
            clog.info(message, "LoanUId" + loanUId + " Saved initial data to costal DB");
        } catch (Exception e) {
            clog.error(message, "Local DB Error Message:-" + e.getMessage());
            result = mapper.createObjectNode().put("success", "false");
            result.put("Error", "Local storage service could not save initaial Loan Data.");
            return mapper.writeValueAsString(result);
        }

        LocalDate dateObj = LocalDate.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        String date = dateObj.format(formatter);
        params.put("currentDate", date);

        // jointapplicant details
        try {

            params.put("IsJoint", Boolean.toString(IsJoint));
            if (IsJoint) {

                isJointApplicantAddressSame = Boolean.parseBoolean(params.get("isJointApplicantAddressSame"));
                params.put("isJointApplicantAddressSame", Boolean.toString(isJointApplicantAddressSame));

                SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");

                if (params.get("jointdob") != null && !params.get("jointdob").isEmpty()) {
                    Date date2 = new SimpleDateFormat("MM/dd/yyyy").parse(params.get("jointdob"));
                    finalResult.put("jointdob", dateFormat.format(date2));
                }
                if (params.get("jointuserexpiryDate") != null && !params.get("jointuserexpiryDate").isEmpty()) {
                    Date date2 = new SimpleDateFormat("MM/dd/yyyy").parse(params.get("jointuserexpiryDate"));
                    finalResult.put("jointuserexpiryDate", dateFormat.format(date2));
                }
                if (params.get("jointuserdateIssued") != null && !params.get("jointuserdateIssued").isEmpty()) {
                    Date date2 = new SimpleDateFormat("MM/dd/yyyy").parse(params.get("jointuserdateIssued"));
                    finalResult.put("jointuserdateIssued", dateFormat.format(date2));
                }
                if (params.get("jointuseremployedMonths") != null
                        && !params.get("jointuseremployedMonths").isEmpty())
                    finalResult.put("convertedjointemployedMonths", Integer.parseInt(jointuseremployedMonths));
                else
                    finalResult.put("convertedjointemployedMonths", 0);

                if (params.get("jointuseremploymentStatus") != null)
                    finalResult.put("jointuseremploymentStatus", params.get("jointuseremploymentStatus"));
                else
                    finalResult.put("jointuseremploymentStatus", "OT");

                if (params.get("jointuserProfession") != null)
                    finalResult.put("jointuserProfession", params.get("jointuserProfession"));
                else
                    finalResult.put("jointuserProfession", "OTHER");

                if (params.get("jointuserEmployer") != null)
                    finalResult.put("jointuserEmployer", params.get("jointuserEmployer"));
                else
                    finalResult.put("jointuserEmployer", "OTHER");

                if (params.get("jointcitizenShip").equalsIgnoreCase("US CITIZEN")) {
                    finalResult.put("formattedjointcitizenShip", citizenshipStatus1);
                }
                if (params.get("jointcitizenShip").equalsIgnoreCase("PERMANENT RESIDENT")) {
                    finalResult.put("formattedjointcitizenShip", citizenshipStatus2);
                }
                if (params.get("jointcitizenShip").equalsIgnoreCase("NOT A US CITIZEN")) {
                    finalResult.put("formattedjointcitizenShip", citizenshipStatus3);
                }
                if (params.get("JointApplicantIsEmployee") != null
                        && !params.get("JointApplicantIsEmployee").toString().isEmpty()) {
                    JointIsEmployee = Boolean.parseBoolean(params.get("JointApplicantIsEmployee"));
                }
                params.put("JointIsEmployee", Boolean.toString(JointIsEmployee));

            }

        } catch (Exception e1) {
            clog.error(message, "Error" + e1.getMessage());
            result = mapper.createObjectNode().put("success", "false");
            result.put("Error", "Could not parse jointapplicant details from allparams");
            return mapper.writeValueAsString(result);

        }

        try {
            // Meridianlink Process
            decisionLoanBody = buildDecisionLoanRequest(finalResult, params);

            if (decisionLoanBody != "") {
                clog.info(message, "LoanUId" + loanUId + " Decision Loan Body:-" + decisionLoanBody);
                Future<String> future = this.submitApplication(params, userId, decisionLoanBody, message);
                result.put("success", true);
                result.put("message", "Application submitted");
                result.put("loanUId", loanUId);
                return mapper.writeValueAsString(result);
            }
            clog.error(message, "LoanUId" + loanUId + " Decision Loan body could not be framed.");
            result = mapper.createObjectNode().put("success", "false");
            result.put("Error", "Disition body could not be framed");
            return mapper.writeValueAsString(result);

        } catch (Exception ex1) {
            clog.error(message, "LoanUId" + loanUId + "Disition body could not be framed:-" + ex1.getMessage());
            result = mapper.createObjectNode().put("success", "false");
            result.put("Error", "Disition body could not be framed");
            return mapper.writeValueAsString(result);

        }

    }

    public Future<String> submitApplication(Map<String, String> params, @NonNull String userId, String decisionLoanBody,
            ConnectorMessage message) throws IOException, TemplateException {

        return executor.submit(() -> {

            try {
                URI decisionLoanUrl = new URI(params.get("meridianLinkApiUrl"));
                clog.warn(message, "LoanUId" + loanUId + " Sending request to Meridian");
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
                clog.info(message, "LoanUId" + loanUId + " MERIDIAN Responce" + decisionLoanResponseEntity);
                Document decisionLoanResponse;
                try {

                    decisionLoanResponse = new SAXBuilder()
                            .build(new StringReader(decisionLoanResponseEntity.getBody()));

                    status = XMLHelper.getElementAttributeValue(decisionLoanResponse, "DECISION", "status");

                    error = XMLHelper.getElementAttributeValue(decisionLoanResponse, "ERROR", "type");
                    if (error == null) {
                        xpressAppid = XMLHelper.getElementAttributeValue(decisionLoanResponse, "RESPONSE",
                                "xpress_app_id");
                        xpressAppnumber = XMLHelper.getElementAttributeValue(decisionLoanResponse, "RESPONSE",
                                "xpress_app_number");
                    } else {
                        xpressAppid = XMLHelper.getElementAttributeValue(decisionLoanResponse, "ERROR",
                                "xpress_app_id");
                        xpressAppnumber = XMLHelper.getElementAttributeValue(decisionLoanResponse, "ERROR",
                                "xpress_app_number");
                    }

                    if (xpressAppid == null) {
                        throw new JDOMException(
                                "xpressAppid not found in meridianlink response decisionLoanResponseEntity:-"
                                        + decisionLoanResponseEntity);
                    }
                    if (status == null && xpressAppid != null) {
                        status = "REFERRED";
                    }
                    clog.warn(message, "LoanUId" + loanUId + " Before saving MeridinLink response  to costal DB");
                    ObjectNode savingDecisionXAObject = mapper.createObjectNode().put("loanId", xpressAppid);
                    savingDecisionXAObject.put("loanNumber", xpressAppnumber);
                    savingDecisionXAObject.put("loanStatus", status);

                    clog.info(message,
                            "Response Object body send to costal DB:- "
                                    + mapper.writeValueAsString(savingDecisionXAObject));

                    LocalStorageAdapter localStorageAdapter = new LocalStorageAdapter();

                    try {

                        ResponseEntity<String> response = localStorageAdapter
                                .SavingDecisionXAApplicationRequest(mapper.writeValueAsString(savingDecisionXAObject),
                                        loanUId,
                                        params, HttpMethod.PUT, false);
                        ObjectMapper mapper = new ObjectMapper();
                        JsonNode dBResult = mapper.readTree(response.getBody());
                        clog.warn(message, "LoanUId" + loanUId + " Saved Meridian response to DB");

                    } catch (RestClientException serverEx) {
                        clog.error(message, "LoanUId" + loanUId +
                                " Error in saving meridian response to Costal DB :-" + serverEx.getMessage());
                        throw new RestClientException("Local storage service could not save meridian response Data.");
                    }
                    return "";
                } catch (JDOMException jdomEx) {
                    clog.error(message, "LoanUId" + loanUId +
                            " Error while parsing meridian link response into a DOM :\n" + jdomEx.getMessage());
                    throw new IOException("Problem parsing meridian link response into a DOM", jdomEx);
                }

            } catch (URISyntaxException ex) {
                clog.error(message, "LoanUId" + loanUId + "Problem with meridian link request. Returned Error \n"
                        + ex.getMessage());
                throw new IOException("Problem with meridian link request", ex);
            }
        });
    }

    public String buildDecisionLoanRequest(@NonNull final ObjectNode localStorageApplicationNode,
            @NonNull final Map<String, String> params)
            throws TemplateException, IOException {
        String loanCategory = params.get("loanCat");
        boolean orderPlasticByDefault = true;
        boolean addthemtocertificate = true;
        if (params.get("orderPlasticByDefault") != null) {
            orderPlasticByDefault = Boolean.valueOf(params.get("orderPlasticByDefault").toString());
        }
        if (params.get("addthemtocertificate") != null) {
            addthemtocertificate = Boolean.valueOf(params.get("addthemtocertificate").toString());
        }

        Template decisionLoanTemplate;

        // get the template for making
        if (loanCategory.equalsIgnoreCase("CERTIFICATE")) {
            if (addthemtocertificate) {
                decisionLoanTemplate = freemarkerCfg.getTemplate("MeridianLink/certificateWithoutDivident.ftlx");

            } else
                decisionLoanTemplate = freemarkerCfg.getTemplate("MeridianLink/certificate.ftlx");

        } else if (loanCategory.equalsIgnoreCase("CHECKING")) {
            if (orderPlasticByDefault) {
                decisionLoanTemplate = freemarkerCfg.getTemplate("MeridianLink/checking.ftlx");

            } else
                decisionLoanTemplate = freemarkerCfg.getTemplate("MeridianLink/checkingwithoutplastic.ftlx");

        } else {
            decisionLoanTemplate = freemarkerCfg.getTemplate("MeridianLink/decisionXA.ftlx");
        }

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