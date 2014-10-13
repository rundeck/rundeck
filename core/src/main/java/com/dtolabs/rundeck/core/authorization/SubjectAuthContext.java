/*
 Copyright 2013 SimplifyOps Inc, <http://simplifyops.com>

 Licensed under the Apache License, Version 2.0 (the "License");
 you may not use this file except in compliance with the License.
 You may obtain a copy of the License at

     http://www.apache.org/licenses/LICENSE-2.0

 Unless required by applicable law or agreed to in writing, software
 distributed under the License is distributed on an "AS IS" BASIS,
 WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 See the License for the specific language governing permissions and
 limitations under the License.
 */

package com.dtolabs.rundeck.core.authorization;

import com.dtolabs.rundeck.core.authentication.Username;

import javax.security.auth.Subject;
import java.util.Map;
import java.util.Set;

/**
 * Wraps a Subject and Authorization to provide AuthContext
 */
public class SubjectAuthContext implements NamedAuthContext {
    private Subject subject;
    private Authorization authorization;

    public SubjectAuthContext(Subject subject, Authorization authorization) {
        this.subject = subject;
        this.authorization = authorization;
    }

    @Override
    public String getUsername() {
        Set<Username> principals = subject.getPrincipals(Username.class);
        if(principals!=null && principals.size()>0){
            return principals.iterator().next().getName();
        }
        return null;
    }

    @Override
    public Decision evaluate(Map<String, String> resource, String action, Set<Attribute> environment) {
        return authorization.evaluate(resource, subject, action, environment);
    }

    @Override
    public Set<Decision> evaluate(Set<Map<String, String>> resources, Set<String> actions, Set<Attribute> environment) {
        return authorization.evaluate(resources, subject, actions, environment);
    }
}
