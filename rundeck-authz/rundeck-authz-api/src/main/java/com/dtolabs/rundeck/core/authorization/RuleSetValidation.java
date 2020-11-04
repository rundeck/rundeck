package com.dtolabs.rundeck.core.authorization;

public interface RuleSetValidation<T extends AclRuleSetSource>
        extends Validation{
    T getSource();
}
