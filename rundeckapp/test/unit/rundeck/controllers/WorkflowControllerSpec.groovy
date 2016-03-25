package rundeck.controllers

import grails.test.mixin.Mock
import grails.test.mixin.TestFor
import rundeck.CommandExec
import rundeck.Workflow
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

}
