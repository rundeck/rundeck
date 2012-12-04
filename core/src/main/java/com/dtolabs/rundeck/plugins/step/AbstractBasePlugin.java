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
* AbstractBasePlugin.java
* 
* User: Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
* Created: 11/29/12 10:52 AM
* 
*/
package com.dtolabs.rundeck.plugins.step;

import com.dtolabs.rundeck.core.execution.workflow.steps.PropertyResolverFactory;
import com.dtolabs.rundeck.core.plugins.Plugin;
import com.dtolabs.rundeck.core.plugins.configuration.ConfigurationException;
import com.dtolabs.rundeck.core.plugins.configuration.Describable;
import com.dtolabs.rundeck.core.plugins.configuration.Description;
import com.dtolabs.rundeck.core.plugins.configuration.Property;
import com.dtolabs.rundeck.core.plugins.configuration.PropertyScope;
import com.dtolabs.rundeck.plugins.descriptions.PluginDescription;
import com.dtolabs.rundeck.plugins.descriptions.PluginProperty;
import com.dtolabs.rundeck.plugins.descriptions.SelectValues;
import com.dtolabs.rundeck.plugins.util.DescriptionBuilder;
import com.dtolabs.rundeck.plugins.util.PropertyBuilder;

import java.lang.reflect.Field;
import java.util.*;


/**
 * AbstractBasePlugin provides base functionality for describable plugin implementations. <p> Subclasses can annotate
 * fields to declare them as configuration properties. </p> <p> Use {@link PluginProperty} to declare a field as a
 * property, and use {@link SelectValues} to annotate a String field to declare multiple select values and whether it is
 * restricted to those values or not. </p> <p> Scope of the input value can be specified with the {@link
 * com.dtolabs.rundeck.plugins.descriptions.PluginProperty#scope()} value.  If not specified, the default scope used is
 * </p> <p> Annotate your subclass with the {@link PluginDescription} to declare a title and description for the plugin
 * type. </p> <p> To add properties programmatically, subclasses can override {@link
 * #buildDescription(com.dtolabs.rundeck.plugins.util.DescriptionBuilder)} and modify the description that is being
 * built at runtime, such as redeclaring property descriptions, titles, select values, and validators. Any properties
 * added that do not correspond to annotated fields will be available via {@link #getExtraConfiguration()} when the
 * plugin is executed.</p>
 *
 * @author Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
 */
public abstract class AbstractBasePlugin implements Describable {

    private Description builtDescription;
    private Map<String, Object> extraConfiguration;

    /**
     * Returns the description built from introspection.
     */
    @Override
    public final Description getDescription() {
        if (null == builtDescription) {
            buildDescription();
        }
        return builtDescription;
    }

    /**
     * Subclasses can override this method to add additional custom properties or modify the automatically generated
     * propeties of the description.
     */
    protected void buildDescription(final DescriptionBuilder builder) {
        //default does nothing
    }

    private void buildDescription() {
        //analyze this class to determine properties
        final DescriptionBuilder builder = DescriptionBuilder.builder();
        final Plugin annotation1 = this.getClass().getAnnotation(Plugin.class);
        if (null == annotation1) {
            return;
        }
        final String pluginName = annotation1.name();
        builder
            .name(pluginName)
            .title(pluginName)
            .description("");

        final PluginDescription descAnnotation = this.getClass().getAnnotation(PluginDescription.class);
        if (null != descAnnotation) {
            final String title = descAnnotation.title();
            builder
                .title(!"".equals(title) ? title : pluginName)
                .description(descAnnotation.description());
        }

        for (final Field field : this.getClass().getDeclaredFields()) {
            final PluginProperty annotation = field.getAnnotation(PluginProperty.class);
            if (null == annotation) {
                continue;
            }
            final Property pbuild = propertyFromField(field, annotation);
            if (null == pbuild) {
                continue;
            }
            builder.property(pbuild);
        }
        buildDescription(builder);
        builtDescription = builder.build();
    }

    private Field fieldForPropertyName(final String name) {
        for (final Field field : this.getClass().getDeclaredFields()) {
            final PluginProperty annotation = field.getAnnotation(PluginProperty.class);
            if (null == annotation) {
                continue;
            }
            if (!"".equals(annotation.name()) && name.equals(annotation.name())) {
                return field;
            } else if ("".equals(annotation.name()) && name.equals(field.getName())) {
                return field;
            }
        }
        return null;
    }

    private Property.Type propertyTypeFromFieldType(Class clazz) {
        if (clazz == Integer.class || clazz == int.class) {
            return Property.Type.Integer;
        } else if (clazz == Long.class || clazz == long.class) {
            return Property.Type.Long;
        } else if (clazz == Boolean.class || clazz == boolean.class) {
            return Property.Type.Boolean;
        } else if (clazz == String.class) {
            return Property.Type.String;
        }
        return null;
    }

