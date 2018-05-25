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

package com.dtolabs.rundeck.core.authorization;

import com.dtolabs.rundeck.core.authorization.providers.EnvironmentalContext;

import java.util.Map;
import java.util.Set;

public class AclRuleBuilder {
    AclRuleImpl aclRuleImpl;

    private AclRuleBuilder() {
        aclRuleImpl = new AclRuleImpl();
    }

    private AclRuleBuilder(AclRuleBuilder fromBuilder) {
        this(fromBuilder.build());
    }
    private AclRuleBuilder(AclRule fromRule) {
        aclRuleImpl = new AclRuleImpl(fromRule);
    }

    public static AclRuleBuilder builder(AclRule fromRule) {
        return new AclRuleBuilder(fromRule);
    }
    public static AclRuleBuilder builder(AclRuleBuilder fromBuilder) {
        return new AclRuleBuilder(fromBuilder);
    }

    public static AclRuleBuilder builder() {
        return new AclRuleBuilder();
    }

    public AclRuleBuilder sourceIdentityAppend(final String sourceIdentity) {
        aclRuleImpl.sourceIdentity = null != aclRuleImpl.sourceIdentity
                                     ? aclRuleImpl.sourceIdentity + sourceIdentity
                                     : sourceIdentity;
        return this;
    }
    public AclRuleBuilder sourceIdentity(final String sourceIdentity) {
        aclRuleImpl.sourceIdentity = sourceIdentity;
        return this;
    }

    public AclRuleBuilder description(final String description) {
        aclRuleImpl.description = description;
        return this;
    }

    public AclRuleBuilder resourceType(final String resourceType) {
        aclRuleImpl.resourceType = resourceType;
        return this;
    }

    public AclRuleBuilder regexMatch(final boolean regexMatch) {
        aclRuleImpl.regexMatch = regexMatch;
        return this;
    }

    public AclRuleBuilder regexResource(final Map<String, Object> resource) {
        regexMatch(resource != null && resource.size() > 0);
        aclRuleImpl.regexResource = resource;
        return this;
    }

    public AclRuleBuilder containsMatch(final boolean containsMatch) {
        aclRuleImpl.containsMatch = containsMatch;
        return this;
    }

    public AclRuleBuilder containsResource(final Map<String, Object> resource) {
        containsMatch(resource != null && resource.size() > 0);
        aclRuleImpl.containsResource = resource;
        return this;
    }

    public AclRuleBuilder subsetMatch(final boolean subsetMatch) {
        aclRuleImpl.subsetMatch = subsetMatch;
        return this;
    }

    public AclRuleBuilder subsetResource(final Map<String, Object> resource) {
        subsetMatch(resource != null && resource.size() > 0);
        aclRuleImpl.subsetResource = resource;
        return this;
    }
    public AclRuleBuilder equalsMatch(final boolean equalsMatch) {
        aclRuleImpl.equalsMatch = equalsMatch;
        return this;
    }

    public AclRuleBuilder equalsResource(final Map<String, Object> resource) {
        equalsMatch(resource != null && resource.size() > 0);
        aclRuleImpl.equalsResource = resource;
        return this;
    }
    public AclRuleBuilder username(final String username) {
        aclRuleImpl.username = username;
        return this;
    }

    public AclRuleBuilder group(final String group) {
        aclRuleImpl.group = group;
        return this;
    }

    public AclRuleBuilder allowActions(final Set<String> allowActions) {
        aclRuleImpl.allowActions = allowActions;
        return this;
    }

    public AclRuleBuilder environment(final EnvironmentalContext environment) {
        aclRuleImpl.environment = environment;
        return this;
    }

    public AclRuleBuilder denyActions(final Set<String> denyActions) {
        aclRuleImpl.denyActions = denyActions;
        return this;
    }

    public AclRule build() {
        return new AclRuleImpl(aclRuleImpl);
    }
}