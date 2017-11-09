package org.rundeck.plugin.nodes;

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
            DataContext data = sharedDataContext.getData(view);
            Map<String, Map<String, String>> mapdata = data.getData();


            StringWriter stringWriter = new StringWriter();
            try {
                objectMapper.writeValue(stringWriter, mapdata);

                HashMap<String, String> meta = new HashMap<>();
                meta.put("content-data-type", "application/json");
                meta.put("content-meta:table-title", viewString(view));
                context.getExecutionContext().getExecutionListener().log(
                        2,
                        stringWriter.toString(),
                        meta
                );
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

    }

    public String viewString(final ContextView view) {
        if (view.isGlobal()) {
            return "global";
        }
        if (null != view.getNodeName()) {
            return (view.getStepNumber() != null ? view.getStepNumber() : "") + "@" + view.getNodeName();
        }
        if (null != view.getStepNumber()) {
            return view.getStepNumber().toString();
        }
        return view.toString();
    }


}
