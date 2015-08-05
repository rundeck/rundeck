package com.dtolabs.rundeck.core.authorization;

import com.dtolabs.rundeck.core.authorization.providers.AclContext;

import javax.security.auth.Subject;
import java.util.List;
import java.util.Set;

/**
 * Created by greg on 7/17/15.
 */
public interface AclRuleSet {
    public Set<AclRule> getRules();
}
