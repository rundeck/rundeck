package com.dtolabs.rundeck.core.plugins.configuration;

/**
 * Can validate types other than string
 */
public interface PropertyObjectValidator
        extends PropertyValidator
{
    /**
     * @param value input value that is not a string
     * @return true if valid
     * @throws ValidationException with validation message if invalid
     */
    public boolean isValid(Object value) throws ValidationException;
}
