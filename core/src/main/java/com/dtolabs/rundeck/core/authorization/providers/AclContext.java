package com.dtolabs.rundeck.core.authorization.providers;

import com.dtolabs.rundeck.core.authorization.AclRule;
import com.dtolabs.rundeck.core.authorization.AclRuleBuilder;

import java.util.Map;
import java.util.Set;


public interface AclContext {

    public ContextDecision includes(Map<String, String> resource, String action);

    Set<AclRule> createRules(AclRuleBuilder prototype);
}