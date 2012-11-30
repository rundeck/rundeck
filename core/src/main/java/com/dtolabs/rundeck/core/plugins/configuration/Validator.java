/*
 * Copyright 2011 DTO Solutions, Inc. (http://dtosolutions.com)
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

/*
* Validator.java
* 
* User: Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
* Created: 7/28/11 2:37 PM
* 
*/
package com.dtolabs.rundeck.core.plugins.configuration;

import java.util.*;

/**
 * Validator utility class can create a validation report for a set of input properties and a configuration
 * description.
 *
 * @author Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
 */
public class Validator {
    /**
     * A validation report
     */
    public static class Report {
        private HashMap<String, String> errors = new HashMap<String, String>();

        /**
         * Return a map of errors, keyed by property name.
         */
        public HashMap<String, String> getErrors() {
            return errors;
        }

        /**
         * Return true if all property values were valid
         */
        public boolean isValid() {
            return 0 == errors.size();
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
        final Report report = new Report();
        final List<Property> properties = desc.getProperties();
        if(null!=properties){
            for (final Property property : properties) {
                final String key = property.getName();
                final String value = props.getProperty(key);
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
                                report.errors.put(key, "Invalid value");
                            }
                        } catch (ValidationException e) {
                            report.errors.put(key, "Invalid value: " + e.getMessage());
                        }
                    }
                }
            }
        }
        return report;
    }

    /**
     * Converts a set of input configuration keys using the description's configuration to property mapping, or the same
     * input if the description has no mapping
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
    private static Map<String, String> performMapping(final Map<String, String> input,
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
     */
    public static Map<String, String> demapProperties(final Map<String, String> input, final Description desc) {
        final Map<String, String> mapping = desc.getPropertiesMapping();
        if (null == mapping) {
            return input;
        }
        final Map<String, String> rev = new HashMap<String, String>();
        for (final Map.Entry<String, String> entry : mapping.entrySet()) {
            rev.put(entry.getValue(), entry.getKey());
        }
        return performMapping(input, rev, true);
    }
}
