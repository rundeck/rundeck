package com.dtolabs.rundeck.core.utils

import com.dtolabs.rundeck.core.common.NodeEntryImpl
import spock.lang.Specification

/**
 * Tests for {@link NodeSet}
 */
class NodeSetSpec extends Specification {

    private static NodeEntryImpl node(String name, String hostname, List<String> tags) {
        new NodeEntryImpl(nodename: name, hostname: hostname, tags: tags as Set)
    }

    def "exclude filter removes nodes independently of the filter exclusions"() {
        given:
        def nodeset = NodeSet.fromFilter('tags: web !tags: db')
        nodeset.exclude.dominant = true
        nodeset.excludeFilter = NodeSet.fromFilter('hostname: testhost1')

        expect:
        nodeset.acceptNode(node('app1', 'app1', ['web']))
        !nodeset.acceptNode(node('db1', 'db1', ['web', 'db']))
        !nodeset.acceptNode(node('test1', 'testhost1', ['web']))
        !nodeset.acceptNode(node('other1', 'other1', ['prod']))
    }

    def "exclude filter with blank filter excludes only matching nodes"() {
        given:
        def nodeset = new NodeSet()
        nodeset.excludeFilter = NodeSet.fromFilter('tags: db')

        expect:
        !nodeset.blank
        nodeset.acceptNode(node('app1', 'app1', ['web']))
        !nodeset.acceptNode(node('db1', 'db1', ['db']))
    }

    def "blank exclude filter has no effect"() {
        given:
        def nodeset = NodeSet.fromFilter('tags: web')
        nodeset.excludeFilter = new NodeSet()

        expect:
        nodeset.acceptNode(node('app1', 'app1', ['web']))
        !nodeset.acceptNode(node('other1', 'other1', ['prod']))
    }

    def "single node name ignores exclude filter"() {
        given:
        def nodeset = new NodeSet('app1')
        nodeset.excludeFilter = NodeSet.fromFilter('name: app1')

        expect:
        nodeset.acceptNode(node('app1', 'app1', ['web']))
    }
}
