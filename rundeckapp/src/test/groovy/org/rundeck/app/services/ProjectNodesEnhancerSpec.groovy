/*
 * Copyright 2019 Rundeck, Inc. (http://rundeck.com)
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

package org.rundeck.app.services

import com.dtolabs.rundeck.core.common.IProjectNodes
import com.dtolabs.rundeck.core.common.NodeEntryImpl
import com.dtolabs.rundeck.core.common.NodeSetImpl
import com.dtolabs.rundeck.plugins.nodes.NodeEnhancerPlugin
import spock.lang.Specification

class ProjectNodesEnhancerSpec extends Specification {

    def "get project nodes"() {
        given:
            def sut = new ProjectNodesEnhancer()
            def projectnodes = Mock(IProjectNodes)
            sut.projectNodes = projectnodes
            def nodeA = new NodeEntryImpl('nodeA')
            nodeA.attributes.put('attrA', 'valA1')
            nodeA.attributes.put('attrB', 'valB1')
            nodeA.getTags().add('tagA')
            nodeA.getTags().add('tagB')

            def nodeB = new NodeEntryImpl('nodeB')
            nodeB.getTags().add('tagB')
            def nodeSet = new NodeSetImpl()
            nodeSet.putNodes([nodeA, nodeB])

            def plugin1 = Mock(NodeEnhancerPlugin)
            sut.plugins << new TypedNodeEnhancerPlugin(plugin1, 'testPlugin')


            def newNodeA = new NodeEntryImpl('nodeA')
            newNodeA.attributes.put('attrB', 'valB2')


        when:
            def result = sut.nodeSet
        then:
            1 * projectnodes.getNodeSet() >> nodeSet

            1 * plugin1.updateNode(null, {it.nodename=='nodeA'}, false) >> {
                it[1].addAttribute 'attrB', 'valB2'
                it[1].removeTag('tagA')
            }
            1 * plugin1.updateNode(null, {it.nodename=='nodeB'}, false)
            result
            result.nodeNames.containsAll(['nodeA', 'nodeB'])
            nodeA.tags == new HashSet(['tagA', 'tagB'])
            result.getNode('nodeA').tags == new HashSet([ 'tagB'])
            result.getNode('nodeA').attributes == [nodename: 'nodeA', attrA: 'valA1', attrB: 'valB2']

            result.getNode('nodeB') == nodeB

    }


    def "get project nodes skipping plugin"() {
        given:
        def sut = new ProjectNodesEnhancer()
        sut.ignorePlugins = ignorePlugins

        def projectnodes = Mock(IProjectNodes)
        sut.projectNodes = projectnodes
        def nodeA = new NodeEntryImpl('nodeA')
        nodeA.attributes.put('attrA', 'valA1')
        nodeA.attributes.put('attrB', 'valB1')
        nodeA.getTags().add('tagA')
        nodeA.getTags().add('tagB')

        def nodeB = new NodeEntryImpl('nodeB')
        nodeB.getTags().add('tagB')
        def nodeSet = new NodeSetImpl()
        nodeSet.putNodes([nodeA, nodeB])

        def plugin1 = Mock(NodeEnhancerPlugin)
        sut.plugins << new TypedNodeEnhancerPlugin(plugin1, 'testPlugin')


        def newNodeA = new NodeEntryImpl('nodeA')
        newNodeA.attributes.put('attrB', 'valB2')


        when:
        def result = sut.nodeSet
        then:
        1 * projectnodes.getNodeSet() >> nodeSet

        calls * plugin1.updateNode(null, {it.nodename=='nodeA'}, false) >> {
            it[1].addAttribute 'attrB', 'valB2'
            it[1].removeTag('tagA')
        }
        calls * plugin1.updateNode(null, {it.nodename=='nodeB'}, false)


        where:
        ignorePlugins   | calls
        ['testPlugin']  | 0
        null            | 1

    }

}
