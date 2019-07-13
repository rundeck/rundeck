/*
 * Copyright 2019 Rundeck, Inc. (http://rundeck.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.dtolabs.rundeck.server.plugins.webhook

import com.dtolabs.rundeck.core.dispatcher.DataContextUtils
import com.dtolabs.rundeck.core.execution.ExecutionReference
import com.dtolabs.rundeck.core.execution.workflow.steps.FailureReason
import com.dtolabs.rundeck.core.jobs.JobExecutionError
import com.dtolabs.rundeck.core.jobs.JobNotFound
import com.dtolabs.rundeck.core.jobs.JobReference
import com.dtolabs.rundeck.core.jobs.JobService
import com.dtolabs.rundeck.core.plugins.Plugin
import com.dtolabs.rundeck.core.plugins.configuration.StringRenderingConstants
import com.dtolabs.rundeck.core.utils.MapData
import com.dtolabs.rundeck.core.utils.OptsUtil
import com.dtolabs.rundeck.core.webhook.WebhookEventException
import com.dtolabs.rundeck.plugins.ServiceNameConstants
import com.dtolabs.rundeck.plugins.descriptions.PluginDescription
import com.dtolabs.rundeck.plugins.descriptions.PluginProperty
import com.dtolabs.rundeck.plugins.descriptions.RenderingOption
import com.dtolabs.rundeck.plugins.descriptions.RenderingOptions
import com.dtolabs.rundeck.plugins.webhook.WebhookData
import com.dtolabs.rundeck.plugins.webhook.WebhookEventContext
import com.dtolabs.rundeck.plugins.webhook.WebhookEventPlugin
import com.fasterxml.jackson.databind.ObjectMapper
import org.rundeck.utils.UUIDPropertyValidator
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import rundeck.Webhook


@Plugin(name='pagerduty-run-job',service= ServiceNameConstants.WebhookEvent)
@PluginDescription(title="PagerDuty Webhook Run Job",description="Run a job on webhook event")
class PagerDutyWebhookEventPlugin implements WebhookEventPlugin {
    static final String JSON_DATA_TYPE = "application/json"
    static final ObjectMapper mapper = new ObjectMapper()

    static Logger log = LoggerFactory.getLogger(JobRunWebhookEventPlugin)

    Map config

    @Override
    void onEvent(final WebhookEventContext context, final WebhookData data) throws WebhookEventException {

        Config conf = mapper.convertValue(config, Config.class)


        if(JSON_DATA_TYPE != data.contentType) {
            throw new WebhookEventException("Posted data was not json",WebhookJobRunHandlerFailureReason.InvalidContentType)
        }

        JobService jobService = context.services.getService(JobService)

        try {

            //expand values in the argString/jobOptions map

            final Map<String, String> webhookMap = new HashMap<>()
            webhookMap.put("id", data.id)
            if (null != data.project) {
                webhookMap.put("project", data.project)
            }

            Object webhookdata

            try {
                webhookdata = mapper.readValue(data.data, HashMap)
            } catch(Exception ex) {
                throw new WebhookEventException("Unable to parse posted JSON data: ${ex.message}",WebhookJobRunHandlerFailureReason.JsonParseError)
            }

            List<Object> messages = webhookdata.get('messages')

            messages.each { m ->
                Map<String, Map<String, String>> eventData = new HashMap<>()
                webhookMap.put("raw", mapper.writeValueAsString(m))

                Map<String, Map<String, String>> localDataContext = DataContextUtils.context("webhook", webhookMap)
                if (m instanceof Map) {
                    toStringMap(m)
                    eventData = m
                }

                def flatMap = flattenMap(eventData)
                localDataContext = DataContextUtils.addContext("data", flatMap, localDataContext)

                conf.rules.each { r ->
                    if (! isRuleMatch(r, flatMap))
                        return

                    def jobId = r.jobId


                    def expandedJobId = DataContextUtils.replaceDataReferencesInString(
                            jobId,
                            localDataContext
                    )

                    JobReference jobReference = jobService.jobForID(
                            expandedJobId, data.project
                    )

                    def expandedNodeFilter = DataContextUtils.replaceDataReferencesInString(r.nodeFilter, localDataContext)
                    def expandedAsUser = DataContextUtils.replaceDataReferencesInString("", localDataContext)
                    //TODO: switch to use Map instead of String
                    String[] args = r.jobOpts ? OptsUtil.burst(r.jobOpts) : new String[0]
                    String[] expandedArgs = DataContextUtils.replaceDataReferencesInArray(args, localDataContext)
                    String expandedArgString = OptsUtil.join(expandedArgs)
                    log.info(
                            "starting job ${expandedJobId} with args: ${expandedArgString}, " +
                                    "nodeFilter: ${expandedNodeFilter} asUser: ${expandedAsUser}"
                    )
                    ExecutionReference exec =
                            jobService.runJob(
                                    jobReference,
                                    expandedArgString,
                                    expandedNodeFilter,
                                    expandedAsUser
                            )
                    log.info("job result: ${exec}")
                }

            }
        } catch (JobNotFound nf) {
            log.error("Cannot run job, not found: ${expandedJobId} in project ${data.project}: ${nf}")

            throw new WebhookEventException("Cannot run job, not found: ${expandedJobId} in project ${data.project}: ${nf}",
                    WebhookJobRunHandlerFailureReason.JobNotFound
            )
        } catch (JobExecutionError nf) {
            if(log.isDebugEnabled()) {
                log.debug(
                        "Error attempting to run job: ${expandedJobId} in project ${data.project}: ${nf}",

                        nf
                )
            }else{
                log.error("Error attempting to run job: ${expandedJobId} in project ${data.project}: " +
                          "${nf}")
            }
            throw new WebhookEventException("Error attempting to run job: $expandedJobId in project $data.project: $nf",
                    WebhookJobRunHandlerFailureReason.ExecutionError,
            )
        }
    }

    boolean isRuleMatch(RoutingRule rule, Map<String, Map<String, String>> event) {
        if (rule.conditions.empty)
            return true

        def evaluations = rule.conditions.collect {c ->
            if (c.path == null || c.path.empty)
                return true

            switch (c.condition) {
                case "contains":
                    return contains(c, event)
                case "matches":
                    return matches(c, event)

            }
        }

        switch (rule.policy) {
            case "any":
                return evaluations.any {e -> e}
            case "all":
                return evaluations.every {e -> e}
        }
    }

    boolean contains(Condition condition, Map<String, Map<String, String>> event) {
        def value = event.get(condition.path)

        value instanceof String ?
            value.contains(condition.value) :
            false
    }

    boolean matches(Condition condition, Map<String, Map<String, String>> event) {
        def value = event.get(condition.path)

        value instanceof String ?
            value == condition.value :
            false
    }

    Map<String, String> flattenMap(Map<String, Map<String, String>> map) {
        map.collectEntries { k, v ->
            v instanceof Map ?
                    flattenMap(v).collectEntries { k1, v1 ->
                        def key = k + '.' + k1
                        [ (key): v1 ]
                    } :
                    [ (k): v ]
        } as Map<String, String>
    }

    void toStringMap(Map map) {
        map.each { k, v ->
            if (v instanceof Map)
                toStringMap(v)
            else
                map[k] = v.toString()
        } as Map<String, Map<String, String>>
    }

    static enum WebhookJobRunHandlerFailureReason implements FailureReason {
        InvalidContentType,
        JsonParseError,
        JobNotFound,
        ExecutionError
    }

}


enum PolicyType {
    ANY("any"),
    ALL("all")
}

enum ConditionType {
    CONTAINS("contains"),
    EQUALS("equals")
}

class RoutingRule {
    public String policy
    public String jobId
    public String jobOpts
    public String nodeFilter
    public List<Condition> conditions
}

class Condition {
    public String path
    public String value
    public String condition
}

class Config {
    List<RoutingRule> rules
}