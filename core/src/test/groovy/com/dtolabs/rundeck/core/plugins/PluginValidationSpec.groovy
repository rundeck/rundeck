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

package com.dtolabs.rundeck.core.plugins

import spock.lang.Specification

import static com.dtolabs.rundeck.core.plugins.PluginValidation.State.INCOMPATIBLE
import static com.dtolabs.rundeck.core.plugins.PluginValidation.State.INVALID
import static com.dtolabs.rundeck.core.plugins.PluginValidation.State.VALID

class PluginValidationSpec extends Specification {
    def "combine states #input and #with is #out"() {
        expect:
            input.or(with) == out

        where:
            input        | with         | out
            VALID        | VALID        | VALID
            VALID        | INVALID      | INVALID
            VALID        | INCOMPATIBLE | INCOMPATIBLE
            INVALID      | VALID        | INVALID
            INVALID      | INVALID      | INVALID
            INVALID      | INCOMPATIBLE | INVALID
            INCOMPATIBLE | VALID        | INCOMPATIBLE
            INCOMPATIBLE | INCOMPATIBLE | INCOMPATIBLE
            INCOMPATIBLE | INVALID      | INVALID
    }
}
