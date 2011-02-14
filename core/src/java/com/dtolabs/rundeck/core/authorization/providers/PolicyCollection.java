package com.dtolabs.rundeck.core.authorization.providers;

import java.util.Collection;
import java.util.Set;

import javax.security.auth.Subject;
import javax.xml.xpath.XPathExpressionException;

import com.dtolabs.rundeck.core.authorization.Attribute;
import com.dtolabs.rundeck.core.authorization.providers.PoliciesXml.Context;

public interface PolicyCollection {
    
    /**
     * For a given policy collection, return all the group names associated with it.
     * @return collection of group names.
     * @throws XPathExpressionException
     */
    public Collection<String> groupNames() throws InvalidCollection;

    public long countPolicies() throws InvalidCollection;

    public Collection<AclContext> matchedContexts(Subject subject,
            Set<Attribute> environment) throws InvalidCollection;

}