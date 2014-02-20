package com.dtolabs.rundeck.server.plugins

import com.dtolabs.rundeck.core.plugins.configuration.Validator

/**
 * ValidatedPlugin holds a validation report and a validity result
 * @author greg
 * @since 2014-02-19
 */
class ValidatedPlugin {
    Validator.Report report
    boolean valid
}
