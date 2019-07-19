package com.dtolabs.rundeck.server.plugins.pd

import com.dtolabs.rundeck.core.execution.workflow.steps.StepException
import com.dtolabs.rundeck.core.plugins.Plugin
import com.dtolabs.rundeck.core.plugins.configuration.PropertyScope
import com.dtolabs.rundeck.plugins.ServiceNameConstants
import com.dtolabs.rundeck.plugins.descriptions.PluginProperty
import com.dtolabs.rundeck.plugins.step.PluginStepContext
import com.dtolabs.rundeck.plugins.step.StepPlugin


@Plugin(name = "PagerDuty Event Step", service = ServiceNameConstants.WorkflowStep)
class PagerDutyEventStepPlugin implements StepPlugin {
    @PluginProperty(title="routing_key", scope= PropertyScope.Instance)
    String routing_key

    @PluginProperty(title="dedupe_key", scope=PropertyScope.Instance)
    String dedupe_key

    @PluginProperty(
            title = "event_action",
            description = "Event action [trigger|acknowledge|resolve]",
            scope=PropertyScope.Instance)
    String event_action

    @PluginProperty(
            title = "payload.summary",
            scope = PropertyScope.Instance)
    String payload_summary

    @PluginProperty(
            title = "payload.source",
            scope = PropertyScope.Instance
    )
    String payload_source

    @PluginProperty(
            title = "payload.severity",
            description = "[critical|error|warning|info]",
            scope = PropertyScope.Instance)
    String payload_severity

    void executeStep(
            final PluginStepContext context,
            final Map<String, Object> configuration) throws StepException {

        return

    }
}