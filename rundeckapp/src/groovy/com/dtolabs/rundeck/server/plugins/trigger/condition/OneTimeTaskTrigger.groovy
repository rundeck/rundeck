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

package com.dtolabs.rundeck.server.plugins.trigger.condition

import com.dtolabs.rundeck.core.plugins.Plugin
import com.dtolabs.rundeck.core.plugins.configuration.PropertyValidator
import com.dtolabs.rundeck.core.plugins.configuration.StringRenderingConstants
import com.dtolabs.rundeck.core.plugins.configuration.ValidationException
import com.dtolabs.rundeck.plugins.ServiceNameConstants
import com.dtolabs.rundeck.plugins.descriptions.PluginDescription
import com.dtolabs.rundeck.plugins.descriptions.PluginProperty
import com.dtolabs.rundeck.plugins.descriptions.RenderingOption
import com.dtolabs.rundeck.plugins.descriptions.RenderingOptions
import com.dtolabs.rundeck.server.plugins.trigger.PluginBaseMetaTrait
import org.quartz.Trigger
import org.quartz.TriggerBuilder
import org.rundeck.core.tasks.TaskPluginTypes
import org.rundeck.core.tasks.TaskTrigger

import java.text.ParseException
import java.text.SimpleDateFormat

@Plugin(name = OneTimeTaskTrigger.PROVIDER_NAME, service = TaskPluginTypes.TaskTrigger)
@PluginDescription(title = 'One Time',
        description = '''Triggers once at a certain time and date''')

class OneTimeTaskTrigger
        implements TaskTrigger, QuartzSchedulerTaskTrigger, TimeZonePropertyTrait, PluginBaseMetaTrait {
    static final String PROVIDER_NAME = 'onetime'
    String type = PROVIDER_NAME

    @PluginProperty(title = "Time and Date", description = "A single time and date in the future.\n\nISO 8601 format is expected", required = true, validatorClass = ISO8601Validator)
    @RenderingOptions([
            @RenderingOption(key = StringRenderingConstants.SELECTION_ACCESSOR_KEY, value = 'datetime'),
            @RenderingOption(key = StringRenderingConstants.DISPLAY_TYPE_KEY, value = 'datetime'),
            @RenderingOption(key = 'dateFormat', value = 'YYYY-MM-DDTHH:mm:ssZ'),
    ])
    String time

    @Override
    Trigger buildQuartzTrigger(TriggerBuilder builder) {
        Date date = createDate(time, createTimeZone())
        return builder.startAt(date).build()

    }

    static Date createDate(String time, TimeZone timeZone) {
        SimpleDateFormat iso8601 = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ");
        if (timeZone) {
            iso8601.setTimeZone(timeZone)
        }
        iso8601.parse(time)
    }

    static class ISO8601Validator implements PropertyValidator {
        @Override
        boolean isValid(final String value) throws ValidationException {
            try {
                return createDate(value, null) != null
            } catch (ParseException e) {
                throw new ValidationException(e.message, e)
            }
        }
    }
    Map meta = [glyphicon: 'calendar']
}
