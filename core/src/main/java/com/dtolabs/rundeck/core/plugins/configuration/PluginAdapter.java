package com.dtolabs.rundeck.core.plugins.configuration;

import com.dtolabs.rundeck.plugins.descriptions.PluginCustomConfig;
import com.dtolabs.rundeck.plugins.descriptions.PluginProperty;
import com.dtolabs.rundeck.plugins.descriptions.SelectLabels;
import com.dtolabs.rundeck.plugins.util.DescriptionBuilder;
import com.dtolabs.rundeck.plugins.util.PropertyBuilder;

import java.lang.reflect.Field;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * Utility for creating {@link Description}s from Plugin class annotations and setting property values for annotated
 * property fields.
 *
 */
public interface PluginAdapter {
    /**
     * @param object potential plugin object annotated with {@link com.dtolabs.rundeck.core.plugins.Plugin}
     * @return true if the object has a valid Plugin annotation
     */
    boolean canBuildDescription(Object object);

    /**
     * @param object  the object
     * @param builder builder
     * @return Create a Description using a builder by analyzing the annotations on a plugin object, and including
     *         annotations on fields as DescriptionProperties.
     */
    Description buildDescription(Object object, DescriptionBuilder builder);

    /**
     * @param type    the type
     * @param builder builder
     * @return Create a Description using a builder by analyzing the annotations on a plugin type, and including
     *         annotations on fields as DescriptionProperties.
     */
    Description buildDescription(Class<?> type, DescriptionBuilder builder);

    /**
     * @param object                          the object
     * @param builder                         builder
     * @param includeAnnotatedFieldProperties if true, add DescriptionProperties to the Description based on annotations
     *                                        of fields in the class of the instance
     * @return Create a Description using a builder by analyzing the annotations on a plugin object.
     */
    Description buildDescription(
            Object object,
            DescriptionBuilder builder,
            boolean includeAnnotatedFieldProperties
    );

    /**
     * @param builder                         builder
     * @param includeAnnotatedFieldProperties if true, add DescriptionProperties to the Description based on annotations
     *                                        of fields in the class of the instance
     * @return Create a Description using a builder by analyzing the annotations on a plugin object.
     */
    Description buildDescription(
            Class<?> type,
            DescriptionBuilder builder,
            boolean includeAnnotatedFieldProperties
    );

    String getPluginNameAnnotation(Class<?> aClass);

    Map<String, String> loadPluginMetadata(Class<?> clazz);

    /**
     * Return the list of properties by introspecting the annotated fields for {@link PluginProperty}
     *
     * @param object object
     * @return list of properties, may be empty
     */
    List<Property> buildFieldProperties(Object object);

    /**
     * Return the list of properties by introspecting the annotated fields for {@link PluginProperty}
     *
     * @param aClass class
     * @return list of properties, may be empty
     */
    List<Property> buildFieldProperties(Class<?> aClass);

    /**
     * Add properties based on introspection of the object
     *
     * @param object  object
     * @param builder builder
     */
    void buildFieldProperties(Object object, DescriptionBuilder builder);

    /**
     * Add properties based on introspection of a class
     *
     * @param aClass  class
     * @param builder builder
     */
    void buildFieldProperties(Class<?> aClass, DescriptionBuilder builder);

    Field fieldForPropertyName(String name, Object object);

    Collection<Field> collectFields(Object object);

    Collection<Field> collectClassFields(Class<?> aClass);

    Property.Type propertyTypeFromFieldType(Class clazz);

    Property propertyFromField(Field field, PluginProperty annotation);

    void extractSelectLabels(
            PropertyBuilder pbuild,
            String[] values,
            SelectLabels labelsAnnotation
    );

    boolean notBlank(String string);

    /**
     * Set field values on a plugin object by using annotated field values to create a Description, and setting field
     * values to resolved property values. Any resolved properties that are not mapped to a field will  be included in
     * the return result.
     *
     * @param resolver property resolver
     * @param object   plugin object
     * @return Map of resolved properties that were not configured in the object's fields
     */
    Map<String, Object> configureProperties(
            PropertyResolver resolver,
            Object object
    );

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
    Map<String, Object> configureProperties(
            PropertyResolver resolver,
            Description description,
            Object object, PropertyScope defaultScope
    );

    /**
     * Set field values on an object using introspection and input values for those properties
     *
     * @param object      object
     * @param inputConfig input
     * @return Map of resolved properties that were not configured in the object's fields
     */
    Map<String, Object> configureObjectFieldsWithProperties(
            Object object,
            Map<String, Object> inputConfig
    );

    /**
     * Set field values on an object given a list of properties and input values for those properties
     *
     * @param object      object
     * @param properties  properties
     * @param inputConfig input
     * @return Map of resolved properties that were not configured in the object's fields
     */
    Map<String, Object> configureObjectFieldsWithProperties(
            Object object,
            List<Property> properties,
            Map<String, Object> inputConfig
    );

    /**
     * Retrieve the Description's Properties mapped to resolved values given the resolver, using InsanceOnly default
     * scope.
     *
     * @param resolver    property resolver
     * @param description plugin description
     * @return All mapped properties by name and value.
     */
    Map<String, Object> mapDescribedProperties(
            PropertyResolver resolver,
            Description description
    );

    /**
     * Retrieve the Description's Properties mapped to resolved values given the resolver, with a default property
     * scope
     *
     * @param resolver             property resolver
     * @param description          plugin description
     * @param defaultPropertyScope default scope for unspecified property scopes
     * @return All mapped properties by name and value.
     */
    Map<String, Object> mapDescribedProperties(
            PropertyResolver resolver,
            Description description, PropertyScope defaultPropertyScope
    );

    /**
     * Retrieve the Properties mapped to resolved values given the resolver, with a default property scope
     *
     * @param resolver             property resolver
     * @param properties           properties
     * @param defaultPropertyScope default scope for unspecified property scopes
     * @return All mapped properties by name and value.
     */
    Map<String, Object> mapProperties(
            PropertyResolver resolver,
            List<Property> properties, PropertyScope defaultPropertyScope
    );

    /**
     * Set config on fields annotated with PluginConfig {@link PluginCustomConfig}
     *
     * @param object
     * @param config
     */
    void setConfig(Object object, Object config);

    PluginCustomConfig getCustomConfigAnnotation(Object providerInstance);

}
