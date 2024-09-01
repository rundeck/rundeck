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

import com.dtolabs.rundeck.plugins.descriptions.*;
import com.dtolabs.rundeck.plugins.util.DescriptionBuilder;

import java.util.*;


/**
 * Utility for creating {@link Description}s from Plugin class annotations and setting property values for annotated
 * property fields.
 *
 * @author Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
 * @deprecated use {@link PluginAdapterImpl}
 */
@Deprecated
public class PluginAdapterUtility {
    private static final PluginAdapterImpl PLUGIN_ADAPTER = new PluginAdapterImpl();

    /**
     * @param object potential plugin object annotated with {@link com.dtolabs.rundeck.core.plugins.Plugin}
     * @return true if the object has a valid Plugin annotation
     */
    public static boolean canBuildDescription(final Object object) {
        return PLUGIN_ADAPTER.canBuildDescription(object);
    }

    /**
     * @return Create a Description using a builder by analyzing the annotations on a plugin object, and including
     * annotations on fields as DescriptionProperties.
     *
     * @param object  the object
     * @param builder builder
     */
    public static Description buildDescription(final Object object, final DescriptionBuilder builder) {
        return PLUGIN_ADAPTER.buildDescription(object, builder);
    }
    /**
     * @return Create a Description using a builder by analyzing the annotations on a plugin type, and including
     * annotations on fields as DescriptionProperties.
     *
     * @param type  the type
     * @param builder builder
     */
    public static Description buildDescription(final Class<?> type, final DescriptionBuilder builder) {
        return PLUGIN_ADAPTER.buildDescription(type, builder);
    }

    /**
     * @return Create a Description using a builder by analyzing the annotations on a plugin object.
     *
     * @param object  the object
     * @param builder builder
     * @param includeAnnotatedFieldProperties
     *                if true, add DescriptionProperties to the Description based on annotations of fields in the class of the instance
     */
    public static Description buildDescription(
            final Object object,
            final DescriptionBuilder builder,
            final boolean includeAnnotatedFieldProperties
    ) {
        return PLUGIN_ADAPTER.buildDescription(object, builder, includeAnnotatedFieldProperties);
    }

    /**
     * @return Create a Description using a builder by analyzing the annotations on a plugin object.
     *
     * @param builder builder
     * @param includeAnnotatedFieldProperties
     *                if true, add DescriptionProperties to the Description based on annotations of fields in the class of the instance
     */
    public static Description buildDescription(
            final Class<?> type,
            final DescriptionBuilder builder,
            final boolean includeAnnotatedFieldProperties
    ) {
        return PLUGIN_ADAPTER.buildDescription(type, builder, includeAnnotatedFieldProperties);
    }


    /**
     * Return the list of properties by introspecting the annotated fields for {@link PluginProperty}
     * @param object object
     * @return list of properties, may be empty
     */
    public static List<Property> buildFieldProperties(final Object object) {
        return PLUGIN_ADAPTER.buildFieldProperties(object);
    }

    /**
     * Return the list of properties by introspecting the annotated fields for {@link PluginProperty}
     * @param aClass class
     * @return list of properties, may be empty
     */
    public static List<Property> buildFieldProperties(final Class<?> aClass) {
        return PLUGIN_ADAPTER.buildFieldProperties(aClass);
    }

    /**
     * Add properties based on introspection of a class
     * @param aClass class
     * @param builder builder
     */
    public static void buildFieldProperties(final Class<?> aClass, final DescriptionBuilder builder) {
        PLUGIN_ADAPTER.buildFieldProperties(aClass, builder);
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
        return PLUGIN_ADAPTER.configureProperties(resolver, object);
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
        return PLUGIN_ADAPTER.configureProperties(resolver, description, object, defaultScope);
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
    )
    {
        return PLUGIN_ADAPTER.configureObjectFieldsWithProperties(object, inputConfig);
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
    )
    {
        return PLUGIN_ADAPTER.configureObjectFieldsWithProperties(object, properties, inputConfig);
    }


    /**
     * Retrieve the Description's Properties mapped to resolved values given the resolver, using InsanceOnly default scope.
     * @param resolver property resolver
     * @param description plugin description
     * @return All mapped properties by name and value.
     */
    public static Map<String, Object> mapDescribedProperties(final PropertyResolver resolver,
                                                             final Description description) {
        return PLUGIN_ADAPTER.mapDescribedProperties(resolver, description);
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
        return PLUGIN_ADAPTER.mapDescribedProperties(resolver, description, defaultPropertyScope);
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
        return PLUGIN_ADAPTER.mapProperties(resolver, properties, defaultPropertyScope);
    }

    /**
     * Set config on fields annotated with PluginConfig {@link PluginCustomConfig}
     * @param object
     * @param config
     */
    public static void setConfig(final Object object, Object config) {
        PLUGIN_ADAPTER.setConfig(object, config);
    }

    public static PluginCustomConfig getCustomConfigAnnotation(final Object providerInstance) {
        return PLUGIN_ADAPTER.getCustomConfigAnnotation(providerInstance);
    }

}
