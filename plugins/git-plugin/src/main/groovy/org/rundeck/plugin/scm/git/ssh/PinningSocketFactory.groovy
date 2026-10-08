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

package org.rundeck.plugin.scm.git.ssh

import groovy.transform.CompileStatic

import javax.net.SocketFactory

/**
 * Socket factory whose sockets connect to a previously checked address.
 * The address passed to {@link Socket#connect} is not used for the remote endpoint.
 */
@CompileStatic
class PinningSocketFactory extends SocketFactory {

    private final InetAddress pinned

    PinningSocketFactory(InetAddress pinned) {
        this.pinned = pinned
    }

    @Override
    Socket createSocket() {
        return new PinnedSocket(pinned)
    }

    @Override
    Socket createSocket(String host, int port) throws IOException {
        Socket socket = createSocket()
        socket.connect(new InetSocketAddress(pinned, port))
        return socket
    }

    @Override
    Socket createSocket(String host, int port, InetAddress localAddress, int localPort) throws IOException {
        Socket socket = createSocket()
        socket.bind(new InetSocketAddress(localAddress, localPort))
        socket.connect(new InetSocketAddress(pinned, port))
        return socket
    }

    @Override
    Socket createSocket(InetAddress address, int port) throws IOException {
        return createSocket(address?.hostAddress, port)
    }

    @Override
    Socket createSocket(InetAddress address, int port, InetAddress localAddress, int localPort) throws IOException {
        return createSocket(address?.hostAddress, port, localAddress, localPort)
    }

    @CompileStatic
    private static class PinnedSocket extends Socket {
        private final InetAddress pinned

        PinnedSocket(InetAddress pinned) {
            this.pinned = pinned
        }

        @Override
        void connect(SocketAddress endpoint, int timeout) throws IOException {
            int port = endpoint instanceof InetSocketAddress ? ((InetSocketAddress) endpoint).port : 0
            super.connect(new InetSocketAddress(pinned, port), timeout)
        }
    }
}
