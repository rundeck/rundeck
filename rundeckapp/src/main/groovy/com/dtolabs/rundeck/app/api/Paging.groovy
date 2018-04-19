/*
 * Copyright 2017 Rundeck, Inc. (http://rundeck.com)
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

package com.dtolabs.rundeck.app.api

import com.dtolabs.rundeck.app.api.marshall.ApiResource
import com.dtolabs.rundeck.app.api.marshall.XmlAttribute

/**
 * @author greg
 * @since 2/24/17
 */
@ApiResource
class Paging {
    @XmlAttribute
    Integer count
    @XmlAttribute
    Integer max
    @XmlAttribute
    Integer offset
    @XmlAttribute
    Integer total

    static Paging fromMap(Map map) {
        new Paging(count: map.count, max: map.max, offset: map.offset, total: map.total)
    }
}
