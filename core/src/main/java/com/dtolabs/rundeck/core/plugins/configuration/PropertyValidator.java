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
* PropertyValidator.java
* 
* User: Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
* Created: 11/29/12 5:19 PM
* 
*/
package com.dtolabs.rundeck.core.plugins.configuration;

import java.util.*;


/**
 * Validator can validate a value
 */
public interface PropertyValidator {
    public boolean isValid(String value) throws ValidationException;

    /**
     * Custom validator to check for any property from a defined plugin.
     * @param value Regex to validate
     * @param props Plugin properties
     * @return Boolean indicating validity of presented values
     * @throws ValidationException
     */
    default boolean isValid(String value, Map<String,Object> props) throws ValidationException {
        return isValid(value);
    }
}
