package org.rundeck.core.auth.access;

import lombok.Getter;

/**
 * Authorization check failed
 */
@Getter
public class UnauthorizedAccess
        extends Exception
{
    public UnauthorizedAccess(final String action, final String type, final String name) {
        super(String.format("Unauthorized for %s access to %s %s", action, type, name));
        this.action = action;
        this.type = type;
        this.name = name;
    }

    /**
     * Action
     */
    private final String action;
    /**
     * Resource type
     */
    private final String type;
    /**
     * Resource name
     */
    private final String name;
}
