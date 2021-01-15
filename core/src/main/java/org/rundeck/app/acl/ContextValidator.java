package org.rundeck.app.acl;

import com.dtolabs.rundeck.core.authorization.RuleSetValidation;
import com.dtolabs.rundeck.core.authorization.providers.PolicyCollection;

import java.io.IOException;

/**
 * Adds Context param to validation methods
 *
 * @param <T> context type
 */
public interface ContextValidator<T> {

    /**
     * Validate the yaml aclpolicy, optionally within a specific project context
     *
     * @param ident identity string for the sources
     * @param text  yaml aclpolicy text
     * @return validation
     */
    RuleSetValidation<PolicyCollection> validateYamlPolicy(T context, String ident, String text) throws IOException;

    /**
     * Validate the yaml aclpolicy within a specific project context
     *
     * @param fname filename
     * @return validation
     */
    RuleSetValidation<PolicyCollection> validatePolicyFile(T context, String fname) throws IOException;
}
