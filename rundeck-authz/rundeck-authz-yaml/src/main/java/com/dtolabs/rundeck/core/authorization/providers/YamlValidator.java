package com.dtolabs.rundeck.core.authorization.providers;

import com.dtolabs.rundeck.core.authorization.AuthorizationUtil;
import com.dtolabs.rundeck.core.authorization.ValidationSet;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Date;

public class YamlValidator
        implements Validator
{
    public static YamlValidator create() {
        return new YamlValidator();
    }

    @Override
    public PoliciesValidation validateYamlPolicy(String ident, String text) throws IOException {
        return validateYamlPolicy(null, ident, text);
    }

    /**
     * Validate the yaml aclpolicy, optionally within a specific project context
     *
     * @param project name of project to force the context of all policies, or null to not force a context
     * @param ident   identity string for the sources
     * @param text    yaml aclpolicy text
     * @return validation
     */
    @Override
    public PoliciesValidation validateYamlPolicy(String project, String ident, String text) throws IOException {
        ValidationSet validation = new ValidationSet();
        CacheableYamlSource source = YamlProvider.sourceFromString(ident, text, new Date(), validation);
        PolicyCollection policies = YamlProvider.policiesFromSource(
                source,
                project != null ? AuthorizationUtil.projectContext(project) : null,
                validation
        );
        validation.complete();
        return new PoliciesValidation(validation, policies);
    }

    /**
     * Validate the yaml aclpolicy, optionally within a specific project context
     *
     * @param project name of project to force the context of all policies, or null to not force a context
     * @param ident   identity string for the sources
     * @param source  yaml aclpolicy file
     * @return validation
     */
    @Override
    public PoliciesValidation validateYamlPolicy(String project, String ident, File source)
            throws IOException
    {
        ValidationSet validation = new ValidationSet();
        PolicyCollection policies = null;
        try (FileInputStream stream = new FileInputStream(source)) {

            CacheableYamlSource streamSource = YamlProvider.sourceFromStream(ident, stream, new Date(), validation);
            policies = YamlProvider.policiesFromSource(
                    streamSource,
                    project != null ? AuthorizationUtil.projectContext(project) : null,
                    validation
            );
        }
        validation.complete();
        return new PoliciesValidation(validation, policies);
    }

    @Override
    public PoliciesValidation validateYamlPolicy(File file) throws IOException {
        ValidationSet validation = new ValidationSet();
        PolicyCollection
                policies =
                YamlProvider.policiesFromSource(YamlProvider.sourceFromFile(file, validation), null, validation);
        validation.complete();
        return new PoliciesValidation(validation, policies);

    }
}
