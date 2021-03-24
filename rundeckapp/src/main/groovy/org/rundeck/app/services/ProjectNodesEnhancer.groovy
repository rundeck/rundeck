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

import com.dtolabs.rundeck.core.common.*
import groovy.transform.CompileStatic

@CompileStatic
class ProjectNodesEnhancer implements IProjectNodes {
    @Delegate IProjectNodes projectNodes

    String project
    List<TypedNodeEnhancerPlugin> plugins = []
    List<String> ignorePlugins = []

    long loadedTime

    @Override
    INodeSet getNodeSet() {
        getNodeSet(false)
    }

    INodeSet getNodeSet(boolean refreshNodeStatus) {
        def nodeset = projectNodes.getNodeSet()
        def newNodes = new NodeSetImpl()
        nodeset.nodeNames.each { String node ->
            INodeEntry origNode = nodeset.getNode(node)
            Map<String, String> attrs = new HashMap<>(origNode.attributes)
            Set<String> tags = new HashSet<>(origNode.tags)
            plugins.each { plugin ->

                if(ignorePlugins?.contains(plugin.type)){
                    return
                }

                ModifiableNode newNode = new ModifiableNode(node)
                newNode.attributes.putAll attrs
                newNode.tags.addAll tags
                plugin.updateNode(project, newNode, refreshNodeStatus)
                if (newNode.attributes != attrs || newNode.tags != tags) {
                    attrs = new HashMap<>(newNode.attributes)
                    tags = new HashSet<>(newNode.tags)
                }
            }
            INodeEntry newNode = new NodeEntryImpl(node)
            newNode.attributes.putAll attrs
            newNode.tags.addAll tags
            newNodes.putNode(newNode)
        }
        return newNodes
    }

    IProjectNodes withProjectNodes(IProjectNodes projectNodes) {
        if (!plugins) {
            return projectNodes
        }
        return new ProjectNodesEnhancer(
                projectNodes: projectNodes,
                plugins: plugins,
                ignorePlugins: ignorePlugins,
                project: project,
                loadedTime: loadedTime
        )
    }
}
