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

import com.dtolabs.rundeck.plugins.scm.ScmPluginException
import groovy.transform.CompileStatic
import org.eclipse.jgit.transport.URIish

/**
 * Checks a Git remote URL against {@link GitScmSecurityConfig} before JGit opens a connection.
 */
@CompileStatic
class GitUrlPolicy {

    /**
     * Rejects the URL when the configured scheme allowlist does not include it.
     * Does nothing when the allowlist property is unset.
     *
     * @param url Git remote URL
     * @param config resolved security config
     * @throws ScmPluginException when the URL cannot be parsed or the scheme is not allowed
     */
    static void assertAllowed(String url, GitScmSecurityConfig config) throws ScmPluginException {
        URIish uri
        try {
            uri = new URIish(url)
        } catch (URISyntaxException e) {
            throw new ScmPluginException('Git URL is not valid.', e)
        }
        assertAllowed(uri, config)
    }

    /**
     * Rejects the URL when the configured scheme allowlist does not include it.
     *
     * @param uri parsed Git remote
     * @param config resolved security config
     * @throws ScmPluginException when the scheme is not allowed
     */
    static void assertAllowed(URIish uri, GitScmSecurityConfig config) throws ScmPluginException {
        if (config == null || config.allowedSchemes == null) {
            return
        }
        String scheme = canonicalScheme(uri)
        if (!config.allowedSchemes.contains(scheme)) {
            throw new ScmPluginException("Git URL scheme '${scheme}' is not allowed.")
        }
    }

    /**
     * Maps a JGit URI to the scheme name used by {@code rundeck.scm.git.allowedSchemes}.
     * Scp-style URLs ({@code git@host:path} and {@code host:path}) count as {@code ssh}.
     * Paths with no host count as {@code file}.
     *
     * @param uri parsed Git remote
     * @return canonical scheme name
     */
    static String canonicalScheme(URIish uri) {
        if (uri.scheme != null) {
            return uri.scheme.toLowerCase(Locale.ROOT)
        }
        if (uri.host != null) {
            return 'ssh'
        }
        return 'file'
    }
}
