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

import com.jcraft.jsch.SocketFactory
import groovy.transform.CompileStatic

/**
 * JSch socket factory that connects to a previously checked address.
 */
@CompileStatic
class JschPinnedSocketFactory implements SocketFactory {

    private final InetAddress pinned

    JschPinnedSocketFactory(InetAddress pinned) {
        this.pinned = pinned
    }

    @Override
    Socket createSocket(String host, int port) throws IOException {
        Socket socket = new Socket()
        socket.connect(new InetSocketAddress(pinned, port))
        return socket
    }

    @Override
    InputStream getInputStream(Socket socket) throws IOException {
        return socket.getInputStream()
    }

    @Override
    OutputStream getOutputStream(Socket socket) throws IOException {
        return socket.getOutputStream()
    }
}
