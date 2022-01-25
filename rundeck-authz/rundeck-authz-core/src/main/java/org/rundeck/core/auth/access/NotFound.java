package org.rundeck.core.auth.access;

import lombok.Getter;

/**
 * A resource is not found
 */
@Getter
public class NotFound
        extends Exception
{
    public NotFound(final String type, final String name) {
        super(String.format("Not found: %s %s", type, name));
        this.type = type;
        this.name = name;
    }


    /**
     * Resource type
     */
    private final String type;
    /**
     * Resource name
     */
    private final String name;
}
