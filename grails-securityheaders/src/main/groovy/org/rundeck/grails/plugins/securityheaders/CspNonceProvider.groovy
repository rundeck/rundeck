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

package org.rundeck.grails.plugins.securityheaders

import groovy.transform.CompileStatic

import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpSession
import java.security.SecureRandom

/**
 * Generates and stores a Content-Security-Policy nonce, scoped to the current HTTP session.
 *
 * <p>The nonce is generated once per session by {@link RundeckSecurityHeadersFilter} (on the first
 * request that establishes a session) and cached as both a session attribute and a request attribute,
 * so that the CSP response header built by {@link CSPSecurityHeaderProvider} and the {@code nonce="..."}
 * attributes rendered onto inline {@code <script>} tags in GSP views all resolve to the same value for
 * the lifetime of the session &mdash; including for HTML fragments fetched via separate AJAX requests
 * and injected into an already-loaded page, whose inline scripts would otherwise carry a mismatched,
 * request-specific nonce that the browser rejects against the parent page's CSP header.</p>
 *
 * <p>See <a href="https://content-security-policy.com/nonce/">content-security-policy.com/nonce</a>.</p>
 */
@CompileStatic
class CspNonceProvider {

    /**
     * Request attribute name under which the per-request nonce value is stored.
     */
    public static final String HTTP_ATTRIBUTE_NAME = 'org.rundeck.csp.nonce'

    /**
     * Number of random bytes used to build a nonce, before base64 encoding.
     */
    public static final int NONCE_BYTE_LENGTH = 16

    private static final SecureRandom RANDOM = new SecureRandom()

    /**
     * Generate a fresh, unpredictable base64-encoded nonce value.
     *
     * @return newly generated nonce value
     */
    static String generateNonce() {
        byte[] bytes = new byte[NONCE_BYTE_LENGTH]
        RANDOM.nextBytes(bytes)
        Base64.encoder.encodeToString(bytes)
    }

    /**
     * Resolve the nonce for the current session (generating and caching one on first use) and store it
     * as a request attribute, unless one was already stored for this request.
     *
     * @param request current request
     * @return the nonce value associated with the request's session
     */
    static String storeNonce(final HttpServletRequest request) {
        if (null == request) {
            return null
        }
        String existing = getNonce(request)
        if (existing) {
            return existing
        }
        HttpSession session = request.getSession(true)
        String nonce
        synchronized (session) {
            Object sessionValue = session.getAttribute(HTTP_ATTRIBUTE_NAME)
            if (sessionValue != null) {
                nonce = sessionValue.toString()
            } else {
                nonce = generateNonce()
                session.setAttribute(HTTP_ATTRIBUTE_NAME, nonce)
            }
        }
        request.setAttribute(HTTP_ATTRIBUTE_NAME, nonce)
        nonce
    }

    /**
     * Read the nonce value previously stored for the request, if any.
     *
     * @param request current request
     * @return the nonce value, or null if none was stored
     */
    static String getNonce(final HttpServletRequest request) {
        Object value = request?.getAttribute(HTTP_ATTRIBUTE_NAME)
        value != null ? value.toString() : null
    }
}
