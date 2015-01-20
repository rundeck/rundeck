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
* PluginAdapterUtility.java
* 
* User: Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
* Created: 12/11/12 9:08 AM
* 
*/
package com.dtolabs.rundeck.core.plugins.configuration;

import java.lang.reflect.Field;
import java.util.*;

import com.dtolabs.rundeck.core.common.PropertyRetriever;
import com.dtolabs.rundeck.core.plugins.Plugin;
import com.dtolabs.rundeck.plugins.descriptions.*;
import com.dtolabs.rundeck.plugins.util.DescriptionBuilder;
import com.dtolabs.rundeck.plugins.util.PropertyBuilder;


/**
 * Utility for creating {@link Description}s from Plugin class annotations and setting property values for annotated
 * property fields.
 *
 * @author Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
 */
public class PluginAdapterUtility {

    /**
     * @param object potential plugin object annotated with {@link com.dtolabs.rundeck.core.plugins.Plugin}
     * @return true if the object has a valid Plugin annotation
     */
    public static boolean canBuildDescription(final Object object) {
        final Plugin annotation1 = object.getClass().getAnnotation(Plugin.class);
        return null != annotation1;
    }

    /**
     * @return Create a Description using a builder by analyzing the annotations on a plugin object, and including
     * annotations on fields as DescriptionProperties.
     *
     * @param object  the object
     * @param builder builder
     */
    public static Description buildDescription(final Object object, final DescriptionBuilder builder) {
        return buildDescription(object, builder, true);
    }

