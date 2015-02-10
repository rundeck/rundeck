package org.rundeck.plugin.flowcontrol;

import com.dtolabs.rundeck.core.execution.workflow.steps.StepException;
import com.dtolabs.rundeck.core.plugins.Plugin;
import com.dtolabs.rundeck.plugins.ServiceNameConstants;
import com.dtolabs.rundeck.plugins.descriptions.PluginDescription;
import com.dtolabs.rundeck.plugins.descriptions.PluginProperty;
import com.dtolabs.rundeck.plugins.step.PluginStepContext;
import com.dtolabs.rundeck.plugins.step.StepPlugin;

import java.util.Map;

/**
 * Step to allow halting workflow progress with a custom status
 */
@Plugin(name = FlowControlWorkflowStep.PROVIDER_NAME, service = ServiceNameConstants.WorkflowStep)
@PluginDescription(title = "Flow Control", description = "Control Workflow execution behavior.\n\n" +
                                                         "*Halt* indicates that the execution should halt.\n" +
                                                         "Enter a *Status* to halt with a custom status string. " +
                                                         "Otherwise, enable *Fail* to exit with failure, or not to " +
                                                         "exit with success.")

public class FlowControlWorkflowStep implements StepPlugin {

    public static final String PROVIDER_NAME = "flow-control";
    @PluginProperty(title = "Halt", description = "Halt execution?", required = true, defaultValue = "false")
    boolean halt;
    @PluginProperty(title = "Fail", description = "Halt with fail result?", defaultValue = "true")
    boolean fail;
    @PluginProperty(title = "Status", description = "Use a custom exit status message.")
    String status;

    @Override
    public void executeStep(final PluginStepContext context, final Map<String, Object> configuration)
            throws StepException
    {
        if (halt) {
            if (null == context.getFlowControl()) {
                context.getLogger().log(
                        0,
                        "[" +
                        PROVIDER_NAME +
                        "] HALT requested, but no FlowControl available in this context"
                );
                return;
            }
            if (null != status) {
                context.getFlowControl().Halt(status);
            } else {
                context.getFlowControl().Halt(fail);
            }
        } else if (context.getFlowControl() != null) {
            context.getFlowControl().Continue();
        }
    }
}
