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

package com.dtolabs.shared.resources

import com.sun.net.httpserver.HttpServer
import spock.lang.Specification

import java.net.InetSocketAddress
import java.util.concurrent.atomic.AtomicBoolean

class ResourceXMLParserSpec extends Specification {

    File dnefile1
    File testfile1
    File testfile2
    File legitimateDtdFile

    def setup() {
        dnefile1 = new File("test-does-not-exist.xml")
        testfile1 = new File("src/test/resources/com/dtolabs/shared/resources/test-resources1.xml")
        testfile2 = new File("src/test/resources/com/dtolabs/shared/resources/test-resources2.xml")
        legitimateDtdFile = new File("src/test/resources/com/dtolabs/shared/resources/test-resources-legitimate-dtd.xml")
    }

    ResourceXMLReceiver collectingReceiver(final List<ResourceXMLParser.Entity> items, final boolean continueParsing = true) {
        new ResourceXMLReceiver() {
            @Override
            boolean resourceParsed(final ResourceXMLParser.Entity entity) {
                items << entity
                return continueParsing
            }

            @Override
            void resourcesParsed(final ResourceXMLParser.EntitySet entities) {
            }
        }
    }

    def "parse throws FileNotFoundException for missing file"() {
        given:
        def parser = new ResourceXMLParser(dnefile1)

        when:
        parser.parse()

        then:
        thrown(FileNotFoundException)
    }

    def "parse succeeds with no receiver"() {
        given:
        def parser = new ResourceXMLParser(testfile1)

        when:
        parser.parse()

        then:
        noExceptionThrown()
    }

    def "parse invokes receiver callbacks"() {
        given:
        def parser = new ResourceXMLParser(testfile1)
        def items = []
        boolean resourcesParsedCalled = false
        parser.setReceiver(new ResourceXMLReceiver() {
            @Override
            boolean resourceParsed(final ResourceXMLParser.Entity entity) {
                items << entity
                return true
            }

            @Override
            void resourcesParsed(final ResourceXMLParser.EntitySet entities) {
                resourcesParsedCalled = true
            }
        })

        when:
        parser.parse()

        then:
        resourcesParsedCalled
        items.size() == 1
    }

    def "parse stops early when receiver returns false"() {
        given:
        def parser = new ResourceXMLParser(testfile1)
        def items = []
        boolean resourcesParsedCalled = false
        parser.setReceiver(new ResourceXMLReceiver() {
            @Override
            boolean resourceParsed(final ResourceXMLParser.Entity entity) {
                items << entity
                return false
            }

            @Override
            void resourcesParsed(final ResourceXMLParser.EntitySet entities) {
                resourcesParsedCalled = true
            }
        })

        when:
        parser.parse()

        then:
        resourcesParsedCalled
        items.size() == 1
    }

    def "parse reads all node attributes"() {
        given:
        def parser = new ResourceXMLParser(testfile2)
        def items = []
        parser.setReceiver(collectingReceiver(items))

        when:
        parser.parse()

        then:
        items.size() == 1
        def entity = items[0]
        entity.getResourceType() == "node"
        entity.getName() == "node1"
        entity.getProperties().size() == 14
        entity.getProperty(ResourceXMLConstants.COMMON_NAME) == "node1"
        entity.getProperty(ResourceXMLConstants.COMMON_DESCRIPTION) == "description1"
        entity.getProperty(ResourceXMLConstants.COMMON_TAGS) == "tag1,tag2"
        entity.getProperty(ResourceXMLConstants.NODE_HOSTNAME) == "hostname1"
        entity.getProperty(ResourceXMLConstants.NODE_USERNAME) == "username1"
        entity.getProperty(ResourceXMLConstants.NODE_OS_ARCH) == "osArch1"
        entity.getProperty(ResourceXMLConstants.NODE_OS_FAMILY) == "osFamily1"
        entity.getProperty(ResourceXMLConstants.NODE_OS_NAME) == "osName1"
        entity.getProperty(ResourceXMLConstants.NODE_OS_VERSION) == "osVersion1"
        entity.getProperty(ResourceXMLConstants.NODE_EDIT_URL) == "EditURL"
        entity.getProperty(ResourceXMLConstants.NODE_REMOTE_URL) == "RemoteURL"
        entity.getProperty("testattribute") == "testvalue"
        entity.getProperty("testattribute2") == "test value2"
        entity.getProperty("testattribute3") == "test value3"
    }

