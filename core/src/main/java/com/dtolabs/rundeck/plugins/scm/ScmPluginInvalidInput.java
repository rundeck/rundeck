/*
 * Copyright 2016 SimplifyOps, Inc. (http://simplifyops.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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
