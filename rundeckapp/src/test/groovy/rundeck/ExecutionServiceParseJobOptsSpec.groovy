package rundeck
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

import grails.testing.gorm.DataTest
import grails.testing.services.ServiceUnitTest
import grails.testing.spring.AutowiredTest
import rundeck.services.ExecutionService
import rundeck.services.JobLifecycleComponentService
import spock.lang.Specification
import spock.lang.Unroll

/**
 * Created by greg on 2/17/15.
 */
class ExecutionServiceParseJobOptsSpec extends Specification implements ServiceUnitTest<ExecutionService>, DataTest, AutowiredTest {

    Class[] getDomainClassesToMock() {
        [Execution, User, ScheduledExecution, Workflow, CommandExec, Option, ExecReport, LogFileStorageRequest, ReferencedExecution, ScheduledExecutionStats]
    }

    def setup(){
        service.jobLifecycleComponentService = Mock(JobLifecycleComponentService)
    }

    @Unroll
    def "parse job opts from string multivalue"() {
        given:
        ScheduledExecution se = new ScheduledExecution()
        se.addToOptions(new Option(name: 'opt1', enforced: false, multivalued: true, delimiter: ','))
        final opt2 = new Option(name: 'opt2', enforced: true, multivalued: true, delimiter: ' ')
        opt2.delimiter = ' '
        opt2.valuesList = 'a,b,abc'
        se.addToOptions(opt2)


        when:
        def result = service.parseJobOptsFromString(se, argString)

        then:
        result == expected

        where:
        argString                | expected
        '-opt1 test'             | [opt1: ['test']]
        '-opt1 test,x'           | [opt1: ['test', 'x']]
        '-opt1 \'test x\''       | [opt1: ['test x']]
        '-opt2 a'                | [opt2: ['a']]
        '-opt2 a,b'              | [opt2: ['a,b']]
        '-opt2 \'blah zah nah\'' | [opt2: ['blah', 'zah', 'nah']]
    }
}
