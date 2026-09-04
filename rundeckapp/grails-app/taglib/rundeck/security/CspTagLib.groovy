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

package rundeck.security

import org.rundeck.grails.plugins.securityheaders.CspNonceProvider

/**
 * Tags exposing Content-Security-Policy values to GSP views.
 */
class CspTagLib {
    def static namespace = "security"

    /**
     * Emit the Content-Security-Policy nonce generated for the current request, for use as the
     * `nonce` attribute of an inline script tag, e.g.
     * {@code <script nonce="${security.cspNonce()}">}.
     *
     * Emits nothing when no nonce is available for the request (e.g. security headers disabled).
     */
    def cspNonce = { attrs, body ->
        String nonce = CspNonceProvider.getNonce(request)
        if (nonce) {
            out << nonce
        }
    }
}
