/*
 * Copyright 2016 SimplifyOps, Inc. (http://simplifyops.com)
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

package com.dtolabs.rundeck.app.api.jobs.info

import com.dtolabs.rundeck.app.api.marshall.ApiVersion
import com.dtolabs.rundeck.app.api.marshall.ElementName
import com.dtolabs.rundeck.app.api.marshall.Ignore
import com.dtolabs.rundeck.app.api.marshall.ApiResource
import com.dtolabs.rundeck.app.api.marshall.XmlAttribute
import grails.validation.Validateable
import rundeck.ScheduledExecution

/**
 * Resource view used by /project/jobs listing, and /job/[id]/info, and /scheduler
 */
@ApiResource
@ElementName('job')
class JobInfo {
    @XmlAttribute
    String id

    String name
    String group
    String project
    String description

    @XmlAttribute
    String href

    @XmlAttribute
    String permalink

    @ApiVersion(17)
    @Ignore(onlyIfNull = true)
    @XmlAttribute
    Boolean scheduled

    @ApiVersion(17)
    @Ignore(onlyIfNull = true)
    @XmlAttribute
    Boolean scheduleEnabled

    @ApiVersion(17)
    @Ignore(onlyIfNull = true)
    @XmlAttribute
    Boolean enabled

    @ApiVersion(17)
    @Ignore(onlyIfNull = true)
    @XmlAttribute
    String serverNodeUUID

    @ApiVersion(17)
    @Ignore(onlyIfNull = true)
    @XmlAttribute
    Boolean serverOwner

    @ApiVersion(18)
    @Ignore(onlyIfNull = true)
    @XmlAttribute
    Long averageDuration

    @ApiVersion(18)
    @Ignore(onlyIfNull = true)
    @XmlAttribute
    Date nextScheduledExecution

    @ApiVersion(20)
    @Ignore(onlyIfNull = true)
    @XmlAttribute
    List<Date> futureScheduledExecutions

//    Map blah=[
//            z:'x'
//    ]
//    renders as: <blah z="x"/>
//
//    @SubElement
//    List<Map> submap = [
//            [a: 'b']
//    ]
//    renders as: <map><entry key="a">b</entry></map>

    static JobInfo from(ScheduledExecution se, href, permalink, Map extra = [:]) {
        new JobInfo([id             : se.extid,
                     name           : (se.jobName),
                     group          : (se.groupPath),
                     project        : (se.project),
                     description    : (se.description),
                     href           : href,
                     permalink      : permalink,
                     scheduled      : se.scheduled,
                     scheduleEnabled: se.scheduleEnabled,
                     enabled        : se.executionEnabled
                    ] + extra?.subMap('serverNodeUUID', 'serverOwner', 'averageDuration', 'nextScheduledExecution',
                        'futureScheduledExecutions')
        )
    }
}
