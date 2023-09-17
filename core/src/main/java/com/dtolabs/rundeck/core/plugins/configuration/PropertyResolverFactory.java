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
* PropertyResolverFactory.java
* 
* User: Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
* Created: 12/3/12 4:13 PM
* 
*/
package com.dtolabs.rundeck.core.plugins.configuration;

import com.dtolabs.rundeck.core.common.IFramework;
import com.dtolabs.rundeck.core.common.IRundeckProject;
import com.dtolabs.rundeck.core.common.PropertyRetriever;
import com.dtolabs.rundeck.core.execution.ExecutionContext;
import com.dtolabs.rundeck.core.execution.workflow.StepExecutionContext;
import com.dtolabs.rundeck.core.utils.IPropertyLookup;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;


/**
 * Factory for different property resolvers for use by plugins
 *
 * @author Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
 */
public class PropertyResolverFactory {
    public static final String SEP = ".";
    public static final String PROJECT_PREFIX = "project" + SEP;
    public static final String FRAMEWORK_PREFIX = "framework" + SEP;
    public static final String PLUGIN_PREFIX = "plugin" + SEP;

    public static String pluginPropertyPrefix(final String stepType, final String pluginName) {
        return PLUGIN_PREFIX + stepType + SEP + pluginName + SEP;
    }

    public static String projectPropertyPrefix(final String basePrefix) {
        return PROJECT_PREFIX + basePrefix;
    }

    public static String frameworkPropertyPrefix(final String basePrefix) {
        return FRAMEWORK_PREFIX + basePrefix;
    }

    /**
     * Return All property values for the input property set mapped by name to value.
     *
     * @param list property list
     * @param resolver property resolver
     * @return All mapped properties by name and value.
     */
    public static Map<String, Object> mapPropertyValues(final List<Property> list, final PropertyResolver resolver) {
        final Map<String, Object> inputConfig = new HashMap<String, Object>();
        for (final Property property : list) {
            final Object value = resolver.resolvePropertyValue(property.getName(), property.getScope());
            if (null == value) {
                continue;
            }
            inputConfig.put(property.getName(), value);
        }
        return inputConfig;
    }

    /**
     * @param context step context
     * @param instanceProperties instance property values
     * @param pluginType service type name
     * @param providerName provider name
     * @return Create a PropertyResolver for a plugin for resolving Framework, Project and instance scoped properties.
     */
    public static PropertyResolver createStepPluginRuntimeResolver(final StepExecutionContext context,
                                                                   final Map<String, Object> instanceProperties,
                                                                   final String pluginType,
                                                                   final String providerName) {

        final String projectPrefix = projectPropertyPrefix(pluginPropertyPrefix(pluginType, providerName));
        final String frameworkPrefix = frameworkPropertyPrefix(pluginPropertyPrefix(pluginType, providerName));

        return createResolver(instanceRetriever(instanceProperties),
                                           projectRetriever(projectPrefix,
                                                            context.getFramework(),
                                                            context.getFrameworkProject()),
                                           frameworkRetriever(frameworkPrefix, context.getFramework())
        );
    }
    /**
     *
     * @param context step context
     * @param instanceProperties instance property values
     * @param pluginType service type name
     * @param providerName provider name
     * @return Create a PropertyResolver for a plugin for resolving Framework, Project and instance scoped properties.
     */
    public static PropertyResolver createPluginRuntimeResolver(final ExecutionContext context,
                                                                   final Map<String, Object> instanceProperties,
                                                                   final String pluginType,
                                                                   final String providerName) {

        return createPluginRuntimeResolver(
            context.getFrameworkProject(),
            context.getFramework(),
            instanceProperties,
            pluginType,
            providerName
        );
    }

