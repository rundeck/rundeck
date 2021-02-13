package com.dtolabs.rundeck.core.authorization.providers;

import com.dtolabs.rundeck.core.authorization.RuleSetValidation;
import com.dtolabs.rundeck.core.authorization.Validation;

import java.util.List;
import java.util.Map;


/**
 * Combines validation results with the policies
 */
public class PoliciesValidation
        implements RuleSetValidation<PolicyCollection>
{
    Validation validation;
    PolicyCollection policies;

    public PoliciesValidation(
            final Validation validation,
            final PolicyCollection policies
    )
    {
        this.validation = validation;
        this.policies = policies;
    }

    @Override
    public PolicyCollection getSource() {
        return policies;
    }

    @Override
    public boolean isValid() {
        return validation.isValid();
    }

    @Override
    public Map<String, List<String>> getErrors() {
        return validation.getErrors();
    }
}
