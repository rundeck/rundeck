package com.dtolabs.rundeck.server.plugins.pd

import com.dtolabs.rundeck.core.plugins.Plugin
import com.dtolabs.rundeck.core.plugins.configuration.PropertyScope
import com.dtolabs.rundeck.plugins.ServiceNameConstants
import com.dtolabs.rundeck.plugins.descriptions.PluginProperty
import com.dtolabs.rundeck.plugins.notification.NotificationPlugin
import com.github.dikhan.pagerduty.client.events.PagerDutyEventsClient
import com.github.dikhan.pagerduty.client.events.domain.AcknowledgeIncident
import com.github.dikhan.pagerduty.client.events.domain.Payload
import com.github.dikhan.pagerduty.client.events.domain.ResolveIncident
import com.github.dikhan.pagerduty.client.events.domain.Severity
import com.github.dikhan.pagerduty.client.events.domain.TriggerIncident
import org.apache.log4j.Logger

import java.time.OffsetDateTime


@Plugin(name='PagerDuty Event Notification',service= ServiceNameConstants.Notification)
class PagerDutyEventNotificationPlugin implements NotificationPlugin {
    static Logger log = Logger.getLogger(PagerDutyEventNotificationPlugin)

    static String ACTION_TRIGGER = "trigger"
    static String ACTION_ACKKNOWLEDGE = "acknowledge"
    static String ACTION_RESOLVE = "resolve"

    @PluginProperty(title="routing_key", scope=PropertyScope.Instance)
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


    boolean postNotification(String trigger, Map executionData, Map config) {
        log.error(routing_key)

        sendEvent()

        return true
    }

    void sendEvent() {
        def payload = createPayload()

        switch(event_action) {
            case ACTION_TRIGGER:
                sendTrigger(payload); break
            case ACTION_ACKKNOWLEDGE:
                sendAck(); break
            case ACTION_RESOLVE:
                sendResolve(); break
        }
    }

    void sendTrigger(Payload payload) {
        TriggerIncident incident = TriggerIncident.TriggerIncidentBuilder
                .newBuilder(routing_key, payload)
                .setDedupKey(dedupe_key)
                .build()

        PagerDutyEventsClient client = PagerDutyEventsClient.create()

        client.trigger(incident)
    }

    void sendAck() {
        AcknowledgeIncident incident = AcknowledgeIncident.AcknowledgeIncidentBuilder
            .newBuilder(routing_key, dedupe_key)
            .build()

        PagerDutyEventsClient client = PagerDutyEventsClient.create()

        client.acknowledge(incident)
    }

    void sendResolve() {
        ResolveIncident incident = ResolveIncident.ResolveIncidentBuilder
            .newBuilder(routing_key, dedupe_key)
            .build()

        PagerDutyEventsClient client = PagerDutyEventsClient.create()

        client.resolve(incident)
    }

    Payload createPayload() {
        Payload.Builder.newBuilder()
            .setSummary(payload_summary)
            .setSource(payload_source)
            .setSeverity(Severity.valueOf(payload_severity.toUpperCase()))
            .setTimestamp(OffsetDateTime.now())
            .build()
    }
}