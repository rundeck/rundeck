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
* RuntimePropertyResolver.java
* 
* User: Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
* Created: 12/3/12 4:15 PM
* 
*/
package com.dtolabs.rundeck.core.execution.workflow.steps;

import com.dtolabs.rundeck.core.common.PropertyRetriever;
import com.dtolabs.rundeck.core.plugins.configuration.PropertyScope;
import com.dtolabs.rundeck.plugins.step.PropertyResolver;


/**
 * RuntimePropertyResolver resolves properties across Framework, Project and Instance scopes, using {@link
 * PropertyRetriever}s as sources for the different scopes.
 *
 * @author Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
 */
class RuntimePropertyResolver implements PropertyResolver {
    private PropertyRetriever instanceScopeResolver;
    private PropertyRetriever projectScopeResolver;
    private PropertyRetriever frameworkScopeResolver;

    RuntimePropertyResolver(final PropertyRetriever instanceScopeResolver,
                            final PropertyRetriever projectScopeResolver,
                            final PropertyRetriever frameworkScopeResolver) {
        this.instanceScopeResolver = instanceScopeResolver;
        this.projectScopeResolver = projectScopeResolver;
        this.frameworkScopeResolver = frameworkScopeResolver;
    }

    private boolean allowsInstanceScope(PropertyScope scope) {
        return scope == PropertyScope.Instance;
    }

    private boolean allowsProjectScope(PropertyScope scope) {
        return allowsInstanceScope(scope) || scope == PropertyScope.Project;
    }

    private boolean allowsFrameworkScope(PropertyScope scope) {
        return allowsProjectScope(scope) || scope == PropertyScope.Framework;
    }

    /**
     * Resolve the property value
     *
     * @throws IllegalArgumentException if the scope is null or {@link PropertyScope#Unspecified}
     */
    @Override
    public String resolvePropertyValue(final String name, final PropertyScope scope) {
        if (null == scope || scope == PropertyScope.Unspecified) {
            throw new IllegalArgumentException("scope must be specified");
        }
        String value = null;
        if (allowsInstanceScope(scope) || scope == PropertyScope.InstanceOnly) {
            if (null != instanceScopeResolver) {
                value = instanceScopeResolver.getProperty(name);
            }
        }
        if (null != value || scope == PropertyScope.InstanceOnly) {
            return value;
        }

        if (allowsProjectScope(scope) || scope == PropertyScope.ProjectOnly) {
            if (null != projectScopeResolver) {
                value = projectScopeResolver.getProperty(name);
            }
        }
        if (null != value || scope == PropertyScope.ProjectOnly) {
            return value;
        }
        if (null != frameworkScopeResolver) {
            if (allowsFrameworkScope(scope)) {
                value = frameworkScopeResolver.getProperty(name);
            }
        }
        return value;
    }
}
