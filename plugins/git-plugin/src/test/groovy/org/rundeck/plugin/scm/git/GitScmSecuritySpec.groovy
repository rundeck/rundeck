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

import com.dtolabs.rundeck.core.common.PropertyRetriever
import com.dtolabs.rundeck.plugins.scm.ScmPluginException
import org.eclipse.jgit.errors.TransportException
import spock.lang.Specification

class GitScmSecuritySpec extends Specification {

    def cleanup() {
        System.clearProperty(GitScmSecurityConfig.ALLOWED_SCHEMES)
        System.clearProperty(GitScmSecurityConfig.BLOCK_INTERNAL)
    }

    def "unset properties leave every scheme allowed and do not block internal addresses"() {
        when:
        def config = GitScmSecurityConfig.resolve(null)

        then:
        config.allowedSchemes == null
        !config.blockInternalAddresses
        GitUrlPolicy.canonicalScheme(new org.eclipse.jgit.transport.URIish('/tmp/repo.git')) == 'file'
        GitUrlPolicy.canonicalScheme(new org.eclipse.jgit.transport.URIish('git@github.com:org/repo.git')) == 'ssh'
        GitUrlPolicy.canonicalScheme(new org.eclipse.jgit.transport.URIish('host:repo.git')) == 'ssh'
        GitUrlPolicy.assertAllowed('/tmp/repo.git', config)
        GitUrlPolicy.assertAllowed('git://github.com/org/repo.git', config)
        GitUrlPolicy.assertAllowed('ftp://host/repo.git', config)
    }

    def "allowlist accepts https http and ssh including scp syntax and rejects other schemes"() {
        given:
        def config = GitScmSecurityConfig.resolve(retriever(GitScmSecurityConfig.ALLOWED_SCHEMES, 'https, http, ssh'))

        expect:
        GitUrlPolicy.assertAllowed(allowed, config)

        when:
        GitUrlPolicy.assertAllowed(rejected, config)

        then:
        def error = thrown(ScmPluginException)
        error.message == "Git URL scheme '${scheme}' is not allowed."

        where:
        allowed                          | rejected                         | scheme
        'https://github.com/org/repo.git'| '/tmp/repo.git'                  | 'file'
        'http://git.internal/repo.git'   | 'file:///tmp/repo.git'           | 'file'
        'ssh://git@github.com/org/repo.git' | 'git://github.com/org/repo.git' | 'git'
        'git@github.com:org/repo.git'    | 'ftp://host/repo.git'            | 'ftp'
        'host:repo.git'                  | 'amazon-s3://bucket/repo.git'    | 'amazon-s3'
    }

    def "framework property wins over a system property"() {
        given:
        System.setProperty(GitScmSecurityConfig.ALLOWED_SCHEMES, 'https')
        def config = GitScmSecurityConfig.resolve(retriever(GitScmSecurityConfig.ALLOWED_SCHEMES, 'http,https,ssh'))

        expect:
        config.allowedSchemes == ['http', 'https', 'ssh'] as Set
        GitUrlPolicy.assertAllowed('http://git.internal/repo.git', config)
    }

    def "system property is used when the retriever has no value"() {
        given:
        System.setProperty(GitScmSecurityConfig.BLOCK_INTERNAL, 'true')

        expect:
        GitScmSecurityConfig.resolve(retriever('other', 'x')).blockInternalAddresses
    }

    def "transport failures are replaced and branch-not-found is kept"() {
        given:
        def leaked = new TransportException('invalid advertisement of secret-line')
        def missing = new TransportException("Remote branch 'dev2' not found in upstream origin")
        def blocked = new TransportException('wrapped', new IOException(InternalAddressGuard.HOST_NOT_ALLOWED))

        expect:
        GitTransportErrors.userFacing(leaked, 'main') == GitTransportErrors.GENERIC_ACCESS_MESSAGE
        GitTransportErrors.userFacing(missing, 'dev2') == null
        GitTransportErrors.userFacing(blocked, 'main') == InternalAddressGuard.HOST_NOT_ALLOWED
        GitTransportErrors.userFacing(new IllegalStateException('No changes to local git repo'), 'main') == null
    }

    def "internal addresses are blocked and public literals are not"() {
        expect:
        InternalAddressGuard.isBlocked(InetAddress.getByName(blocked))
        !InternalAddressGuard.isBlocked(InetAddress.getByName('1.1.1.1'))

        where:
        blocked << ['127.0.0.1', '10.1.2.3', '192.168.1.9', '172.16.0.4', '169.254.169.254', '::1', 'fd00::1']
    }

    def "ipv4-mapped private addresses are blocked"() {
        given:
        byte[] mapped = new byte[16]
        mapped[10] = (byte) 0xff
        mapped[11] = (byte) 0xff
        mapped[12] = (byte) 10
        mapped[15] = (byte) 1

        expect:
        InternalAddressGuard.isBlocked(InetAddress.getByAddress(mapped))
    }

    def "pinPublic returns a literal public address and rejects a private one"() {
        expect:
        InternalAddressGuard.pinPublic('1.1.1.1').hostAddress == '1.1.1.1'

        when:
        InternalAddressGuard.pinPublic('10.1.2.3')

        then:
        def error = thrown(IOException)
        error.message == InternalAddressGuard.HOST_NOT_ALLOWED
        !error.message.contains('10.1.2.3')
    }

    def "pinned http connects to the checked address"() {
        given:
        ServerSocket server = new ServerSocket(0)
        InetAddress pinned = InetAddress.getByAddress('example.test', [127, 0, 0, 1] as byte[])
        URL original = new URL("http://example.test:${server.localPort}/info/refs")
        URL bound = PinnedHttp.bind(original, pinned)
        Socket accepted = null
        Thread listener = Thread.start {
            accepted = server.accept()
        }

        when:
        URLConnection connection = bound.openConnection()
        connection.connectTimeout = 1000
        connection.readTimeout = 1000
        try {
            connection.connect()
        } catch (IOException ignored) {
        }
        listener.join(2000)

        then:
        accepted != null
        accepted.localAddress.isLoopbackAddress()

        cleanup:
        accepted?.close()
        server.close()
    }

    def "assertPublicHost does not include the resolved address"() {
        when:
        InternalAddressGuard.assertPublicHost('127.0.0.1')

        then:
        def error = thrown(IOException)
        error.message == InternalAddressGuard.HOST_NOT_ALLOWED
        !error.message.contains('127.0.0.1')
    }

    def "blocking http factory rejects an internal host before opening the delegate"() {
        given:
        def delegate = Mock(org.eclipse.jgit.transport.http.HttpConnectionFactory)
        def factory = new BlockingHttpConnectionFactory(delegate)

        when:
        factory.create(new URL('http://127.0.0.1/info/refs?service=git-upload-pack'))

        then:
        thrown(IOException)
        0 * delegate.create(_)
    }

    private static PropertyRetriever retriever(String key, String value) {
        return new PropertyRetriever() {
            @Override
            String getProperty(String name) {
                return name == key ? value : null
            }
        }
    }
}
