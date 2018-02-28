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

package com.dtolabs.rundeck.server.plugins.tasks.condition

import com.dtolabs.rundeck.core.plugins.Plugin
import com.dtolabs.rundeck.plugins.descriptions.PluginDescription
import com.dtolabs.rundeck.plugins.descriptions.PluginProperty
import com.dtolabs.rundeck.plugins.descriptions.SelectValues
import com.dtolabs.rundeck.server.plugins.tasks.PluginBaseMetaTrait
import org.quartz.SimpleScheduleBuilder
import org.quartz.Trigger
import org.quartz.TriggerBuilder
import org.rundeck.core.tasks.TaskPluginTypes
import org.rundeck.core.tasks.TaskTrigger

@Plugin(name = SimpleScheduleTaskTrigger.PROVIDER_NAME, service = TaskPluginTypes.TaskTrigger)
@PluginDescription(title = 'Simple Schedule',
        description = '''Set a simple interval schedule''')

class SimpleScheduleTaskTrigger implements TaskTrigger, QuartzSchedulerTaskTrigger, PluginBaseMetaTrait {
    static final String PROVIDER_NAME = 'simple'
    String type = PROVIDER_NAME

    @PluginProperty(title = "Interval", description = "Time interval", required = true)
    Integer interval;

    @PluginProperty(title = "Unit", description = "Time Unit", required = true)
    @SelectValues(values = ['Seconds', 'Minutes', 'Hours'])
    String unit;

    @PluginProperty(title = "Repeat", description = "Repeat count. Leave blank to repeat forever.  If set, the trigger will repeat a maximum number of times", required = false)
    Integer repeatCount;


    @Override
    Trigger buildQuartzTrigger(TriggerBuilder builder) {
        def Trigger trigger

        def schedule = SimpleScheduleBuilder.simpleSchedule()

        //TODO: what if configured interval too short, e.g. milliseconds?
        //TODO: set a start time

        if (repeatCount > 0) {

            schedule.withRepeatCount(repeatCount);
        } else {
            schedule.repeatForever()
        }
        if (unit == 'Seconds') {
            schedule.withIntervalInSeconds(interval)
        } else if (unit == 'Minutes') {
            schedule.withIntervalInMinutes(interval)
        } else if (unit == 'Hours') {
            schedule.withIntervalInHours(interval)
        }

        trigger = builder.withSchedule(schedule)
        //.startAt() //TODO: set a start time
                         .build()


        return trigger
    }

    @Override
    boolean isValidSchedule() {
        interval > 0
    }
    Map meta = [glyphicon: 'calendar']
}
