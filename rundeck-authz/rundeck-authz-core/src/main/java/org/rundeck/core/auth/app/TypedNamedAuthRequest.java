package org.rundeck.core.auth.app;

/**
 * Auth request for a resource type
 */
public interface TypedNamedAuthRequest
        extends NamedAuthRequest
{
    /**
     * @return type name
     */
    String getType();

}
