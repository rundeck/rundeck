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
import com.dtolabs.rundeck.plugins.descriptions.PluginDescription
import com.dtolabs.rundeck.plugins.descriptions.PluginProperty
import com.dtolabs.rundeck.plugins.descriptions.SelectValues
import org.quartz.SimpleScheduleBuilder
import org.quartz.Trigger
import org.quartz.TriggerBuilder
import org.rundeck.core.triggers.TriggerCondition

@Plugin(name = SimpleScheduleTriggerCondition.PROVIDER_NAME, service = 'TriggerCondition')
@PluginDescription(title = 'Simple Schedule',
        description = '''Set a simple interval schedule''')

class SimpleScheduleTriggerCondition implements TriggerCondition, QuartzSchedulerCondition {
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
    Trigger createTrigger(final String qJobName, final String qGroupName) {
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

        trigger = TriggerBuilder.newTrigger()
                                .withIdentity(qJobName, qGroupName)
                                .withSchedule(schedule)
        //.startAt() //TODO: set a start time
                                .build()


        return trigger
    }
}
