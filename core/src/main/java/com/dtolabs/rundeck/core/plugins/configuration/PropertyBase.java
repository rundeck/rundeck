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
class PropertyBase implements Property {
    private final String              title;
    private final String              name;
    private final String              description;
    private final boolean             required;
    private final String              defaultValue;
    private final PropertyValidator   validator;
    private final PropertyScope       scope;
    private final Map<String, Object> renderingOptions;
    private final Type                type;
    private final ValuesGenerator     valuesGenerator;
    private final Class<?>            embeddedType;
    private final Class<?>            embeddedPluginType;
    private final List<String>        selectValues;
    private final Map<String, String> selectLabels;

    public PropertyBase(Property base) {
        this(
            base.getType(),
            base.getName(),
            base.getTitle(),
            base.getDescription(),
            base.isRequired(),
            base.getDefaultValue(),
            base.getSelectValues(),
            base.getSelectLabels(),
            base.getValidator(),
            base.getValuesGenerator(),
            base.getScope(),
            base.getRenderingOptions(),
            base.getEmbeddedType(),
            base.getEmbeddedPluginType()
        );
    }

    public PropertyBase(
        final Type type,
        final String name,
        final String title,
        final String description,
        final boolean required,
        final String defaultValue,
        final List<String> selectValues,
        final Map<String, String> selectLabels,
        final PropertyValidator validator,
        final ValuesGenerator valuesGenerator,
        final PropertyScope scope,
        final Map<String, Object> renderingOptions,
        final Class<?> embeddedType,
        final Class<?> embeddedPluginType
    ) {
        this.type = type;
        this.title = title;
        this.name = name;
        this.description = description;
        this.required = required;
        this.defaultValue = defaultValue;
        this.validator = validator;
        this.valuesGenerator = valuesGenerator;
        this.scope = scope;
        this.selectValues = selectValues;
        this.selectLabels = selectLabels;
        this.renderingOptions = renderingOptions == null ? Collections.<String, Object> emptyMap() : Collections
                .unmodifiableMap(renderingOptions);
        this.embeddedType = embeddedType;
        this.embeddedPluginType = embeddedPluginType;
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
        return selectValues;
    }

    @Override
    public Map<String, String> getSelectLabels() {
        return selectLabels;
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
    public Type getType() {
        return type;
    }

    @Override
    public ValuesGenerator getValuesGenerator() {
        return valuesGenerator;
    }

    @Override
    public Class<?> getEmbeddedType() {
        return embeddedType;
    }

    @Override
    public Class<?> getEmbeddedPluginType() {
        return embeddedPluginType;
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
               (embeddedType != null ? ", embeddedType=" + embeddedType : "") +
               (embeddedPluginType != null ? ", embeddedPluginType=" + embeddedPluginType : "") +
               '}';
    }
}
