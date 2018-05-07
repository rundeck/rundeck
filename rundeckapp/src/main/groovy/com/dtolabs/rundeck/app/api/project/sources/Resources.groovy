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

package com.dtolabs.rundeck.app.api.project.sources

import com.dtolabs.rundeck.app.api.marshall.ApiResource
import com.dtolabs.rundeck.app.api.marshall.Ignore
import com.dtolabs.rundeck.app.api.marshall.XmlAttribute

@ApiResource
class Resources {
    @Ignore(onlyIfNull = true)
    @XmlAttribute
    Boolean empty
    @Ignore(onlyIfNull = true)
    String description

    @Ignore(onlyIfNull = true)
    String content

    @XmlAttribute
    Boolean writeable
    @XmlAttribute
    String href
}
