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
* PluginAdapterUtility.java
* 
* User: Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
* Created: 12/11/12 9:08 AM
* 
*/
package com.dtolabs.rundeck.core.plugins.configuration;

import com.dtolabs.rundeck.core.common.PropertyRetriever;
import com.dtolabs.rundeck.core.plugins.MultiPluginProviderLoader;
import com.dtolabs.rundeck.core.plugins.Plugin;
import com.dtolabs.rundeck.plugins.descriptions.*;
import com.dtolabs.rundeck.plugins.util.DescriptionBuilder;
import com.dtolabs.rundeck.plugins.util.PropertyBuilder;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.util.*;


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
            buildFieldProperties(object, builder);
        }
        builder.collaborate(object);
        return builder.build();
    }

    /**
     * Return the list of properties by introspecting the annotated fields for {@link PluginProperty}
     * @param object object
     * @return list of properties, may be empty
     */
    public static List<Property> buildFieldProperties(final Object object) {
        return buildFieldProperties(object.getClass());
    }

    /**
     * Return the list of properties by introspecting the annotated fields for {@link PluginProperty}
     * @param aClass class
     * @return list of properties, may be empty
     */
    public static List<Property> buildFieldProperties(final Class<?> aClass) {
        DescriptionBuilder builder = DescriptionBuilder.builder();
        buildFieldProperties(aClass, builder);
        return builder.buildProperties();
    }

    /**
     * Add properties based on introspection of the object
     * @param object object
     * @param builder builder
     */
    public static void buildFieldProperties(final Object object, final DescriptionBuilder builder) {
        buildFieldProperties(object.getClass(), builder);
    }

    /**
     * Add properties based on introspection of a class
     * @param aClass class
     * @param builder builder
     */
    public static void buildFieldProperties(final Class<?> aClass, final DescriptionBuilder builder) {
        for (final Field field : collectClassFields(aClass)) {
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
        return collectClassFields(object.getClass());
    }

    private static Collection<Field> collectClassFields(final Class<?> aClass) {
        ArrayList<Field> fields = new ArrayList<Field>();
        Class<?> clazz = aClass;
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
        } else if (clazz == String[].class || Set.class.isAssignableFrom(clazz) || List.class.isAssignableFrom(clazz)) {
            return Property.Type.Options;
        } else if ( Map.class.isAssignableFrom(clazz) ) {
            return Property.Type.Map;
        }
        return null;
    }

    private static Property propertyFromField(final Field field, final PluginProperty annotation) {
        final PropertyBuilder pbuild = PropertyBuilder.builder();
        //determine type
        Property.Type type = propertyTypeFromFieldType(field.getType());
        final EmbeddedPluginProperty embedPluginAnnotation = field.getAnnotation(EmbeddedPluginProperty.class);
        final EmbeddedTypeProperty embedTypeAnnotation = field.getAnnotation(EmbeddedTypeProperty.class);

        if (null == type) {
            if (embedTypeAnnotation != null) {
                //embed an object of the given type
                type = Property.Type.Embedded;
                pbuild.embeddedType(field.getType());
            } else if (embedPluginAnnotation != null) {
                type = Property.Type.Embedded;
                pbuild.embeddedPluginType(field.getType());
            } else {
                return null;
            }
        }
        pbuild.type(type);
        if (type == Property.Type.Options) {
            final SelectValues selectAnnotation = field.getAnnotation(SelectValues.class);
            if (null != selectAnnotation) {
                String[] values = selectAnnotation.values();
                pbuild.values(values);

                extractSelectLabels(pbuild, values, field.getAnnotation(SelectLabels.class));
            }
            if (Set.class.isAssignableFrom(field.getType()) || List.class.isAssignableFrom(field.getType())) {
                if (embedTypeAnnotation != null && embedTypeAnnotation.type() != Object.class) {
                    //embed an object of the given type
                    pbuild.embeddedType(embedTypeAnnotation.type());
                } else if (embedPluginAnnotation != null && embedPluginAnnotation.type() != Object.class) {
                    //embed a Plugin of the given type
                    pbuild.embeddedPluginType(embedPluginAnnotation.type());
                }
            }
        }else if (type == Property.Type.String) {
            StringRenderingConstants.DisplayType renderBehaviour = StringRenderingConstants.DisplayType.SINGLE_LINE;
            //set select/freeselect
            final SelectValues selectAnnotation = field.getAnnotation(SelectValues.class);
            if (null != selectAnnotation) {
                pbuild.type(
                        selectAnnotation.multiOption() ? Property.Type.Options :
                        (selectAnnotation.freeSelect() ? Property.Type.FreeSelect : Property.Type.Select)
                );
                String[] values = selectAnnotation.values();
                pbuild.values(values);
                pbuild.dynamicValues(selectAnnotation.dynamicValues());
                extractSelectLabels(pbuild, values, field.getAnnotation(SelectLabels.class));
            }
            final DynamicSelectValues dynamicValuesAnnotation = field.getAnnotation(DynamicSelectValues.class);
            if (null != dynamicValuesAnnotation &&
                (
                        notBlank(dynamicValuesAnnotation.generatorClassName()) ||
                        !Object.class.equals(dynamicValuesAnnotation.generatorClass())
                )) {
                //attempt to create a ValuesGenerator
                Class<?> genClass = dynamicValuesAnnotation.generatorClass();
                String genClassName = dynamicValuesAnnotation.generatorClassName();
                if (notBlank(genClassName)) {
                    try {
                        genClass = Class.forName(genClassName);
                    } catch (ClassNotFoundException e) {
                        e.printStackTrace();
                    }
                }
                try {
                    if (ValuesGenerator.class.isAssignableFrom(genClass)) {
                        ValuesGenerator valuesGenerator = (ValuesGenerator) genClass.getDeclaredConstructor()
                                                                                    .newInstance();
                        pbuild.valuesGenerator(valuesGenerator);
                    }
                } catch (NoSuchMethodException | InvocationTargetException |
                        IllegalAccessException | InstantiationException e) {
                    e.printStackTrace();
                }
            }

            if (field.getAnnotation(TextArea.class) != null) {
                renderBehaviour = StringRenderingConstants.DisplayType.MULTI_LINE;
            }

            if (field.getAnnotation(Password.class) != null) {
                renderBehaviour = StringRenderingConstants.DisplayType.PASSWORD;
            }

            pbuild.renderingOption(StringRenderingConstants.DISPLAY_TYPE_KEY, renderBehaviour);


        }

        RenderingOption option = field.getAnnotation(RenderingOption.class);
        if(option!=null) {
            pbuild.renderingOption(option.key(), option.value());
        }
        RenderingOptions options = field.getAnnotation(RenderingOptions.class);
        if(options!=null) {
            for (RenderingOption renderingOption : options.value()) {
                pbuild.renderingOption(renderingOption.key(), renderingOption.value());
            }
        }


        String name = annotation.name();
        if (null == name || "".equals(name)) {
            name = field.getName();
        }
        pbuild.name(name);

        if (notBlank(annotation.title())) {
            pbuild.title(annotation.title());
        } else {
            pbuild.title(name);
        }

        pbuild.description(annotation.description());

        if (notBlank(annotation.defaultValue())) {
            pbuild.defaultValue(annotation.defaultValue());
        }
        pbuild.required(annotation.required());

        pbuild.scope(annotation.scope());

        if (notBlank(annotation.validatorClassName()) || !Object.class.equals(annotation.validatorClass())) {
            //attempt to create a validator
            Class<?> validatorClass = annotation.validatorClass();
            String validatorClassName = annotation.validatorClassName();
            if (notBlank(validatorClassName)) {
                try {
                    validatorClass = Class.forName(validatorClassName);
                } catch (ClassNotFoundException e) {
                    e.printStackTrace();
                }
            }
            try {
                if (PropertyValidator.class.isAssignableFrom(validatorClass)) {
                    PropertyValidator validator = (PropertyValidator) validatorClass.getDeclaredConstructor()
                                                                                    .newInstance();
                    pbuild.validator(validator);
                }
            } catch (NoSuchMethodException | InvocationTargetException |
                    IllegalAccessException | InstantiationException e) {
                e.printStackTrace();
            }
        }

        return pbuild.build();
    }

    private static void extractSelectLabels(
            final PropertyBuilder pbuild,
            final String[] values,
            final SelectLabels labelsAnnotation
    )
    {
        if (null != labelsAnnotation) {
            String[] labels = labelsAnnotation.values();
            HashMap<String, String> labelsMap = new HashMap<>();
            for (int i = 0; i < values.length && i < labels.length; i++) {
                labelsMap.put(values[i], labels[i]);
            }
            pbuild.labels(labelsMap);
        }
    }

    private static boolean notBlank(final String string) {
        return null != string && !"".equals(string);
    }

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
     * @deprecated use
     * {@link #configureProperties(PropertyResolver, Description, Object, PropertyScope, MultiPluginProviderLoader)}
     */
    public static Map<String, Object> configureProperties(final PropertyResolver resolver,
                                                          final Description description,
                                                          final Object object, PropertyScope defaultScope
    ) {

        return configureProperties(resolver, description, object, defaultScope, null);
    }

    /**
     * Set field values on a plugin object by using a Description, and setting field values to resolved property values.
     * Any resolved properties that are not mapped to a field will  be included in the return result.
     *
     * @param resolver     the property resolver
     * @param description  the property descriptions
     * @param object       the target object, which can implement {@link Configurable}, otherwise introspection will be
     *                     used
     * @param defaultScope a default property scope to assume for unspecified properties
     * @return Map of resolved properties that were not configured in the object's fields
     */
    public static Map<String, Object> configureProperties(
        final PropertyResolver resolver,
        final Description description,
        final Object object,
        PropertyScope defaultScope,
        final MultiPluginProviderLoader loader
    ) {
        Map<String, Object> inputConfig = mapDescribedProperties(resolver, description, defaultScope);
        if (object instanceof Configurable) {
            Configurable configObject = (Configurable) object;
            Properties configuration = new Properties();
            configuration.putAll(convertStringValues(inputConfig));
            try {
                configObject.configure(configuration);
            } catch (ConfigurationException e) {

            }
        } else {
            inputConfig = configureObjectFieldsWithProperties(object, description.getProperties(), inputConfig, loader);
        }
        return inputConfig;
    }

    public static Map<String, String> convertStringValues(final Map<String, Object> inputConfig) {
        HashMap<String, String> result = new HashMap<>();
        for (String s : inputConfig.keySet()) {
            result.put(s, inputConfig.get(s).toString());
        }
        return result;
    }

    /**
     * Set field values on an object using introspection and input values for those properties
     * @param object object
     * @param inputConfig input
     * @return Map of resolved properties that were not configured in the object's fields
     */
    public static Map<String, Object> configureObjectFieldsWithProperties(
            final Object object,
            final Map<String, Object> inputConfig
    ) {
        return configureObjectFieldsWithProperties(object, inputConfig, null);
    }

    /**
     * Set field values on an object using introspection and input values for those properties
     *
     * @param object      object
     * @param inputConfig input
     * @return Map of resolved properties that were not configured in the object's fields
     */
    public static Map<String, Object> configureObjectFieldsWithProperties(
        final Object object,
        final Map<String, Object> inputConfig,
        final MultiPluginProviderLoader loader
    ) {
        return configureObjectFieldsWithProperties(object, buildFieldProperties(object), inputConfig, loader);
    }

    /**
     * Set field values on an object given a list of properties and input values for those properties
     * @param object object
     * @param properties properties
     * @param inputConfig input
     * @return Map of resolved properties that were not configured in the object's fields
     */
    public static Map<String, Object> configureObjectFieldsWithProperties(
            final Object object,
            final List<Property> properties,
            final Map<String, Object> inputConfig
    ) {
        return configureObjectFieldsWithProperties(object, properties, inputConfig, null);
    }

    /**
     * Set field values on an object given a list of properties and input values for those properties
     *
     * @param object      object
     * @param properties  properties
     * @param inputConfig input
     * @return Map of resolved properties that were not configured in the object's fields
     */
    public static Map<String, Object> configureObjectFieldsWithProperties(
        final Object object,
        final List<Property> properties,
        final Map<String, Object> inputConfig,
        final MultiPluginProviderLoader loader
    ) {
        HashMap<String, Object> modified = new HashMap<>(inputConfig);
        for (final Property property : properties) {
            if (null != modified.get(property.getName())) {
                if (setValueForProperty(property, modified.get(property.getName()), object, loader)) {
                    modified.remove(property.getName());
                }
            }
        }
        return modified;
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
        return mapProperties(resolver, properties, defaultPropertyScope);
    }

    /**
     * Retrieve the Properties mapped to resolved values given the resolver, with a default property
     * scope
     * @param resolver property resolver
     * @param properties properties
     * @param defaultPropertyScope default scope for unspecified property scopes
     *
     * @return All mapped properties by name and value.
     */
    public static Map<String, Object> mapProperties(
            final PropertyResolver resolver,
            final List<Property> properties, final PropertyScope defaultPropertyScope
    )
    {
        final PropertyResolver defaulted =
                PropertyResolverFactory.withDefaultValues(
                        PropertyResolverFactory.withDefaultScope(
                                null != defaultPropertyScope ? defaultPropertyScope
                                                             : PropertyScope.InstanceOnly, resolver
                        ),
                        new PropertyDefaultValues(properties)
                );

        return PropertyResolverFactory.mapPropertyValues(properties, defaulted);
    }

    protected static Object createInstanceFromType(final Class<?> execClass) {


        try {
            final Constructor<?> method = execClass.getDeclaredConstructor(new Class[0]);
            return method.newInstance();
        } catch (NoSuchMethodException | InstantiationException | InvocationTargetException | IllegalAccessException
            e) {
            throw new RuntimeException("Unable to create instance of type: " + execClass.getName(), e);
        }

    }

    private static Object createEmbeddedPlugin(
        Class<?> type,
        String provider,
        Map<String, Object> config,
        final MultiPluginProviderLoader loader
    ) {

        if (loader == null) {
            throw new UnsupportedOperationException("createEmbeddedPlugin");
        }
        return loader.load(type, provider, config);

    }

    private static Object createEmbeddedObject(
        Class<?> type,
        Map<String, Object> config,
        final MultiPluginProviderLoader loader
    ) {

        Object inst = createInstanceFromType(type);
        configureObjectFieldsWithProperties(inst, config, loader);
        return inst;
    }

    private static Object createEmbeddedFieldValue(
        Property property,
        Map<String, Object> config,
        final MultiPluginProviderLoader loader
    ) {
        Class<?> embeddedType = property.getEmbeddedType();
        Class<?> embeddedPluginType = property.getEmbeddedPluginType();

        if (null != embeddedType) {
            return createEmbeddedObject(embeddedType, config, loader);
        } else if (null != embeddedPluginType) {
            Object providerEntry = config.get("type");
            Object configEntry = config.get("config");
            if (null != providerEntry
                && providerEntry instanceof String
                && null != configEntry
                && Map.class.isAssignableFrom(configEntry.getClass())) {
                String provider = (String) providerEntry;
                Map<String, Object> provConfig = (Map<String, Object>) configEntry;
                return createEmbeddedPlugin(embeddedPluginType, provider, provConfig, loader);
            } else {
                throw new IllegalStateException(
                    String.format(
                        "Cannot map property {%s type: %s} to to embedded Plugin type: %s. Expected a Map with " +
                        "'config' and 'type' entries, saw: %s",
                        property.getName(),
                        property.getType(),
                        embeddedPluginType.getName(),
                        config
                    ));
            }
        } else {
            throw new IllegalStateException(
                String.format(
                    "Cannot map property {%s type: %s} to to embedded field value, the property has no embeddedType " +
                    "or embeddedPluginType",
                    property.getName(),
                    property.getType(),
                    config
                ));
        }

    }

    /**
     * Set instance field value for the given property, returns true if the field value was set, false otherwise
     */
    private static boolean setValueForProperty(
        final Property property,
        final Object value,
        final Object object,
        final MultiPluginProviderLoader loader
    ) {
        final Field field = fieldForPropertyName(property.getName(), object);
        if (null == field) {
            return false;
        }
        final Object resolvedValue;
        final Property.Type type = property.getType();
        final Property.Type ftype = propertyTypeFromFieldType(field.getType());


        if (type != Property.Type.Embedded && ftype != property.getType()
            && !(
            ftype == Property.Type.String
            && (
                property.getType() == Property.Type.Select ||
                property.getType() == Property.Type.FreeSelect ||
                property.getType() == Property.Type.Options
            )
        )) {

            throw new IllegalStateException(
                String.format(
                    "cannot map property {%s type: %s} to field {%s type: %s}",
                    property.getName(),
                    property.getType(),
                    field.getName(),
                    ftype
                ));
        }
        if (type == Property.Type.Embedded
            || type == Property.Type.Options
               && (
                   property.getEmbeddedPluginType() != null
                   || property.getEmbeddedType() != null
               )) {
            resolvedValue = mapValueForEmbeddedType(property, value, field, type, ftype, loader);
        } else if (type == Property.Type.Integer) {
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
        } else if (type == Property.Type.Options) {
            Collection<String> resolvedValueSet = null;
            if (value instanceof String) {
                String valstring = (String) value;
                //not a String field
                if (field.getType().isAssignableFrom(Set.class)) {
                    HashSet<String> strings = new HashSet<>();
                    strings.addAll(Arrays.asList(valstring.split(", *")));
                    resolvedValueSet = strings;
                    resolvedValue = strings;
                } else if (field.getType().isAssignableFrom(List.class)) {
                    ArrayList<String> strings = new ArrayList<>();
                    strings.addAll(Arrays.asList(valstring.split(", *")));
                    resolvedValueSet = new HashSet<>(strings);
                    resolvedValue = strings;
                } else if (field.getType() == String[].class) {
                    ArrayList<String> strings = new ArrayList<>();
                    strings.addAll(Arrays.asList(valstring.split(", *")));
                    resolvedValueSet = new HashSet<>(strings);
                    resolvedValue = strings.toArray(new String[strings.size()]);
                } else if (field.getType() == String.class) {
                    resolvedValueSet = new HashSet<>();
                    resolvedValueSet.addAll(Arrays.asList(valstring.split(", *")));
                    resolvedValue = value;
                } else {
                    return false;
                }

            } else if (value instanceof Collection) {
                resolvedValueSet = (Collection<String>) value;
                if (field.getType().isAssignableFrom(Set.class)) {
                    resolvedValue = new HashSet<>(resolvedValueSet);
                } else if (field.getType().isAssignableFrom(List.class)) {
                    resolvedValue = new ArrayList<>(resolvedValueSet);
                } else if (field.getType() == String[].class) {
                    ArrayList<Object> strings = new ArrayList<>(resolvedValueSet);
                    resolvedValue = strings.toArray(new String[strings.size()]);
                } else {
                    return false;
                }

            } else if (value.getClass() == String[].class) {
                String[] valCollection = (String[]) value;
                if (field.getType().isAssignableFrom(Set.class)) {
                    resolvedValue = new HashSet<>(Arrays.asList(valCollection));
                } else if (field.getType().isAssignableFrom(List.class)) {
                    resolvedValue = new ArrayList<>(Arrays.asList(valCollection));
                } else if (field.getType() == String[].class) {

                    resolvedValue = valCollection;
                } else {
                    return false;
                }
                resolvedValueSet = Arrays.asList(valCollection);

            } else {
                //XXX
                return false;
            }
            if (!field.getAnnotation(SelectValues.class).dynamicValues() &&
                !property.getSelectValues().containsAll(resolvedValueSet)) {
                throw new RuntimeException(
                    String.format(
                        "Some options values were not allowed for property %s: %s",
                        property.getName(),
                        resolvedValue
                    ));
            }
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
                if (!field.getAnnotation(SelectValues.class).dynamicValues() &&
                    !property.getSelectValues().contains(resolvedValue)) {
                    throw new RuntimeException(
                        String.format("value not allowed for property %s: %s", property.getName(), resolvedValue));
                }
            } else {
                //XXX
                return false;
            }
        } else if (type == Property.Type.Map) {
            if (value instanceof Map) {
                resolvedValue = value;
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

    private static Object mapValueForEmbeddedType(
        final Property property,
        final Object value,
        final Field field,
        final Property.Type type,
        final Property.Type ftype,
        final MultiPluginProviderLoader loader
    ) {
        final Object resolvedValue;
        if (ftype == Property.Type.Options) {
            //list of embedded objects

            if (!(value instanceof Collection)) {
                throw new IllegalStateException(
                    String.format(
                        "Cannot map property {%s type: %s} to embedded List field, expected a " +
                        "Collection of" +
                        " Maps, but saw type: %s",
                        property.getName(),
                        property.getType(),
                        value.getClass().getName()
                    ));
            }
            List values = new ArrayList<>((Collection) value);
            List objects = new ArrayList();
            for (Object o : values) {
                if (Map.class.isAssignableFrom(o.getClass())) {
                    Map<String, Object> config = (Map<String, Object>) o;
                    objects.add(createEmbeddedFieldValue(property, config, loader));
                } else {
                    throw new IllegalStateException(
                        String.format(
                            "Cannot map property {%s type: %s} to embedded List field, expected a " +
                            "Collection of" +
                            " Maps, but an entry was of type: %s",
                            property.getName(),
                            property.getType(),
                            o.getClass().getName()
                        ));
                }
            }
            if (field.getType().isAssignableFrom(Set.class)) {
                HashSet valueset = new HashSet<>();
                valueset.addAll(objects);
                resolvedValue = valueset;
            } else {
                resolvedValue = objects;
            }

        } else if (null == ftype) {
            //single embedded object

            if (!Map.class.isAssignableFrom(value.getClass())) {
                throw new IllegalStateException(
                    String.format(
                        "Cannot map property {%s type: %s} to embedded type field, expected a " +
                        "Map, but saw type: %s",
                        property.getName(),
                        property.getType(),
                        value.getClass().getName()
                    ));

            }
            Map<String, Object> config = (Map<String, Object>) value;
            resolvedValue = createEmbeddedFieldValue(property, config, loader);

        } else {
            //invalid
            throw new IllegalStateException(
                String.format(
                    "Cannot map property {%s type: %s} to embedded type field, expected field to be a Collection, or " +
                    "Object, but saw type: %s",
                    property.getName(),
                    property.getType(),
                    ftype.getClass().getName()
                ));
        }
        return resolvedValue;
    }

    private static void setFieldValue(final Field field, final Object value, final Object object)
            throws IllegalAccessException {
        if (!field.isAccessible()) {
            field.setAccessible(true);
        }
        field.set(object, value);
    }
}
