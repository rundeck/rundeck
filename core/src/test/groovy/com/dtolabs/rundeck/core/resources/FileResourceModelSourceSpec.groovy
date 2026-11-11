/*
 * Copyright 2026 PagerDuty, Inc.
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

package com.dtolabs.rundeck.core.resources

import com.dtolabs.rundeck.core.common.Framework
import com.dtolabs.rundeck.core.resources.format.ResourceFormatParserService
import com.dtolabs.rundeck.core.resources.format.ResourceXMLFormatParser
import com.dtolabs.shared.resources.ResourceXMLGenerator
import com.dtolabs.shared.resources.ResourceXMLParser
import spock.lang.Specification

/**
 * Verifies file resource parsing behavior that is specific to Rundeck-managed node caches.
 */
class FileResourceModelSourceSpec extends Specification {
    private static final String MAX_GENERAL_ENTITY_SIZE_LIMIT = 'jdk.xml.maxGeneralEntitySizeLimit'
    private static final String TOTAL_ENTITY_SIZE_LIMIT = 'jdk.xml.totalEntitySizeLimit'

    def "cache XML reads escaped values larger than Java 25 entity limits"() {
        given:
        File cacheFile = File.createTempFile('rundeck-node-cache', '.xml')
        cacheFile.deleteOnExit()
        String value = ('&' * 100_001) + '"quoted" <tagged>'
        ResourceXMLParser.Entity entity = new ResourceXMLParser.Entity()
        entity.setName('node1')
        entity.setResourceType('node')
        entity.setProperty('largeAttribute', value)
        ResourceXMLGenerator generator = new ResourceXMLGenerator(cacheFile)
        generator.addEntity(entity)
        generator.generate()

        ResourceFormatParserService parserService = Mock(ResourceFormatParserService) {
            getParserForFormat(ResourceXMLFormatParser.SERVICE_PROVIDER_TYPE) >> new ResourceXMLFormatParser()
        }
        Framework framework = Mock(Framework) {
            getResourceFormatParserService() >> parserService
        }
        Properties properties = new Properties()
        properties.setProperty(FileResourceModelSource.Configuration.PROJECT, 'test-project')
        properties.setProperty(FileResourceModelSource.Configuration.FILE, cacheFile.absolutePath)
        properties.setProperty(
            FileResourceModelSource.Configuration.FORMAT,
            ResourceXMLFormatParser.SERVICE_PROVIDER_TYPE
        )
        properties = FileResourceModelSource.enableCacheXmlEntityLimits(properties)
        FileResourceModelSource source = new FileResourceModelSource(framework)
        source.configure(properties)

        String previousMaxGeneralLimit = System.getProperty(MAX_GENERAL_ENTITY_SIZE_LIMIT)
        String previousTotalLimit = System.getProperty(TOTAL_ENTITY_SIZE_LIMIT)
        System.setProperty(MAX_GENERAL_ENTITY_SIZE_LIMIT, '100000')
        System.setProperty(TOTAL_ENTITY_SIZE_LIMIT, '100000')

        when:
        def nodes = source.getNodes()

        then:
        nodes.getNode('node1').getAttribute('largeAttribute') == value
        System.getProperty(MAX_GENERAL_ENTITY_SIZE_LIMIT) == '100000'
        System.getProperty(TOTAL_ENTITY_SIZE_LIMIT) == '100000'

        cleanup:
        restoreSystemProperty(MAX_GENERAL_ENTITY_SIZE_LIMIT, previousMaxGeneralLimit)
        restoreSystemProperty(TOTAL_ENTITY_SIZE_LIMIT, previousTotalLimit)
        cacheFile.delete()
    }

