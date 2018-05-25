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

/**
 * Created by greg on 7/20/15.
 */
public class AclRuleImpl implements AclRule {

    String sourceIdentity;
    String description;
    String resourceType;
    boolean regexMatch;
    Map<String, Object> regexResource;
    boolean containsMatch;
    Map<String, Object> containsResource;
    boolean subsetMatch;
    Map<String, Object> subsetResource;
    boolean equalsMatch;

    Map<String, Object> equalsResource;
    String username;
    String group;
    Set<String> allowActions;
    EnvironmentalContext environment;
    Set<String> denyActions;

    AclRuleImpl() {

    }

    AclRuleImpl(AclRule prototype) {

        sourceIdentity = prototype.getSourceIdentity();
        description = (prototype.getDescription());
        resourceType = (prototype.getResourceType());
        regexMatch = (prototype.isRegexMatch());
        regexResource = (prototype.getRegexResource());
        containsMatch = (prototype.isContainsMatch());
        containsResource = (prototype.getContainsResource());
        subsetMatch = (prototype.isSubsetMatch());
        subsetResource = (prototype.getSubsetResource());
        equalsMatch = (prototype.isEqualsMatch());
        equalsResource = (prototype.getEqualsResource());
        username = (prototype.getUsername());
        group = (prototype.getGroup());
        allowActions = (prototype.getAllowActions());
        denyActions = (prototype.getDenyActions());
        environment = (prototype.getEnvironment());
    }


    @Override
    public String getSourceIdentity() {
        return sourceIdentity;
    }

    @Override
    public String getDescription() {
        return description;
    }

    @Override
    public String getResourceType() {
        return resourceType;
    }

    @Override
    public boolean isRegexMatch() {
        return regexMatch;
    }

    @Override
    public boolean isContainsMatch() {
        return containsMatch;
    }

    @Override
    public boolean isSubsetMatch() {
        return subsetMatch;
    }

    @Override
    public String getUsername() {
        return username;
    }

    @Override
    public String getGroup() {
        return group;
    }

    @Override
    public Set<String> getAllowActions() {
        return allowActions;
    }

    @Override
    public EnvironmentalContext getEnvironment() {
        return environment;
    }

    @Override
    public Set<String> getDenyActions() {
        return denyActions;
    }

    @Override
    public String toString() {
        return "ACLRule<"  + sourceIdentity + ">{" +
               "'" + description + '\'' +
               " context=" + environment +
               " type='" + resourceType + '\'' +
               (regexMatch?" match " :"") +
               (null!=regexResource? ", resource=" + regexResource : "") +
               (containsMatch?" contains " :"") +
               (null!=containsResource? ", resource=" + containsResource : "") +
               (equalsMatch?" equals " :"") +
               (null!=equalsResource? ", resource=" + equalsResource : "") +
               (subsetMatch?" subset " :"") +
               (null!=subsetResource? ", resource=" + subsetResource : "") +
               " for: {"+
               (null!=username?" username='" + username + '\'':"") +
               (null!=group?" group='" + group + '\'':"") +
               "}" +
               (allowActions!=null&& allowActions.size()>0? " allow=" + allowActions : "") +
               ( denyActions!=null && denyActions.size()>0 ?" deny=" + denyActions : "" ) +
               '}';
    }

    @Override
    public boolean isEqualsMatch() {
        return equalsMatch;
    }

    @Override
    public Map<String, Object> getRegexResource() {
        return regexResource;
    }

    @Override
    public Map<String, Object> getContainsResource() {
        return containsResource;
    }

    @Override
    public Map<String, Object> getSubsetResource() {
        return subsetResource;
    }

    @Override
    public Map<String, Object> getEqualsResource() {
        return equalsResource;
    }
}
