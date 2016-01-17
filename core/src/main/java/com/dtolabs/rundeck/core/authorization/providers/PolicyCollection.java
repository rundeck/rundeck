package com.dtolabs.rundeck.core.authorization.providers;

import java.util.Collection;
import java.util.Set;

import javax.security.auth.Subject;
import javax.xml.xpath.XPathExpressionException;

import com.dtolabs.rundeck.core.authorization.AclRuleSetSource;
import com.dtolabs.rundeck.core.authorization.Attribute;

public interface PolicyCollection extends AclRuleSetSource {

    /**
     * For a given policy collection, return all the group names associated with it.
     * @return collection of group names.
     */
    public Collection<String> groupNames() ;

    public long countPolicies() ;

    public Collection<AclContext> matchedContexts(Subject subject,
            Set<Attribute> environment) ;

}