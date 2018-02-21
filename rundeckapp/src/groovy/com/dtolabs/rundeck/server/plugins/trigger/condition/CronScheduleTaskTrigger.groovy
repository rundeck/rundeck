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
import com.dtolabs.rundeck.core.plugins.configuration.ValidationException
import com.dtolabs.rundeck.plugins.ServiceNameConstants
import com.dtolabs.rundeck.plugins.descriptions.PluginDescription
import com.dtolabs.rundeck.plugins.descriptions.PluginProperty
import com.dtolabs.rundeck.server.plugins.trigger.PluginBaseMetaTrait
import org.quartz.CronExpression
import org.quartz.CronScheduleBuilder
import org.quartz.DateBuilder
import org.quartz.Trigger
import org.quartz.TriggerBuilder
import org.rundeck.core.tasks.TaskPluginTypes
import org.rundeck.core.tasks.TaskTrigger

import java.text.ParseException

/**
 *
 */

@Plugin(name = CronScheduleTaskTrigger.PROVIDER_NAME, service = TaskPluginTypes.TaskTrigger)
@PluginDescription(title = 'Cron Schedule',
        description = '''Use a Cron expression for a schedule''')
class CronScheduleTaskTrigger
        implements TaskTrigger, QuartzSchedulerTaskTrigger, TimeZonePropertyTrait, PluginBaseMetaTrait {
    static final String PROVIDER_NAME = 'cron'
    String type = PROVIDER_NAME

    @PluginProperty(title = "Cron Expression", description = "A Cron expression.", validatorClass = CronScheduleTaskTrigger.CronValidator, required = true)
    String cronExpression

    static class CronValidator implements PropertyValidator {
        @Override
        boolean isValid(final String value) throws ValidationException {
            try {
                CronExpression.validateExpression(value)
            } catch (ParseException e) {
                throw new ValidationException(e.message, e)
            }
            true
        }
    }


    Trigger buildQuartzTrigger(TriggerBuilder builder) {
        def Trigger trigger
        try {

            def schedule = CronScheduleBuilder.cronSchedule(cronExpression)
            if (timeZone) {
                schedule.inTimeZone(createTimeZone())
            }
            trigger = builder.withSchedule(schedule)
            //.startAt() //TODO: set a start time
                             .build()

        } catch (java.text.ParseException ex) {
            throw new RuntimeException("Failed creating trigger. Invalid cron expression: " + cronExpression)
        }
        return trigger
    }

    @Override
    boolean isValidSchedule() {
        CronExpression.isValidExpression(cronExpression)
    }
    Map meta = [glyphicon: 'calendar']
}
