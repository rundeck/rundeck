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

@Plugin(name='webhook-run-job',service= ServiceNameConstants.WebhookEvent)
@PluginDescription(title="Webhook Run Job",description="Run a job on webhook event")
class JobRunWebhookEventPlugin implements WebhookEventPlugin {
    Map config

    static final String JSON_DATA_TYPE = "application/json"
    static final ObjectMapper mapper = new ObjectMapper()

    static Logger log = LoggerFactory.getLogger(JobRunWebhookEventPlugin)


    @PluginProperty(required = true, title = 'Job', description = 'Job to run.', validatorClass = UUIDPropertyValidator)
    @RenderingOptions(
            [
                    @RenderingOption(key = StringRenderingConstants.SELECTION_ACCESSOR_KEY, value = 'RUNDECK_JOB'),
                    @RenderingOption(key = 'selectionComponent', value = 'uuid'),
                    @RenderingOption(key = StringRenderingConstants.DISPLAY_TYPE_KEY, value = 'RUNDECK_JOB')
            ]
    )
    String jobId

    @PluginProperty(title="Options", description = "Job Option arguments, in the form `-opt1 value -opt2 \"other value\"`")
    String argString

    @PluginProperty(title="Node Filter", description = "Node Filter string to apply, leave blank for default")
    String nodeFilter

    @PluginProperty(title="As User", description = "Username to run job as, leave blank for default, requires authorization.")
    String asUser

    @Override
    void onEvent(final WebhookEventContext context, final WebhookData data) throws WebhookEventException {
        log.debug("webhook event: ${data.id} ")

        if(JSON_DATA_TYPE != data.contentType) {
            throw new WebhookEventException("Posted data was not json",WebhookJobRunHandlerFailureReason.InvalidContentType)
        }

        JobService jobService = context.services.getService(JobService)
        String expandedJobId = jobId
        try {

            //expand values in the argString/jobOptions map

            final Map<String, String> webhookMap = new HashMap<>()
            webhookMap.put("id", data.id)
            if (null != data.project) {
                webhookMap.put("project", data.project)
            }
            Map<String, Map<String, String>> localDataContext = DataContextUtils.context("webhook", webhookMap)

//            if (context.contextData != null) {
//                localDataContext = DataContextUtils.addContext(
//                        "context",
//                        MapData.toStringStringMap(context.contextData),
//                        localDataContext
//                )
//            }

//            final Map<String, String> eventMetaMap = createEventMetaMap(event);
//            localDataContext = DataContextUtils.addContext("event", eventMetaMap, localDataContext);
//
//            Map<String, String> sourceData = createEventSourceMap(event);
//            localDataContext = DataContextUtils.addContext("eventSource", sourceData, localDataContext);
            try {
                def webhookdata = mapper.readValue(data.data, HashMap)
                Map<String, String> eventData = new HashMap<>()
                if (webhookdata != null && webhookdata instanceof Map) {
                    eventData = MapData.toStringStringMap((Map) webhookdata)
                }
                localDataContext = DataContextUtils.addContext("data", eventData, localDataContext)
            } catch(Exception ex) {
                throw new WebhookEventException("Unable to parse posted JSON data: ${ex.message}",WebhookJobRunHandlerFailureReason.JsonParseError)
            }

            expandedJobId = DataContextUtils.replaceDataReferencesInString(
                    jobId,
                    localDataContext
            )

            JobReference jobReference = jobService.jobForID(
                    expandedJobId, data.project
            )

            def expandedNodeFilter = DataContextUtils.replaceDataReferencesInString(nodeFilter, localDataContext)
            def expandedAsUser = DataContextUtils.replaceDataReferencesInString(asUser, localDataContext)
            //TODO: switch to use Map instead of String
            String[] args = argString ? OptsUtil.burst(argString) : new String[0]
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

    static enum WebhookJobRunHandlerFailureReason implements FailureReason {
        InvalidContentType,
        JsonParseError,
        JobNotFound,
        ExecutionError
    }

}
