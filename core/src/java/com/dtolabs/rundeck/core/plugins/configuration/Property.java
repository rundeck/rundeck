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
* Property.java
* 
* User: Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
* Created: 7/27/11 4:53 PM
* 
*/
package com.dtolabs.rundeck.core.plugins.configuration;

import java.util.List;

/**
 * Property is ...
 *
 * @author Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
 */
public interface Property {
    static enum Type {
        String,
        Boolean,
        Integer,
        Long,
        Select,
        FreeSelect,
//        MultiSelect,
//        MultiFreeSelect
    }

    static interface Validator {
        public boolean isValid(String value) throws ValidationException;
    }

    /**
     * Return descriptive name of the property
     */
    public String getName();

    /**
     * Return property key to use
     */
    public String getKey();

    /**
     * Return description of the values of the property
     */
    public String getDescription();

    public Type getType();

    public Validator getValidator();

    public boolean isRequired();

    public String getDefaultValue();
    public List<String> getSelectValues();

}
