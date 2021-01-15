package com.dtolabs.rundeck.core.authorization.providers;

import com.dtolabs.rundeck.core.authorization.Attribute;
import com.dtolabs.rundeck.core.authorization.AuthorizationUtil;
import com.dtolabs.rundeck.core.authorization.RuleSetValidation;
import com.dtolabs.rundeck.core.authorization.ValidationSet;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Date;
import java.util.Set;

/**
 * Implements BaseValidator, and provides a factory via {@link #factory()}
 */
public class BaseValidatorImpl
        implements BaseValidator
{
    private final String project;

    public BaseValidatorImpl(final String project) {
        this.project = project;
    }

    /**
     * @return factory
     */
    public static ValidatorFactory factory() {
        return new ValidatorFactory() {
            public BaseValidator forProjectOnly(String project) {
                return new BaseValidatorImpl(project);
            }

            public BaseValidator create() {
                return new BaseValidatorImpl(null);
            }
        };
    }

    @Override
    public PoliciesValidation validateYamlPolicy(String ident, String text) throws IOException {
        ValidationSet validation = new ValidationSet();
        CacheableYamlSource source = YamlProvider.sourceFromString(ident, text, new Date(), validation);
        PolicyCollection policies = YamlProvider.policiesFromSource(
                source,
                getForcedContext(),
                validation
        );
        validation.complete();
        return new PoliciesValidation(validation, policies);
    }

    @Override
    public RuleSetValidation<PolicyCollection> validateYamlPolicy(
            final String ident, final File source
    ) throws IOException
    {
        ValidationSet validation = new ValidationSet();
        PolicyCollection policies = null;
        try (FileInputStream stream = new FileInputStream(source)) {
            policies = YamlProvider.policiesFromSource(
                    YamlProvider.sourceFromStream(ident, stream, new Date(), validation),
                    getForcedContext(),
                    validation
            );
        }
        validation.complete();
        return new PoliciesValidation(validation, policies);
    }

    private Set<Attribute> getForcedContext() {
        return project != null ? AuthorizationUtil.projectContext(project) : null;
    }

    @Override
    public PoliciesValidation validateYamlPolicy(File file) throws IOException {
        ValidationSet validation = new ValidationSet();
        PolicyCollection
                policies =
                YamlProvider.policiesFromSource(
                        YamlProvider.sourceFromFile(file, validation),
                        getForcedContext(),
                        validation
                );
        validation.complete();
        return new PoliciesValidation(validation, policies);

    }
}
