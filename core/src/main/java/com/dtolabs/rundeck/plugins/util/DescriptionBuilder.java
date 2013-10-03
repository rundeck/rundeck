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
     * Add a property mapping
     */
    public DescriptionBuilder mapping(final Map<String,String> mapping) {
        this.mapping.putAll(mapping);
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

    /**
     * Add a new property, or replace an existing property with the same name by passing in a builder.
     */
    public DescriptionBuilder property(final PropertyBuilder property) {
        replaceOrAddProperty(property.build());
        return this;
    }

    /**
     * Add a new property, or replace an existing property with the same name.
     */
    public DescriptionBuilder property(final Property property) {
        replaceOrAddProperty(property);
        return this;
    }

    /**
     * Remove a previously defined property by name
     */
    public DescriptionBuilder removeProperty(final String name) {
        final Property found = findProperty(name);
        if (null != found) {
            properties.remove(found);
        }
        return this;
    }

    private Property findProperty(String name) {
        for (final Property property : properties) {
            if (property.getName().equals(name)) {
                return property;
            }
        }
        return null;
    }

    private void replaceOrAddProperty(Property p) {

        for (final Property property : properties) {
            if (property.getName().equals(p.getName())) {
                properties.set(properties.indexOf(property), p);
                return;
            }
        }
        properties.add(p);
    }

    /**
     * Returns a new {@link PropertyBuilder} preconfigured with an existing property or a new one to add a new property.
     * Be sure to call {@link #property(com.dtolabs.rundeck.core.plugins.configuration.Property)} to add the result of
     * the final call to {@link com.dtolabs.rundeck.plugins.util.PropertyBuilder#build()}.
     */
    public PropertyBuilder property(final String name) {
        final Property found = findProperty(name);
        if (null != found) {
            return PropertyBuilder.builder(found);
        } else {
            return PropertyBuilder.builder().name(name);
        }
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

            @Override
            public String toString() {
                return "PropertyDescription{" +
                        "name = " + getName() + ", " +
                        "title = " + getTitle() + ", " +
                        "description = " + getDescription() + ", " +
                        "properties = " + getProperties() + ", " +
                        "mapping = " + getPropertiesMapping() +
                        "}";
            }
        };
    }

    /**
     * Allows the Collaborator to assist using this DescriptionBuilder.
     */
    public DescriptionBuilder collaborate(final Collaborator colab) {
        colab.buildWith(this);
        return this;
    }

    /**
     * Allows any object to collaborate on this DescriptionBuilder if it implements {@link Collaborator}, otherwise
     * simply returns this builder.
     */
    public DescriptionBuilder collaborate(final Object colab) {
        if (colab instanceof Collaborator) {
            return collaborate((Collaborator) colab);
        }
        return this;
    }

    /**
     * Allows a class to collaborate in building a Description using a DescriptionBuilder
     */
    public static interface Collaborator {
        /**
         * visit the builder and do some work
         */
        public void buildWith(DescriptionBuilder builder);
    }
}
