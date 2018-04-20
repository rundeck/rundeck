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

package com.dtolabs.rundeck.app.api.jobs.upload

import com.dtolabs.rundeck.app.api.Paging
import com.dtolabs.rundeck.app.api.marshall.ApiResource
import com.dtolabs.rundeck.app.api.marshall.ElementName
import com.dtolabs.rundeck.app.api.marshall.Ignore
import com.dtolabs.rundeck.app.api.marshall.SubElement
import com.dtolabs.rundeck.app.api.marshall.XmlAttribute

/**
 * @author greg
 * @since 2/24/17
 */

@ApiResource
@ElementName('jobFiles')
class JobFileInfoList {
    JobFileInfoList(final List<JobFileInfo> files, Map params) {
        this.files = files
        if (params) {
            this.paging = Paging.fromMap(params)
        }
    }
    @Ignore(onlyIfNull = true)
    Paging paging
    List<JobFileInfo> files
}
