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
import com.dtolabs.rundeck.plugins.ServiceNameConstants
import com.dtolabs.rundeck.plugins.descriptions.DynamicSelectValues
import com.dtolabs.rundeck.plugins.descriptions.PluginDescription
import com.dtolabs.rundeck.plugins.descriptions.PluginProperty
import com.dtolabs.rundeck.plugins.descriptions.SelectValues
import org.quartz.CalendarIntervalScheduleBuilder
import org.quartz.DateBuilder.IntervalUnit
import org.quartz.Trigger
import org.quartz.TriggerBuilder
import org.rundeck.core.tasks.TaskPluginTypes
import org.rundeck.core.tasks.TaskTrigger

import static org.rundeck.core.tasks.TaskPluginTypes.TaskTrigger

@Plugin(name = CalendarIntervalTaskTrigger.PROVIDER_NAME, service = TaskPluginTypes.TaskTrigger)
@PluginDescription(title = 'Calendar Interval Schedule',
        description = '''Calendar based interval schedules''')

class CalendarIntervalTaskTrigger implements TaskTrigger, QuartzSchedulerTaskTrigger, TimeZonePropertyTrait {
    static final String PROVIDER_NAME = 'interval'
    String type = PROVIDER_NAME

    @PluginProperty(title = "Interval", description = "Number of units", required = true)
    int interval

    @PluginProperty(title = "Unit", description = "Time unit", validatorClass = IntervalUnitValidator, required = true)
    @SelectValues(values = [], dynamicValues = true)
    @DynamicSelectValues(generatorClass = IntervalUnitGenerator)
    String unit


    static class IntervalUnitGenerator implements ValuesGenerator {
        @Override
        List<String> generateValuesStrings() {
            IntervalUnit.values()*.toString()
        }
    }

    private IntervalUnit getIntervalUnit() {
        IntervalUnit.valueOf(unit)
    }


    static class IntervalUnitValidator implements PropertyValidator {
        @Override
        boolean isValid(final String value) throws ValidationException {
            try {
                return null != IntervalUnit.valueOf(value)
            } catch (IllegalArgumentException ignored) {
                throw new ValidationException("Not a valid unit: " + value)
            }
        }
    }

    @Override
    Trigger buildQuartzTrigger(TriggerBuilder builder) {
        def Trigger trigger

        def schedule = CalendarIntervalScheduleBuilder.calendarIntervalSchedule()

        if (timeZone) {
            schedule.inTimeZone(TimeZone.getTimeZone(timeZone))
        }
        //TODO: what if configured interval too short, e.g. milliseconds?
        //TODO: set a start time
        schedule.withInterval(interval, intervalUnit)

        trigger = builder.withSchedule(schedule)
        //.startAt() //TODO: set a start time
                         .build()


        return trigger
    }

}
