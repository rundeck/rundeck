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
package com.dtolabs.rundeck.core.execution.workflow.steps;

import com.dtolabs.rundeck.core.data.UnexpandableBehavior;
import com.dtolabs.rundeck.core.plugins.configuration.Description;
import com.dtolabs.rundeck.core.plugins.configuration.Property;

import java.util.HashMap;
import java.util.Map;

/**
 * Helpers for resolving per-property {@link UnexpandableBehavior} during step config expansion.
 */
public final class UnexpandableBehaviorSupport {

    private UnexpandableBehaviorSupport() {
    }

    /**
     * Build per-field behavior map. Resolution order per property:
     * <ol>
     *   <li>{@link Property#getUnexpandableBehaviorFrom()} → instance config (or that field's default)</li>
     *   <li>{@link Property#getUnexpandableBehavior()} if set</li>
     *   <li>derive from {@link Property#isBlankIfUnexpandable()}</li>
     * </ol>
     *
     * @param pluginBlankIfUnexpandedDefault when blankIfUnexpandable=true, use this plugin-level default
     * @param instanceConfiguration          raw step config before expansion; used for From selectors
     */
    public static Map<String, UnexpandableBehavior> buildBehaviorMap(
            Description description,
            boolean pluginBlankIfUnexpandedDefault,
            Map<String, ?> instanceConfiguration
    ) {
        Map<String, UnexpandableBehavior> map = new HashMap<>();
        if (description == null || description.getProperties() == null) {
            return map;
        }
        for (Property p : description.getProperties()) {
            map.put(
                    p.getName(),
                    resolveBehavior(p, description, pluginBlankIfUnexpandedDefault, instanceConfiguration)
            );
        }
        return map;
    }

    /** Convenience: plugin blank default {@code true}, no instance config. */
    public static Map<String, UnexpandableBehavior> buildBehaviorMap(Description description) {
        return buildBehaviorMap(description, true, null);
    }

    static UnexpandableBehavior resolveBehavior(
            Property p,
            Description description,
            boolean pluginBlankIfUnexpandedDefault,
            Map<String, ?> instanceConfiguration
    ) {
        UnexpandableBehavior fromConfig = resolveFromSibling(p, description, instanceConfiguration);
        if (fromConfig != null) {
            return fromConfig;
        }
        if (p.getUnexpandableBehavior() != null) {
            return p.getUnexpandableBehavior();
        }
        if (!p.isBlankIfUnexpandable()) {
            return UnexpandableBehavior.PRESERVE;
        }
        return UnexpandableBehavior.fromBlankIfUnexpandable(pluginBlankIfUnexpandedDefault);
    }

    private static UnexpandableBehavior resolveFromSibling(
            Property p,
            Description description,
            Map<String, ?> instanceConfiguration
    ) {
        String fromKey = p.getUnexpandableBehaviorFrom();
        if (fromKey == null || fromKey.isEmpty()) {
            return null;
        }
        String raw = null;
        if (instanceConfiguration != null) {
            Object value = instanceConfiguration.get(fromKey);
            if (value != null) {
                raw = String.valueOf(value);
            }
        }
        if (raw == null || raw.trim().isEmpty()) {
            raw = defaultValueForProperty(description, fromKey);
        }
        return UnexpandableBehavior.parse(raw);
    }

    private static String defaultValueForProperty(Description description, String propertyName) {
        if (description == null || description.getProperties() == null) {
            return null;
        }
        for (Property sibling : description.getProperties()) {
            if (propertyName.equals(sibling.getName())) {
                return sibling.getDefaultValue();
            }
        }
        return null;
    }
}
