package com.coastal24.connectorservices.SavingApplicationinfo.handler;

import static org.springframework.util.MimeTypeUtils.APPLICATION_XML_VALUE;
import java.io.IOException;
import java.io.StringWriter;
import java.net.URI;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jdom2.Element;
import com.coastal24.connectorservices.SavingApplicationinfo.Config;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.jdom2.Document;
import org.jdom2.Attribute;
import org.jdom2.input.SAXBuilder;

import java.util.ArrayList;
import java.util.Arrays;
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
import java.io.StringReader;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import lombok.NonNull;

/**
 * Handler to Save the Details to the storage DB.
 */

public class getDepositRateChangesHandler extends HandlerBase {

    public static ObjectMapper mapper = Config.getObjectMapper();

    private static final HttpHeaders meridianLinkHeaders = new HttpHeaders();
    static {
        meridianLinkHeaders.set("Content-Type", APPLICATION_XML_VALUE);
    }

    @Override
    public String generateResponse(Map<String, String> params, String userId, ConnectorMessage message)
            throws IOException, TemplateException {

        ObjectNode result = mapper.createObjectNode();

        String decodedPassword, decodedUserId;
        double maxApy = Double.MIN_VALUE;
        double maxRate = Double.MIN_VALUE;
        String product_Code = "";
        List<String> productcodeList = new ArrayList<String>();

        final String meridianLinkApiPasswordBase64 = params.get("meridianLinkApiPasswordBase64");
        final String meridianLinkApiUserIdBase64 = params.get("meridianLinkApiUserIdBase64");
        // new
        try {
            product_Code = params.get("TierProductCodes");

            if (!product_Code.isEmpty()) {
                productcodeList = Arrays.asList(product_Code.split(","));
            }
        } catch (Exception e) {
            clog.error(message, e.getMessage());
        }

        decodedPassword = new String(Base64.getDecoder().decode(meridianLinkApiPasswordBase64));
        decodedUserId = new String(Base64.getDecoder().decode(meridianLinkApiUserIdBase64));
        params.put("meridianLinkApiUserId", decodedUserId);
        params.put("meridianLinkApiPassword", decodedPassword);

        try {
            // Meridianlink Process
            final String decisionLoanBody = buildDecisionLoanRequest(result, params);
            clog.warn(message, "depositRate body:-" + decisionLoanBody);
            URI decisionLoanUrl = new URI(params.get("depositRateChangesApiurl"));
            clog.warn(message, "Before sending data to depositRate API" + decisionLoanBody);
            ResponseEntity<String> decisionLoanResponseEntity = restTemplate
                    .exchange(new RequestEntity<>(decisionLoanBody, meridianLinkHeaders, HttpMethod.POST,
                            decisionLoanUrl, String.class), String.class);

            if (!decisionLoanResponseEntity.getStatusCode().equals(HttpStatus.OK)) {

                throw new IOException("There was a problem sending that request to meridianlink");
            }

            clog.warn(message, "Response from depositRate API" + decisionLoanResponseEntity);

            Document decisionLoanResponse;
            decisionLoanResponse = new SAXBuilder()
                    .build(new StringReader(decisionLoanResponseEntity.getBody()));

            Element classElement = decisionLoanResponse.getRootElement();

            Element response = classElement.getChild("RESPONSE");

            Element products = response.getChild("PRODUCTS");

            List<Element> productList = products.getChildren("PRODUCT");

            ArrayNode depositRateProduct = mapper.createArrayNode();

            for (int temp = 0; temp < productList.size(); temp++) {
                Element loanResponse = productList.get(temp);

                ObjectNode productObject = (ObjectNode) depositRateProduct.addObject();

                Attribute productCode = loanResponse.getAttribute("product_code");
                productObject.put("product_code", productCode.getValue());

                for (Element product : productList) {

                    Element tiers = product.getChild("TIERS");

                    if (productcodeList != null && productcodeList.size() > 0
                            && productcodeList.contains(productCode.getValue())) {

                        if (tiers != null) {
                            List<Element> tierList = tiers.getChildren("TIER");

                            for (Element tier : tierList) {

                                Attribute apy = tier.getAttribute("apy");
                                Attribute rate = tier.getAttribute("rate");

                                double apyValue = Double.parseDouble(apy.getValue());

                                if (apyValue > maxApy) {
                                    maxApy = apyValue;
                                    maxRate = rate.getDoubleValue();
                                }

                            }

                        }
                        productObject.put("apy", Double.toString(maxApy));
                        productObject.put("rate", Double.toString(maxRate));
                    }

                    else {
                        Attribute apy = loanResponse.getAttribute("apy");
                        productObject.put("apy", apy.getValue());
                        Attribute rate = loanResponse.getAttribute("rate");
                        productObject.put("rate", rate.getValue());

                    }
                }

                Attribute isActive = loanResponse.getAttribute("is_active");
                productObject.put("is_active", isActive.getValue());

            }

            result.set("PRODUCTS", depositRateProduct);
            result.put("success", true);
            clog.warn(message, "Response from depositRate API" + result);

        } catch (

        Exception serverEx) {
            clog.error(message, "depositRate Error :- " + serverEx.getMessage());
            throw new RestClientException("Error in depositRate API");

        }

        return mapper.writeValueAsString(result);
    }

    public String buildDecisionLoanRequest(@NonNull final ObjectNode localStorageApplicationNode,
            @NonNull final Map<String, String> params) throws TemplateException, IOException {

        // get the template for making
        Template decisionLoanTemplate = freemarkerCfg.getTemplate("MeridianLink/depositRate.ftlx");
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