package org.rundeck.plugin.vars;

import com.dtolabs.rundeck.core.data.DataContext;
import com.dtolabs.rundeck.core.data.MultiDataContext;
import com.dtolabs.rundeck.core.dispatcher.ContextView;
import com.dtolabs.rundeck.core.execution.workflow.steps.StepException;
import com.dtolabs.rundeck.core.plugins.Plugin;
import com.dtolabs.rundeck.plugins.ServiceNameConstants;
import com.dtolabs.rundeck.plugins.descriptions.PluginDescription;
import com.dtolabs.rundeck.plugins.descriptions.PluginProperty;
import com.dtolabs.rundeck.plugins.step.PluginStepContext;
import com.dtolabs.rundeck.plugins.step.StepPlugin;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

@Plugin(name = UpvarWorkflowStep.PROVIDER_NAME, service = ServiceNameConstants.WorkflowStep)
@PluginDescription(title = "Up Var",
        description = "Up vars.")


public class UpvarWorkflowStep implements StepPlugin {
    public static final String PROVIDER_NAME = "upvar";


    @Override
    public void executeStep(
            final PluginStepContext context, final Map<String, Object> configuration
    ) throws StepException
    {
        MultiDataContext<ContextView, DataContext> sharedDataContext = context.getExecutionContext()
                .getSharedDataContext()
                .consolidate();
        ObjectMapper objectMapper = new ObjectMapper();
        Set<ContextView> keys = new TreeSet<>(sharedDataContext.getKeys());
        for (ContextView view : keys) {
            if(!view.isGlobal()){
                DataContext data = sharedDataContext.getData(view);
                if(null != data.get("export")){
                    context.getOutputContext().addOutput(ContextView.global(),"globals",data.get("export"));
                }

            }
        }

    }


}
