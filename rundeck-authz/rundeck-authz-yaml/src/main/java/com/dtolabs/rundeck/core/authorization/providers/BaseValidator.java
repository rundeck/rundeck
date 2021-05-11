package com.dtolabs.rundeck.core.authorization.providers;

import com.dtolabs.rundeck.core.authorization.RuleSetValidation;

import java.io.File;
import java.io.IOException;

/**
 * Validator for yaml ACL policy
 */
public interface BaseValidator {
    /**
     * Validate the yaml aclpolicy
     *
     * @param file yaml aclpolicy file
     * @return validation
     */
    RuleSetValidation<PolicyCollection> validateYamlPolicy(File file) throws IOException;

    /**
     * Validate the yaml aclpolicy, optionally within a specific project context
     *
     * @param ident identity string for the sources
     * @param text  yaml aclpolicy text
     * @return validation
     */
    RuleSetValidation<PolicyCollection> validateYamlPolicy(String ident, String text) throws IOException;

    /**
     * Validate the yaml aclpolicy, optionally within a specific project context
     *
     * @param ident  identity string for the sources
     * @param source yaml aclpolicy file
     * @return validation
     */
    RuleSetValidation<PolicyCollection> validateYamlPolicy(String ident, File source) throws IOException;
}