    /**
     * @param project            project name
     * @param framework          frameork
     * @param instanceProperties instance property values
     * @param pluginType         service type name
     * @param providerName       provider name
     * @return Create a PropertyResolver for a plugin for resolving Framework, Project and instance scoped properties.
     */
    public static PropertyResolver createPluginRuntimeResolver(
        String project,
        IFramework framework,
        final Map<String, Object> instanceProperties,
        final String pluginType,
        final String providerName
    )
    {

        final String projectPrefix = projectPropertyPrefix(pluginPropertyPrefix(pluginType, providerName));
        final String frameworkPrefix = frameworkPropertyPrefix(pluginPropertyPrefix(pluginType, providerName));

        return createResolver(
            instanceRetriever(instanceProperties),
            projectRetriever(projectPrefix, framework, project),
            frameworkRetriever(frameworkPrefix, framework)
        );
    }

    /**
     * @return Create a PropertyResolver for a plugin for resolving Framework, Project and instance scoped properties.
     * @param framework the framework
     * @param projectName name of the project, or null to disable project property resolution
     * @param pluginType service type name
     * @param providerName provider name
     * @param instanceProperties instance properties, or null
     */
    public static PropertyResolver createFrameworkProjectRuntimeResolver(final IFramework framework,
            final String projectName,
            final Map<String, Object> instanceProperties,
            final String pluginType,
            final String providerName) {

        final String projectPrefix = projectPropertyPrefix(pluginPropertyPrefix(pluginType, providerName));
        final String frameworkPrefix = frameworkPropertyPrefix(pluginPropertyPrefix(pluginType, providerName));

        return createResolver(instanceRetriever(instanceProperties),
                null != projectName ? projectRetriever(projectPrefix, framework, projectName) : null,
                frameworkRetriever(frameworkPrefix, framework)
        );
    }/**
     * @return Create a PropertyResolver for a plugin for resolving Framework, Project and instance scoped properties.
     * @param framework the framework
     * @param projectName name of the project, or null to disable project property resolution
     * @param instanceProperties instance properties, or null
     */
    public static Factory createFrameworkProjectRuntimeResolverFactory(
            final IFramework framework,
            final String projectName,
            final Map<String, Object> instanceProperties
    )
    {
        return pluginPrefixedScoped(
                null!=instanceProperties?instanceRetriever(instanceProperties):null,
                null!=projectName?instanceRetriever(framework.getFrameworkProjectMgr().getFrameworkProject(projectName).getProperties()):null,
                framework.getPropertyRetriever()
        );
    }


    /**
     * create resolver using service name and provider name arguments
     */
    public static interface Factory {
        PropertyResolver create(String service, Description description);
    }

    public static Factory creates(PropertyResolver resolver){
        return (a, b) -> resolver;
    }

    /**
     * @return Create a PropertyResolver for a plugin for resolving Framework, Project and instance scoped properties.
     * @param framework the framework property lookup
     * @param projectLookup the project property lookup
     * @param pluginType service type name
     * @param providerName provider name
     * @param instanceProperties instance properties, or null
     */
    public static PropertyResolver createFrameworkProjectRuntimeResolver(
            final IPropertyLookup framework,
            final IPropertyLookup projectLookup,
            final Map<String, Object> instanceProperties,
            final String pluginType,
            final String providerName) {

        final String projectPrefix = projectPropertyPrefix(pluginPropertyPrefix(pluginType, providerName));
        final String frameworkPrefix = frameworkPropertyPrefix(pluginPropertyPrefix(pluginType, providerName));

        return createResolver(instanceRetriever(instanceProperties),
                null != projectLookup ? prefixedRetriever(projectPrefix, projectLookup) : null,
                prefixedRetriever(frameworkPrefix, framework)
        );
    }

    /**
     * @return Create a resolver from a set of retrievers, possibly null
     *
     * @param instanceRetriever retriever for instance properties
     * @param projectRetriever retriever for project properties
     * @param frameworkRetriever retriever for framework properties
     */
    public static PropertyResolver createResolver(PropertyRetriever instanceRetriever,
            PropertyRetriever projectRetriever, PropertyRetriever frameworkRetriever) {
        return new RuntimePropertyResolver(instanceRetriever, projectRetriever, frameworkRetriever);
    }

