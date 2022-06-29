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

import com.dtolabs.rundeck.core.Constants
import com.dtolabs.rundeck.core.common.FrameworkProjectMgr
import com.dtolabs.rundeck.core.common.IFramework
import com.dtolabs.rundeck.core.common.INodeEntry
import com.dtolabs.rundeck.core.common.IProjectNodes
import com.dtolabs.rundeck.core.common.IRundeckProjectConfig
import com.dtolabs.rundeck.core.common.NodeEntryImpl
import com.dtolabs.rundeck.core.common.NodeSetImpl
import com.dtolabs.rundeck.core.common.ProjectManager
import com.dtolabs.rundeck.core.plugins.ConfiguredPlugin
import com.dtolabs.rundeck.core.plugins.ValidatedPlugin
import com.dtolabs.rundeck.core.utils.NodeSet
import com.dtolabs.rundeck.plugins.nodes.IModifiableNodeEntry
import com.dtolabs.rundeck.plugins.nodes.NodeEnhancerPlugin
import grails.test.mixin.TestFor
import org.grails.spring.beans.factory.InstanceFactoryBean
import org.grails.testing.GrailsUnitTest
import rundeck.services.FrameworkService
import rundeck.services.NodeService
import rundeck.services.PluginService
import spock.lang.Specification

//@TestFor(EnhancedNodeService)
class EnhancedNodeServiceSpec extends Specification implements GrailsUnitTest {

    Closure doWithSpring() {
        { ->
            nodeService(InstanceFactoryBean, Mock(NodeService))
            frameworkService(InstanceFactoryBean, Mock(FrameworkService))
            pluginService(InstanceFactoryBean, Mock(PluginService))
        }
    }

    def "project with no plugins "() {
        given:
            def nodeset = new NodeSetImpl()
            def nodeA = new NodeEntryImpl('nodeA')
            nodeA.getAttributes().putAll(['test1': 'blah'])
            nodeset.putNode(nodeA)
            def sut = new EnhancedNodeService()
            sut.enabled = true
            sut.nodeService = Mock(NodeService)
            sut.frameworkService = Mock(FrameworkService)
            sut.pluginService = Mock(PluginService)
            sut.frameworkService.getRundeckFramework() >> Mock(IFramework) {
                1 * getFrameworkProjectMgr() >> Mock(ProjectManager) {
                    1 * loadProjectConfig('AProject') >> Mock(IRundeckProjectConfig) {
                        getProjectProperties() >> projProps
                    }
                }
            }
            sut.nodeService.getNodes('AProject') >> Mock(IProjectNodes) {
                getNodeSet() >> nodeset
            }

        when:
            def result = sut.getNodes('AProject')

        then:
            result != null
            result.getNodeSet() != null
            result.getNodeSet().getNodeNames().size() == 1
            result.getNodeSet().getNodeNames().contains 'nodeA'
            def testNodeA = result.getNodeSet().getNode('nodeA')
            testNodeA != null
            testNodeA.attributes['test1'] == 'blah'

            0 * sut.pluginService.validatePluginConfig(*_)
            0 * sut.pluginService.configurePlugin(*_)

        where:
            projProps | _
            [:]       | _
    }

    def "project with enhanced nodes plugin"() {
        given:
            def nodeset = new NodeSetImpl()
            def nodeA = new NodeEntryImpl('nodeA')
            nodeA.getAttributes().putAll(['test1': 'blah'])
            nodeset.putNode(nodeA)
            def sut = new EnhancedNodeService()
            sut.enabled = true
            sut.nodeService = Mock(NodeService)
            sut.frameworkService = Mock(FrameworkService)
            sut.pluginService = Mock(PluginService)
            sut.frameworkService.getRundeckFramework() >> Mock(IFramework) {
                1 * getFrameworkProjectMgr() >> Mock(ProjectManager) {
                    1 * loadProjectConfig('AProject') >> Mock(IRundeckProjectConfig) {
                        getProjectProperties() >> projProps
                    }
                }
            }
            sut.nodeService.getNodes('AProject') >> Mock(IProjectNodes) {
                getNodeSet() >> nodeset
            }

        when:
            def result = sut.getNodes('AProject')
            def resultNodes = result.getNodeSet()

        then:
            resultNodes != null
            resultNodes.getNodeNames().size() == 1
            resultNodes.getNodeNames().contains 'nodeA'
            def testNodeA = resultNodes.getNode('nodeA')
            testNodeA != null
            testNodeA.attributes['test1'] == 'blah'
            if(!skip){
                testNodeA.attributes['monkey'] == 'disaster'
            }else {
                testNodeA.attributes['monkey'] == null
            }
            1 * sut.pluginService.validatePluginConfig('NodeEnhancer', 'asdf', [:]) >> new ValidatedPlugin(valid: true)
            1 * sut.pluginService.configurePlugin('asdf', 'NodeEnhancer', [:]) >>
            new ConfiguredPlugin<NodeEnhancerPlugin>(new AddAddtributesPlugin(attributes: [monkey: 'disaster'], skip: skip), [:])


        where:
            projProps                       | skip
            ['nodes.plugin.1.type': 'asdf'] | false
            ['nodes.plugin.1.type': 'asdf'] | true
    }

