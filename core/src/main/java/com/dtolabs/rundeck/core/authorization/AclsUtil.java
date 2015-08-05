package com.dtolabs.rundeck.core.authorization;

import com.dtolabs.rundeck.core.authorization.providers.Policies;
import com.dtolabs.rundeck.core.authorization.providers.SAREAuthorization;

import java.util.HashSet;
import java.util.Set;

/**
 * Created by greg on 7/21/15.
 */
public class AclsUtil {
    public static final boolean useLegacyEvaluator = Boolean.getBoolean(
            "com.dtolabs.rundeck.core.authorization.AclsUtil.useLegacyEvaluator"
    );

    public static Authorization createAuthorization(Policies policies) {
        if (useLegacyEvaluator) {
            return new SAREAuthorization(policies);
        }
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
