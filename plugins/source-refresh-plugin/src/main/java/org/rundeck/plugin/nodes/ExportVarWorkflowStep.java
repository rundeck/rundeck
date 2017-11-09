package org.rundeck.plugin.nodes;

import com.dtolabs.rundeck.core.dispatcher.ContextView;
import com.dtolabs.rundeck.core.execution.workflow.steps.StepException;
import com.dtolabs.rundeck.core.plugins.Plugin;
import com.dtolabs.rundeck.plugins.ServiceNameConstants;
import com.dtolabs.rundeck.plugins.descriptions.PluginDescription;
import com.dtolabs.rundeck.plugins.descriptions.PluginProperty;
import com.dtolabs.rundeck.plugins.step.PluginStepContext;
import com.dtolabs.rundeck.plugins.step.StepPlugin;

import java.util.Map;

@Plugin(name = ExportVarWorkflowStep.PROVIDER_NAME, service = ServiceNameConstants.WorkflowStep)
@PluginDescription(title = "Export Var",
        description = "Export vars.")


public class ExportVarWorkflowStep implements StepPlugin {
    public static final String PROVIDER_NAME = "export-var";
    @PluginProperty(title = "value",
            description = "value.",
            required = true)
    private String value;
    @PluginProperty(title = "name",
            description = "name.",
            required = true)
    private String export;

    @Override
    public void executeStep(PluginStepContext context, Map<String, Object> configuration) throws StepException {
        context.getOutputContext().addOutput("export",export,value);
        //context.getOutputContext().addOutput(ContextView.global(),"globals","test1","testv");
    }

}
