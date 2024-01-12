package com.coastal24.connectorservices.SavingApplicationinfo;

import com.coastal24.connectorservices.SavingApplicationinfo.controller.ControllerBase;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import lombok.NonNull;
import java.io.IOException;
import java.io.InputStream;
import java.io.Writer;
import java.util.Map;

/**
 * Class with helper functions for creating and manipulating JsonNodes.
 * Uses the preconfigured object mapper from the Config singleton.
 */
public class JsonHelper {

    private static ObjectMapper mapper = Config.getObjectMapper();

    public static String getResourceFileAsString(String fileName) throws IOException{
        try (InputStream file = ControllerBase.class.getResourceAsStream(fileName)) {
            return mapper.writeValueAsString(mapper.readValue(file, Object.class));
        }
    }

    /**
     * Builds an ObjectNode with a success property set to the supplied argument.
     * @return
     */
    public ObjectNode buildSuccessObject(boolean success){
        return mapper.createObjectNode().put("success",  success);
    }

    /**
     * Returns a pretty printed string of the supplied JsonNod
     * @throws JsonProcessingException
     */
    public String pretty(@NonNull final JsonNode root) throws JsonProcessingException {
        return mapper.writerWithDefaultPrettyPrinter().writeValueAsString(root);
    }

    /**
     *
     * Returns a pretty printed string of a json object created from the input map.
     */
    public String mapToJsonString(@NonNull final Map<String, String> params) throws JsonProcessingException{
        return this.pretty(mapper.valueToTree(params));
    }

    /**
     * Create an array of objects with a label and value, i.e. :
     * [
     *  { "label": "Name", "value":"Frank J Mann" }, {...}, {...}
     * }
     * @param map the map from which the keys and values will be turned into labels and values.
     */
    public ArrayNode mapToKeyValuePairArray(@NonNull final Map<String, String> map, String keyLabel, String valueLabel){
        ArrayNode result = mapper.createArrayNode();
        map.forEach((key, value) -> {
            result.add(mapper.createObjectNode()
                    .put(keyLabel, key)
                    .put(valueLabel, value));
        });
        return result;
    }

    /**
     * Build a string from the json root at these paths.
     * @param root Root json node to look in
     * @param separator string placed between each peice of text fond
     * @param paths json pointer strings for paths to look
     * @return
     */
    public String buildStringFromFields(ObjectNode root, String separator, String... paths){
        StringBuilder strbld = new StringBuilder();
        for(int i = 0; i < paths.length; i++){
            JsonNode node = root.at(paths[i]);
            strbld.append(node.isMissingNode() ? "" : node.asText() + (i < paths.length - 1 ? separator : ""));
        }
        return strbld.toString().trim();
    }

    public void processTemplateWithParams(Template template, Map<String, ?> params, Writer w) throws IOException, TemplateException {

        template.process(params, w);
    }
}