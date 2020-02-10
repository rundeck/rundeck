/*
 * Copyright 2016 SimplifyOps, Inc. (http://simplifyops.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/*
* Validator.java
* 
* User: Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
* Created: 7/28/11 2:37 PM
* 
*/
package com.dtolabs.rundeck.core.plugins.configuration;

import com.dtolabs.rundeck.plugins.descriptions.PluginCustomConfig;
import org.apache.commons.lang.StringUtils;

import java.lang.reflect.InvocationTargetException;
import java.util.*;

/**
 * Validator utility class can create a validation report for a set of input properties and a configuration
 * description.
 *
 * @author Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
 */
public class Validator {
    /**
     * Return a report for a single error item
     * @param key key
     * @param message message
     * @return report
     */
    public static Report errorReport(String key, String message) {
        return buildReport().error(key, message).build();
    }
    /**
     *
     * @return new Builder for Report
     */
    public static ReportBuilder buildReport() {
        return new ReportBuilder();
    }
    /**
     * Builder for {@link com.dtolabs.rundeck.core.plugins.configuration.Validator.Report}
     */
    public static class ReportBuilder {
        Report report = new Report();

        public ReportBuilder errors(Map<String, String> errors) {
            report.errors.putAll(errors);
            return this;
        }
        public ReportBuilder error(String key, String message) {
            report.errors.put(key, message);
            return this;
        }

        public Report build() {
            return report;
        }
    }
    /**
     * A validation report
     */
    public static class Report {
        private HashMap<String, String> errors = new HashMap<String, String>();

        /**
         * @return a map of errors, keyed by property name.
         */
        public HashMap<String, String> getErrors() {
            return errors;
        }

        /**
         * @return true if all property values were valid
         */
        public boolean isValid() {
            return 0 == errors.size();
        }

        @Override
        public String toString() {
            if(isValid()) {
                return "Property validation OK.";
            }else{
                return "Property validation FAILED. " +
                        "errors=" + errors ;
            }
        }
    }

    /**
     * Validate a set of properties for a description, and return a report.
     *
     * @param props the input properties
     * @param desc  the configuration description
     *
     * @return the validation report
     */
    public static Report validate(final Properties props, final Description desc) {
        final List<Property> properties = desc.getProperties();
        return validate(props, properties);
    }

    /**
     * Validate a set of properties for a description, and return a report.
     *
     * @param props the input properties
     * @param properties  the properties
     *
     * @return the validation report
     */
    public static Report validate(final Properties props, final List<Property> properties) {
        final Report report = new Report();
        validate(props, report, properties, null);
        return report;
    }

    /**
     * Validate a set of properties for a description, and return a report.
     *
     * @param props the input properties as a Map
     * @param properties  the properties
     *
     * @return the validation report
     */
    public static Report validate(final Map<String, String> props, final List<Property> properties) {
        final Report report = new Report();
        validate(props, report, properties, null);
        return report;
    }


    /**
     * Validate, ignoring properties below a scope, if set
     *
     * @param props        input properties
     * @param report       report
     * @param properties   property definitions
     * @param ignoredScope ignore scope
     */
    private static void validate(
            Properties props,
            Report report,
            List<Property> properties,
            PropertyScope ignoredScope
    )
    {
        validate(toStringMap(props), report, properties, ignoredScope);
    }

    private static Map<String, String> toStringMap(final Properties props) {
        HashMap<String, String> map = new HashMap<>();
        for (String stringPropertyName : props.stringPropertyNames()) {
            map.put(stringPropertyName, props.getProperty(stringPropertyName));
        }
        return map;
    }

    private static void validate(Map<String,String> props, Report report, List<Property> properties, PropertyScope ignoredScope) {
        if(null!=properties){
            for (final Property property : properties) {
                if (null != ignoredScope && property.getScope() != null
                        && property.getScope().compareTo(ignoredScope) <= 0) {
                    continue;
                }
                final String key = property.getName();
                final String value = props.get(key);
                if (null == value || "".equals(value)) {
                    if (property.isRequired()) {
                        report.errors.put(key, "required");
                        continue;
                    }
                } else {
                    //try to validate
                    final PropertyValidator validator = property.getValidator();
                    if (null != validator) {
                        try {
                            if (!validator.isValid(value)) {
                                report.errors.put(key, "Invalid value: " + value);
                            }
                        } catch (ValidationException e) {
                            report.errors.put(key, "Invalid value: " + e.getMessage());
                        }
                    }
                }
            }
        }
    }