    /**
     * @return Create a resolver from a set of retrievers, possibly null
     *
     * @param instance instance data
     * @param projectScope project scope
     * @param frameworkScope scope
     */
    public static Factory pluginPrefixedScoped(final PropertyRetriever instance, final PropertyRetriever projectScope, final PropertyRetriever frameworkScope) {
        return (String pluginType, Description description)-> {
            String providerName=description.getName();
            final String projectPrefix = projectPropertyPrefix(pluginPropertyPrefix(pluginType, providerName));
            final String frameworkPrefix = frameworkPropertyPrefix(pluginPropertyPrefix(pluginType, providerName));
            return new ScopedResolver(instance, projectScope, frameworkScope, description, projectPrefix, frameworkPrefix);
        };
    }
    public static class ScopedResolver implements PropertyResolver {
        final PropertyRetriever instance;
        final PropertyRetriever projectScope;
        final PropertyRetriever frameworkScope;
        final Description description;
        final String projectPrefix ;
        final String frameworkPrefix;
        boolean legacyMappingEnabled = true;

        public ScopedResolver(
                final PropertyRetriever instance,
                final PropertyRetriever projectScope,
                final PropertyRetriever frameworkScope,
                final Description description,
                final String projectPrefix,
                final String frameworkPrefix
        )
        {
            this.instance = instance;
            this.projectScope = projectScope;
            this.frameworkScope = frameworkScope;
            this.description = description;
            this.projectPrefix = projectPrefix;
            this.frameworkPrefix = frameworkPrefix;
        }

        @Override
        public Object resolvePropertyValue(final String name, final PropertyScope scope) {
            String value = null;
            if (scope.isInstanceLevel() && instance != null) {
                value = instance.getProperty(name);
            }
            if (null != value || scope == PropertyScope.InstanceOnly) {
                return value;
            }

            if (scope.isProjectLevel() && projectScope != null) {
                value = projectScope.getProperty(projectPrefix + name);

                if (value == null
                    && legacyMappingEnabled
                    && description != null
                    && description.getPropertiesMapping() != null
                    && description.getPropertiesMapping().containsKey(name)) {
                    String key = description.getPropertiesMapping().get(name);
                    value = projectScope.getProperty(key);
                }
            }
            if (null != value || scope == PropertyScope.ProjectOnly) {
                return value;
            }

            value = frameworkScope.getProperty(frameworkPrefix + name);

            if (value == null
                && legacyMappingEnabled
                && description != null
                && description.getFwkPropertiesMapping() != null
                && description.getFwkPropertiesMapping().containsKey(name)) {
                String key = description.getFwkPropertiesMapping().get(name);
                value = frameworkScope.getProperty(key);
            }
            return value;
        }
    }

    /**
     * @return Create a PropertyResolver for a plugin for resolving only instance scoped properties.
     * @param instanceProperties properties
     */
    public static PropertyResolver createInstanceResolver(final Map<String, Object> instanceProperties) {

        return createResolver(instanceRetriever(instanceProperties), null, null);
    }

    /**
     * @param instanceProperties properties
     * @return Create a PropertyResolver for a plugin for resolving only instance scoped properties.
     */
    public static PropertyResolver createInstanceResolver(final Properties instanceProperties) {

        return createResolver(instanceRetriever(instanceProperties), null, null);
    }

    private static PropertyRetriever frameworkRetriever(final String frameworkPrefix, IFramework framework) {
        return prefixedRetriever(frameworkPrefix, framework.getPropertyRetriever());
    }

