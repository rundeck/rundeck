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

import com.dtolabs.rundeck.core.authentication.Group;
import com.dtolabs.rundeck.core.authentication.Urn;
import com.dtolabs.rundeck.core.authentication.Username;
import com.dtolabs.rundeck.core.authorization.providers.Logger;
import com.dtolabs.rundeck.core.authorization.providers.Policies;

import javax.security.auth.Subject;
import java.io.File;
import java.util.HashSet;
import java.util.Set;

/**
 * Created by greg on 7/21/15.
 */
public class AclsUtil {
    public static AclRuleSetAuthorization createFromDirectory(File dir) {
        return createAuthorization(Policies.load(dir));
    }
    public static AclRuleSetAuthorization createFromDirectory(File dir, Logger logger) {
        return createAuthorization(Policies.load(dir, logger));
    }

    /**
     * @return authorization from source
     */
    public static AclRuleSetAuthorization createAuthorization(AclRuleSetSource aclRuleSetSource) {
        return logging(RuleEvaluator.createRuleEvaluator(aclRuleSetSource, TypedSubject.aclSubjectCreator(Username.class, Group.class, Urn.class)));
    }
    public static AclRuleSetAuthorization createAuthorization(Policies policies) {
        return logging(RuleEvaluator.createRuleEvaluator(policies, TypedSubject.aclSubjectCreator(Username.class, Group.class, Urn.class)));
    }

    private static AclRuleSetAuthorization logging(AclRuleSetAuthorization authorization) {
        return new LoggingAuthorization(authorization);
    }

    /**
     * collect the set of groups used in a rule set
     *
     * @param source source
     * @return group names
     */
    public static Set<String> getGroups(AclRuleSetSource source) {
        HashSet<String> strings = new HashSet<>();
        for (AclRule rule : source.getRuleSet().getRules()) {
            if (rule.getGroup() != null) {
                strings.add(rule.getGroup());
            }
        }
        return strings;
    }

    /**
     * Merge to authorization resources, may not be AclRuleSetAuthorization
     * @param a authorization
     * @param b authorization
     * @return a new Authorization that merges both authorization a and b
     */
    public static Authorization appendAuthorization(Authorization a, Authorization b) {
        if(a instanceof AclRuleSetAuthorization && b instanceof AclRuleSetAuthorization){
            return append(a, b);
        }
        return Authorizations.append(a, b);
    }
    
    /**
     * Merge two authorizations which are AclRuleSetAuthorization instances
     * @param a authorization
     * @param b authorization
     * @return a new AclRuleSetAuthorization that merges both authorization a and b
     */
    public static AclRuleSetAuthorization append(Authorization a, Authorization b) {
        //TODO: refactor to receive AclRuleSetAuthorization directly
        AclRuleSetAuthorization a1 = toAclRuleSetSource(a);
        AclRuleSetAuthorization b1 = toAclRuleSetSource(b);
        return append(a1, b1);
    }

    /**
     * Merge two AclRuleSetAuthorization objects
     * @param a authorization
     * @param b authorization
     * @return a new AclRuleSetAuthorization that merges both authorization a and b
     */
    public static AclRuleSetAuthorization append(AclRuleSetAuthorization a, AclRuleSetAuthorization b) {
        if (a!=null || b!=null) {
            return logging(
                    RuleEvaluator.createRuleEvaluator(
                            merge(a, b),
                            TypedSubject.aclSubjectCreator(Username.class, Group.class, Urn.class)
                    )
            );
        }
        throw new IllegalArgumentException();
    }

    private static AclRuleSetAuthorization toAclRuleSetSource(final Authorization a) {
        if(a instanceof AclRuleSetAuthorization){
            return (AclRuleSetAuthorization)a;
        }
        return null;
    }

    public static AclRuleSetSource source(final AclRuleSet a) {
        return a.source();
    }
    public static AclRuleSetSource merge(final AclRuleSetSource a, final AclRuleSetSource b){
        return new AclRuleSetSource() {
            @Override
            public AclRuleSet getRuleSet() {
                HashSet<AclRule> aclRules = new HashSet<>();
                if (a != null) {
                    aclRules.addAll(a.getRuleSet().getRules());
                }
                if (null != b) {
                    aclRules.addAll(b.getRuleSet().getRules());
                }
                return new AclRuleSetImpl(aclRules);
            }
        };
    }

    /**
     * Create URN Subject for a project
     *
     * @param project  project name
     */
    public static Subject getSubjectUrnForProject(String project){
        String urn = "project:"+project;
        Subject t = new Subject();
        t.getPrincipals().add(new Urn(urn));
        return t;
    }
}
