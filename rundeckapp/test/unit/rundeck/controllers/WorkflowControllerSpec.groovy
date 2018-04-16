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

import com.dtolabs.rundeck.plugins.ServiceNameConstants
import com.dtolabs.rundeck.plugins.logging.LogFilterPlugin
import com.dtolabs.rundeck.plugins.util.DescriptionBuilder
import com.dtolabs.rundeck.server.plugins.DescribedPlugin
import grails.test.mixin.Mock
import grails.test.mixin.TestFor
import rundeck.CommandExec
import rundeck.JobExec
import rundeck.ScheduledExecution
import rundeck.Workflow
import rundeck.services.FrameworkService
import rundeck.services.PluginService
import spock.lang.Specification
import spock.lang.Unroll

/**
 * Created by greg on 2/16/16.
 */
@TestFor(WorkflowController)
@Mock([Workflow, CommandExec, JobExec, ScheduledExecution])
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

        controller.frameworkService = Mock(FrameworkService)
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

        controller.frameworkService = Mock(FrameworkService)
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

    @Unroll
    def "test edit action insertFilter"() {

        given:
        Workflow wf = new Workflow(threadcount: 1, keepgoing: true)
        JobExec item1 = new JobExec(jobName: 'blah', jobGroup: 'blee')
        CommandExec item2 = new CommandExec(adhocExecution: true, adhocRemoteString: 'echo something')
        CommandExec item3 = new CommandExec(adhocExecution: true, adhocFilepath: '/xy/z', argString: 'test what')
        wf.addToCommands(item1)
        wf.addToCommands(item2)
        wf.addToCommands(item3)
        item2.storePluginConfigForType(ServiceNameConstants.LogFilter, [[type: 'xyz', config: [:]]])
        item3.storePluginConfigForType(
                ServiceNameConstants.LogFilter,
                [[type: 'xyz', config: [:]], [type: 'xyz', config: [:]]]
        )


        controller.pluginService = Mock(PluginService)
        controller.frameworkService = Mock(FrameworkService)
        def description = DescriptionBuilder.builder().name(filtertype).build()
        when:


        def result = controller._applyWFEditAction(
                wf,
                [action: 'insertFilter', num: stepnum, index: index, filtertype: filtertype, config: config]
        )

        then:
        1 * controller.pluginService.getPluginDescriptor(filtertype, LogFilterPlugin) >>
                new DescribedPlugin(null, description, filtertype)
        1 * controller.frameworkService.validateDescription(description, '', config, _, _, _) >> [
                valid: true, desc: description, props: validconfig
        ]

        result.undo == [action: 'removeFilter', num: stepnum, index: expectindex]
        def pluginconf = wf.commands[stepnum].getPluginConfigForType(ServiceNameConstants.LogFilter)
        pluginconf[index ?: 0] == [type: filtertype, config: validconfig]


        where:
        stepnum | index | filtertype  | config | validconfig | expectindex
        0       | null  | 'test-type' | [:]    | [a: 'b']    | 0
        0       | 0     | 'test-type' | [:]    | [a: 'b']    | 0
        0       | -1    | 'test-type' | [:]    | [a: 'b']    | 0
        1       | 0     | 'test-type' | [:]    | [a: 'b']    | 0
        1       | 1     | 'test-type' | [:]    | [a: 'b']    | 1
        1       | -1    | 'test-type' | [:]    | [a: 'b']    | 1
        2       | 0     | 'test-type' | [:]    | [a: 'b']    | 0
        2       | 1     | 'test-type' | [:]    | [a: 'b']    | 1
        2       | 2     | 'test-type' | [:]    | [a: 'b']    | 2
        2       | -1    | 'test-type' | [:]    | [a: 'b']    | 2
    }

    @Unroll
    def "test edit action modifyFilter"() {

        given:
        Workflow wf = new Workflow(threadcount: 1, keepgoing: true)
        JobExec item1 = new JobExec(jobName: 'blah', jobGroup: 'blee')
        CommandExec item2 = new CommandExec(adhocExecution: true, adhocRemoteString: 'echo something')
        CommandExec item3 = new CommandExec(adhocExecution: true, adhocFilepath: '/xy/z', argString: 'test what')
        wf.addToCommands(item1)
        wf.addToCommands(item2)
        wf.addToCommands(item3)
        item2.storePluginConfigForType(ServiceNameConstants.LogFilter, [[type: 'xyz', config: [a: 'b']]])
        item3.storePluginConfigForType(
                ServiceNameConstants.LogFilter,
                [[type: 'cba', config: [b: 'c']], [type: 'twf', config: [c: 'd']]]
        )


        controller.pluginService = Mock(PluginService)
        controller.frameworkService = Mock(FrameworkService)
        def description = DescriptionBuilder.builder().name(filtertype).build()
        when:


        def result = controller._applyWFEditAction(
                wf,
                [action: 'modifyFilter', num: stepnum, index: index, filtertype: filtertype, config: config]
        )

        then:
        1 * controller.pluginService.getPluginDescriptor(filtertype, LogFilterPlugin) >>
                new DescribedPlugin(null, description, filtertype)
        1 * controller.frameworkService.validateDescription(description, '', config, _, _, _) >> [
                valid: true, desc: description, props: validconfig
        ]

        result.undo == [action: 'modifyFilter', num: stepnum, index: index, filtertype: origtype, config: origconfig]
        def pluginconf = wf.commands[stepnum].getPluginConfigForType(ServiceNameConstants.LogFilter)
        pluginconf[index] == [type: filtertype, config: validconfig]



        where:
        stepnum | index | filtertype  | config | validconfig | origtype | origconfig
        1       | 0     | 'test-type' | [:]    | [a: 'b']    | 'xyz'    | [a: 'b']
        2       | 0     | 'test-type' | [:]    | [a: 'b']    | 'cba'    | [b: 'c']
        2       | 1     | 'test-type' | [:]    | [a: 'b']    | 'twf'    | [c: 'd']
    }

    @Unroll
    def "test edit action removeFilter"() {

        given:
        Workflow wf = new Workflow(threadcount: 1, keepgoing: true)
        JobExec item1 = new JobExec(jobName: 'blah', jobGroup: 'blee')
        CommandExec item2 = new CommandExec(adhocExecution: true, adhocRemoteString: 'echo something')
        CommandExec item3 = new CommandExec(adhocExecution: true, adhocFilepath: '/xy/z', argString: 'test what')
        wf.addToCommands(item1)
        wf.addToCommands(item2)
        wf.addToCommands(item3)
        item2.storePluginConfigForType(ServiceNameConstants.LogFilter, [[type: 'xyz', config: [a: 'b']]])
        item3.storePluginConfigForType(
                ServiceNameConstants.LogFilter,
                [[type: 'cba', config: [b: 'c']], [type: 'twf', config: [c: 'd']]]
        )


        controller.pluginService = Mock(PluginService)
        controller.frameworkService = Mock(FrameworkService)
        def description = DescriptionBuilder.builder().name(filtertype).build()
        when:


        def result = controller._applyWFEditAction(
                wf,
                [action: 'removeFilter', num: stepnum, index: index]
        )

        then:

        result.undo == [action: 'insertFilter', num: stepnum, index: index, filtertype: filtertype, config: config]
        def pluginconf = wf.commands[stepnum].getPluginConfigForType(ServiceNameConstants.LogFilter)
        pluginconf.size() == expectsize



        where:
        stepnum | index | filtertype | config   | expectsize
        1       | 0     | 'xyz'      | [a: 'b'] | 0
        2       | 0     | 'cba'      | [b: 'c'] | 1
        2       | 1     | 'twf'      | [c: 'd'] | 1
    }

    def "test validate filter step"() {
        given:
        controller.pluginService = Mock(PluginService)
        controller.frameworkService = Mock(FrameworkService)
        params.newfiltertype = type
        params.pluginConfig = config
        when:
        def result = controller.validateStepFilter()
        then:
        response.json == [
                report: 'report',
                valid : true,
                saved : [
                        type  : type,
                        config: expectconfig

                ]
        ]
        1 * controller.pluginService.getPluginDescriptor(type, LogFilterPlugin) >>
                new DescribedPlugin(null, null, type)
        1 * controller.frameworkService.validateDescription(null, '', config, null, _, _) >> [
                valid: true, desc: null, props: expectconfig,
                report:'report'
        ]


        where:
        type  | config   | expectconfig
        'abc' | [a: 'b'] | [c: 'd']

    }

    def "test validate filter step not found"() {
        given:
        controller.pluginService = Mock(PluginService)
        controller.frameworkService = Mock(FrameworkService)
        params.newfiltertype = type
        params.pluginConfig = config
        when:
        def result = controller.validateStepFilter()
        then:
        response.json == [
                report: 'The LogFilter provider "' + type + '" was not found',
                valid : false
        ]
        1 * controller.pluginService.getPluginDescriptor(type, LogFilterPlugin) >>
                null
        0 * controller.frameworkService.validateDescription(null, '', config, null, _, _)


        where:
        type  | config   | expectconfig
        'abc' | [a: 'b'] | [c: 'd']

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


    def "insert jobexec using uuid"() {
        given:
        Workflow wf = new Workflow(threadcount: 1, keepgoing: true)
        wf.commands = new ArrayList()
        def inputparams = [jobName: 'blah', jobGroup: 'test/test']
        //wf.commands << new JobExec( inputparams)
        controller.frameworkService = Mock(FrameworkService)

        when:
        def result = controller._applyWFEditAction(
                wf, [action: 'insert', num: 0,
                     params: [jobName: jobName, jobGroup: jobGroup, jobProject: jobProject, uuid:jobUuid,
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
        jobName     |jobUuid    | jobGroup | jobProject | fieldname
        'blah'      |null       | 'ble/ble'| null       | null
        null        |null       | 'ble/ble'| null       | 'jobName'
        null        |'as'       | 'ble/ble'| null       | null
        'blah'      |null       | null     | null       | null
        'blah'      |null       | 'ble/ble'| 'proj'     | null

    }


    def "insert jobexec by uuid project validation"() {
        given:
        Workflow wf = new Workflow(threadcount: 1, keepgoing: true)
        wf.commands = new ArrayList()
        def inputparams = [jobName: 'blah', jobGroup: 'test/test']
        //wf.commands << new JobExec( inputparams)
        controller.frameworkService = Mock(FrameworkService)

        def se = new ScheduledExecution(
                uuid: jobUuid,
                jobName: 'test',
                project: jobproject,
                groupPath: '',
                doNodedispatch: true,
                filter:'name: ${option.nodes}',
                workflow: new Workflow(
                        keepgoing: true,
                        commands: [
                                new CommandExec([
                                        adhocRemoteString: 'test buddy',
                                        argString: '-delay 12 -monkey cheese -particle'
                                ])
                        ]
                )
        ).save()


        when:
        def result = controller._applyWFEditAction(
                wf, [action: 'insert', num: 0,
                     params: [uuid:jobUuid,
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
        jobUuid | jobproject   | fieldname
        'blah'  | 'proj'       | null
        'blah'  | 'projx'      | 'jobProject'

    }

}
