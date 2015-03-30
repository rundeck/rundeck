package com.dtolabs.rundeck.core.common;

import com.dtolabs.rundeck.core.authorization.AuthContext;
import com.dtolabs.rundeck.core.execution.ExecutionContext;
import com.dtolabs.rundeck.core.execution.ExecutionService;
import com.dtolabs.rundeck.core.execution.dispatch.NodeDispatcher;
import com.dtolabs.rundeck.core.execution.service.*;
import com.dtolabs.rundeck.core.execution.workflow.WorkflowExecutionService;
import com.dtolabs.rundeck.core.execution.workflow.steps.StepExecutionService;
import com.dtolabs.rundeck.core.execution.workflow.steps.node.NodeStepExecutionItem;
import com.dtolabs.rundeck.core.execution.workflow.steps.node.NodeStepExecutionService;
import com.dtolabs.rundeck.core.execution.workflow.steps.node.NodeStepExecutor;
import com.dtolabs.rundeck.core.plugins.ServiceProviderLoader;
import com.dtolabs.rundeck.core.resources.ResourceModelSourceService;
import com.dtolabs.rundeck.core.resources.format.ResourceFormatGeneratorService;
import com.dtolabs.rundeck.core.resources.format.ResourceFormatParserService;
import com.dtolabs.rundeck.core.utils.IPropertyLookup;

import java.io.File;
import java.util.Collection;
import java.util.Set;

/**
 * Interface for framework instance
 */
public interface IFramework extends IFrameworkServices, IFrameworkNodes{
    /**
     * Gets DepotMgr for this framework instance
     * @return returns instance of IFrameworkProjectMgr
     */
    ProjectManager getFrameworkProjectMgr();

    /**
     * @return property lookup
     */
    IPropertyLookup getPropertyLookup();

    /**
     * Gets the value of "framework.server.hostname" property
     *
     * @return Returns value of framework.server.hostname property
     */
    String getFrameworkNodeHostname();

    /**
      * Gets the value of "framework.server.name" property
      *
      * @return Returns value of framework.server.name property
      */
    String getFrameworkNodeName();

    /**
     * @return Generate a node entry for the framework with default values
     */
    NodeEntryImpl createFrameworkNode();


    /**
     * @return the nodeset consisting only of the input nodes where the specified actions are all authorized
     * @param project project name
     * @param actions action set
     * @param unfiltered nodes
     * @param authContext authoriziation
     */
    INodeSet filterAuthorizedNodes(
            String project, Set<String> actions, INodeSet unfiltered,
            AuthContext authContext
    );


    /**
     * Gets the {@link com.dtolabs.rundeck.core.common.INodeDesc} value describing the framework node
     * @return the singleton {@link com.dtolabs.rundeck.core.common.INodeDesc} object for this framework instance
     */
    INodeDesc getNodeDesc();

    /**
     * Return true if the node is the local framework node.  Compares the (logical) node names
     * of the nodes after eliding any embedded 'user@' parts.
     * @param node the node
     * @return true if the node's name is the same as the framework's node name
     */
    boolean isLocalNode(INodeDesc node);
}