    def "legitimate bundled project DTD still parses"() {
        given:
        def parser = new ResourceXMLParser(legitimateDtdFile)
        def items = []
        parser.setReceiver(collectingReceiver(items))

        when:
        parser.parse()

        then:
        noExceptionThrown()
        items.size() == 1
        items[0].getName() == "node1"
    }

    def "PUBLIC file:// entity does not leak local file content"() {
        given:
        def secretFile = File.createTempFile("xxe-secret", ".txt")
        secretFile.deleteOnExit()
        secretFile.text = "TOP-SECRET-SENTINEL-VALUE"
        def xml = """<?xml version="1.0"?>
<!DOCTYPE project [
  <!ENTITY xxe PUBLIC "-//attacker//TEXT" "${secretFile.toURI()}">
]>
<project>
  <node name="node1" type="Node1">
    <attribute name="leaked">&xxe;</attribute>
  </node>
</project>
"""
        def parser = new ResourceXMLParser(new ByteArrayInputStream(xml.getBytes("UTF-8")))
        def items = []
        parser.setReceiver(collectingReceiver(items))
        Exception caught = null

        when:
        try {
            parser.parse()
        } catch (Exception e) {
            caught = e
        }

        then:
        !(caught instanceof NullPointerException)
        items.every { !(it.getProperty("leaked") ?: "").contains("TOP-SECRET-SENTINEL-VALUE") }

        cleanup:
        secretFile.delete()
    }

    def "PUBLIC external parameter-entity chain does not trigger outbound HTTP request"() {
        given:
        def contacted = new AtomicBoolean(false)
        def server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0)
        server.createContext("/evil.dtd", { exchange ->
            contacted.set(true)
            exchange.sendResponseHeaders(200, 0)
            exchange.getResponseBody().close()
        })
        server.start()
        def port = server.getAddress().getPort()
        def xml = """<?xml version="1.0"?>
<!DOCTYPE project [
  <!ENTITY % ext PUBLIC "-//attacker//DTD" "http://127.0.0.1:${port}/evil.dtd">
  %ext;
]>
<project>
  <node name="node1" type="Node1"/>
</project>
"""
        def parser = new ResourceXMLParser(new ByteArrayInputStream(xml.getBytes("UTF-8")))

        when:
        try {
            parser.parse()
        } catch (Exception ignored) {
        }

        then:
        !contacted.get()

        cleanup:
        server.stop(0)
    }

    def "EntityResolver.resolveEntity does not throw NullPointerException for null publicId (SYSTEM entity)"() {
        given:
        def resolver = ResourceXMLParser.createEntityResolver()

        when:
        def result = resolver.resolveEntity(null, "file:///etc/hostname")

        then:
        noExceptionThrown()
        result != null
    }

    def "SYSTEM-only entity fails cleanly without leaking file content"() {
        given:
        def secretFile = File.createTempFile("xxe-secret-system", ".txt")
        secretFile.deleteOnExit()
        secretFile.text = "TOP-SECRET-SENTINEL-VALUE-SYSTEM"
        def xml = """<?xml version="1.0"?>
<!DOCTYPE project [
  <!ENTITY xxe SYSTEM "${secretFile.toURI()}">
]>
<project>
  <node name="node1" type="Node1">
    <attribute name="leaked">&xxe;</attribute>
  </node>
</project>
"""
        def parser = new ResourceXMLParser(new ByteArrayInputStream(xml.getBytes("UTF-8")))
        def items = []
        parser.setReceiver(collectingReceiver(items))
        Exception caught = null

        when:
        try {
            parser.parse()
        } catch (Exception e) {
            caught = e
        }

        then:
        !(caught instanceof NullPointerException)
        items.every { !(it.getProperty("leaked") ?: "").contains("TOP-SECRET-SENTINEL-VALUE-SYSTEM") }

        cleanup:
        secretFile.delete()
    }

    def "malformed DOCTYPE declaration fails via ResourceXMLParserException, not silently"() {
        given:
        // malformed internal subset: unterminated ENTITY declaration (missing closing '>')
        def xml = """<?xml version="1.0"?>
<!DOCTYPE project [
  <!ENTITY xxe PUBLIC "-//attacker//TEXT" "file:///etc/hostname"
]>
<project>
  <node name="node1" type="Node1"/>
</project>
"""
        def parser = new ResourceXMLParser(new ByteArrayInputStream(xml.getBytes("UTF-8")))
        def items = []
        parser.setReceiver(collectingReceiver(items))

        when:
        parser.parse()

        then:
        thrown(ResourceXMLParserException)
    }
}
