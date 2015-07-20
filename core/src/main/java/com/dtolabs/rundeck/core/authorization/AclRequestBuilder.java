package com.dtolabs.rundeck.core.authorization;

import java.util.Map;
import java.util.Set;

public class AclRequestBuilder {
    private Set<Attribute> environment;
    private String action;
    private AclSubject subject;
    private Map<String, String> resource;

    public AclRequestBuilder environment(final Set<Attribute> environment) {
        this.environment = environment;
        return this;
    }

    public AclRequestBuilder action(final String action) {
        this.action = action;
        return this;
    }

    public AclRequestBuilder subject(final AclSubject subject) {
        this.subject = subject;
        return this;
    }

    public AclRequestBuilder resource(final Map<String, String> resource) {
        this.resource = resource;
        return this;
    }

    public AclRequest create() {
        return new AclRequestImpl(environment, action, subject, resource);
    }
}