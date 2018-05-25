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

import com.dtolabs.rundeck.core.authorization.providers.Policies;

import java.util.HashSet;
import java.util.Set;

/**
 * Created by greg on 7/21/15.
 */
public class AclsUtil {

    public static Authorization createAuthorization(Policies policies) {
        return RuleEvaluator.createRuleEvaluator(policies);
    }

    /**
     * collect the set of groups used in a rule set
     * @param source source
     * @return group names
     */
    public static Set<String> getGroups(AclRuleSetSource source){
        HashSet<String> strings = new HashSet<>();
        for (AclRule rule : source.getRuleSet().getRules()) {
            if(rule.getGroup()!=null) {
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
        if (a instanceof AclRuleSetSource && b instanceof AclRuleSetSource) {
            return RuleEvaluator.createRuleEvaluator(
                    merge((AclRuleSetSource) a, (AclRuleSetSource) b)
            );
        }
        return new MultiAuthorization(a, b);
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
