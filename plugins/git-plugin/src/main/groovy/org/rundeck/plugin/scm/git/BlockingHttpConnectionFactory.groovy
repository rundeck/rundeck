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
import org.eclipse.jgit.transport.http.HttpConnectionFactory
import org.eclipse.jgit.transport.http.JDKHttpConnectionFactory

/**
 * JGit HTTP factory that rejects internal addresses on every connection, including redirect targets,
 * and opens the socket to the address from that check.
 * Installed per {@code TransportHttp}, not as the JVM-wide factory.
 */
@CompileStatic
class BlockingHttpConnectionFactory implements HttpConnectionFactory {

    private final HttpConnectionFactory delegate

    BlockingHttpConnectionFactory() {
        this(new JDKHttpConnectionFactory())
    }

    BlockingHttpConnectionFactory(HttpConnectionFactory delegate) {
        this.delegate = delegate
    }

    @Override
    HttpConnection create(URL url) throws IOException {
        return open(url, null)
    }

    @Override
    HttpConnection create(URL url, Proxy proxy) throws IOException {
        return open(url, proxy)
    }

    private HttpConnection open(URL url, Proxy proxy) throws IOException {
        InetAddress pinned = InternalAddressGuard.pinPublic(url?.host)
        URL bound = PinnedHttp.bind(url, pinned)
        HttpConnection created = proxy == null ? delegate.create(bound) : delegate.create(bound, proxy)
        return new GuardingHttpConnection(created, pinned)
    }
}
