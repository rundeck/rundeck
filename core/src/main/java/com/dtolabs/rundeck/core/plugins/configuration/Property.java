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
 * Property describes a configuration property of a provider
 *
 * @author Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
 */
public interface Property {
    /**
     * Available property types
     */
    static enum Type {
        /**
         * A simple string input property
         */
        String,
        /**
         * A boolean input property
         */
        Boolean,
        /**
         * An integer input property
         */
        Integer,
        /**
         * A long input property
         */
        Long,
        /**
         * A multiple selection input property
         */
        Select,
        /**
         * A string input property with a select
         */
        FreeSelect,
//        MultiSelect,
//        MultiFreeSelect
    }
    static enum Scope{
        Framework,
        Project,
        Instance
    }

    /**
     * Return descriptive name of the property
     */
    public String getTitle();

    /**
     * Return property key to use
     */
    public String getName();

    /**
     * Return description of the values of the property
     */
    public String getDescription();

    /**
     * Return the property type
     */
    public Type getType();

    /**
     * Return the validator for this property
     */
    public PropertyValidator getValidator();

    /**
     * Return true if an empty value is not allowed
     */
    public boolean isRequired();

    /**
     * Return the default value of the property, or default select value to select
     */
    public String getDefaultValue();

    /**
     * Return a list of values for a select property
     */
    public List<String> getSelectValues();

}
