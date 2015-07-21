package com.dtolabs.rundeck.core.authorization;

import com.dtolabs.rundeck.core.authorization.providers.Policies;
import com.dtolabs.rundeck.core.authorization.providers.SAREAuthorization;

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
}
