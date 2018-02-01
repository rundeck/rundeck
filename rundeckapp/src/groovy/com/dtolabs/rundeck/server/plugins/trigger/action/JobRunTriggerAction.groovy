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

package com.dtolabs.rundeck.server.plugins.trigger.action

import org.rundeck.core.triggers.Action

class JobRunTriggerAction implements Action {
    static final String providerName = 'JobRun'
    String type
    Map data
    String jobId
    Map optionData
    Map extraData

    JobRunTriggerAction(Map data) {
        this.type = providerName
        this.data = data
        this.jobId = data?.jobId
        this.optionData = data?.options
        this.extraData = data?.extra
    }
    String getArgString(){
        'TODO'
    }
    String getFilter(){
        extraData?.filter
    }
    String asUser(){
        extraData?.asUser
    }

    static JobRunTriggerAction fromConfig(Map map) {
        new JobRunTriggerAction(map)
    }

}
