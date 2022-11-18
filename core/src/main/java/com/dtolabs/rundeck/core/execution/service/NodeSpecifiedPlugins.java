package com.dtolabs.rundeck.core.execution.service;

import com.dtolabs.rundeck.core.common.IFrameworkNodes;
import com.dtolabs.rundeck.core.common.INodeEntry;
import com.dtolabs.rundeck.core.common.ProjectManager;
import lombok.Getter;
import lombok.Setter;

/**
 * Provides provider info for {@link FileCopier} and {@link NodeExecutor} given a node and project
 */
public class NodeSpecifiedPlugins
        implements NodeProviderName
{
    @Getter @Setter private ProjectManager projectManager;
    @Getter @Setter private IFrameworkNodes frameworkNodes;

    public <T> String getProviderNameForNodeAndProject(final INodeEntry node, final String project, Class<T> type) {
        if (type.equals(FileCopier.class)) {
            String
                    copiername =
                    FileCopierService.getProviderNameForNode(
                            getFrameworkNodes().isLocalNode(node),
                            getProjectManager().loadProjectConfig(project)
                    );
            String providerAttr = FileCopierService.getNodeAttributeForProvider(getFrameworkNodes().isLocalNode(node));
            //look up node's attribute if it exists
            if (null != node.getAttributes() && null != node.getAttributes().get(providerAttr)) {
                copiername = node.getAttributes().get(providerAttr);
            }
            return copiername;
        } else if (type.equals(NodeExecutor.class)) {
            String
                    copiername =
                    NodeExecutorService.getProviderNameForNode(
                            getFrameworkNodes().isLocalNode(node),
                            getProjectManager().loadProjectConfig(project)
                    );
            String
                    providerAttr =
                    NodeExecutorService.getNodeAttributeForProvider(getFrameworkNodes().isLocalNode(node));
            //look up node's attribute if it exists
            if (null != node.getAttributes() && null != node.getAttributes().get(providerAttr)) {
                copiername = node.getAttributes().get(providerAttr);
            }
            return copiername;
        } else {
            throw new IllegalArgumentException("Unknown type; " + type);
        }
    }
}