    private static PropertyRetriever projectRetriever(final String projectPrefix,
                                                      final IFramework framework,
                                                      final String project) {
        IRundeckProject frameworkProject = framework
                .getFrameworkProjectMgr()
                .getFrameworkProject(project);
        return prefixedRetriever(
                projectPrefix, instanceRetriever(frameworkProject.getProperties())
        );
    }
    private static PropertyRetriever projectRetriever(final String projectPrefix,
                                                      final IPropertyLookup projectLookup) {
        return prefixedRetriever(
                projectPrefix, projectLookup
        );
    }

    /**
     * @return Create a basic retriever
     * @param configuration from a Map of values
     */
    public static PropertyRetriever instanceRetriever(final Properties configuration) {
        return new MapPropertyRetriever(toMap(configuration));
    }

    private static Map<String, ?> toMap(final Properties configuration) {
        Map<String, String> propsMap = new HashMap<>();
        for (String key : configuration.stringPropertyNames()) {
            propsMap.put(key, configuration.getProperty(key));
        }
        return propsMap;
    }

    /**
     * @param configuration from a Map of values
     * @return Create a basic retriever
     */
    public static PropertyRetriever instanceRetriever(final Map<String, ?> configuration) {
        return new MapPropertyRetriever(configuration);
    }
    private static class MapPropertyRetriever implements PropertyRetriever{
        private Map<String,?> map;

        private MapPropertyRetriever(Map<String, ?> map) {
            this.map = map;
        }

        public String getProperty(String name) {
            Object o = null != map ? map.get(name) : null;
            return o != null ? o.toString() : null;
        }

    }

    /**
     * Returns a new resolver that applies a given prefix to any resolved properties before calling a secondary
     * resolver.
     */
    private static PropertyRetriever prefixedRetriever(final String prefix, final PropertyRetriever resolver) {
        return new PrefixRetriever(prefix, resolver);
    }

    private static class PrefixRetriever implements PropertyRetriever {
        private final String prefix;
        private final PropertyRetriever resolver;

        private PrefixRetriever(final String prefix, final PropertyRetriever resolver) {
            this.prefix = prefix;
            this.resolver = resolver;
        }

        public String getProperty(final String name) {
            return resolver.getProperty(prefix + name);
        }
    }

    /**
     * @return a new PropertyResolver using the given scope by default if the requested property scope is unspecified
     * @param scope default scope
     *              @param resolver resolver
     */
    public static PropertyResolver withDefaultScope(final PropertyScope scope, final PropertyResolver resolver) {
        return new DefaultScopeRetriever(scope, resolver);
    }

    private static class DefaultScopeRetriever implements PropertyResolver {
        private final PropertyScope defaultScope;
        private final PropertyResolver resolver;

        private DefaultScopeRetriever(final PropertyScope defaultScope, final PropertyResolver resolver) {
            this.defaultScope = defaultScope;
            this.resolver = resolver;
        }

        public Object resolvePropertyValue(final String name, PropertyScope scope) {
            if (null == scope || scope == PropertyScope.Unspecified) {
                scope = defaultScope;
            }
            return resolver.resolvePropertyValue(name, scope);
        }
    }

    /**
     * @return a new PropertyResolver which will return values taken from the defaults if the given resolver returns
     * null
     * @param resolver resolver
     * @param defaults retriever for default values
     */
    public static PropertyResolver withDefaultValues(final PropertyResolver resolver,
                                                     final PropertyRetriever defaults) {
        return new DefaultValueRetriever(resolver, defaults);
    }

    /**
     * Uses a PropertyRetriever for default values
     */
    private static class DefaultValueRetriever implements PropertyResolver {
        private final PropertyResolver resolver;
        final PropertyRetriever defaults;

        private DefaultValueRetriever(final PropertyResolver resolver,
                                      final PropertyRetriever defaults) {
            this.defaults = defaults;
            this.resolver = resolver;
        }

        public Object resolvePropertyValue(final String name, final PropertyScope scope) {
            final Object value = resolver.resolvePropertyValue(name, scope);
            return null == value ? defaults.getProperty(name) : value;
        }
    }
}
