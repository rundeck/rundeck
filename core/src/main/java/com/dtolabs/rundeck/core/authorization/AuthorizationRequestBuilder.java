package com.dtolabs.rundeck.core.authorization;

import java.util.Map;
import java.util.Set;

public class AuthorizationRequestBuilder {
    private Set<Attribute> environment;
    private String action;
    private AclSubject subject;
    private Map<String, String> resource;

    private AuthorizationRequestBuilder() {
    }

    public AuthorizationRequestBuilder environment(final Set<Attribute> environment) {
        this.environment = environment;
        return this;
    }

    public AuthorizationRequestBuilder action(final String action) {
        this.action = action;
        return this;
    }

    public AuthorizationRequestBuilder subject(final AclSubject subject) {
        this.subject = subject;
        return this;
    }

    public AuthorizationRequestBuilder resource(final Map<String, String> resource) {
        this.resource = resource;
        return this;
    }

    public static AuthorizationRequestBuilder builder() {
        return new AuthorizationRequestBuilder();
    }

    public AuthorizationRequest build() {
        return new AuthorizationRequestImpl(environment, action, subject, resource);
    }
}