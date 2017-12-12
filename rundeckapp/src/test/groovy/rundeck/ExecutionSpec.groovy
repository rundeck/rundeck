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

package rundeck

import grails.test.mixin.Mock
import spock.lang.Specification

/**
 * Created by greg on 9/27/16.
 */
@Mock([ScheduledExecution, Workflow])
class ExecutionSpec extends Specification {
    def "with server uuid"() {
        given:
        def uuid1 = UUID.randomUUID().toString()
        def e1 = new Execution(serverNodeUUID: uuid1,
                               dateStarted: new Date(),
                               dateCompleted: new Date(),
                               failedNodeList: null,
                               succeededNodeList: null,
                               project: "test",
                               user: "user",
                               status: 'true'
        ).save(flush: true)
        when:
        def result = Execution.withServerNodeUUID(uuid1).list()
        then:
        e1 != null
        result == [e1]

    }

}
