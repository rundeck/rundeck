package com.dtolabs.rundeck.core.execution.workflow.steps.node.impl;

import com.dtolabs.rundeck.core.common.Framework;
import com.dtolabs.rundeck.core.common.INodeEntry;
import com.dtolabs.rundeck.core.data.SharedDataContextUtils;
import com.dtolabs.rundeck.core.dispatcher.ContextView;
import com.dtolabs.rundeck.core.dispatcher.DataContextUtils;
import com.dtolabs.rundeck.core.execution.workflow.StepExecutionContext;
import com.dtolabs.rundeck.core.execution.workflow.steps.node.NodeStepException;
import com.dtolabs.rundeck.core.execution.workflow.steps.node.NodeStepExecutionItem;
import com.dtolabs.rundeck.core.execution.workflow.steps.node.NodeStepExecutor;
import com.dtolabs.rundeck.core.execution.workflow.steps.node.NodeStepResult;

public class ScriptNodeStepExecutor implements NodeStepExecutor {
    public static final String SERVICE_IMPLEMENTATION_NAME = "script";
    private Framework framework;
    private ScriptFileNodeStepUtils scriptUtils = new DefaultScriptFileNodeStepUtils();

    public ScriptNodeStepExecutor(Framework framework) {
        this.framework = framework;
    }

    public NodeStepResult executeNodeStep(
            StepExecutionContext context,
            NodeStepExecutionItem item,
            INodeEntry node
    )
            throws NodeStepException
    {
        ScriptFileCommand command = (ScriptFileCommand) item;
        boolean expandTokens = true;
        if (context.getFramework().hasProperty("execution.script.tokenexpansion.enabled")) {
            expandTokens = "true".equals(context.getFramework().getProperty("execution.script.tokenexpansion.enabled"));
        }
        if(null != command.getServerScriptFilePath()){
            expandTokens = command.isExpandTokenInScriptFile();
        }

        String expandedVarsInURL = SharedDataContextUtils.replaceDataReferences(
                command.getServerScriptFilePath(),
                context.getSharedDataContext(),
                //add node name to qualifier to read node-data first
                ContextView.node(node.getNodename()),
                ContextView::nodeStep,
                DataContextUtils.replaceMissingOptionsWithBlank,
                false,
                false
        );
        return scriptUtils.executeScriptFile(
                context,
                node,
                command.getScript(),
                expandedVarsInURL,
                command.getScriptAsStream(),
                command.getFileExtension(),
                command.getArgs(),
                command.getScriptInterpreter(),
                command.getInterpreterArgsQuoted(),
                framework.getExecutionService(),
                expandTokens
        );
    }

    public ScriptFileNodeStepUtils getScriptUtils() {
        return scriptUtils;
    }

    public void setScriptUtils(ScriptFileNodeStepUtils scriptUtils) {
        this.scriptUtils = scriptUtils;
    }
}
