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
package com.dtolabs.rundeck.core.data;

/**
 * Behavior for unresolved {@code ${...}} references during data-context expansion.
 */
public enum UnexpandableBehavior {
    /**
     * Replace unresolved references with a blank string (classic default).
     */
    BLANK,
    /**
     * Leave unresolved references intact (same as {@code blankIfUnexpandable: false}).
     */
    PRESERVE,
    /**
     * Leave bash-like unresolved references (no {@code .} in the token) intact,
     * but blank unresolved Rundeck-style dotted references ({@code ${option.x}}, {@code ${node.hostname}}, …).
     */
    PRESERVE_BASH;

    /**
     * Map classic boolean blankIfUnexpandable to a behavior.
     */
    public static UnexpandableBehavior fromBlankIfUnexpandable(boolean blankIfUnexpandable) {
        return blankIfUnexpandable ? BLANK : PRESERVE;
    }

    /**
     * Parse plugin.yaml / metadata value. Accepts blank, preserve, preserveBash (case-insensitive).
     *
     * @param value raw string, may be null
     * @return behavior or null if absent/unrecognized
     */
    public static UnexpandableBehavior parse(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim();
        if (normalized.isEmpty()) {
            return null;
        }
        switch (normalized.toLowerCase()) {
            case "blank":
                return BLANK;
            case "preserve":
                return PRESERVE;
            case "preservebash":
            case "preserve_bash":
            case "preserve-bash":
                return PRESERVE_BASH;
            default:
                return null;
        }
    }

    /**
     * YAML / API canonical name for this mode.
     */
    public String toConfigValue() {
        switch (this) {
            case BLANK:
                return "blank";
            case PRESERVE:
                return "preserve";
            case PRESERVE_BASH:
                return "preserveBash";
            default:
                return name();
        }
    }
}
