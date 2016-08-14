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

package com.dtolabs.rundeck.core.authorization.providers;

import java.util.Collection;
import java.util.Set;

import javax.security.auth.Subject;
import javax.xml.xpath.XPathExpressionException;

import com.dtolabs.rundeck.core.authorization.AclRuleSetSource;
import com.dtolabs.rundeck.core.authorization.Attribute;

public interface PolicyCollection extends AclRuleSetSource {

    /**
     * For a given policy collection, return all the group names associated with it.
     * @return collection of group names.
     */
    public Collection<String> groupNames() ;

    public long countPolicies() ;

    public Collection<AclContext> matchedContexts(Subject subject,
            Set<Attribute> environment) ;

}