    private Property propertyFromField(final Field field, final PluginProperty annotation) {
        final PropertyBuilder pbuild = PropertyBuilder.builder();
        //determine type
        final Property.Type type = propertyTypeFromFieldType(field.getType());
        if (null == type) {
            return null;
        }
        pbuild.type(type);
        if (type == Property.Type.String) {
            //set select/freeselect
            final SelectValues selectAnnotation = field.getAnnotation(SelectValues.class);
            if (null != selectAnnotation) {
                pbuild.type(selectAnnotation.freeSelect() ? Property.Type.FreeSelect : Property.Type.Select);
                pbuild.values(selectAnnotation.values());
            }
        }

        String name = annotation.name();
        if (null == name || "".equals(name)) {
            name = field.getName();
        }
        pbuild.name(name);

        if (null != annotation.title() && !"".equals(annotation.title())) {
            pbuild.title(annotation.title());
        } else {
            pbuild.title(name);
        }

        pbuild.description(annotation.description());

        if (null != annotation.defaultValue() && !"".equals(annotation.defaultValue())) {
            pbuild.defaultValue(annotation.defaultValue());
        }
        pbuild.required(annotation.required());

        pbuild.scope(annotation.scope());

        return pbuild.build();
    }

    private static final List<PropertyScope> instanceScopes = Arrays.asList(PropertyScope.Instance,
                                                                            PropertyScope.InstanceOnly);

    /**
     * Call this method to set field values on the current instance based on the input configuration
     */
    protected final void configureInstanceScopeProperties(final Map<String, Object> configuration) {
        final Map<String, Object> inputConfig = new HashMap<String, Object>();
        for (final Property property : getDescription().getProperties()) {
            if (property.getScope() != null && property.getScope() != PropertyScope.Unspecified) {
                if (!instanceScopes.contains(property.getScope())) {
                    continue;
                }
            }
            final Object value = configuration.get(property.getName());

            if (null == value) {
                continue;
            }
            if (!setValueForProperty(property, value)) {
                inputConfig.put(property.getName(), value);
            }
        }
        this.extraConfiguration = inputConfig;
    }

    /**
     * Subclasses should call this method to set field values on the current instance by mapping the Description
     * Property's to resolved property values
     */
    protected final void configureProperties(final PropertyResolver resolver) {
        //use a default scope of InstanceOnly if the Property doesn't specify it
        final PropertyResolver defaulted = PropertyResolverFactory.withDefaultScope(PropertyScope.InstanceOnly,
                                                                                    resolver);
        final Map<String, Object> inputConfig = new HashMap<String, Object>();
        for (final Property property : getDescription().getProperties()) {
            final Object value = defaulted.resolvePropertyValue(property.getName(), property.getScope());

            if (null == value) {
                continue;
            }
            if (!setValueForProperty(property, value)) {
                inputConfig.put(property.getName(), value);
            }
        }
        this.extraConfiguration = inputConfig;
    }

    /**
     * Set instance field value for the given property, returns true if the field value was set, false otherwise
     */
    private boolean setValueForProperty(Property property, Object value) {
        final Field field = fieldForPropertyName(property.getName());
        if (null == field) {

            return false;
        }
        Property.Type type = property.getType();
        Property.Type ftype = propertyTypeFromFieldType(field.getType());
        if (ftype != property.getType()
            && !(ftype == Property.Type.String
                 && (property.getType() == Property.Type.Select
                     || property.getType() == Property.Type.FreeSelect))) {

            throw new IllegalStateException(
                "cannot map property {" + property.getName() + " type: " + property.getType() + "} to field {"
                + field.getName() + " type: " + ftype + "}");
        }
        final Object resolvedValue;
        if (type == Property.Type.Integer) {
            final Integer intvalue;
            if (value instanceof String) {
                intvalue = Integer.parseInt((String) value);
            } else if (value instanceof Integer) {
                intvalue = (Integer) value;
            } else {
                //XXX
                return false;
            }
            resolvedValue = intvalue;
        } else if (type == Property.Type.Long) {
            final Long longvalue;
            if (value instanceof String) {
                longvalue = Long.parseLong((String) value);
            } else if (value instanceof Long) {
                longvalue = (Long) value;
            } else if (value instanceof Integer) {
                final int val = (Integer) value;
                longvalue = (long) val;
            } else {
                //XXX
                return false;
            }
            resolvedValue = longvalue;
        } else if (type == Property.Type.Boolean) {
            final Boolean boolvalue;
            if (value instanceof String) {
                boolvalue = Boolean.parseBoolean((String) value);
            } else if (value instanceof Boolean) {
                boolvalue = (Boolean) value;
            } else {
                //XXX
                return false;
            }
            resolvedValue = boolvalue;
        } else if (type == Property.Type.String || type == Property.Type.FreeSelect) {
            if (value instanceof String) {
                resolvedValue = value;
            } else {
                //XXX
                return false;
            }
        } else if (type == Property.Type.Select) {
            if (value instanceof String) {
                resolvedValue = value;
                if (!property.getSelectValues().contains((String) resolvedValue)) {
                    throw new RuntimeException(
                        "value not allowed for property " + property.getName() + ": " + resolvedValue);
                }
            } else {
                //XXX
                return false;
            }
        } else {
            //XXX
            return false;
        }
        try {
            setFieldValue(field, resolvedValue);
        } catch (IllegalAccessException e) {
            throw new RuntimeException("Unable to configure plugin: " + e.getMessage(), e);
        }
        return true;
    }

    private void setFieldValue(final Field field, final Object value) throws IllegalAccessException {
        if (!field.isAccessible()) {
            field.setAccessible(true);
        }
        field.set(this, value);
    }

    /**
     * Returns any property input values which were not mapped to fields.
     */
    public Map<String, Object> getExtraConfiguration() {
        return extraConfiguration;
    }
}
