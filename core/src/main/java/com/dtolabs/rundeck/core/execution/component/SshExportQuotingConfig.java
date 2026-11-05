/*
 * Copyright 2026 Rundeck, Inc. (http://rundeck.com)
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

package com.dtolabs.rundeck.core.execution.component;

/**
 * RUN-4579: execution-context component carrying the resolved decision of whether exported node
 * variable values (the {@code ssh-variable-export-pattern} prefix) should be POSIX shell-quoted.
 *
 * <p>Values flow into the {@code export {key}={value}} prefix that is prepended to the remote SSH
 * command. Without quoting, shell metacharacters in an option value are parsed as command separators
 * (command injection). The value is resolved in the application (Grails) layer via
 * {@code ConfigurationService} (the system-level {@code rundeck.execution.sshExportQuoting} setting),
 * attached to the execution context, and read by
 * {@code NodeExecutorUtils.getExportedVariablesForNode}. Opt-in: defaults to {@code false} (legacy
 * unquoted behavior) when no component is present.
 */
public final class SshExportQuotingConfig {

    /**
     * Component name used when attaching to / retrieving from the execution context.
     */
    public static final String COMPONENT_NAME = "sshExportQuoting";

    private final boolean quoteExportedValues;

    public SshExportQuotingConfig(final boolean quoteExportedValues) {
        this.quoteExportedValues = quoteExportedValues;
    }

    /**
     * @return true when exported variable values should be POSIX shell-quoted
     */
    public boolean isQuoteExportedValues() {
        return quoteExportedValues;
    }
}
