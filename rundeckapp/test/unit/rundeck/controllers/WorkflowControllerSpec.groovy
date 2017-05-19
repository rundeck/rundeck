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
import spock.lang.Specification

/**
 * Created by greg on 2/16/16.
 */
@TestFor(WorkflowController)
@Mock([Workflow, CommandExec, JobExec])
class WorkflowControllerSpec extends Specification {
    def "modify commandexec type empty validation"() {
        given:
        Workflow wf = new Workflow(threadcount: 1, keepgoing: true)
        wf.commands = new ArrayList()
        def inputparams = [(fieldname): 'blah']
        wf.commands << new CommandExec(inputparams)

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

    def "test edit action copy"() {

        given:
        Workflow wf = new Workflow(threadcount: 1, keepgoing: true)
        JobExec item1 = new JobExec(jobName: 'blah', jobGroup: 'blee')
        CommandExec item2 = new CommandExec(adhocExecution: true, adhocRemoteString: 'echo something')
        CommandExec item3 = new CommandExec(adhocExecution: true, adhocFilepath: '/xy/z', argString: 'test what')
        wf.addToCommands(item1)
        wf.addToCommands(item2)
        wf.addToCommands(item3)
        def origlist = [item1, item2, item3]


        when:


        def result = controller._applyWFEditAction(wf, [action: 'copy', num: copyitem])

        then:
        result.error == null
        wf.commands.size() == origlist.size() + 1
        wf.commands.get(copyitem) == origlist[copyitem]
        wf.commands.get(copyitem + 1).toMap() == origlist[copyitem].toMap()

        //test undo
        result.undo != null
        result.undo.action == 'remove'
        result.undo.num == expectitem

        where:
        copyitem | expectitem
        0        | 1
        1        | 2
        2        | 3
    }

    def "test edit action copy undo"() {

        given:
        Workflow wf = new Workflow(threadcount: 1, keepgoing: true)
        JobExec item1 = new JobExec(jobName: 'blah', jobGroup: 'blee')
        CommandExec item2 = new CommandExec(adhocExecution: true, adhocRemoteString: 'echo something')
        CommandExec item3 = new CommandExec(adhocExecution: true, adhocFilepath: '/xy/z', argString: 'test what')
        wf.addToCommands(item1)
        wf.addToCommands(item2)
        wf.addToCommands(item3)
        def origlist = [item1, item2, item3]


        when:


        def result = controller._applyWFEditAction(wf, [action: 'copy', num: copyitem])
        def result2 = controller._applyWFEditAction(wf, result.undo)

        then:
        result.error == null
        wf.commands.size() == origlist.size()
        wf.commands.get(copyitem) == origlist[copyitem]
        (0..<origlist.size()).each {
            wf.commands.get(it).toMap() == origlist[it].toMap()
        }

        //test undo
        result2.undo != null
        result2.undo.action == 'insert'
        result2.undo.num == copyitem+1

        where:
        copyitem | _
        0        | _
        1        | _
        2        | _
    }
}
