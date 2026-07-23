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

package org.rundeck.plugin.scm.git.config

import spock.lang.Specification
import spock.lang.Unroll

class CommonSpec extends Specification {

    @Unroll
    def "getFetchTimeoutSeconds - configured: #configured -> #expected"() {
        given:
        def common = new Common()
        common.fetchTimeout = configured

        expect:
        common.getFetchTimeoutSeconds() == expected

        where:
        configured | expected
        null       | 30
        ''         | 30
        '30'       | 30
        '10'       | 10
        '90'       | 90
        'notanumber' | 30
    }
}
