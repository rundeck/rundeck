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

package rundeck.controllers

import grails.test.mixin.Mock
import grails.test.mixin.TestFor
import rundeck.CommandExec
import rundeck.JobExec
import rundeck.Workflow
import rundeck.services.FrameworkService
import spock.lang.Specification

/**
 * Created by greg on 2/16/16.
 */
@TestFor(WorkflowController)
@Mock([Workflow, CommandExec])
class WorkflowControllerSpec extends Specification {
    def "modify commandexec type empty validation"() {
        given:
        Workflow wf = new Workflow(threadcount: 1, keepgoing: true)
        wf.commands = new ArrayList()
        def inputparams = [(fieldname): 'blah']
        wf.commands << new CommandExec(inputparams)
        controller.frameworkService = Mock(FrameworkService)

        when:
        def result = controller._applyWFEditAction(
                wf,
                [action: 'modify', num: 0, params: [origitemtype: itemtype, (fieldname): '']]
        )

        then:
        result.error
        result.item
        result.item.errors.hasFieldErrors(fieldname)

        where:
        itemtype     | fieldname
        'command'    | 'adhocRemoteString'
        'script'     | 'adhocLocalString'
        'scriptfile' | 'adhocFilepath'

    }

    def "modifyHandler commandexec type empty validation"() {
        given:
        Workflow wf = new Workflow(threadcount: 1, keepgoing: true)
        wf.commands = new ArrayList()
        controller.frameworkService = Mock(FrameworkService)

        def inputparams = [(fieldname): 'blah']
        def cmd = new CommandExec(inputparams)
        cmd.errorHandler = new CommandExec(inputparams)
        wf.commands << cmd

        when:
        def result = controller._applyWFEditAction(
                wf,
                [action: 'modifyHandler', num: 0, params: [origitemtype: itemtype, (fieldname): '']]
        )

        then:
        result.error
        result.item
        result.item.errors.hasFieldErrors(fieldname)

        where:
        itemtype     | fieldname
        'command'    | 'adhocRemoteString'
        'script'     | 'adhocLocalString'
        'scriptfile' | 'adhocFilepath'

    }

    def "insert jobexec type validation"() {
        given:
        Workflow wf = new Workflow(threadcount: 1, keepgoing: true)
        wf.commands = new ArrayList()
        def inputparams = [jobName: 'blah', jobGroup: 'test/test']
        //wf.commands << new JobExec( inputparams)
        controller.frameworkService = Mock(FrameworkService)

        when:
        def result = controller._applyWFEditAction(
                wf, [action: 'insert', num: 0,
                params: [jobName: jobName, jobGroup: jobGroup,description:'desc1',newitemtype: 'job']]
        )

        then:
        //assertEquals(expectedError,result.error)
        if(!fieldname){
            assertNull(result.error)
        }else{
            assertNotNull(result.error)
            result.item.errors.hasFieldErrors(fieldname)
        }



        where:
        jobName     | jobGroup | jobProject | fieldname
        'blah'      | 'ble/ble'| null       | null
        null        | 'ble/ble'| null       | 'jobName'
        'blah'      | null     | null       | null
        'blah'      | 'ble/ble'| 'proj'     | null

    }

    def "insert jobexec project validation"() {
        given:
        Workflow wf = new Workflow(threadcount: 1, keepgoing: true)
        wf.commands = new ArrayList()
        def inputparams = [jobName: 'blah', jobGroup: 'test/test']
        //wf.commands << new JobExec( inputparams)
        controller.frameworkService = Mock(FrameworkService)

        when:
        def result = controller._applyWFEditAction(
                wf, [action: 'insert', num: 0,
                     params: [jobName: jobName, jobGroup: jobGroup, jobProject: jobProject,
                              description:'desc1',newitemtype: 'job']]
        )

        then:
        //assertEquals(expectedError,result.error)
        1 * controller.frameworkService.projectNames(_) >> ['proj','proj2']

        if(!fieldname){
            assertNull(result.error)
        }else{
            assertNotNull(result.error)
            result.item.errors.hasFieldErrors(fieldname)
        }



        where:
        jobName     | jobGroup | jobProject | fieldname
        'blah'      | 'ble/ble'| null       | null
        null        | 'ble/ble'| null       | 'jobName'
        'blah'      | null     | null       | null
        'blah'      | 'ble/ble'| 'proj'     | null
        'blah'      | null     | 'projx'    | 'jobProject'

    }

}
