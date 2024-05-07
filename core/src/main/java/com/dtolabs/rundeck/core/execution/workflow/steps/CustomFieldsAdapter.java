package com.dtolabs.rundeck.core.execution.workflow.steps;

import com.dtolabs.rundeck.core.plugins.configuration.Description;
import com.dtolabs.rundeck.core.plugins.configuration.Property;
import com.dtolabs.rundeck.core.plugins.configuration.StringRenderingConstants;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Data;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Assists in converting plugin property values with DYNAMIC_FORM rendering option, so that variable expansion can be
 * done within the field values of the JSON structure.
 */
public class CustomFieldsAdapter {
    Description description;
    Set<String> dynamicFormProperties = new HashSet<>();
    Map<String, CustomFields> tempCustomFields = new HashMap<>();

    /**
     * Create a new instance
     *
     * @param description description
     * @return instance
     */
    public static CustomFieldsAdapter create(final Description description) {
        return new CustomFieldsAdapter(description);
    }

    /**
     * Represents a custom field entry
     */
    @Data
    public static class CustomField {
        private String key;
        @JsonInclude(JsonInclude.Include.NON_ABSENT)
        private String label;
        private String value;
        @JsonInclude(JsonInclude.Include.NON_ABSENT)
        private String desc;
    }

    /**
     * Represents a list of custom fields, and contains a Map that holds the key/value pairs for easy lookup
     */
    public static class CustomFields {
        Map<String, String> keyedValues = new HashMap<>();
        List<CustomField> fields;

        public CustomFields(final List<CustomField> fields) {
            this.fields = fields;
            this.fields.forEach(f -> keyedValues.put(f.getKey(), f.getValue()));
        }

        public void update(Map<String, String> converted) {
            fields.forEach(f -> f.setValue(converted.get(f.getKey())));
        }
    }

    private CustomFieldsAdapter(final Description description) {
        this.description = description;
        if (description != null) {
            dynamicFormProperties =
                    description.getProperties()
                            .stream()
                            .filter(CustomFieldsAdapter::isDynamicFormProperty)
                            .map(Property::getName)
                            .collect(Collectors.toCollection(HashSet::new))
            ;
        }
    }

    private static boolean isDynamicFormProperty(final Property p) {
        Object displayType = p.getRenderingOptions().get(StringRenderingConstants.DISPLAY_TYPE_KEY);
        return StringRenderingConstants.DisplayType.DYNAMIC_FORM.equals(displayType) ||
                "DYNAMIC_FORM".equals(displayType);
    }

    /**
     * Given a map of converted values, replace the values in the CustomFields object and return the JSON serialized
     *
     * @param customFields CustomFields object
     * @param converted    converted values
     * @return json serialized string of new value, or null if there is an error
     */
    public static String replaceConvertedCustomFieldValues(CustomFields customFields, Map<String, String> converted) {
        customFields.update(converted);
        ObjectMapper objectMapper = new ObjectMapper();
        try {
            return objectMapper.writeValueAsString(customFields.fields);
        } catch (JsonProcessingException e) {
            return null;
        }
    }

    /**
     * Parse the input as a JSON representation of an array of CustomFields
     *
     * @param input input string
     * @return CustomFields object, or null if there is an error
     */
    public static CustomFields convertJsonCustomFieldValues(String input) {
        ObjectMapper objectMapper = new ObjectMapper();
        try {
            List<CustomField> list = objectMapper.readValue(
                    input,
                    new TypeReference<List<CustomField>>() {
                    }
            );
            return new CustomFields(list);
        } catch (JsonProcessingException e) {
            return null;
        }
    }

    /**
     * Given property key, and input value, convert the value if it is a JSON representation of an array of CustomFields
     *
     * @param key   property key
     * @param input input value
     * @return converted value, or original value if not converted
     */
    public Object convertInput(String key, Object input) {
        if (isDynamicFormProperty(key) && input instanceof String) {
            CustomFields o = convertJsonCustomFieldValues((String) input);
            if (o == null) {
                return input;
            }
            tempCustomFields.put(key, o);
            return o.keyedValues;
        }
        return input;

    }

    /**
     * Given property key, and output value, restore the value back to JSON string
     *
     * @param key    property key
     * @param output output value
     * @return json serialized string of new value, or original value if not converted
     */
    public Object convertOutput(String key, Object output) {
        if (isDynamicFormProperty(key) && output instanceof Map && tempCustomFields.containsKey(key)) {
            return replaceConvertedCustomFieldValues(tempCustomFields.get(key), (Map<String, String>) output);
        }
        return output;
    }

    private Boolean isDynamicFormProperty(final String key) {
        return dynamicFormProperties.contains(key);
    }
}
