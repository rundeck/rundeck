/*
 * Copyright 2018 Rundeck, Inc. (http://rundeck.com)
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

package com.dtolabs.rundeck.server.plugins.trigger.action

import com.dtolabs.rundeck.core.plugins.Plugin
import com.dtolabs.rundeck.core.plugins.configuration.StringRenderingConstants
import com.dtolabs.rundeck.plugins.descriptions.PluginDescription
import com.dtolabs.rundeck.plugins.descriptions.PluginProperty
import com.dtolabs.rundeck.plugins.descriptions.RenderingOption
import com.dtolabs.rundeck.plugins.descriptions.RenderingOptions
import org.rundeck.core.triggers.TriggerAction

@Plugin(name = JobRunTriggerAction.PROVIDER_NAME, service = 'TriggerAction')
@PluginDescription(title = 'Run a Job', description = 'Runs a job')
class JobRunTriggerAction implements TriggerAction {
    static final String PROVIDER_NAME = 'JobRun'
    String type
    Map data
    @PluginProperty(required = true, title = 'Job', description = 'Job to run.')
    @RenderingOptions(
            [
                    @RenderingOption(key = StringRenderingConstants.SELECTION_ACCESSOR_KEY, value = 'RUNDECK_JOB'),
                    @RenderingOption(key = 'data.selector.result', value = 'id')
            ]
    )
    String jobId

    @PluginProperty(title = 'Options', description = 'Execution options for the Job')
    @RenderingOptions(
            [
                    @RenderingOption(key = StringRenderingConstants.SELECTION_ACCESSOR_KEY, value = 'RUNDECK_JOB_OPTIONS'),
                    @RenderingOption(key = StringRenderingConstants.ASSOCIATED_PROPERTY_KEY, value = 'jobId'),
                    @RenderingOption(key = StringRenderingConstants.REQUIRED_PROPERTY_KEY, value = 'jobId'),
            ]
    )
    Map optionData
    Map extraData

    JobRunTriggerAction() {

    }

    JobRunTriggerAction(Map data) {
        this.type = PROVIDER_NAME
        this.data = data
        this.jobId = data?.jobId
        this.optionData = data?.options
        this.extraData = data?.extra
    }

    String getArgString() {
        'TODO'
    }

    String getFilter() {
        extraData?.filter
    }

    String getAsUser() {
        extraData?.asUser
    }


}
