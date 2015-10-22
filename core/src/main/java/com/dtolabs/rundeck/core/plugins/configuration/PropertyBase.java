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

import java.util.Collections;
import java.util.List;
import java.util.Map;


/**
 * PropertyBase base implementation of Property
 *
 * @author Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
 */
abstract class PropertyBase implements Property {
    private final String title;
    private final String name;
    private final String description;
    private final boolean required;
    private final String defaultValue;
    private final PropertyValidator validator;
    private final PropertyScope scope;
    private final Map<String, Object> renderingOptions;

    public PropertyBase(final String name, final String title, final String description, final boolean required,
                        final String defaultValue, final PropertyValidator validator) {

        this(name, title, description, required, defaultValue, validator, null);
    }

    public PropertyBase(final String name, final String title, final String description, final boolean required,
                        final String defaultValue, final PropertyValidator validator, final PropertyScope scope) {
        this(name, title, description, required, defaultValue, validator, scope, null);
    }

    public PropertyBase(final String name, final String title, final String description, final boolean required,
                        final String defaultValue, final PropertyValidator validator, final PropertyScope scope,
                        final Map<String, Object> renderingOptions) {
        this.title = title;
        this.name = name;
        this.description = description;
        this.required = required;
        this.defaultValue = defaultValue;
        this.validator = validator;
        this.scope = scope;
        this.renderingOptions = renderingOptions == null ? Collections.<String, Object> emptyMap() : Collections
                .unmodifiableMap(renderingOptions);
    }

    public String getTitle() {
        return title;
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

    public PropertyValidator getValidator() {
        return validator;
    }

    public String getName() {
        return name;
    }

    @Override
    public PropertyScope getScope() {
        return scope;
    }
    
    @Override
    public Map<String, Object> getRenderingOptions() {
        return renderingOptions;
    }

    @Override
    public String toString() {
        return "PropertyBase{" +
               "name='" + name + '\'' +
               (title != null ? ", title='" + title + '\'' : "") +
               (description != null ? ", description='" + description + '\'' : "") +
               ", required=" + required +
               (defaultValue != null ? ", defaultValue='" + defaultValue + '\'' : "") +
               (validator != null ? ", validator=" + validator : "") +
               (scope != null ? ", scope=" + scope : "") +
               (renderingOptions != null ? ", renderingOptions=" + renderingOptions : "") +
               '}';
    }
}
