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

package com.dtolabs.rundeck.core.execution.service;

import java.util.Locale;

/**
 * Shell used by a node executor to evaluate the assembled remote command.
 */
public enum RemoteCommandShell {
    DEFAULT,
    CMD,
    POWERSHELL;

    public static final String PROVIDER_METADATA_KEY = "remote-command-shell";

    public static RemoteCommandShell parse(final String value) {
        if (value == null || value.trim().isEmpty()) {
            return DEFAULT;
        }
        try {
            return valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Unsupported remote command shell: " + value, e);
        }
    }
}
