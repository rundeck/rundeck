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

import grails.testing.gorm.DataTest
import grails.validation.ValidationException
import spock.lang.Specification

/**
 * Created by greg on 9/27/16.
 */
class ExecutionSpec extends Specification implements DataTest {

    def setupSpec() { mockDomains Execution, ScheduledExecution, Workflow, LogFileStorageRequest, Orchestrator }

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


    def "unique log file storage request"() {
        given:
            def uuid1 = UUID.randomUUID().toString()
            def e1 = new Execution(
                serverNodeUUID: uuid1,
                dateStarted: new Date(),
                dateCompleted: new Date(),
                failedNodeList: null,
                succeededNodeList: null,
                project: "test",
                user: "user",
                status: 'true'
            ).save(flush: true,
                   failOnError: true)
            def lfsr1 = new LogFileStorageRequest(
                execution: e1,
                pluginName: 'aplugin',
                completed: false,
                filetype: 'test'
            ).save(flush:true,
                   failOnError: true)
        when:
            def lfsr2 = new LogFileStorageRequest(
                execution: e1,
                pluginName: 'aplugin',
                completed: true,
                filetype: 'xyz'
            ).save(flush:true,
                   failOnError: true)

        then:
            ValidationException e = thrown()

            e.errors.hasFieldErrors('execution')
            e.errors.getFieldError('execution').code=='unique'

    }

    def "the execid for logstore will be the same as the Id if the output filepath is not external or take it from the outputfilepath"() {
        given:
        def e1 = new Execution(
                serverNodeUUID: 'uuid-example',
                dateStarted: new Date(),
                dateCompleted: new Date(),
                failedNodeList: null,
                succeededNodeList: null,
                project: "test",
                user: "user",
                status: 'true'
        )
        e1.outputfilepath = outputFilePath
        e1.id = execId

        when:
        Long returnedExecIdForStorage = e1.getExecIdForLogStore()

        then:
        returnedExecIdForStorage == expectedIdForLogStorage

        where:
        execId | outputFilePath            | expectedIdForLogStorage
        5      | 'ext:11:path/to/file.log' | 11
        5      | 'path/to/file.log'        | 5

    }

    def "should return true if the outputfilepath starts with \"ext:\""(){
        given:
        def e1 = new Execution(
                serverNodeUUID: 'uuid-example',
                dateStarted: new Date(),
                dateCompleted: new Date(),
                failedNodeList: null,
                succeededNodeList: null,
                project: "test",
                user: "user",
                status: 'true'
        )
        e1.outputfilepath = outputFilePath
        e1.id = 1

        when:
        boolean isRemote = e1.isRemoteOutputfilepath()

        then:
        isRemote == exepected

        where:
        outputFilePath            | exepected
        'ext:11:path/to/file.log' | true
        'ext-11:path/to/file.log' | false
        'path/to/file.log'        | false
    }

    def "should return the outputfilepath without any internal marking like \"ext:\""(){
        given:
        def e1 = new Execution(
                serverNodeUUID: 'uuid-example',
                dateStarted: new Date(),
                dateCompleted: new Date(),
                failedNodeList: null,
                succeededNodeList: null,
                project: "test",
                user: "user",
                status: 'true'
        )
        e1.outputfilepath = outputFilePath
        e1.id = 1

        when:
        String filePath = e1.getOutputfilepath()

        then:
        filePath == exepected

        where:
        outputFilePath               | exepected
        'ext:11:path/to/file.log'    | 'path/to/file.log'
        'path2/to2/file2.log'        | 'path2/to2/file2.log'
        'ext:15:path2/to2/file2.log' | 'path2/to2/file2.log'
    }

}