    /**
     * Validate a set of properties for a description, and return a report.
     *
     * @param resolver     property resolver
     * @param description  description
     * @param defaultScope default scope for properties
     *
     * @return the validation report
     */
    public static Report validate(final PropertyResolver resolver, final Description description,
            PropertyScope defaultScope) {
        return validate(resolver, description, defaultScope, null);
    }

    /**
     * Validate a set of properties for a description, and return a report.
     *
     * @param resolver     property resolver
     * @param description  description
     * @param defaultScope default scope for properties
     * @param ignoredScope ignore properties at or below this scope, or null to ignore none
     * @return the validation report
     */
    public static Report validate(final PropertyResolver resolver, final Description description,
            PropertyScope defaultScope, PropertyScope ignoredScope) {
        final Report report = new Report();
        final List<Property> properties = description.getProperties();

        final Map<String, Object> inputConfig = PluginAdapterUtility.mapDescribedProperties(resolver, description,
                defaultScope);
        validate(asProperties(inputConfig), report, properties, ignoredScope);
        return report;
    }

    /**
     * Validate a set of properties for a description, and return a report.
     *
     * @param resolver     property resolver
     * @param properties   list of properties
     * @param defaultScope default scope for properties
     * @param ignoredScope ignore properties at or below this scope, or null to ignore none
     *
     * @return the validation report
     */
    public static Report validateProperties(
            final PropertyResolver resolver,
            final List<Property> properties,
            PropertyScope defaultScope,
            PropertyScope ignoredScope
    )
    {
        final Report report = new Report();

        final Map<String, Object> inputConfig = PluginAdapterUtility.mapProperties(
                resolver, properties, defaultScope
        );
        validate(asProperties(inputConfig), report, properties, ignoredScope);
        return report;
    }

    private static Properties asProperties(Map<String, Object> inputConfig) {
        Properties configuration = new Properties();
        configuration.putAll(inputConfig);
        return configuration;
    }

    /**
     * Converts a set of input configuration keys using the description's configuration to property mapping, or the same
     * input if the description has no mapping
     * @param input input map
     * @param desc plugin description
     * @return mapped values
     */
    public static Map<String, String> mapProperties(final Map<String, String> input, final Description desc) {
        final Map<String, String> mapping = desc.getPropertiesMapping();
        if (null == mapping) {
            return input;
        }
        return performMapping(input, mapping, false);
    }

    /**
     * Convert input keys via the supplied mapping.
     * @param input data
     * @param mapping map to convert key names
     * @param skip if true, ignore input entries when the key is not present in the mapping
     */
    public static Map<String, String> performMapping(final Map<String, String> input,
                                                      final Map<String, String> mapping, final boolean skip) {

        final Map<String, String> props = new HashMap<String, String>();

        for (final Map.Entry<String, String> entry : input.entrySet()) {
            if (null != mapping.get(entry.getKey())) {
                props.put(mapping.get(entry.getKey()), entry.getValue());
            } else if(!skip) {
                props.put(entry.getKey(), entry.getValue());
            }

        }
        return props;
    }

    /**
     * Reverses a set of properties mapped using the description's configuration to property mapping, or the same input
     * if the description has no mapping
     * @param input input map
     * @param desc plugin description
     * @return mapped values
     */
    public static Map<String, String> demapProperties(final Map<String, String> input, final Description desc) {
        final Map<String, String> mapping = desc.getPropertiesMapping();
        return demapProperties(input, mapping, true);
    }

    /**
     * Reverses a set of properties mapped using the specified property mapping, or the same input
     * if the description has no mapping
     * @param input input map
     * @param mapping key value mapping
     * @param skip if true, ignore input entries when the key is not present in the mapping
     * @return mapped values
     */
    public static Map<String, String> demapProperties(
            final Map<String, String> input,
            final Map<String, String> mapping,
            final boolean skip
    )
    {
        if (null == mapping) {
            return input;
        }
        final Map<String, String> rev = new HashMap<String, String>();
        for (final Map.Entry<String, String> entry : mapping.entrySet()) {
            rev.put(entry.getValue(), entry.getKey());
        }
        return performMapping(input, rev, skip);
    }

    public static PluginCustomConfigValidator createCustomPropertyValidator(PluginCustomConfig annotation) {
        if(annotation.validator() == PluginCustomConfigValidator.class) return null;
        try {
            return annotation.validator().getDeclaredConstructor().newInstance();
        } catch (NoSuchMethodException | InvocationTargetException |
                IllegalAccessException | InstantiationException e) {
            e.printStackTrace();
        }

        return null;
    }
}
