/*
 * Copyright 2019 Rundeck, Inc. (http://rundeck.com)
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
package org.rundeck.app


class AppConstants {
    static final String FRAMEWORK_OUTPUT_ALLOW_UNSANITIZED = "framework.output.allowUnsanitized"
    static final String PROJECT_OUTPUT_ALLOW_UNSANITIZED = "project.output.allowUnsanitized"

    /**
     * System-wide default allowlist regex applied to job option values that have no per-option
     * regex of their own. When set (non-empty) an option value must fully match the pattern or the
     * execution is rejected during validation. When absent or empty, no default validation applies.
     * Overridden per-project by {@link #PROJECT_OPTION_INPUT_DEFAULT_PATTERN}.
     *
     * Resolved through ConfigurationService (the {@code rundeck.*} namespace), so it is editable at
     * runtime via the System Configuration UI as well as {@code rundeck-config}. This constant holds
     * the sub-key (without the {@code rundeck.} prefix) used with {@code ConfigurationService.getString}.
     */
    static final String SYSTEM_OPTION_INPUT_DEFAULT_PATTERN = "option.input.validation.default.pattern"
    /**
     * Full config key (with {@code rundeck.} prefix) of {@link #SYSTEM_OPTION_INPUT_DEFAULT_PATTERN},
     * as exposed in the System Configuration UI via SysConfigProp.
     */
    static final String SYSTEM_OPTION_INPUT_DEFAULT_PATTERN_KEY = "rundeck." + SYSTEM_OPTION_INPUT_DEFAULT_PATTERN
    /**
     * Per-project default allowlist regex applied to job option values that have no per-option
     * regex of their own. Takes precedence over {@link #SYSTEM_OPTION_INPUT_DEFAULT_PATTERN}.
     */
    static final String PROJECT_OPTION_INPUT_DEFAULT_PATTERN = "project.option.input.validation.default.pattern"

    /**
     * When true (default), an execution that provides options not declared on the job is created and
     * then failed at start. Resolved through ConfigurationService; this constant holds the sub-key
     * (without the {@code rundeck.} prefix) used with {@code ConfigurationService.getBoolean}.
     */
    static final String SYSTEM_REJECT_UNDECLARED_OPTIONS = "execution.rejectUndeclaredOptions"
    /**
     * Full config key (with {@code rundeck.} prefix) of {@link #SYSTEM_REJECT_UNDECLARED_OPTIONS},
     * as exposed in the System Configuration UI via SysConfigProp.
     */
    static final String SYSTEM_REJECT_UNDECLARED_OPTIONS_KEY = "rundeck." + SYSTEM_REJECT_UNDECLARED_OPTIONS

    /**
     * Opt-in (default false). When true, values exported to remote nodes via the node's
     * {@code ssh-variable-export-pattern} are POSIX shell-quoted, preventing command injection through
     * option values (RUN-4579). Left false by default to preserve the current behavior. Resolved
     * through ConfigurationService; this constant holds the sub-key (without the {@code rundeck.}
     * prefix) used with {@code ConfigurationService.getBoolean}.
     */
    static final String SYSTEM_SSH_EXPORT_QUOTING = "execution.sshExportQuoting"
    /**
     * Full config key (with {@code rundeck.} prefix) of {@link #SYSTEM_SSH_EXPORT_QUOTING}, as exposed
     * in the System Configuration UI via SysConfigProp.
     */
    static final String SYSTEM_SSH_EXPORT_QUOTING_KEY = "rundeck." + SYSTEM_SSH_EXPORT_QUOTING
}
