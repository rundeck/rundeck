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
 * Validator is ...
 *
 * @author Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
 */
public class Validator {
    public static class Report {
        private HashMap<String, String> errors = new HashMap<String, String>();

        public HashMap<String, String> getErrors() {
            return errors;
        }
        public boolean isValid() {
            return 0 == errors.size();
        }
    }

    public static Report validate(Properties props, Description desc) {
        final Report report = new Report();
        for (final Property property : desc.getProperties()) {
            final String key = property.getKey();
            final String value = props.getProperty(key);
            if (null == value || "".equals(value)) {
                if (property.isRequired()) {
                    report.errors.put(key, "required");
                    continue;
                }
            } else {
                //try to validate
                final Property.Validator validator = property.getValidator();
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
        return report;
    }
}
