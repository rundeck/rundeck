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

package com.dtolabs.rundeck.app.support

import grails.validation.Validateable

/**
 * Created by greg on 8/27/16.
 */
@Validateable
class PluginResourceReq {
    String service
    String name
    String path
    static constraints = {
        service(nullable: false, blank: false, matches: /^[-a-zA-Z0-9\.]+$/)
        name(nullable: false, blank: false, matches: /^[-a-zA-Z0-9\.]+$/)
        path(nullable: true, blank: true, matches: /^(((?!\.)[-a-zA-Z0-9+_\.]+)(\/((?!\.)[-a-zA-Z0-9+_\.]+))*)*$/)
    }
}
