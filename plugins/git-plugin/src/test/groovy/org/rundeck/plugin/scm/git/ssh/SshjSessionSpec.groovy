/*
 * Copyright 2016 SimplifyOps, Inc. (http://simplifyops.com)
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

import org.eclipse.jgit.errors.TransportException
import org.eclipse.jgit.transport.URIish
import spock.lang.Specification

import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Covers two correctness fixes from RUN-4881:
 *  - StrictHostKeyChecking only enables verification for the literal value "yes" (verifies
 *    only when explicitly requested, not fail-open or fail-closed on a missing/other value).
 *  - the port handling bug where a primitive-int `port != null` check was always true after
 *    autoboxing, sending ssh.connect(host, -1) instead of falling back to the default port.
 *
 * Both are exercised without a real SSH server: "user.home" is redirected to an empty temp
 * directory so SSHJ's default known_hosts lookup is deterministic regardless of the machine
 * running the test, and port handling is verified via loopback sockets this test controls.
 */
class SshjSessionSpec extends Specification {
    File fakeHome
    String realHome

    def setup() {
        realHome = System.getProperty("user.home")
        fakeHome = File.createTempDir("SshjSessionSpec", "-home")
        System.setProperty("user.home", fakeHome.absolutePath)
    }

    def cleanup() {
        System.setProperty("user.home", realHome)
        fakeHome.deleteDir()
    }

    def "StrictHostKeyChecking=yes loads known_hosts and fails fast when none exists"() {
        given:
        def uri = new URIish('ssh://nonexistent-host.invalid/repo.git')

        when:
        new SshjSession(uri, [StrictHostKeyChecking: 'yes'], null, null)

        then: "it never gets as far as attempting a connection"
        def e = thrown(TransportException)
        e.cause.message == 'Could not load known_hosts'
    }

    def "a missing or non-yes StrictHostKeyChecking value skips known_hosts and attempts to connect directly"() {
        given: "a definitely-unreachable loopback port, so connect() fails fast"
        def freePort = unusedLoopbackPort()
        def uri = new URIish("ssh://127.0.0.1:${freePort}/repo.git")

        when:
        new SshjSession(uri, sshConfig, null, null)

        then: "it got past host-key loading and attempted a real connection instead"
        def e = thrown(TransportException)
        e.cause.message != 'Could not load known_hosts'

        where:
        sshConfig << [[:], [StrictHostKeyChecking: 'no'], [StrictHostKeyChecking: 'ask'], [StrictHostKeyChecking: 'other']]
    }

    def "falls back to the default port instead of passing an invalid negative port"() {
        given: "no port configured on the uri"
        def uri = new URIish('ssh://127.0.0.1/repo.git')
        assert uri.port == -1

        when: "whatever happens when actually connecting (success or a network-level failure) is" +
                " irrelevant here and depends on the test machine; only the port value matters"
        Throwable caught = null
        try {
            new SshjSession(uri, [:], null, null)
        } catch (Throwable t) {
            caught = t
        }

        then: "never the raw IllegalArgumentException an invalid negative port would throw" +
                " (sshj validates the port, via InetSocketAddress, before attempting any connection)"
        !(caught instanceof IllegalArgumentException)
    }

    def "honors an explicitly configured port"() {
        given: "a loopback server bound to a specific, otherwise-unused port"
        def server = new ServerSocket(0, 1, InetAddress.getByName('127.0.0.1'))
        def port = server.localPort
        def accepted = new AtomicBoolean(false)
        def acceptor = Thread.start {
            try {
                def socket = server.accept()
                accepted.set(true)
                socket.close()
            } catch (IOException ignored) {
                //server closed while waiting; fine
            }
        }
        def uri = new URIish("ssh://127.0.0.1:${port}/repo.git")

        when:
        try {
            new SshjSession(uri, [:], null, null)
        } catch (Exception ignored) {
            //the stub server doesn't speak SSH, so the handshake is expected to fail;
            //only whether it connected to the right port matters here
        }
        acceptor.join(TimeUnit.SECONDS.toMillis(5))

        then:
        accepted.get()

        cleanup:
        server.close()
    }

    private static int unusedLoopbackPort() {
        def socket = new ServerSocket(0, 1, InetAddress.getByName('127.0.0.1'))
        try {
            return socket.localPort
        } finally {
            socket.close()
        }
    }
}
