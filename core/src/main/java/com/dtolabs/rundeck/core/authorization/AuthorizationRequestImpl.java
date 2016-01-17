package com.dtolabs.rundeck.core.authorization;

import java.util.Map;
import java.util.Set;

/**
 * Created by greg on 7/20/15.
 */
public class AuthorizationRequestImpl implements AuthorizationRequest {

    private final Map<String, String> resource;
    private final AclSubject subject;
    private final String action;
    private final Set<Attribute> environment;

    public AuthorizationRequestImpl(
            final Set<Attribute> environment,
            final String action,
            final AclSubject subject,
            final Map<String, String> resource
    )
    {
        this.environment = environment;
        this.action = action;
        this.subject = subject;
        this.resource = resource;
    }

    @Override
    public Map<String, String> getResource() {
        return resource;
    }

    @Override
    public AclSubject getSubject() {
        return subject;
    }

    @Override
    public String getAction() {
        return action;
    }

    @Override
    public Set<Attribute> getEnvironment() {
        return environment;
    }
}
