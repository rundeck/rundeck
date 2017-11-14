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
    @PluginProperty(title = "Group",
            description = "Group of the new variable.",
            required = true)
    private String group;
    @PluginProperty(title = "Name",
            description = "Name of the new variable.",
            required = true)
    private String export;

    public enum UpvarFailureReason implements FailureReason {
        EmptyValue,
        NoGroup,
        NoName
    }

    @Override
    public void executeStep(PluginStepContext context, Map<String, Object> configuration) throws StepException {
        if(null== value || value.trim().length() == 0){
            //empty value or not resolved
            throw new StepException(
                    "Empty value",
                    UpvarFailureReason.EmptyValue
            );
        }
        if(group== value || group.trim().length() == 0){
            //empty value or not resolved
            throw new StepException(
                    "Required Group",
                    UpvarFailureReason.NoGroup
            );
        }
        if(export== value || export.trim().length() == 0){
            //empty value or not resolved
            throw new StepException(
                    "Required Name",
                    UpvarFailureReason.NoName
            );
        }
        context.getOutputContext().addOutput(ContextView.global(),group,export,value);
    }

    /**
     * Properties set for test purpose
     * @param value
     * @param group
     * @param name
     */
    public void setProperties(String value, String group, String name){
        this.value= value;
        this.group = group;
        this.export=name;
    }

}