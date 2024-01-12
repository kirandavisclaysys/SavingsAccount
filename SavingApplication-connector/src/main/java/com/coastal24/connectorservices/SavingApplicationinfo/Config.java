package com.coastal24.connectorservices.SavingApplicationinfo;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xtensifi.dspco.ConnectorMessage;
import freemarker.template.Configuration;
import freemarker.template.TemplateExceptionHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestTemplate;
import java.io.IOException;

/**
 * Class for configuring instances of common objects
 */
@Slf4j
public class Config {

    private static RestTemplate restTemplate;
    private static ObjectMapper mapper = getObjectMapper();

    private static Configuration fmCfg = null; // this class provides singleton access to this per freemarker docs

    final public static String savingDecisionXAUrlTemplate = "/loanApplication";
    final public static String getsavingDecisionXAUrlTemplate = "/loanApplication/{userId}";
    final public static String getallsavingDecisionXAUrlTemplate = "/allLoanApplications/{userId}";

    // fake connectorMessage because ConnectorLogging requires one...
    public static ConnectorMessage LOG_CM;

    static {
        // manage a single instance of the rest template
        restTemplate = new RestTemplate();

        try {
            LOG_CM = mapper.readValue("{\n" + "            \"externalServicePayload\":{\n"
                    + "        \"requestType\":{\n" + "            \"connector\":\"TransfersConnector\",\n"
                    + "                    \"version\":\"1.0\",\n" + "                    \"method\":\"errorMethod\"\n"
                    + "        },\n" + "        \"payload\":{\n" + "            \"valuePair\":[]\n" + "        },\n"
                    + "        \"userData\":{\n" + "            \"userId\":\"abc12345-6789-abcd-abcd-09b2f99daf02\",\n"
                    + "                    \"firstName\":\"first\",\n" + "                    \"middleName\":\"M\",\n"
                    + "                    \"lastName\":\"Last\",\n"
                    + "                    \"emailAddress\":\"nope@noreply.com\",\n"
                    + "                    \"homePhone\":\"555-555-5555\",\n"
                    + "                    \"mobilePhone\":null,\n" + "                    \"mailingAddress\":{\n"
                    + "                \"line1\":\"123 MAIN ST\",\n" + "                        \"line2\":null,\n"
                    + "                        \"city\":\"RALEIGH\",\n" + "                        \"state\":\"NC\",\n"
                    + "                        \"zipCode\":\"90210\"\n" + "            }\n" + "        }\n" + "    },\n"
                    + "            \"connectorParametersResponse\":{\n" + "        \"parameters\":{\n"
                    + "            \"valuePair\":[\n" + "\n" + "         ]\n" + "        },\n"
                    + "        \"method\":{\n" + "            \"parameters\":{\n" + "                \"valuePair\":[\n"
                    + "\n" + "            ]\n" + "            },\n" + "            \"isValid\":true\n" + "        },\n"
                    + "        \"connectorController\":\"\"\n" + "    },\n" + "            \"response\":\"\",\n"
                    + "            \"responseStatus\":{\n" + "        \"statusCode\":\"\",\n"
                    + "                \"statusDescription\":\"\",\n" + "                \"status\":\"\",\n"
                    + "                \"statusReason\":\"\",\n" + "                \"requiredFields\":[\n" + "\n"
                    + "      ]\n" + "    }\n" + "}", ConnectorMessage.class);

        } catch (IOException e) {
            log.error("failed to create logCM: ", e);
        }

    }

    // the ObjectMapper is managed here and not Autowired because of some autowiring
    // issue on the constellation platform
    public static ObjectMapper getObjectMapper() {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.enable(DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS);
        return objectMapper;
    }

    public static RestTemplate getRestTemplate() {
        return restTemplate;
    }

    /**
     * Creates headers for requests to the temporary storage service using a given
     * API key.
     */
    public static HttpHeaders getJsonHeaders(String apiKey) {
        HttpHeaders jsonHeaders = new HttpHeaders();
        jsonHeaders.setContentType(MediaType.APPLICATION_JSON);
        jsonHeaders.set("X-API-key", apiKey);
        return jsonHeaders;
    }

    public static HttpHeaders getXmlHeaders(String apiKey) {
        HttpHeaders xmlHeaders = new HttpHeaders();
        xmlHeaders.setContentType(MediaType.APPLICATION_XML);
        xmlHeaders.set("X-API-key", apiKey);
        return xmlHeaders;
    }

    // static singleton access (docs recommend this)
    public static Configuration getFreemarkerConfiguration() {
        if (fmCfg == null) {
            fmCfg = new Configuration(freemarker.template.Configuration.VERSION_2_3_22);
            try {
                fmCfg.setClassForTemplateLoading(Config.class, "/templates");
            } catch (Exception ioex) {
                throw new RuntimeException("Cannot use specified directory for templates.");
            }
            fmCfg.setRecognizeStandardFileExtensions(true); // uses xml escaping in ftlx files, etc...
            fmCfg.setDefaultEncoding("UTF-8");
            fmCfg.setTemplateExceptionHandler(TemplateExceptionHandler.RETHROW_HANDLER);
            fmCfg.setLogTemplateExceptions(false);
        }

        return fmCfg;
    }
}