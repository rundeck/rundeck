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

import com.dtolabs.rundeck.core.plugins.configuration.ValuesGenerator
import com.dtolabs.rundeck.plugins.descriptions.DynamicSelectValues
import com.dtolabs.rundeck.plugins.descriptions.PluginProperty
import com.dtolabs.rundeck.plugins.descriptions.SelectValues

import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.ZonedDateTime

/**
 * Defines a timeZone field
 */
trait TimeZonePropertyTrait {
    @PluginProperty(title = "Time Zone", description = "Time Zone to use for schedule")
    @DynamicSelectValues(generatorClass = TimeZonePropertyTrait.TimeZoneGenerator)
    @SelectValues(freeSelect = true, values = [])
    String timeZone

    TimeZone createTimeZone() {
        timeZone ? TimeZone.getTimeZone(timeZone) : null
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
}
