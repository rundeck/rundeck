package com.dtolabs.rundeck.core.authorization;

import com.dtolabs.rundeck.core.authorization.providers.EnvironmentalContext;

import java.util.Map;
import java.util.Set;

/**
 * Created by greg on 7/20/15.
 */
public class AclRuleImpl implements AclRule {

    private String sourceIdentity;
    private String description;
    private Map<String, Object> resource;
    private String resourceType;
    private boolean regexMatch;
    private boolean containsMatch;
    private boolean equalsMatch;
    private String username;
    private String group;
    private Set<String> allowActions;
    private EnvironmentalContext environment;
    private Set<String> denyActions;

    public AclRuleImpl(
            final String sourceIdentity,
            final String description,
            final Map<String, Object> resource,
            final String resourceType,
            final boolean regexMatch,
            final boolean containsMatch,
            final boolean equalsMatch,
            final String username,
            final String group,
            final Set<String> allowActions,
            final Set<String> denyActions,
            final EnvironmentalContext environment
    )
    {
        this.sourceIdentity = sourceIdentity;
        this.description = description;
        this.resource = resource;
        this.resourceType = resourceType;
        this.regexMatch = regexMatch;
        this.containsMatch = containsMatch;
        this.equalsMatch = equalsMatch;
        this.username = username;
        this.group = group;
        this.allowActions = allowActions;
        this.denyActions = denyActions;
        this.environment = environment;
    }

    @Override
    public String getSourceIdentity() {
        return sourceIdentity;
    }

    @Override
    public String getDescription() {
        return description;
    }

    @Override
    public Map<String, Object> getResource() {
        return resource;
    }

    @Override
    public String getResourceType() {
        return resourceType;
    }

    @Override
    public boolean isRegexMatch() {
        return regexMatch;
    }

    @Override
    public boolean isContainsMatch() {
        return containsMatch;
    }

    @Override
    public String getUsername() {
        return username;
    }

    @Override
    public String getGroup() {
        return group;
    }

    @Override
    public Set<String> getAllowActions() {
        return allowActions;
    }

    @Override
    public EnvironmentalContext getEnvironment() {
        return environment;
    }

    @Override
    public Set<String> getDenyActions() {
        return denyActions;
    }

    @Override
    public String toString() {
        return "ACLRule<"  + sourceIdentity + ">{" +
               "'" + description + '\'' +
               " context=" + environment +
               " type='" + resourceType + '\'' +
               (regexMatch?" match " :"") +
               (containsMatch?" contains " :"") +
               (equalsMatch?" equals " :"") +
               (null!=resource? ", resource=" + resource : "") +
               " for: {"+
               (null!=username?" username='" + username + '\'':"") +
               (null!=group?" group='" + group + '\'':"") +
               "}" +
               (allowActions!=null&& allowActions.size()>0? " allow=" + allowActions : "") +
               ( denyActions!=null && denyActions.size()>0 ?" deny=" + denyActions : "" ) +
               '}';
    }

    @Override
    public boolean isEqualsMatch() {
        return equalsMatch;
    }
}
