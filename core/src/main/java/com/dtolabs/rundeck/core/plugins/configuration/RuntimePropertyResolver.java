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
package com.dtolabs.rundeck.core.plugins.configuration;

import com.dtolabs.rundeck.core.common.PropertyRetriever;
import org.apache.log4j.Logger;


/**
 * RuntimePropertyResolver resolves properties across Framework, Project and Instance scopes, using {@link
 * PropertyRetriever}s as sources for the different scopes.
 *
 * @author Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
 */
class RuntimePropertyResolver implements PropertyResolver {
    static final Logger log = Logger.getLogger(RuntimePropertyResolver.class);
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

    /**
     * Resolve the property value
     *
     * @throws IllegalArgumentException if the scope is null or {@link PropertyScope#Unspecified}
     */
    public String resolvePropertyValue(final String name, final PropertyScope scope) {
        if (null == scope || scope == PropertyScope.Unspecified) {
            throw new IllegalArgumentException("scope must be specified");
        }
        String value = null;
        if (scope.isInstanceLevel()) {
            if (null != instanceScopeResolver) {
                value = instanceScopeResolver.getProperty(name);
                log.trace("resolvePropertyValue(" + scope + ")(I) " + name + " = " + value);
            }
        }
        if (null != value || scope == PropertyScope.InstanceOnly) {
            log.debug("resolvePropertyValue(" + scope + ") " + name + " = " + value);
            return value;
        }

        if (scope.isProjectLevel()) {
            if (null != projectScopeResolver) {
                value = projectScopeResolver.getProperty(name);
                log.trace("resolvePropertyValue(" + scope + ")(P) " + name + " = " + value);
            }
        }
        if (null != value || scope == PropertyScope.ProjectOnly) {
            log.debug("resolvePropertyValue(" + scope + ") " + name + " = " + value);
            return value;
        }
        if (null != frameworkScopeResolver) {
            if (scope.isFrameworkLevel()) {
                value = frameworkScopeResolver.getProperty(name);
                log.trace("resolvePropertyValue(" + scope + ")(F) " + name + " = " + value);
            }
        }
        log.debug("resolvePropertyValue(" + scope + ") " + name + " = " + value);
        return value;
    }
}
