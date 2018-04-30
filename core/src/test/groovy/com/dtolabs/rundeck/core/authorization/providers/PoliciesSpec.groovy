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

package com.dtolabs.rundeck.core.authorization.providers

import com.dtolabs.rundeck.core.authorization.AuthorizationUtil
import spock.lang.Specification

/**
 * @author greg
 * @since 4/6/17
 */
class PoliciesSpec extends Specification {

    def "load dir missing context"() {
        when:
        def result = Policies.load(
                new File(
                        "src/test/resources/com/dtolabs/rundeck/core/authorization/impcontext"
                )
        );
        then:
        result.count() == 0
    }

    def "load dir with context"() {
        given:
        def context = AuthorizationUtil.projectContext('someproj')

        when:
        def result = Policies.load(
                new File(
                        "src/test/resources/com/dtolabs/rundeck/core/authorization/impcontext"
                ),
                context
        );
        then:
        result.count() == 1
    }
}
