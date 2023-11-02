package org.rundeck.plugin.scriptnodestep;

import com.dtolabs.rundeck.core.common.INodeEntry;
import com.dtolabs.rundeck.core.data.SharedDataContextUtils;
import com.dtolabs.rundeck.core.dispatcher.ContextView;
import com.dtolabs.rundeck.core.dispatcher.DataContextUtils
import com.dtolabs.rundeck.core.execution.workflow.StepExecutionContext;
import com.dtolabs.rundeck.core.execution.workflow.steps.node.impl.DefaultScriptFileNodeStepUtils;
import com.dtolabs.rundeck.core.utils.OptsUtil;
import com.dtolabs.rundeck.plugins.step.PluginStepContext;

public class ScriptFileNodeStepExecutor {
    private final String scriptInterpreter;
    private final Boolean interpreterArgsQuoted;
    private final String fileExtension;
    private final String argString;
    private final String adhocFilepath;
    private final String adhocLocalString;
    private final boolean expandTokenInScriptFile;

    private final DefaultScriptFileNodeStepUtils scriptUtils = new DefaultScriptFileNodeStepUtils();

public ScriptFileNodeStepExecutor(
            String scriptInterpreter,
            Boolean interpreterArgsQuoted,
            String fileExtension,
            String argString,
            String adhocFilepath,
            String adhocLocalString,
            boolean expandTokenInScriptFile
    ) {
        this.scriptInterpreter = scriptInterpreter;
        this.interpreterArgsQuoted = interpreterArgsQuoted;
        this.fileExtension = fileExtension;
        this.argString = argString;
        this.adhocFilepath = adhocFilepath;
        this.adhocLocalString = adhocLocalString;
        this.expandTokenInScriptFile = expandTokenInScriptFile;
    }

    public void executeScriptFile(PluginStepContext context, Map<String, Object> configuration, INodeEntry entry) {
        boolean expandTokens = true;
        if (context.getFramework().hasProperty("execution.script.tokenexpansion.enabled")) {
            expandTokens = "true".equals(context.getFramework().getProperty("execution.script.tokenexpansion.enabled"));
        }
        if(null != adhocFilepath){
            expandTokens = expandTokenInScriptFile;
        }

        String expandedVarsInURL = SharedDataContextUtils.replaceDataReferences(
                adhocFilepath ?: "",
                context.getExecutionContext().getSharedDataContext(),
                //add node name to qualifier to read node-data first
                ContextView.node(entry.getNodename()),
                ContextView::nodeStep,
                DataContextUtils.replaceMissingOptionsWithBlank,
                false,
                false
        );

        if( DataContextUtils.hasOptionsInString(expandedVarsInURL) ){
            Map<String, Map<String, String>> optionsContext = new HashMap();
            optionsContext.put("option", context.getDataContext().get("option"));
            expandedVarsInURL = DataContextUtils.replaceDataReferencesInString(expandedVarsInURL, optionsContext);
        }

        final String[] args;
        if (null != argString) {
            args = OptsUtil.burst(argString);
        } else {
            args = new String[0];
        }

        scriptUtils.executeScriptFile(
                context.getExecutionContext() as StepExecutionContext,
                entry,
                this.adhocLocalString,
                expandedVarsInURL,
                null,
                fileExtension,
                args,
                scriptInterpreter,
                interpreterArgsQuoted,
                context.getFramework().getExecutionService(),
                expandTokens
        );
    }
}
