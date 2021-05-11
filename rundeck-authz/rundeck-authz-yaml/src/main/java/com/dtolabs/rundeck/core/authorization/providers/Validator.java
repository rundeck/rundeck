package com.dtolabs.rundeck.core.authorization.providers;

import com.dtolabs.rundeck.core.authorization.RuleSetValidation;

import java.io.File;
import java.io.IOException;

/**
 * Validates Yaml ACL content
 */
public interface Validator extends BaseValidator{
    /**
     * Validate the yaml aclpolicy, optionally within a specific project context
     *
     * @param project name of project to force the context of all policies, or null to not force a context
     * @param ident   identity string for the sources
     * @param source  file source
     * @return validation
     */
    RuleSetValidation<PolicyCollection> validateYamlPolicy(String project, String ident, File source) throws IOException;

    /**
     * Validate the yaml aclpolicy, optionally within a specific project context
     *
     * @param project name of project to force the context of all policies, or null to not force a context
     * @param ident   identity string for the sources
     * @param text    yaml aclpolicy text
     * @return validation
     */
    RuleSetValidation<PolicyCollection> validateYamlPolicy(String project, String ident, String text) throws IOException;
}
