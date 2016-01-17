package com.dtolabs.rundeck.plugins.scm;

import com.dtolabs.rundeck.core.plugins.configuration.Validator;

/**
 * Indicates the action had invalid input, and includes a validation report, e.g using {@link Validator#buildReport()}
 */
public class ScmPluginInvalidInput extends ScmPluginException {
    final private Validator.Report report;

    public ScmPluginInvalidInput(final Validator.Report report) {
        this.report = report;
    }

    public ScmPluginInvalidInput(
            final String message,
            final Validator.Report report
    )
    {
        super(message);
        this.report = report;
    }

    public ScmPluginInvalidInput(
            final String message,
            final Throwable cause,
            final Validator.Report report
    )
    {
        super(message, cause);
        this.report = report;
    }

    public ScmPluginInvalidInput(
            final Throwable cause,
            final Validator.Report report
    )
    {
        super(cause);
        this.report = report;
    }

    public ScmPluginInvalidInput(
            final String message,
            final Throwable cause,
            final boolean enableSuppression,
            final boolean writableStackTrace,
            final Validator.Report report
    )
    {
        super(message, cause, enableSuppression, writableStackTrace);
        this.report = report;
    }

    public Validator.Report getReport() {
        return report;
    }
}
