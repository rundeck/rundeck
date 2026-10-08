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

import javax.net.ssl.SSLSocketFactory

/**
 * Opens the TCP socket to a previously checked address and uses the original host name for TLS.
 */
@CompileStatic
class PinnedSslSocketFactory extends SSLSocketFactory {

    private final InetAddress pinned
    private final String originalHost
    private final SSLSocketFactory delegate

    PinnedSslSocketFactory(InetAddress pinned, String originalHost, SSLSocketFactory delegate) {
        this.pinned = pinned
        this.originalHost = originalHost
        this.delegate = delegate
    }

    @Override
    String[] getDefaultCipherSuites() {
        return delegate.defaultCipherSuites
    }

    @Override
    String[] getSupportedCipherSuites() {
        return delegate.supportedCipherSuites
    }

    @Override
    Socket createSocket(Socket socket, String host, int port, boolean autoClose) throws IOException {
        return delegate.createSocket(socket, originalHost, port, autoClose)
    }

    @Override
    Socket createSocket(String host, int port) throws IOException {
        return layer(connectTcp(port, null, 0), port)
    }

    @Override
    Socket createSocket(String host, int port, InetAddress localAddress, int localPort) throws IOException {
        return layer(connectTcp(port, localAddress, localPort), port)
    }

    @Override
    Socket createSocket(InetAddress address, int port) throws IOException {
        return layer(connectTcp(port, null, 0), port)
    }

    @Override
    Socket createSocket(InetAddress address, int port, InetAddress localAddress, int localPort) throws IOException {
        return layer(connectTcp(port, localAddress, localPort), port)
    }

    private Socket connectTcp(int port, InetAddress localAddress, int localPort) throws IOException {
        Socket tcp = new Socket()
        if (localAddress != null) {
            tcp.bind(new InetSocketAddress(localAddress, localPort))
        }
        tcp.connect(new InetSocketAddress(pinned, port))
        return tcp
    }

    private Socket layer(Socket tcp, int port) throws IOException {
        return delegate.createSocket(tcp, originalHost, port, true)
    }
}
