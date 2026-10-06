/*
 * Copyright 2018 Rundeck, Inc. (http://rundeck.com)
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

package org.rundeck.plugin.scm.git

import com.dtolabs.rundeck.core.common.PropertyRetriever
import groovy.transform.CompileStatic

/**
 * Framework-level controls for Git SCM URL handling.
 * <p>
 * Both properties are unset by default, which keeps every URL that works today.
 * The property retriever is read on each Git operation. Callers may supply a retriever that
 * reads System Configuration first, then {@code framework.properties}.
 * A blank value is unset: it does not reject every scheme, and the next source is used.
 * JVM system properties apply only when the retriever leaves the key blank.
 * <ul>
 *   <li>{@code rundeck.scm.git.allowedSchemes} — comma-separated list such as {@code https,http,ssh}.
 *       {@code ssh} includes {@code ssh://} and scp-style {@code git@host:path} / {@code host:path}.
 *       {@code file} includes {@code file://} and local paths. Unset means no scheme restriction.</li>
 *   <li>{@code rundeck.scm.git.blockInternalAddresses} — when {@code true}, reject loopback, link-local,
 *       and private addresses, including HTTP redirects. Default {@code false}.</li>
 * </ul>
 */
@CompileStatic
class GitScmSecurityConfig {

    static final String ALLOWED_SCHEMES = 'rundeck.scm.git.allowedSchemes'
    static final String BLOCK_INTERNAL = 'rundeck.scm.git.blockInternalAddresses'

    /**
     * Lower-case schemes that may be cloned. {@code null} means every scheme is allowed.
     */
    final Set<String> allowedSchemes

    /**
     * When {@code true}, Git remotes that resolve to an internal address are rejected.
     */
    final boolean blockInternalAddresses

    private GitScmSecurityConfig(Set<String> allowedSchemes, boolean blockInternalAddresses) {
        this.allowedSchemes = allowedSchemes
        this.blockInternalAddresses = blockInternalAddresses
    }

    /**
     * Resolves config from the framework property retriever, then JVM system properties.
     * A non-blank framework value wins over a system property.
     *
     * @param retriever framework properties, or {@code null} to read system properties only
     * @return resolved config; both controls are off when the properties are unset
     */
    static GitScmSecurityConfig resolve(PropertyRetriever retriever) {
        String schemes = firstValue(retriever, ALLOWED_SCHEMES)
        String block = firstValue(retriever, BLOCK_INTERNAL)
        return new GitScmSecurityConfig(parseSchemes(schemes), parseBoolean(block))
    }

    private static String firstValue(PropertyRetriever retriever, String key) {
        if (retriever != null) {
            String configured = retriever.getProperty(key)
            if (configured != null && configured.trim()) {
                return configured.trim()
            }
        }
        String systemValue = System.getProperty(key)
        if (systemValue != null && systemValue.trim()) {
            return systemValue.trim()
        }
        return null
    }

    private static Set<String> parseSchemes(String raw) {
        if (raw == null) {
            return null
        }
        Set<String> schemes = new LinkedHashSet<String>()
        for (String part : raw.split(',')) {
            String scheme = part.trim().toLowerCase(Locale.ROOT)
            if (scheme) {
                schemes.add(scheme)
            }
        }
        return Collections.unmodifiableSet(schemes)
    }

    private static boolean parseBoolean(String raw) {
        if (raw == null) {
            return false
        }
        String value = raw.trim().toLowerCase(Locale.ROOT)
        return value == 'true' || value == 'yes' || value == 'on'
    }
}
