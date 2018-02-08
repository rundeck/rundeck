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
import com.dtolabs.rundeck.plugins.descriptions.DynamicSelectValues
import com.dtolabs.rundeck.plugins.descriptions.PluginDescription
import com.dtolabs.rundeck.plugins.descriptions.PluginProperty
import com.dtolabs.rundeck.plugins.descriptions.SelectValues
import org.quartz.*
import org.rundeck.core.triggers.TriggerCondition

import java.util.Calendar
import java.util.regex.Pattern

@Plugin(name = DailyIntervalTriggerCondition.PROVIDER_NAME, service = 'TriggerCondition')
@PluginDescription(title = 'Daily Interval Schedule',
        description = '''Daily interval scheduler with start and end times of day, and days of week''')

class DailyIntervalTriggerCondition implements TriggerCondition, QuartzSchedulerCondition {
    static final String PROVIDER_NAME = 'daily'
    String type = PROVIDER_NAME

    @PluginProperty(title = "Interval", description = "Number of units", required = true)
    int interval

    @PluginProperty(title = "Unit", description = "Time unit", validatorClass = CalendarIntervalTriggerCondition.IntervalUnitValidator, required = true)
    @SelectValues(values = [], dynamicValues = true)
    @DynamicSelectValues(generatorClass = CalendarIntervalTriggerCondition.IntervalUnitGenerator)
    String unit

    @PluginProperty(title = "Days of Week", description = "Include these days of the week", required = false)
    @SelectValues(values = ['Sunday', 'Monday', 'Tuesday', 'Wednesday', 'Thursday', 'Friday'])
    List<String> daysOfWeek
    private static Map<String, Integer> daymap = [
            Sunday   : Calendar.SUNDAY,
            Monday   : Calendar.MONDAY,
            Tuesday  : Calendar.TUESDAY,
            Wednesday: Calendar.WEDNESDAY,
            Thursday : Calendar.THURSDAY,
            Friday   : Calendar.FRIDAY,
            Saturday : Calendar.SATURDAY
    ]

    @PluginProperty(title = "Daily Start Time", description = "Time of day to start, in HH:MM or HH:MM:SS format, using 24 hour clock. Blank means no set time of day to start.",
            validatorClass = TimeOfDayValidator, required = false)
    String startingDailyAt

    @PluginProperty(title = "Daily End Time", description = "Time of day to end, in HH:MM or HH:MM:SS format, using 24 hour clock. Blank means no set time of day to end. Mutually exclusive with `Daily Repeat Count`.",
            validatorClass = TimeOfDayValidator, required = false)
    String endingDailyAt
    @PluginProperty(title = "Daily Repeat Count", description = "Maximum number of times to repeat in the day. After this count is reached, the schedule will stop. Requires that `Daily Start Time` is also set. Mutually exclusive with `Daily End Time`.",
            required = false)
    int endingAfterRepeat


    private DateBuilder.IntervalUnit getIntervalUnit() {
        DateBuilder.IntervalUnit.valueOf(unit)
    }
    static Pattern TIME_OFDAY_PAT = Pattern.compile(/^(\d{2}):(\d{2})(:(\d{2}))?$/)
    static class TimeOfDayValidator implements PropertyValidator {

        public static final String INVALID_FORMAT = "Must match HH:MM or HH:MM:SS format, using 24 hour clock."

        @Override
        boolean isValid(final String value) throws ValidationException {
            parseTimeOfDay(value) != null
        }

        static TimeOfDay parseTimeOfDay(String value) throws ValidationException {
            def matcher = TIME_OFDAY_PAT.matcher(value.trim())
            if (!matcher.matches()) {
                throw new ValidationException(INVALID_FORMAT)
            }
            def vals = value.split(/:/)

            if (vals.length < 2 || vals.length > 3 || vals.any { !it }) {
                throw new ValidationException(INVALID_FORMAT)
            }
            def ints = vals.collect Integer.&parseInt

            if (ints[0] > 23 || ints[1] > 59 || ints.size() == 3 && ints[2] > 59) {
                throw new ValidationException(INVALID_FORMAT)
            }
            if (ints.size() > 2) {
                return TimeOfDay.hourMinuteAndSecondOfDay(ints[0], ints[1], ints[2])
            }
            return TimeOfDay.hourAndMinuteOfDay(ints[0], ints[1])

        }
    }

    private TimeOfDay getStartingTimeOfDay() {
        TimeOfDayValidator.parseTimeOfDay(startingDailyAt)
    }

    private TimeOfDay getEndingTimeOfDay() {
        TimeOfDayValidator.parseTimeOfDay(endingDailyAt)
    }

    private Set<Integer> toDaysOfWeek() {
        daysOfWeek?.collect { daymap[it] }
    }

    @Override
    Trigger createTrigger(final String qJobName, final String qGroupName) {
        def schedule = DailyTimeIntervalScheduleBuilder.dailyTimeIntervalSchedule()


        schedule.withInterval(interval, intervalUnit)

        if (daysOfWeek) {
            schedule.onDaysOfTheWeek(toDaysOfWeek())
        } else {
            schedule.onEveryDay()
        }

        if (startingDailyAt) {
            schedule.startingDailyAt(startingTimeOfDay)
        }
        if (endingDailyAt) {
            schedule.endingDailyAt(endingTimeOfDay)
        }
        if (endingAfterRepeat && startingDailyAt) {
            schedule.endingDailyAfterCount(endingAfterRepeat)
        }

        //TODO: what if configured interval too short, e.g. milliseconds?


        return TriggerBuilder.newTrigger()
                             .withIdentity(qJobName, qGroupName)
                             .withSchedule(schedule)
        //.startAt() //TODO: set a start time
                             .build()


    }
}
