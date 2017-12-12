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

package com.dtolabs.rundeck.app.support

import spock.lang.Specification

/**
 * @author greg
 * @since 6/28/17
 */
class ProjectArchiveParamsSpec extends Specification {
    def "to archive options"() {
        given:
        def archiveParams = new ProjectArchiveParams(input)

        when:
        def result = archiveParams.toArchiveOptions()

        then:
        result != null

        where:

        input                       | _
        [exportAll       : 'true', exportJobs: 'true',
         exportExecutions: 'true',
         exportConfigs   : 'true',
         exportReadmes   : 'true',
         exportAcls      : 'true',] | _
        [exportAll       : 'true', exportJobs: 'true',
         exportExecutions: 'true',
         exportConfigs   : 'true',
         exportReadmes   : 'true',
         ] | _
    }
}
