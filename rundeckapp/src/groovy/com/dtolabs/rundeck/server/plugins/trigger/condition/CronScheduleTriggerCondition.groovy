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
import com.dtolabs.rundeck.core.plugins.configuration.ValuesGenerator
import com.dtolabs.rundeck.plugins.descriptions.DynamicSelectValues
import com.dtolabs.rundeck.plugins.descriptions.PluginDescription
import com.dtolabs.rundeck.plugins.descriptions.PluginProperty
import com.dtolabs.rundeck.plugins.descriptions.SelectValues
import org.quartz.CronExpression
import org.quartz.CronScheduleBuilder
import org.quartz.Trigger
import org.quartz.TriggerBuilder
import org.rundeck.core.triggers.TriggerCondition

import java.text.ParseException
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.ZonedDateTime

/**
 *
 */

@Plugin(name = CronScheduleTriggerCondition.PROVIDER_NAME, service = 'TriggerCondition')
@PluginDescription(title = 'Cron Schedule',
        description = '''Use a Cron expression for a schedule''')
class CronScheduleTriggerCondition implements TriggerCondition, QuartzSchedulerCondition {
    static final String PROVIDER_NAME = 'cron'
    String type = PROVIDER_NAME

    @PluginProperty(title = "Time Zone", description = "Time Zone to use for schedule")
    @DynamicSelectValues(generatorClass = CronScheduleTriggerCondition.TimeZoneGenerator)
    @SelectValues(freeSelect = true, values = [])
    String timeZone

    @PluginProperty(title = "Cron Expression", description = "A Cron expression.", validatorClass = CronScheduleTriggerCondition.CronValidator, required = true)
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

    static class TimeZoneGenerator implements ValuesGenerator {


        @Override
        List<ValuesGenerator.Entry> generateValues() {
            return TimeZone.getAvailableIDs().collect {
                entry(it, describe(TimeZone.getTimeZone(it)))
            }
        }

        private static String describe(TimeZone zone) {
            LocalDateTime dt = LocalDateTime.now();
            ZonedDateTime zdt = dt.atZone(zone.toZoneId());
            ZoneOffset offset = zdt.getOffset();
            return "$zone.ID (GMT $offset)"
        }

    }


    def Trigger createTrigger(String qJobName, String qGroupName) {
        def Trigger trigger
        try {

            def schedule = CronScheduleBuilder.cronSchedule(cronExpression)
            if (timeZone) {
                schedule.inTimeZone(TimeZone.getTimeZone(timeZone))
            }
            trigger = TriggerBuilder.newTrigger()
                                    .withIdentity(qJobName, qGroupName)
                                    .withSchedule(schedule)
            //.startAt() //TODO: set a start time
                                    .build()

        } catch (java.text.ParseException ex) {
            throw new RuntimeException("Failed creating trigger. Invalid cron expression: " + cronExpression)
        }
        return trigger
    }

}
