package com.dtolabs.rundeck.core.authorization;

import com.dtolabs.rundeck.core.authorization.providers.AclContext;
import com.dtolabs.rundeck.core.authorization.providers.PolicyCollection;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

/**
 * Created by greg on 7/17/15.
 */
public class AclRuleSetImpl implements AclRuleSet {
    private final Set<AclRule> rules;

    public AclRuleSetImpl(final Set<AclRule> rules) {
        this.rules = rules;
    }

    @Override
    public Set<AclRule> getRules() {
        return rules;
    }

}
