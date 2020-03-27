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
import com.dtolabs.rundeck.core.authentication.Username;
import com.dtolabs.rundeck.core.authorization.providers.Logger;
import com.dtolabs.rundeck.core.authorization.providers.Policies;

import java.io.File;
import java.util.HashSet;
import java.util.Set;

/**
 * Created by greg on 7/21/15.
 */
public class AclsUtil {
    public static Authorization createFromDirectory(File dir) {
        return createAuthorization(Policies.load(dir));
    }
    public static Authorization createFromDirectory(File dir, Logger logger) {
        return createAuthorization(Policies.load(dir, logger));
    }
    public static Authorization createAuthorization(Policies policies) {
        return logging(RuleEvaluator.createRuleEvaluator(policies, TypedSubject.aclSubjectCreator(Username.class, Group.class)));
    }

    private static Authorization logging(Authorization authorization) {
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
     * Merge to authorization resources
     * @param a authorization
     * @param b authorization
     * @return a new Authorization that merges both authorization a and b
     */
    public static Authorization append(Authorization a, Authorization b) {
        AclRuleSetSource a1 = toAclRuleSetSource(a);
        AclRuleSetSource b1 = toAclRuleSetSource(b);
        if (a1!=null && b1!=null) {
            return logging(
                    RuleEvaluator.createRuleEvaluator(
                            merge(a1, b1),
                            TypedSubject.aclSubjectCreator(Username.class, Group.class)
                    )
            );
        }
        return logging(new MultiAuthorization(unwrapLogging(a), unwrapLogging(b)));
    }

    private static Authorization unwrapLogging(final Authorization b) {
        if(b instanceof LoggingAuthorization){
            return ((LoggingAuthorization) b).getAuthorization();
        }
        return b;
    }

    private static AclRuleSetSource toAclRuleSetSource(final Authorization a) {
        if(a instanceof AclRuleSetSource){
            return (AclRuleSetSource)a;
        }else if(a instanceof LoggingAuthorization && ((LoggingAuthorization)a).getAuthorization() instanceof AclRuleSetSource){
            return (AclRuleSetSource) ((LoggingAuthorization) a).getAuthorization();
        }
        return null;
    }

    public static AclRuleSetSource source(final AclRuleSet a) {
        return new AclRuleSetSource() {
            @Override
            public AclRuleSet getRuleSet() {
                return a;
            }
        };
    }
    public static AclRuleSetSource merge(final AclRuleSetSource a, final AclRuleSetSource b){
        return new AclRuleSetSource() {
            @Override
            public AclRuleSet getRuleSet() {
                HashSet<AclRule> aclRules = new HashSet<>(a.getRuleSet().getRules());
                aclRules.addAll(b.getRuleSet().getRules());
                return new AclRuleSetImpl(aclRules);
            }
        };
    }
}
