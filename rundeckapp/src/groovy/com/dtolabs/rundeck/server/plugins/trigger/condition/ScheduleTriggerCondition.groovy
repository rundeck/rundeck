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

import org.quartz.CronScheduleBuilder
import org.quartz.Trigger
import org.quartz.TriggerBuilder
import org.rundeck.core.triggers.Condition

/**
 *
 */
class ScheduleTriggerCondition implements Condition {
    static final String providerName = 'schedule'
    Map config

    @Override
    Map getConditionData() {
        [:]
    }
    String timeZone
    String cronExpression


    ScheduleTriggerCondition(Map config) {
        this.config = config
        timeZone = config?.timeZone
        cronExpression = config?.cronExpression
    }

    static ScheduleTriggerCondition fromConfig(Map config) {
        new ScheduleTriggerCondition(config)
    }

    String getConditionType() {
        return ScheduleTriggerCondition.providerName
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
