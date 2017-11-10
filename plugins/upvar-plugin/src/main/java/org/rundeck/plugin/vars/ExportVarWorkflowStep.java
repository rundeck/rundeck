package org.rundeck.plugin.vars;

import com.dtolabs.rundeck.core.dispatcher.ContextView;
import com.dtolabs.rundeck.core.execution.workflow.steps.FailureReason;
import com.dtolabs.rundeck.core.execution.workflow.steps.StepException;
import com.dtolabs.rundeck.core.plugins.Plugin;
import com.dtolabs.rundeck.plugins.ServiceNameConstants;
import com.dtolabs.rundeck.plugins.descriptions.PluginDescription;
import com.dtolabs.rundeck.plugins.descriptions.PluginProperty;
import com.dtolabs.rundeck.plugins.step.PluginStepContext;
import com.dtolabs.rundeck.plugins.step.StepPlugin;

import java.util.Map;

@Plugin(name = ExportVarWorkflowStep.PROVIDER_NAME, service = ServiceNameConstants.WorkflowStep)
@PluginDescription(title = "Export Variable",
        description = "Create a variable on the export group.")


public class ExportVarWorkflowStep implements StepPlugin {
    public static final String PROVIDER_NAME = "export-var";
    @PluginProperty(title = "Value",
            description = "Value of the variable, can be a reference to another variable, like ${data.var1} or ${data.var1@node1}.",
            required = true)
    private String value;
    @PluginProperty(title = "Name",
            description = "Name of the new variable.",
            required = true)
    private String export;

    public enum UpvarFailureReason implements FailureReason {
        EmptyValue
    }

    @Override
    public void executeStep(PluginStepContext context, Map<String, Object> configuration) throws StepException {
        if(value.trim().length() == 0){
            //empty value or not resolved
            throw new StepException(
                    "Empty value",
                    UpvarFailureReason.EmptyValue
            );
        }
        context.getOutputContext().addOutput("export",export,value);
    }

}