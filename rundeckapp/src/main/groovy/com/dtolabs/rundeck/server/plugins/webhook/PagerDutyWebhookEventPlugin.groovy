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
import com.dtolabs.rundeck.core.utils.OptsUtil
import com.dtolabs.rundeck.core.webhook.WebhookEventException
import com.dtolabs.rundeck.plugins.ServiceNameConstants
import com.dtolabs.rundeck.plugins.descriptions.PluginDescription
import com.dtolabs.rundeck.plugins.webhook.WebhookData
import com.dtolabs.rundeck.plugins.webhook.WebhookEventContext
import com.dtolabs.rundeck.plugins.webhook.WebhookEventPlugin
import com.fasterxml.jackson.databind.ObjectMapper
import com.jayway.jsonpath.Configuration
import com.jayway.jsonpath.Option
import com.jayway.jsonpath.PathNotFoundException
import org.codehaus.groovy.runtime.typehandling.GroovyCastException
import org.rundeck.utils.UUIDPropertyValidator
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import rundeck.Webhook

import com.jayway.jsonpath.JsonPath


@Plugin(name='pagerduty-run-job',service= ServiceNameConstants.WebhookEvent)
@PluginDescription(title="PagerDuty Webhook Run Job",description="Run a job on webhook event")
class PagerDutyWebhookEventPlugin implements WebhookEventPlugin {
    static final String JSON_DATA_TYPE = "application/json"
    static final ObjectMapper mapper = new ObjectMapper()

    static Logger log = LoggerFactory.getLogger(JobRunWebhookEventPlugin)

    Map config