    def "project configuration cannot enable cache XML entity limits"() {
        given:
        File resourceFile = File.createTempFile('rundeck-node-source', '.xml')
        resourceFile.deleteOnExit()
        ResourceXMLParser.Entity entity = new ResourceXMLParser.Entity()
        entity.setName('node1')
        entity.setResourceType('node')
        entity.setProperty('largeAttribute', '&' * 100_001)
        ResourceXMLGenerator generator = new ResourceXMLGenerator(resourceFile)
        generator.addEntity(entity)
        generator.generate()

        ResourceFormatParserService parserService = Mock(ResourceFormatParserService) {
            getParserForFormat(ResourceXMLFormatParser.SERVICE_PROVIDER_TYPE) >> new ResourceXMLFormatParser()
        }
        Framework framework = Mock(Framework) {
            getResourceFormatParserService() >> parserService
        }
        Properties properties = new Properties()
        properties.setProperty(FileResourceModelSource.Configuration.PROJECT, 'test-project')
        properties.setProperty(FileResourceModelSource.Configuration.FILE, resourceFile.absolutePath)
        properties.setProperty(
            FileResourceModelSource.Configuration.FORMAT,
            ResourceXMLFormatParser.SERVICE_PROVIDER_TYPE
        )
        properties.setProperty('cacheXmlEntityLimitsEnabled', 'true')
        FileResourceModelSource source = new FileResourceModelSource(framework)
        source.configure(properties)

        String previousMaxGeneralLimit = System.getProperty(MAX_GENERAL_ENTITY_SIZE_LIMIT)
        String previousTotalLimit = System.getProperty(TOTAL_ENTITY_SIZE_LIMIT)
        System.setProperty(MAX_GENERAL_ENTITY_SIZE_LIMIT, '100000')
        System.setProperty(TOTAL_ENTITY_SIZE_LIMIT, '100000')

        when:
        source.getNodes()

        then:
        thrown(ResourceModelSourceException)
        System.getProperty(MAX_GENERAL_ENTITY_SIZE_LIMIT) == '100000'
        System.getProperty(TOTAL_ENTITY_SIZE_LIMIT) == '100000'

        cleanup:
        restoreSystemProperty(MAX_GENERAL_ENTITY_SIZE_LIMIT, previousMaxGeneralLimit)
        restoreSystemProperty(TOTAL_ENTITY_SIZE_LIMIT, previousTotalLimit)
        resourceFile.delete()
    }

    def "cache XML entity limits preserve external entity protection"() {
        given:
        File secretFile = File.createTempFile('rundeck-node-cache-secret', '.txt')
        secretFile.deleteOnExit()
        secretFile.text = 'TOP-SECRET-CACHE-SENTINEL'
        File cacheFile = File.createTempFile('rundeck-node-cache-xxe', '.xml')
        cacheFile.deleteOnExit()
        cacheFile.text = """<?xml version="1.0"?>
<!DOCTYPE project [
  <!ENTITY xxe SYSTEM "${secretFile.toURI()}">
]>
<project>
  <node name="node1" hostname="localhost">
    <attribute name="leaked">&xxe;</attribute>
  </node>
</project>
"""

        ResourceFormatParserService parserService = Mock(ResourceFormatParserService) {
            getParserForFormat(ResourceXMLFormatParser.SERVICE_PROVIDER_TYPE) >> new ResourceXMLFormatParser()
        }
        Framework framework = Mock(Framework) {
            getResourceFormatParserService() >> parserService
        }
        Properties properties = new Properties()
        properties.setProperty(FileResourceModelSource.Configuration.PROJECT, 'test-project')
        properties.setProperty(FileResourceModelSource.Configuration.FILE, cacheFile.absolutePath)
        properties.setProperty(
            FileResourceModelSource.Configuration.FORMAT,
            ResourceXMLFormatParser.SERVICE_PROVIDER_TYPE
        )
        properties = FileResourceModelSource.enableCacheXmlEntityLimits(properties)
        FileResourceModelSource source = new FileResourceModelSource(framework)
        source.configure(properties)
        Exception caught = null
        def nodes = null

        when:
        try {
            nodes = source.getNodes()
        } catch (Exception e) {
            caught = e
        }

        then:
        !(caught instanceof NullPointerException)
        nodes == null || !(nodes.getNode('node1')?.getAttribute('leaked') ?: '').contains(
            'TOP-SECRET-CACHE-SENTINEL'
        )

        cleanup:
        cacheFile.delete()
        secretFile.delete()
    }

    private static void restoreSystemProperty(final String name, final String value) {
        if (value == null) {
            System.clearProperty(name)
        } else {
            System.setProperty(name, value)
        }
    }
}
