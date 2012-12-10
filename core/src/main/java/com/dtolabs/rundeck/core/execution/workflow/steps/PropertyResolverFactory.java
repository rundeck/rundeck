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
* PropertyResolverFactory.java
* 
* User: Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
* Created: 12/3/12 4:13 PM
* 
*/
package com.dtolabs.rundeck.core.execution.workflow.steps;

import com.dtolabs.rundeck.core.common.Framework;
import com.dtolabs.rundeck.core.common.PropertyRetriever;
import com.dtolabs.rundeck.core.execution.workflow.StepExecutionContext;
import com.dtolabs.rundeck.core.plugins.configuration.Property;
import com.dtolabs.rundeck.core.plugins.configuration.PropertyScope;
import com.dtolabs.rundeck.plugins.step.PluginStepItem;
import com.dtolabs.rundeck.plugins.step.PropertyResolver;

import java.util.HashMap;
import java.util.List;
import java.util.Map;


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
     * Create a PropertyResolver for a plugin for resolving Framework, Project and instance scoped properties.
     */
    public static PropertyResolver createStepPluginRuntimeResolver(final StepExecutionContext context,
                                                                   final String pluginType,
                                                                   final PluginStepItem step) {

        final String projectPrefix = projectPropertyPrefix(pluginPropertyPrefix(pluginType, step.getType()));
        final String frameworkPrefix = frameworkPropertyPrefix(pluginPropertyPrefix(pluginType, step.getType()));

        return new RuntimePropertyResolver(instanceRetriever(step),
                                           projectRetriever(projectPrefix,
                                                            context.getFramework(),
                                                            context.getFrameworkProject()),
                                           frameworkRetriever(context, frameworkPrefix)
        );
    }

    private static PropertyRetriever frameworkRetriever(final StepExecutionContext context,
                                                        final String frameworkPrefix) {
        return prefixedRetriever(frameworkPrefix,
                                 context.getFramework().getPropertyRetriever());
    }

    private static PropertyRetriever projectRetriever(final String projectPrefix,
                                                      final Framework framework,
                                                      final String project) {
        return prefixedRetriever(projectPrefix, framework
            .getFrameworkProjectMgr()
            .getFrameworkProject(project)
            .getPropertyRetriever());
    }

    private static PropertyRetriever instanceRetriever(final PluginStepItem step) {
        return new PluginStepPropertyRetriever(step);
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

        @Override
        public String getProperty(final String name) {
            return resolver.getProperty(prefix + name);
        }
    }

    /**
     * Return a new PropertyResolver using the given scope by default if the requested property scope is unspecified
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

        @Override
        public Object resolvePropertyValue(final String name, PropertyScope scope) {
            if (null == scope || scope == PropertyScope.Unspecified) {
                scope = defaultScope;
            }
            return resolver.resolvePropertyValue(name, scope);
        }
    }

    /**
     * Return a new PropertyResolver which will return values taken from the defaults if the given resolver returns
     * null
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

        @Override
        public Object resolvePropertyValue(final String name, final PropertyScope scope) {
            final Object value = resolver.resolvePropertyValue(name, scope);
            return null == value ? defaults.getProperty(name) : value;
        }
    }
}
