package com.coastal24.connectorservices.SavingApplicationinfo.util;

import lombok.NonNull;
import org.jdom2.*;
import org.jdom2.filter.Filters;
import org.jdom2.input.SAXBuilder;
import org.jdom2.output.Format;
import org.jdom2.output.XMLOutputter;
import org.jdom2.xpath.XPathFactory;
import java.io.IOException;
import java.io.StringReader;
import java.util.*;

/**
 *
 */
public class XMLHelper {

    // the jdom2 implementation of xpathfactory is thread-safe
    private static XPathFactory xpfac = XPathFactory.instance();

    /**
     * Get the value of the specified element.
     * If there are multiple elements with the same tag name, only the first is returned
     * @param doc
     * @param tag
     * @return the value or null of the tag is not found
     */
    public static String getElementValue(@NonNull final Document doc, @NonNull final String tag) {
        final List<Element> nodeList = getNodeListByTagName(doc, tag);
        if (nodeList.size() < 1) {
            return null;
        } else {
            return nodeList.get(0).getTextNormalize();
        }
    }

    /**
     * Get the value of the specified element that's descended from a context element.
     * @param context
     * @param tag
     * @return
     */
    public static String getElementValue(@NonNull final Element context, @NonNull final String tag) {
        Element matched = xpfac.compile(tag, Filters.element()).evaluateFirst(context);
        if(matched == null){
            return null;
        }
        return matched.getTextNormalize();
    }

    /**
     * Get a list of values for all instances of the specified element.
     * @param doc
     * @param tag
     * @return list of values
     */
    public static ArrayList<String> getAllElementValues(@NonNull final Document doc, @NonNull final String tag){
        final List<Element> nodeList = getNodeListByTagName(doc, tag);
        ArrayList<String> values = new ArrayList<>(nodeList.size());
        if(nodeList.size() < 1) {
            return null;
        } else {
            for(Element node : nodeList){
                values.add(node.getTextNormalize());
            }
        }
        return values;
    }

    /**
     * Determine if the specified tag is contained in the document.
     * @param doc
     * @param tag
     * @return
     */
    public static boolean hasNode(@NonNull final Document doc, @NonNull final String tag) {
        List<Element> nodeList = getNodeListByTagName(doc, tag);
        return nodeList != null && nodeList.size() > 0;
    }

    /**
     * Finds the list of nodes with the specified tag name. Handles namespace or no namespace.
     * @param doc
     * @param tag
     * @return
     */
    public static List<Element> getNodeListByTagName(@NonNull final Document doc, @NonNull final String tag) {
        List<Element> nodes;
        if (tag.contains(":")) {
            try {
                String[] split = tag.split(":");
                Namespace namespace = doc.getRootElement().getNamespace(split[0]);
                nodes = xpfac.compile("//" + tag, Filters.element(), null, Arrays.asList(namespace)).evaluate(doc);
            }
            catch (Exception iaex){
                nodes = Collections.emptyList();
            }
        } else {
            nodes = xpfac.compile("//" + tag, Filters.element()).evaluate(doc);
        }
        return nodes;
    }

    /**
     * Get the value of the specified attribute of the specified tag.
     * @param doc
     * @param tag
     * @param attribute
     * @return
     */
    public static String getElementAttributeValue(@NonNull final Document doc, @NonNull final String tag, @NonNull final String attribute) {
        final List<Element> nodeList = getNodeListByTagName(doc, tag);
        if (nodeList.size() < 1) {
            return null;
        } else {
            Attribute attr;
            Element node = nodeList.get(0);
            if (attribute.contains(":")) {
                final String[] split = attribute.split(":");
                try {
                    attr = node.getAttribute(split[1], node.getNamespace(split[0]));
                }
                catch (IllegalArgumentException iaex){
                    return null; // if the namespace hasn't been declared
                }
            } else {
                attr = node.getAttribute(attribute);
            }
            return Optional.ofNullable(attr).isPresent() ? attr.getValue() : null;
        }
    }


    /**
     * Finds an attribute in ANY tag in the given document and returns its value, This ignores namespaces
     */
    public static String getAnyAttributeValue(@NonNull Document doc, @NonNull String attributeName){
        Attribute attr = xpfac.compile("//@*[local-name()='" + attributeName +"']", Filters.attribute()).evaluateFirst(doc);
        if (attr == null) {
            return null;
        }
        return attr.getValue();
    }


    /**
     * Pretty prints an XML string
     */
    public  static String prettyXML(String xmlString) throws IOException, JDOMException {
        final Document document = new SAXBuilder().build(new StringReader(xmlString));
        XMLOutputter outputter = new XMLOutputter();
        outputter.setFormat(Format.getPrettyFormat().setOmitDeclaration(true));
        return outputter.outputString(document);
    }


    /**
     * Converts strings "0" to false, "true"/"false" to true/false, and any other string to true
     */
    public static boolean xmlStrToBool(String val){
        return !(val.trim().equals("0") || val.trim().toLowerCase().equals("false"));
    }


    public static String boolToXmlStr(boolean val){
        return val ? "1" : "0";
    }
}