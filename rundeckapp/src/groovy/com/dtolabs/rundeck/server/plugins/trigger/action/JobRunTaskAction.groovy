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
import com.dtolabs.rundeck.core.plugins.configuration.PropertyValidator
import com.dtolabs.rundeck.core.plugins.configuration.StringRenderingConstants
import com.dtolabs.rundeck.core.plugins.configuration.ValidationException
import com.dtolabs.rundeck.plugins.descriptions.PluginDescription
import com.dtolabs.rundeck.plugins.descriptions.PluginProperty
import com.dtolabs.rundeck.plugins.descriptions.RenderingOption
import com.dtolabs.rundeck.plugins.descriptions.RenderingOptions
import com.dtolabs.rundeck.server.plugins.trigger.PluginBaseMetaTrait
import org.rundeck.core.tasks.TaskAction
import org.rundeck.core.tasks.TaskPluginTypes

@Plugin(name = JobRunTaskAction.PROVIDER_NAME, service = TaskPluginTypes.TaskAction)
@PluginDescription(title = 'Run a Job', description = 'Runs a job')
class JobRunTaskAction implements TaskAction, PluginBaseMetaTrait {
    static final String PROVIDER_NAME = 'JobRun'
    String type
    Map data
    @PluginProperty(required = true, title = 'Job', description = 'Job to run.', validatorClass = UUIDValidator)
    @RenderingOptions(
            [
                    @RenderingOption(key = StringRenderingConstants.SELECTION_ACCESSOR_KEY, value = 'RUNDECK_JOB'),
                    @RenderingOption(key = StringRenderingConstants.SELECTION_COMPONENT_KEY, value = 'uuid'),
                    @RenderingOption(key = StringRenderingConstants.DISPLAY_TYPE_KEY, value = 'RUNDECK_JOB')
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

    static class UUIDValidator implements PropertyValidator {
        @Override
        boolean isValid(final String value) throws ValidationException {
            try {
                UUID.fromString(value)
            } catch (IllegalArgumentException e) {
                throw new ValidationException(e.message, e)
            }
            true
        }
    }

    JobRunTaskAction() {

    }

    JobRunTaskAction(Map data) {
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

    Map meta = [glyphicon: 'play']

}
