package com.dtolabs.rundeck.core.authorization;

import com.dtolabs.rundeck.core.authorization.providers.EnvironmentalContext;

import java.util.Map;
import java.util.Set;

public class AclRuleBuilder {
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
    private Set<String> denyActions;
    private EnvironmentalContext environment;

    private AclRuleBuilder() {
    }

    private AclRuleBuilder(AclRuleBuilder fromBuilder) {
        this(fromBuilder.build());
    }
    private AclRuleBuilder(AclRule fromRule) {
        sourceIdentity(fromRule.getSourceIdentity());
        description(fromRule.getDescription());
        resource(fromRule.getResource());
        resourceType(fromRule.getResourceType());
        regexMatch(fromRule.isRegexMatch());
        containsMatch(fromRule.isContainsMatch());
        equalsMatch(fromRule.isEqualsMatch());
        username(fromRule.getUsername());
        group(fromRule.getGroup());
        allowActions(fromRule.getAllowActions());
        denyActions(fromRule.getDenyActions());
        environment(fromRule.getEnvironment());
    }

    public static AclRuleBuilder builder(AclRule fromRule) {
        return new AclRuleBuilder(fromRule);
    }
    public static AclRuleBuilder builder(AclRuleBuilder fromBuilder) {
        return new AclRuleBuilder(fromBuilder);
    }

    public static AclRuleBuilder builder() {
        return new AclRuleBuilder();
    }

    public AclRuleBuilder sourceIdentityAppend(final String sourceIdentity) {
        this.sourceIdentity = null != this.sourceIdentity ? this.sourceIdentity + sourceIdentity : sourceIdentity;
        return this;
    }
    public AclRuleBuilder sourceIdentity(final String sourceIdentity) {
        this.sourceIdentity = sourceIdentity;
        return this;
    }

    public AclRuleBuilder description(final String description) {
        this.description = description;
        return this;
    }

    public AclRuleBuilder resource(final Map<String, Object> resource) {
        this.resource = resource;
        return this;
    }

    public AclRuleBuilder resourceType(final String resourceType) {
        this.resourceType = resourceType;
        return this;
    }

    public AclRuleBuilder regexMatch(final boolean regexMatch) {
        this.regexMatch = regexMatch;
        return this;
    }

    public AclRuleBuilder containsMatch(final boolean containsMatch) {
        this.containsMatch = containsMatch;
        return this;
    }

    public AclRuleBuilder equalsMatch(final boolean equalsMatch) {
        this.equalsMatch = equalsMatch;
        return this;
    }

    public AclRuleBuilder username(final String username) {
        this.username = username;
        return this;
    }

    public AclRuleBuilder group(final String group) {
        this.group = group;
        return this;
    }

    public AclRuleBuilder allowActions(final Set<String> allowActions) {
        this.allowActions = allowActions;
        return this;
    }

    public AclRuleBuilder environment(final EnvironmentalContext environment) {
        this.environment = environment;
        return this;
    }

    public AclRuleBuilder denyActions(final Set<String> denyActions) {
        this.denyActions = denyActions;
        return this;
    }

    public AclRule build() {
        return new AclRuleImpl(
                sourceIdentity,
                description,
                resource,
                resourceType,
                regexMatch,
                containsMatch,
                equalsMatch,
                username,
                group,
                allowActions,
                denyActions, environment
        );
    }
}