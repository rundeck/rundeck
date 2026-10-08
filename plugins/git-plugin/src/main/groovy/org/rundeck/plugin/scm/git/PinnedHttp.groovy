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

import groovy.transform.CompileStatic

import javax.net.ssl.HostnameVerifier
import javax.net.ssl.HttpsURLConnection
import javax.net.ssl.SSLSession

/**
 * Builds an HTTP URL that opens its socket to an address already checked by {@link InternalAddressGuard}.
 * The original host name is kept for TLS.
 */
@CompileStatic
class PinnedHttp {

    /**
     * @param original Git HTTP URL
     * @param pinned address returned for {@code original.host}
     * @return URL whose connection uses {@code pinned}
     */
    static URL bind(URL original, InetAddress pinned) {
        URLStreamHandler handler = new URLStreamHandler() {
            @Override
            protected URLConnection openConnection(URL url) throws IOException {
                return openPinned(original, pinned, null)
            }

            @Override
            protected URLConnection openConnection(URL url, Proxy proxy) throws IOException {
                return openPinned(original, pinned, proxy)
            }
        }
        return new URL(null, original.toExternalForm(), handler)
    }

    private static URLConnection openPinned(URL original, InetAddress pinned, Proxy proxy) throws IOException {
        URL literal = literalUrl(original, pinned)
        HttpURLConnection connection = proxy == null
                ? (HttpURLConnection) literal.openConnection()
                : (HttpURLConnection) literal.openConnection(proxy)
        if (connection instanceof HttpsURLConnection) {
            HttpsURLConnection https = (HttpsURLConnection) connection
            https.setSSLSocketFactory(new PinnedSslSocketFactory(pinned, original.host, https.SSLSocketFactory))
            https.setHostnameVerifier(new OriginalHostVerifier(original.host, https.hostnameVerifier))
        }
        return connection
    }

    private static URL literalUrl(URL original, InetAddress pinned) throws MalformedURLException {
        String host = pinned instanceof Inet6Address ? "[${pinned.hostAddress}]" : pinned.hostAddress
        if (original.port > 0) {
            return new URL(original.protocol, host, original.port, original.file)
        }
        return new URL(original.protocol, host, original.file)
    }

    @CompileStatic
    private static class OriginalHostVerifier implements HostnameVerifier {
        private final String host
        private final HostnameVerifier delegate

        OriginalHostVerifier(String host, HostnameVerifier delegate) {
            this.host = host
            this.delegate = delegate
        }

        @Override
        boolean verify(String hostname, SSLSession session) {
            return delegate.verify(host, session)
        }
    }
}
