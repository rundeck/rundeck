/*
 * Copyright 2012 DTO Labs, Inc. (http://dtolabs.com)
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
 *
 */

/*
* DescriptionBuilder.java
* 
* User: Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
* Created: 11/21/12 10:47 AM
* 
*/
package com.dtolabs.rundeck.plugins.util;

import com.dtolabs.rundeck.core.plugins.configuration.Description;
import com.dtolabs.rundeck.core.plugins.configuration.Property;
import com.dtolabs.rundeck.core.plugins.configuration.PropertyUtil;

import java.util.*;


/**
 * DescriptionBuilder is a builder for creating a Description object.
 *
 * @author Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
 */
public class DescriptionBuilder {
    private ArrayList<Property> properties;
    private HashMap<String, String> mapping;
    private String name;
    private String title;
    private String description;

    private DescriptionBuilder() {
        properties = new ArrayList<Property>();
        mapping = new HashMap<String, String>();
    }

    private DescriptionBuilder(final Description original) {
        properties = new ArrayList<Property>(original.getProperties());
        mapping = new HashMap<String, String>(original.getPropertiesMapping());
        this.name = original.getName();
        this.title = original.getTitle();
        this.description = original.getDescription();
    }

    /**
     * Start a builder
     */
    public static DescriptionBuilder builder() {
        return new DescriptionBuilder();
    }

    /**
     * Start a builder with a given description
     */
    public static DescriptionBuilder builder(final Description original) {
        return new DescriptionBuilder(original);
    }

    /**
     * Set the name
     */
    public DescriptionBuilder name(final String name) {
        this.name = name;
        return this;
    }

    /**
     * Set a title string
     */
    public DescriptionBuilder title(final String title) {
        this.title = title;
        return this;
    }

    /**
     * Set a description string
     */
    public DescriptionBuilder description(final String description) {
        this.description = description;
        return this;
    }

    /**
     * Add a property mapping
     */
    public DescriptionBuilder mapping(final String key, final String name) {
        mapping.put(key, name);
        return this;
    }

    /**
     * Add a String property
     */
    public DescriptionBuilder stringProperty(final String name,
                                             final String defaultValue,
                                             final boolean required,
                                             final String propTitle, final String propDescription) {
        properties.add(PropertyUtil.string(name, propTitle, propDescription, required, defaultValue));
        return this;
    }

    /**
     * Add a Boolean property
     */
    public DescriptionBuilder booleanProperty(final String name,
                                              final String defaultValue,
                                              final boolean required,
                                              final String propTitle, final String propDescription) {
        properties.add(PropertyUtil.bool(name, propTitle, propDescription, required, defaultValue));
        return this;
    }

    /**
     * Add an Integer property
     */
    public DescriptionBuilder integerProperty(final String name,
                                              final String defaultValue,
                                              final boolean required,
                                              final String propTitle, final String propDescription) {
        properties.add(PropertyUtil.integer(name, propTitle, propDescription, required, defaultValue));
        return this;
    }

    /**
     * Add a Select property
     */
    public DescriptionBuilder selectProperty(final String name,
                                             final String defaultValue,
                                             final boolean required,
                                             final String propTitle,
                                             final String propDescription, final List<String> selectValues) {
        properties.add(PropertyUtil.select(name, propTitle, propDescription, required, defaultValue, selectValues));
        return this;
    }

    /**
     * Add a FreeSelect property
     */
    public DescriptionBuilder freeSelectProperty(final String name,
                                                 final String defaultValue,
                                                 final boolean required,
                                                 final String propTitle,
                                                 final String propDescription, final List<String> selectValues) {
        properties.add(PropertyUtil.freeSelect(name, propTitle, propDescription, required, defaultValue, selectValues));
        return this;
    }
    public DescriptionBuilder property(final Property property) {
        properties.add(property);
        return this;
    }

    /**
     * Remove a previously defined property by name
     */
    public DescriptionBuilder removeProperty(final String name) {
        Property found = null;
        for (final Property property : properties) {
            if (property.getName().equals(name)) {
                found = property;
                break;
            }
        }
        if (null != found) {
            properties.remove(found);
        }
        return this;
    }

    /**
     * Build the description
     */
    public Description build() {
        if (null == name) {
            throw new IllegalStateException("name is not set");
        }
        final String title1 = null != title ? title : name;
        final List<Property> properties1 = Collections.unmodifiableList(properties);
        final Map<String, String> mapping1 = Collections.unmodifiableMap(mapping);
        return new Description() {
            @Override
            public String getName() {
                return name;
            }

            @Override
            public String getTitle() {
                return title1;
            }

            @Override
            public String getDescription() {
                return description;
            }

            @Override
            public List<Property> getProperties() {
                return properties1;
            }

            @Override
            public Map<String, String> getPropertiesMapping() {
                return mapping1;
            }
        };
    }
}
