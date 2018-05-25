package com.dtolabs.rundeck.core.execution.workflow;

import com.dtolabs.rundeck.core.plugins.Plugin;
import com.dtolabs.rundeck.core.plugins.configuration.Validator;
import com.dtolabs.rundeck.core.rules.RuleEngine;
import com.dtolabs.rundeck.plugins.ServiceNameConstants;
import com.dtolabs.rundeck.plugins.descriptions.PluginDescription;
import com.dtolabs.rundeck.plugins.descriptions.PluginProperty;
import com.dtolabs.rundeck.plugins.descriptions.RenderingOption;
import com.dtolabs.rundeck.plugins.descriptions.RenderingOptions;

import static com.dtolabs.rundeck.core.plugins.configuration.StringRenderingConstants.*;

/**
 * Created by greg on 5/5/16.
 */
@Plugin(name = "sequential", service = ServiceNameConstants.WorkflowStrategy)
@PluginDescription(title = "Sequential",
                   description = "Run each step in order. Execute a step on all nodes before proceeding to the next " +
                                 "step")

public class SequentialWorkflowStrategy implements WorkflowStrategy {
    public static String PROVIDER_NAME = "sequential";

    @PluginProperty(
            title = " ",
            defaultValue = "<table>\n" +
                           "    <tr><td>1.</td><td class=\"nodea\">NodeA</td> <td class=\"step1\">step " +
                           "1</td></tr>\n" +
                           "    <tr><td>2.</td><td class=\"nodeb\">NodeB</td> <td " +
                           "class=\"step1\">\"</td></tr>\n" +
                           "    <tr><td>3.</td><td class=\"nodea\">NodeA</td> <td class=\"step2\">step " +
                           "2</td></tr>\n" +
                           "    <tr><td>4.</td><td class=\"nodeb\">NodeB</td> <td " +
                           "class=\"step2\">\"</td></tr>\n" +
                           "    <tr><td>5.</td><td class=\"nodea\">NodeA</td> <td>step 3</td></tr>\n" +
                           "    <tr><td>6.</td><td class=\"nodeb\">NodeB</td> <td>\"</td></tr>\n" +
                           "</table>")

    @RenderingOptions(
            {
                    @RenderingOption(key = DISPLAY_TYPE_KEY, value = "STATIC_TEXT"),
                    @RenderingOption(key = STATIC_TEXT_CONTENT_TYPE_KEY, value = "text/html"),
                    @RenderingOption(key = GROUP_NAME, value = "Explain"),
                    @RenderingOption(key = GROUPING, value = "secondary"),
            }
    )
    String info;

    @Override
    public int getThreadCount() {
        return 1;
    }

    @Override
    public void setup(final RuleEngine ruleEngine, StepExecutionContext context, IWorkflow workflow) {

    }

    @Override
    public Validator.Report validate(final IWorkflow workflow) {

        return null;
    }

    @Override
    public WorkflowStrategyProfile getProfile() {

        return new SequentialStrategyProfile();
    }

}