    /**
     * @return Create a Description using a builder by analyzing the annotations on a plugin object.
     *
     * @param object  the object
     * @param builder builder
     * @param includeAnnotatedFieldProperties
     *                if true, add DescriptionProperties to the Description based on annotations of fields in the class of the instance
     */
    public static Description buildDescription(final Object object, final DescriptionBuilder builder,
                                               final boolean includeAnnotatedFieldProperties) {
        //analyze this class to determine properties
        final Plugin annotation1 = object.getClass().getAnnotation(Plugin.class);
        if (null != annotation1) {
            final String pluginName = annotation1.name();
            builder
                    .name(pluginName)
                    .title(pluginName)
                    .description("");
        }

        final PluginDescription descAnnotation = object.getClass().getAnnotation(PluginDescription.class);
        if (null != descAnnotation) {
            if (!"".equals(descAnnotation.title())) {
                builder.title(descAnnotation.title());
            }
            if (!"".equals(descAnnotation.description())) {
                builder.description(descAnnotation.description());
            }
        }

        if (includeAnnotatedFieldProperties) {
            for (final Field field : collectFields(object)) {
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
        }
        builder.collaborate(object);
        return builder.build();
    }

    private static Field fieldForPropertyName(final String name, final Object object) {
        for (final Field field : collectFields(object)) {
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

    private static Collection<Field> collectFields(final Object object){
        ArrayList<Field> fields = new ArrayList<Field>();
        Class<?> clazz = object.getClass();
        do{
          fields.addAll(Arrays.asList(clazz.getDeclaredFields()));
          clazz = clazz.getSuperclass();
        }
        while(clazz != Object.class);
        return fields;
    }

    private static Property.Type propertyTypeFromFieldType(final Class clazz) {
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

    private static Property propertyFromField(final Field field, final PluginProperty annotation) {
        final PropertyBuilder pbuild = PropertyBuilder.builder();
        //determine type
        final Property.Type type = propertyTypeFromFieldType(field.getType());
        if (null == type) {
            return null;
        }
        pbuild.type(type);
        if (type == Property.Type.String) {
            StringRenderingConstants.DisplayType renderBehaviour = StringRenderingConstants.DisplayType.SINGLE_LINE;
            //set select/freeselect
            final SelectValues selectAnnotation = field.getAnnotation(SelectValues.class);
            if (null != selectAnnotation) {
                pbuild.type(selectAnnotation.freeSelect() ? Property.Type.FreeSelect : Property.Type.Select);
                pbuild.values(selectAnnotation.values());
            }
            
            if (field.getAnnotation(TextArea.class) != null) {
                renderBehaviour = StringRenderingConstants.DisplayType.MULTI_LINE;
            }

            if (field.getAnnotation(Password.class) != null) {
                renderBehaviour = StringRenderingConstants.DisplayType.PASSWORD;
            }

            pbuild.renderingOption(StringRenderingConstants.DISPLAY_TYPE_KEY, renderBehaviour);
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
     * Set field values on a plugin object by using annotated field values to create a Description, and setting field
     * values to resolved property values. Any resolved properties that are not mapped to a field will  be included in
     * the return result.
     *
     * @param resolver property resolver
     * @param object plugin object
     * @return Map of resolved properties that were not configured in the object's fields
     */
    public static Map<String, Object> configureProperties(final PropertyResolver resolver,
                                                          final Object object) {
        //use a default scope of InstanceOnly if the Property doesn't specify it
        return configureProperties(resolver, buildDescription(object, DescriptionBuilder.builder()), object,
                PropertyScope.InstanceOnly);
    }

    /**
     * Set field values on a plugin object by using a Description, and setting field values to resolved property values.
     * Any resolved properties that are not mapped to a field will  be included in the return result.
     *
     * @param resolver the property resolver
     * @param description the property descriptions
     * @param object the target object, which can implement {@link Configurable}, otherwise introspection will be used
     * @param defaultScope a default property scope to assume for unspecified properties
     *
     * @return Map of resolved properties that were not configured in the object's fields
     */
    public static Map<String, Object> configureProperties(final PropertyResolver resolver,
            final Description description,
            final Object object, PropertyScope defaultScope) {
        final Map<String, Object> inputConfig = mapDescribedProperties(resolver, description, defaultScope);
        if (object instanceof Configurable) {
            Configurable configObject = (Configurable) object;
            Properties configuration = new Properties();
            configuration.putAll(inputConfig);
            try {
                configObject.configure(configuration);
            } catch (ConfigurationException e) {

            }
        } else {
            for (final Property property : description.getProperties()) {
                if (null != inputConfig.get(property.getName())) {
                    if (setValueForProperty(property, inputConfig.get(property.getName()), object)) {
                        inputConfig.remove(property.getName());
                    }
                }
            }
        }
        return inputConfig;
    }

    private static class PropertyDefaultValues implements PropertyRetriever {
        private Map<String, String> properties;

        private PropertyDefaultValues(final List<Property> properties1) {
            properties = new HashMap<String, String>();
            for (final Property property : properties1) {
                if (null != property.getDefaultValue()) {
                    properties.put(property.getName(), property.getDefaultValue());
                }

            }
        }

        public String getProperty(final String name) {
            return properties.get(name);
        }

    }

    /**
     * Retrieve the Description's Properties mapped to resolved values given the resolver, using InsanceOnly default scope.
     * @param resolver property resolver
     * @param description plugin description
     * @return All mapped properties by name and value.
     */
    public static Map<String, Object> mapDescribedProperties(final PropertyResolver resolver,
                                                             final Description description) {
        //use a default scope of InstanceOnly if the Property doesn't specify it
        //use property default value if otherwise not resolved
        return mapDescribedProperties(resolver, description, null);
    }

    /**
     * Retrieve the Description's Properties mapped to resolved values given the resolver, with a default property
     * scope
     * @param resolver property resolver
     * @param description plugin description
     * @param defaultPropertyScope default scope for unspecified property scopes
     *
     * @return All mapped properties by name and value.
     */
    public static Map<String, Object> mapDescribedProperties(final PropertyResolver resolver,
            final Description description, final PropertyScope defaultPropertyScope) {
        final List<Property> properties = description.getProperties();
        final PropertyResolver defaulted =
                PropertyResolverFactory.withDefaultValues(
                        PropertyResolverFactory.withDefaultScope(null != defaultPropertyScope ? defaultPropertyScope
                                : PropertyScope.InstanceOnly, resolver),
                        new PropertyDefaultValues(properties)
                );

        return PropertyResolverFactory.mapPropertyValues(properties, defaulted);
    }

    /**
     * Set instance field value for the given property, returns true if the field value was set, false otherwise
     */
    private static boolean setValueForProperty(final Property property, final Object value, final Object object) {
        final Field field = fieldForPropertyName(property.getName(), object);
        if (null == field) {
            return false;
        }
        final Property.Type type = property.getType();
        final Property.Type ftype = propertyTypeFromFieldType(field.getType());
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
            setFieldValue(field, resolvedValue, object);
        } catch (IllegalAccessException e) {
            throw new RuntimeException("Unable to configure plugin: " + e.getMessage(), e);
        }
        return true;
    }

    private static void setFieldValue(final Field field, final Object value, final Object object)
            throws IllegalAccessException {
        if (!field.isAccessible()) {
            field.setAccessible(true);
        }
        field.set(object, value);
    }
}
