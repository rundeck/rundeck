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
import org.quartz.CronScheduleBuilder
import org.quartz.Trigger
import org.quartz.TriggerBuilder
import org.rundeck.core.triggers.TriggerCondition

/**
 *
 */

@Plugin(name = ScheduleTriggerCondition.PROVIDER_NAME, service = 'TriggerCondition')
@PluginDescription(title = 'Schedule',
        description = '''Triggers actions on a schedule''')
class ScheduleTriggerCondition implements TriggerCondition {
    static final String PROVIDER_NAME = 'schedule'
    Map config

    @Override
    Map getConditionData() {
        [:]
    }
    @PluginProperty
    String timeZone
    @PluginProperty
    String cronExpression


    ScheduleTriggerCondition() {

    }
    ScheduleTriggerCondition(Map config) {
        this.config = config
        timeZone = config?.timeZone
        cronExpression = config?.cronExpression
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
                    .build()

        } catch (java.text.ParseException ex) {
            throw new RuntimeException("Failed creating trigger. Invalid cron expression: " + cronExpression)
        }
        return trigger
    }

}