    def "project with skipping enhanced nodes plugin"() {
        given:
        def nodeset = new NodeSetImpl()
        def nodeA = new NodeEntryImpl('nodeA')
        nodeA.getAttributes().putAll(['test1': 'blah'])
        nodeset.putNode(nodeA)
        def sut = new EnhancedNodeService()
        sut.enabled = true
        sut.nodeService = Mock(NodeService)
        sut.frameworkService = Mock(FrameworkService)
        sut.pluginService = Mock(PluginService)
        sut.frameworkService.getRundeckFramework() >> Mock(IFramework) {
            1 * getFrameworkProjectMgr() >> Mock(ProjectManager) {
                1 * loadProjectConfig('AProject') >> Mock(IRundeckProjectConfig) {
                    getProjectProperties() >> projProps
                }
            }
        }
        sut.nodeService.getNodes('AProject') >> Mock(IProjectNodes) {
            getNodeSet() >> nodeset
        }

        when:
        def result = sut.getNodes('AProject', skipping)
        def resultNodes = result.getNodeSet()

        then:
        resultNodes != null
        resultNodes.getNodeNames().size() == 1
        resultNodes.getNodeNames().contains 'nodeA'
        def testNodeA = resultNodes.getNode('nodeA')
        testNodeA != null
        testNodeA.attributes == attributes
        1 * sut.pluginService.validatePluginConfig('NodeEnhancer', 'asdf', [:]) >> new ValidatedPlugin(valid: true)
        1 * sut.pluginService.configurePlugin('asdf', 'NodeEnhancer', [:]) >>
                new ConfiguredPlugin<NodeEnhancerPlugin>(new AddAddtributesPlugin(attributes: [monkey: 'disaster']), [:])


        where:
        projProps                       | skipping  | attributes
        ['nodes.plugin.1.type': 'asdf'] | null      | [nodename:'nodeA', test1:'blah',monkey: 'disaster']
        ['nodes.plugin.1.type': 'asdf'] | ['asdf']  | [nodename:'nodeA', test1:'blah']
    }

    def "get excluded plugins list"(){
        given:
        def ens = new EnhancedNodeService()
        TypedNodeEnhancerPlugin enhancerPlugin1 = Mock(TypedNodeEnhancerPlugin){
            shouldSkip(_) >> true
            type >> 'example1'
        }
        TypedNodeEnhancerPlugin enhancerPlugin2 = Mock(TypedNodeEnhancerPlugin){
            shouldSkip(_) >> true
            type >> 'example2'
        }
        TypedNodeEnhancerPlugin enhancerPlugin3 = Mock(TypedNodeEnhancerPlugin){
            shouldSkip(_) >> false
            type >> 'example3'
        }
        ProjectNodesEnhancer projectNodesEnhancer = Mock(ProjectNodesEnhancer){
            plugins >> [enhancerPlugin1, enhancerPlugin2, enhancerPlugin3]
        }

        when:
        def excludedList = ens.getExcludedPlugins("Aproject", projectNodesEnhancer, excluded)
        then:
        excludedList.size() == excludedResult.size()
        excludedList.each {
            excludedResult.contains(it)
        }

        where:
        excludedResult                          | excluded
        ['example1', 'example2', 'example4']    | ['example4']

    }

    static class AddAddtributesPlugin implements NodeEnhancerPlugin {
        Map<String, String> attributes = [:]
        boolean skip

        @Override
        void updateNode(final String project, final IModifiableNodeEntry node) {
            node.attributes.putAll(attributes)
            node
        }

        @Override
        boolean shouldSkip(String projectName){
            return skip
        }
    }
}