    @Override
    void onEvent(final WebhookEventContext context, final WebhookData data) throws WebhookEventException {
        if(JSON_DATA_TYPE != data.contentType) {
            throw new WebhookEventException("Posted data was not json",WebhookJobRunHandlerFailureReason.InvalidContentType)
        }

        Config conf
        try {
            conf = mapper.convertValue(config, Config.class)
        } catch(Exception ex) {
            throw new WebhookEventException("Unable to load config: ${ex.message}", WebhookJobRunHandlerFailureReason.ConfigLoadError)
        }

        JobService jobService = context.services.getService(JobService)

        //expand values in the argString/jobOptions map

        final Map<String, String> webhookMap = new HashMap<>()
        webhookMap.put("id", data.id)
        if (null != data.project) {
            webhookMap.put("project", data.project)
        }

        Map webhookdata

        try {
            webhookdata = mapper.readValue(data.data, HashMap)
        } catch(Exception ex) {
            throw new WebhookEventException("Unable to parse posted JSON data: ${ex.message}",WebhookJobRunHandlerFailureReason.JsonParseError)
        }

        List<Map> messages = extractMessageBatches(conf.batchKey, webhookdata)

        messages.each { m ->
            conf.rules.each { r ->
                if (! r.isMatch(m))
                    return

                def jobId = r.jobId


                def expandedJobId = template(jobId, m)

                JobReference jobReference = jobService.jobForID(
                        expandedJobId, data.project
                )

                def expandedNodeFilter = template(r.nodeFilter, m)
                def expandedAsUser = ''

                def options = processJobOptions(r.jobOptions, m)

                log.info(
                        "starting job ${expandedJobId} with args: ${options}, " +
                                "nodeFilter: ${expandedNodeFilter} asUser: ${expandedAsUser}"
                )

                try {
                    ExecutionReference exec =
                            jobService.runJob(
                                    jobReference,
                                    options,
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
        }
    }

    static List<Map> extractMessageBatches(String batchKey, Map data) {
        List<Map> messages = []
        try {
            if ( ! batchKey.empty )
                messages = JsonPath.read(data, batchKey)
            else
                messages = [data]
        } catch(PathNotFoundException ex) {
            //TODO: ???
        } catch(GroovyCastException) {
            //TODO: Not a List
        }

        return messages.flatten()
    }

    static Map<String, String> processJobOptions(List<JobOption> options, Map event) {
        Map<String, String> retOptions = new HashMap<String, String>()

        options.each { o ->
            def renderedValue = template(o.value, event)

            retOptions.put(o.name, renderedValue)
        }

        return retOptions
    }

    static Map<String, String> flattenMap(Map map) {
        map.collectEntries { k, v ->
            v instanceof Map ?
                    flattenMap(v).collectEntries { k1, v1 ->
                        def key = k + '.' + k1
                        [ (key): v1 ]
                    } :
                    [ (k): v ]
        } as Map<String, String>
    }

    static Map<String, Map<String, String>> toStringMap(Map map) {
        final Map<String, Map<String, String>> nuMap = new HashMap()

        def doStringMap
        doStringMap = { Map oldMap, Map newMap ->
            oldMap.each { k, v ->
                if (v instanceof Map) {
                    newMap[k] = new HashMap()
                    doStringMap(v, newMap[k])
                }
                else {
                    newMap[k] = v.toString()
                }

            } as Map<String, Map<String, String>>
        }
        doStringMap(map, nuMap)
        nuMap
    }

    static String template(String input, Map data, Object defaultValue = '') {
        try {
            if (input.startsWith('$')) {
                def val = JsonPath.read(data, input)

                if (val instanceof List || val instanceof Map)
                    return mapper.writeValueAsString(val)
                else
                    return val.toString()

            } else {
                String value
                if (input.startsWith('\$'))
                    value = input.substring(1)
                else
                    value = input

                return groovyTemplate(value, data, defaultValue)
            }
        } catch(PathNotFoundException ex) {
            return defaultValue
        }
    }

    static String groovyTemplate(String input, Map data, Object defaultValue = '') {
        def engine = new groovy.text.SimpleTemplateEngine()

        try {

            def template = engine.createTemplate(input)

            template.make([
                    data: data.withDefault { defaultValue },
                    path: { String p ->
                        JsonPath.read(data, p)
                    }
            ].withDefault { defaultValue }).toString()
        } catch(GroovyRuntimeException ex) {
            //TODO: Template parse may have failed or possibly during make
        }
    }

    static enum WebhookJobRunHandlerFailureReason implements FailureReason {
        InvalidContentType,
        JsonParseError,
        JobNotFound,
        ExecutionError,
        ConfigLoadError,
    }

}

class JobOption {
    public String name
    public String value
}

class RoutingRule {
    public String name
    public String description
    public String policy
    public String jobId
    public String jobArgString
    public List<JobOption> jobOptions
    public String nodeFilter
    public String user
    public List<Condition> conditions

    boolean isMatch(Map event) {
        if (conditions.empty)
            return true

        def evaluations = conditions.collect {c ->
            println(c.isMatch(event))
            return c.isMatch(event)
        }

        switch (policy) {
            case "any":
                return evaluations.any {e -> e}
            case "all":
                return evaluations.every {e -> e}
        }
    }
}

class Condition {
    public String path
    public String value
    public String condition

    private jpath = JsonPath.using(Configuration.defaultConfiguration().addOptions(Option.DEFAULT_PATH_LEAF_TO_NULL))

    boolean isMatch(Map event) {
        // Handle empty conditions until better validation
        if (path == null || path.empty)
            return true

        switch (condition) {
            case "contains":
                return contains(event)
            case "matches":
                return matches(event)
            case "exists":
                return exists(event)

        }
    }

    boolean contains(Map event) {
        def res = jpath.parse(event).read(path)

        res instanceof String ?
                res.contains(value) :
                false
    }

    boolean matches(Map event) {
        def res = jpath.parse(event).read(path)

        res instanceof String ?
                res == value :
                false
    }

    boolean exists(Map event) {
        def res = jpath.parse(event).read(path)

        if (res == null)
            return false

        return true
    }
}

class Config {
    String batchKey
    List<RoutingRule> rules
}