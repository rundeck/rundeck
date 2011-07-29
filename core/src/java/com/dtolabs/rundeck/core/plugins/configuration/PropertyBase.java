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
* PropertyBase.java
* 
* User: Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
* Created: 7/27/11 5:42 PM
* 
*/
package com.dtolabs.rundeck.core.plugins.configuration;

import java.util.List;

/**
* PropertyBase is ...
*
* @author Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
*/
public abstract class PropertyBase implements Property {
    private final String name;
    private final String key;
    private final String description;
    private final boolean required;
    private final String defaultValue;
    private final Validator validator;

    public PropertyBase(final String key, final String name, final String description, final boolean required,
                        final String defaultValue, final Validator validator) {

        this.name = name;
        this.key = key;
        this.description = description;
        this.required = required;
        this.defaultValue = defaultValue;
        this.validator = validator;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public boolean isRequired() {
        return required;
    }

    public String getDefaultValue() {
        return defaultValue;
    }

    public List<String> getSelectValues() {
        return null;
    }

    public Validator getValidator() {
        return validator;
    }

    public String getKey() {
        return key;
    }
}
