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
import org.eclipse.jgit.transport.http.HttpConnection

import javax.net.ssl.HostnameVerifier
import javax.net.ssl.KeyManager
import javax.net.ssl.TrustManager
import java.security.KeyManagementException
import java.security.NoSuchAlgorithmException
import java.security.SecureRandom

/**
 * Re-checks the Git host immediately before the HTTP connection performs network IO.
 */
@CompileStatic
class GuardingHttpConnection implements HttpConnection {

    private final HttpConnection delegate
    private final String host

    GuardingHttpConnection(HttpConnection delegate, String host) {
        this.delegate = delegate
        this.host = host
    }

    private void check() throws IOException {
        InternalAddressGuard.assertPublicHost(host)
    }

    @Override
    int getResponseCode() throws IOException {
        check()
        return delegate.getResponseCode()
    }

    @Override
    URL getURL() {
        return delegate.getURL()
    }

    @Override
    String getResponseMessage() throws IOException {
        check()
        return delegate.getResponseMessage()
    }

    @Override
    Map<String, List<String>> getHeaderFields() {
        return delegate.getHeaderFields()
    }

    @Override
    void setRequestProperty(String key, String value) {
        delegate.setRequestProperty(key, value)
    }

    @Override
    void setRequestMethod(String method) throws ProtocolException {
        delegate.setRequestMethod(method)
    }

    @Override
    void setUseCaches(boolean usecaches) {
        delegate.setUseCaches(usecaches)
    }

    @Override
    void setConnectTimeout(int timeout) {
        delegate.setConnectTimeout(timeout)
    }

    @Override
    void setReadTimeout(int timeout) {
        delegate.setReadTimeout(timeout)
    }

    @Override
    String getContentType() {
        return delegate.getContentType()
    }

    @Override
    InputStream getInputStream() throws IOException {
        check()
        return delegate.getInputStream()
    }

    @Override
    String getHeaderField(String name) {
        return delegate.getHeaderField(name)
    }

    @Override
    List<String> getHeaderFields(String name) {
        return delegate.getHeaderFields(name)
    }

    @Override
    int getContentLength() {
        return delegate.getContentLength()
    }

    @Override
    void setInstanceFollowRedirects(boolean followRedirects) {
        delegate.setInstanceFollowRedirects(followRedirects)
    }

    @Override
    void setDoOutput(boolean dooutput) {
        delegate.setDoOutput(dooutput)
    }

    @Override
    void setFixedLengthStreamingMode(int contentLength) {
        delegate.setFixedLengthStreamingMode(contentLength)
    }

    @Override
    OutputStream getOutputStream() throws IOException {
        check()
        return delegate.getOutputStream()
    }

    @Override
    void setChunkedStreamingMode(int chunklen) {
        delegate.setChunkedStreamingMode(chunklen)
    }

    @Override
    String getRequestMethod() {
        return delegate.getRequestMethod()
    }

    @Override
    boolean usingProxy() {
        return delegate.usingProxy()
    }

    @Override
    void connect() throws IOException {
        check()
        delegate.connect()
    }

    @Override
    void configure(KeyManager[] keyManagers, TrustManager[] trustManagers, SecureRandom random)
            throws NoSuchAlgorithmException, KeyManagementException {
        delegate.configure(keyManagers, trustManagers, random)
    }

    @Override
    void setHostnameVerifier(HostnameVerifier hostnameVerifier)
            throws NoSuchAlgorithmException, KeyManagementException {
        delegate.setHostnameVerifier(hostnameVerifier)
    }
}
