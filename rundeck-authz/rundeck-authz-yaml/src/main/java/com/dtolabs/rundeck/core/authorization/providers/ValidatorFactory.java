package com.dtolabs.rundeck.core.authorization.providers;

/**
 * Factory for validators
 */
public interface ValidatorFactory {
    /**
     * Create validator for strict project environment (policies must not declare environment, and will be validated in
     * the context of the project)
     *
     * @param project project name
     */
    BaseValidator forProjectOnly(String project);

    /**
     * Create validator
     */
    BaseValidator create();
}
