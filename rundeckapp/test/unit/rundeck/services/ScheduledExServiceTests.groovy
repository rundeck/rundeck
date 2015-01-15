

package rundeck.services

import grails.test.mixin.TestMixin
import grails.test.runtime.DirtiesRuntime
import org.codehaus.groovy.grails.plugins.databinding.DataBindingGrailsPlugin;


import grails.test.mixin.Mock
import grails.test.mixin.TestFor
import grails.test.mixin.web.ControllerUnitTestMixin;

import org.junit.Assert
import org.junit.Before;
import org.quartz.*
import org.quartz.impl.matchers.GroupMatcher
import org.quartz.spi.JobFactory
import org.springframework.context.MessageSource

import rundeck.*
import rundeck.controllers.ScheduledExecutionController

/*
 * Copyright 2011 DTO Solutions, Inc. (http://dtosolutions.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
 
/*
 * ScheduledExecutionServiceTests.java
 * 
 * User: Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
 * Created: 6/22/11 5:55 PM
 * 
 */
@TestFor(ScheduledExecutionService)
@Mock([Execution, FrameworkService, WorkflowStep, CommandExec, JobExec, PluginStep, Workflow, ScheduledExecution, Option, Notification])
@TestMixin(ControllerUnitTestMixin)
class ScheduledExServiceTests {
    
    


    static void assertLength(int length, Object[] array){
        Assert.assertEquals(length,array.length)
    }
    /**
     * Test getByIDorUUID method.
     */
    @DirtiesRuntime
    public void testGetByIDorUUID() {
        def testService = new ScheduledExecutionService()
        def myuuid='testUUID'//'89F375E0-7096-4490-8265-4F94793BEC2F'
        ScheduledExecution se = new ScheduledExecution(
                uuid: myuuid,
                jobName: 'blue',
                project: 'AProject',
                groupPath: 'some/where',
                description: 'a job',
                argString: '-a b -c d',
                workflow: new Workflow(keepgoing: true, commands: [new CommandExec([adhocRemoteString: 'test buddy', argString: '-delay 12 -monkey cheese -particle'])]),
        )
        if (!se.validate()) {
            se.errors.allErrors.each {
                println it.toString()
            }
        }
        assertNotNull(se.save())
        Long id = se.id

        ScheduledExecution.metaClass.static.findByUuid = { uuid -> uuid == myuuid ? se : null }

        def result = testService.getByIDorUUID(myuuid)
        assertNotNull(result)
        assertEquals(se, result)

        result = testService.getByIDorUUID('testblah')
        assertNull(result)

        def result2 = testService.getByIDorUUID(id)
        assertNotNull(result2)
        assertEquals(se, result2)

        def result3 = testService.getByIDorUUID(id.toString())
        assertNotNull(result3)
        assertEquals(se, result3)
    }

    /**
     * test overlap between internal ID and UUID values, the ID should take precedence (return first)
     */
    @DirtiesRuntime
    public void testGetByIDorUUIDWithOverlap() {

        def testService = new ScheduledExecutionService()
        ScheduledExecution se = new ScheduledExecution(
                uuid: 'testUUID',
                jobName: 'blue',
                project: 'AProject',
                adhocExecution: true,
                adhocFilepath: '/this/is/a/path',
                groupPath: 'some/where',
                description: 'a job',
                argString: '-a b -c d',
                workflow: new Workflow(keepgoing: true, commands: [new CommandExec([adhocRemoteString: 'test buddy', argString: '-delay 12 -monkey cheese -particle'])]),
        )
        se.save()
        long id = se.id
        String idstr = id.toString()

        ScheduledExecution se2 = new ScheduledExecution(
                uuid: idstr,
                jobName: 'blue',
                project: 'AProject',
                adhocExecution: true,
                adhocFilepath: '/this/is/a/path',
                groupPath: 'some/where',
                description: 'a job',
                argString: '-a b -c d',
                workflow: new Workflow(keepgoing: true, commands: [new CommandExec([adhocRemoteString: 'test buddy', argString: '-delay 12 -monkey cheese -particle'])]),
        )
        se2.save()
        long id2 = se2.id

        ScheduledExecution.metaClass.static.findByUuid = { uuid -> uuid == 'testUUID' ? se : uuid == idstr ? se2 : null }
        assertEquals(se, ScheduledExecution.findByUuid('testUUID'))

        def result = testService.getByIDorUUID(id)
        assertNotNull(result)
        assertEquals(se, result)

        //result should be se 1 because ID has precedence
        result = testService.getByIDorUUID(idstr)
        assertNotNull(result)
        assertEquals(se, result)

        //test with se2 uuid directly, should return se1
        result = testService.getByIDorUUID(se2.uuid)
        assertNotNull(result)
        assertEquals(se, result)

        //test se2 id
        result = testService.getByIDorUUID(id2.toString())
        assertNotNull(result)
        assertEquals(se2, result)
    }

    public void testDoValidateEmptyInput() {

            def fwkControl = mockFor(FrameworkService, true)
            fwkControl.demand.getFrameworkFromUserSession { session, request -> return null }
            fwkControl.demand.existsFrameworkProject { project, framework -> return false }
            service.frameworkService = fwkControl.createMock()

            def params = [:]
            def results = service._dovalidate(params, 'test', 'test', null)
            if (results.scheduledExecution.errors.hasErrors()) {
                results.scheduledExecution.errors.allErrors.each {
                    System.err.println(it);
                }
            }
            assertTrue(results.failed)
            assertNotNull(results.scheduledExecution)
            assertTrue(results.scheduledExecution instanceof ScheduledExecution)
            final ScheduledExecution execution = results.scheduledExecution
            assertNotNull(execution)
            assertNotNull(execution.errors)
            assertTrue(execution.errors.hasErrors())
            assertTrue(execution.errors.hasFieldErrors('project'))
            assertTrue(execution.errors.hasFieldErrors('workflow'))
            assertFalse(!!execution.jobName)
            assertTrue(execution.errors.hasFieldErrors('jobName'))
            assertFalse(execution.errors.hasFieldErrors('name'))
            assertFalse(execution.errors.hasFieldErrors('type'))
            assertFalse(execution.errors.hasFieldErrors('command'))
        }

    public void testDoValidateBasic() {
            def fwkControl = mockFor(FrameworkService, true)
            fwkControl.demand.getFrameworkFromUserSession { session, request -> return null }
            fwkControl.demand.existsFrameworkProject { project, framework ->
                assertEquals 'testProject', project
                return true
            }
            fwkControl.demand.getCommand { project, type, command, framework ->
                assertEquals 'testProject', project
                assertEquals 'aType', type
                assertEquals 'aCommand', command
                return null
            }
        service.frameworkService = fwkControl.createMock()

            def params = [jobName: 'monkey1', project: 'testProject', description: 'blah',
                    workflow: [threadcount: 1, keepgoing: true, "commands[0]": [adhocExecution: true, adhocRemoteString: 'a command']]
            ]
            def results = service._dovalidate(params, 'test', 'test', null)
            assertFalse(results.failed)
            assertNotNull(results.scheduledExecution)
            assertTrue(results.scheduledExecution instanceof ScheduledExecution)
            final ScheduledExecution execution = results.scheduledExecution
            assertNotNull(execution)
            assertNotNull(execution.errors)
            assertFalse(execution.errors.hasErrors())
            assertFalse(execution.errors.hasFieldErrors())
            assertFalse(execution.errors.hasFieldErrors('project'))
            assertFalse(execution.errors.hasFieldErrors('description'))
            assertFalse(execution.errors.hasFieldErrors('jobName'))
            assertEquals 'monkey1', execution.jobName
            assertEquals 'testProject', execution.project
            assertEquals 'blah', execution.description
            assertNotNull execution.workflow
            assertEquals 1, execution.workflow.commands.size()
            final CommandExec exec = execution.workflow.commands[0]
            assertEquals 'a command', exec.adhocRemoteString
            assertTrue exec.adhocExecution
        }

    public void testDoValidateInvalidJobName() {
            def fwkControl = mockFor(FrameworkService, true)
            fwkControl.demand.getFrameworkFromUserSession { session, request -> return null }
            fwkControl.demand.existsFrameworkProject { project, framework ->
                assertEquals 'testProject', project
                return true
            }
            fwkControl.demand.getCommand { project, type, command, framework ->
                assertEquals 'testProject', project
                assertEquals 'aType', type
                assertEquals 'aCommand', command
                return null
            }
        service.frameworkService = fwkControl.createMock()

            def params = [jobName: 'test/monkey', project: 'testProject', description: 'blah', adhocExecution: true, adhocRemoteString: 'a command']
            def results = service._dovalidate(params, 'test', 'test', null)
            assertTrue(results.failed)
            def sce = results.scheduledExecution

            assertTrue sce.errors.hasErrors()
            assertTrue sce.errors.hasFieldErrors('jobName')

    }
    /**
     * input params nodeInclude/nodeExclude* should be converted into the filter string
     */
    public void testDoValidateNodeDispatchOldFilterAsString() {
            def fwkControl = mockFor(FrameworkService, true)
            fwkControl.demand.getFrameworkFromUserSession { session, request -> return null }
            fwkControl.demand.existsFrameworkProject { project, framework ->
                assertEquals 'testProject', project
                return true
            }
            service.frameworkService = fwkControl.createMock()

        def params = [jobName: 'monkey1', project: 'testProject', description: 'blah',
                workflow: [threadcount: 1, keepgoing: true, "commands[0]": [adhocExecution: true,
                        adhocRemoteString: 'a command']],
                doNodedispatch: true,
                    nodeIncludeName: "bongo",
            nodeExcludeOsFamily: "windows",
            nodeIncludeTags: "spaghetti"]
            def results = service._dovalidate(params, 'test', 'test', null)
            assertFalse(results.failed)
            def sce = results.scheduledExecution

            assertFalse sce.errors.hasErrors()
            assertNotNull sce.filter
            assertEquals "name: bongo tags: spaghetti !os-family: windows",sce.filter
    }

    public void testDoValidateNodedispatchIsBlank() {
        def testService = new ScheduledExecutionService()

        //test nodedispatch true, threadcount should default to 1 if input is blank
        def fwkControl = mockFor(FrameworkService, true)
        fwkControl.demand.getFrameworkFromUserSession { session, request -> return null }
        fwkControl.demand.existsFrameworkProject { project, framework ->
            assertEquals 'testProject', project
            return true
        }
        fwkControl.demand.getCommand { project, type, command, framework ->
            assertEquals 'testProject', project
            assertEquals 'aType', type
            assertEquals 'aCommand', command
            return null
        }
        testService.frameworkService = fwkControl.createMock()

        def params = [jobName: 'monkey1', project: 'testProject', description: 'blah',
                workflow: [threadcount: 1, keepgoing: true, "commands[0]": [adhocExecution: true, adhocRemoteString: 'a remote string']],
                doNodedispatch: 'true', nodeInclude: 'blah', nodeThreadcount: ""]
        def results = testService._dovalidate(params, 'test', 'test', null)
        assertFalse(results.failed)
        assertNotNull(results.scheduledExecution)
        assertTrue(results.scheduledExecution instanceof ScheduledExecution)
        final ScheduledExecution execution = results.scheduledExecution
        assertNotNull(execution)
        assertNotNull(execution.errors)
        assertFalse(execution.errors.hasErrors())
        assertFalse(execution.errors.hasFieldErrors())
        assertTrue(execution.doNodedispatch)
        assertNotNull(execution.nodeThreadcount)
        assertEquals(1, execution.nodeThreadcount)
    }

    public void testDoValidateWorkflow() {
        def testService = new ScheduledExecutionService()
        if (true) {//test job with options
            def fwkControl = mockFor(FrameworkService, true)
            fwkControl.demand.getFrameworkFromUserSession { session, request -> return null }
            fwkControl.demand.existsFrameworkProject { project, framework ->
                assertEquals 'testProject', project
                return true
            }
            testService.frameworkService = fwkControl.createMock()

            def params = [jobName: 'monkey1', project: 'testProject', description: 'blah',
                    workflow: [threadcount: 1, keepgoing: true, "commands[0]": [adhocExecution: true, adhocRemoteString: 'a remote string']]
            ]
            def results = testService._dovalidate(params, 'test', 'test', null)
            if (results.scheduledExecution.errors.hasErrors()) {
                results.scheduledExecution.errors.allErrors.each {
                    System.err.println(it);
                }
            }
            assertFalse results.failed
            def ScheduledExecution scheduledExecution = results.scheduledExecution
            assertNotNull(scheduledExecution)
            assertTrue(scheduledExecution instanceof ScheduledExecution)
            final ScheduledExecution execution = scheduledExecution
            assertNotNull(execution)
            assertNotNull(execution.errors)
            assertFalse(execution.errors.hasErrors())
            final Workflow wf = execution.workflow
            assertNotNull(wf)
            assertEquals(1, wf.commands.size())
            final Iterator iterator = wf.commands.iterator()
            assert iterator.hasNext()
            final CommandExec next = iterator.next()
            assertNotNull(next)
            assertFalse(next instanceof JobExec)
            assertEquals('a remote string', next.adhocRemoteString)
        }
        if (true) {//test job with options
            def fwkControl = mockFor(FrameworkService, true)
            fwkControl.demand.getFrameworkFromUserSession { session, request -> return null }
            fwkControl.demand.existsFrameworkProject { project, framework ->
                assertEquals 'testProject', project
                return true
            }
            testService.frameworkService = fwkControl.createMock()

            def params = [jobName: 'monkey1', project: 'testProject', description: 'blah', adhocExecution: false, type: '', name: '', command: '',
                    workflow: [threadcount: 1, keepgoing: true,
                            "commands[0]": [adhocExecution: true, adhocRemoteString: "do something"],
                            "commands[1]": [adhocExecution: true, adhocLocalString: "test dodah"],
                            "commands[2]": [jobName: 'test1', jobGroup: 'a/test'],
                    ]
            ]
            def results = testService._dovalidate(params, 'test', 'test', null)
            if (results.scheduledExecution.errors.hasErrors()) {
                results.scheduledExecution.errors.allErrors.each {
                    System.err.println(it);
                }
            }
            assertFalse results.failed
            def ScheduledExecution scheduledExecution = results.scheduledExecution
            assertNotNull(scheduledExecution)
            assertTrue(scheduledExecution instanceof ScheduledExecution)
            final ScheduledExecution execution = scheduledExecution
            assertNotNull(execution)
            assertNotNull(execution.errors)
            assertFalse(execution.errors.hasErrors())
            final Workflow wf = execution.workflow
            assertNotNull(wf)
            assertLength(3, wf.commands as Object[])
            final Iterator iterator = wf.commands.iterator()
            assert iterator.hasNext()
            final CommandExec next = iterator.next()
            assertNotNull(next)
            assertFalse(next instanceof JobExec)
            assertEquals('do something', next.adhocRemoteString)
            final CommandExec next2 = iterator.next()
            assertNotNull(next2)
            assertFalse(next2 instanceof JobExec)
            assertEquals('test dodah', next2.adhocLocalString)
            assertTrue('adhocExecution', next2.adhocExecution)
            final JobExec next3 = iterator.next()
            assertNotNull(next3)
            assertTrue(next3 instanceof JobExec)
            assertEquals('test1', next3.jobName)
            assertEquals('a/test', next3.jobGroup)
        }
    }

    public void testDoValidateWorkflowStepFirstErrorhandlers() {
        def testService = new ScheduledExecutionService()
        def fwkControl = mockFor(FrameworkService, true)
        fwkControl.demand.getFrameworkFromUserSession { session, request -> return null }
        fwkControl.demand.existsFrameworkProject { project, framework ->
            assertEquals 'testProject', project
            return true
        }
        testService.frameworkService = fwkControl.createMock()

        //step first allows any combination of step and error handler types
        def params = [jobName: 'monkey1', project: 'testProject', description: 'blah',
                workflow: new Workflow(keepgoing: true, strategy: 'step-first', commands: [
                        new CommandExec(adhocRemoteString: 'test command1', adhocExecution: true, errorHandler:
                                new JobExec(jobGroup: 'test1', jobName: 'blah')),
                        new CommandExec(adhocRemoteString: 'test command2', adhocExecution: true, errorHandler:
                                new CommandExec(adhocRemoteString: 'test ehcommand', adhocExecution: true)),
                        new JobExec(jobGroup: 'test1', jobName: 'blah2', errorHandler:
                                new CommandExec(adhocRemoteString: 'test command1', adhocExecution: true)),
                        new JobExec(jobGroup: 'test1', jobName: 'blah3', errorHandler:
                                new JobExec(jobGroup: 'test1', jobName: 'blah4')),
                ])
        ]
        def results = testService._dovalidate(params, 'test', 'test', null)
        if (results.scheduledExecution.errors.hasErrors()) {
            results.scheduledExecution.errors.allErrors.each {
                System.err.println(it);
            }
        }
        assertFalse results.failed

        assertFalse results.scheduledExecution.workflow.commands[0].errors.hasErrors()
        assertFalse results.scheduledExecution.workflow.commands[1].errors.hasErrors()
        assertFalse results.scheduledExecution.workflow.commands[2].errors.hasErrors()
        assertFalse results.scheduledExecution.workflow.commands[3].errors.hasErrors()
    }

    public void testDoValidateWorkflowNodeFirstErrorhandlers() {
        def testService = new ScheduledExecutionService()
        def fwkControl = mockFor(FrameworkService, true)
        fwkControl.demand.getFrameworkFromUserSession { session, request -> return null }
        fwkControl.demand.existsFrameworkProject { project, framework ->
            assertEquals 'testProject', project
            return true
        }
        testService.frameworkService = fwkControl.createMock()

        //Node first rejects non-Node error handler steps for Node steps.
        def params = [jobName: 'monkey1', project: 'testProject', description: 'blah',
                workflow: new Workflow(keepgoing: true, strategy: 'node-first', commands: [
                        new CommandExec(adhocRemoteString: 'test command1', adhocExecution: true, errorHandler:
                                new JobExec(jobGroup: 'test1', jobName: 'blah')),
                        new CommandExec(adhocRemoteString: 'test command2', adhocExecution: true, errorHandler:
                                new CommandExec(adhocRemoteString: 'test ehcommand', adhocExecution: true)),
                        new JobExec(jobGroup: 'test1', jobName: 'blah2', errorHandler:
                                new CommandExec(adhocRemoteString: 'test command1', adhocExecution: true)),
                        new JobExec(jobGroup: 'test1', jobName: 'blah3', errorHandler:
                                new JobExec(jobGroup: 'test1', jobName: 'blah4')),
                ])
        ]
        def results = testService._dovalidate(params, 'test', 'test', null)
        results.scheduledExecution.workflow.commands.each { cmd ->
            if (cmd.errors.hasErrors()) {
                cmd.errors.allErrors.each {
                    System.out.println("command: " + cmd + ", error: " + it);
                }
            }
        }
        assertTrue results.failed
        assertTrue results.scheduledExecution.workflow.commands[0].errors.hasErrors()
        assertTrue results.scheduledExecution.workflow.commands[0].errors.hasFieldErrors('errorHandler')

        //no error in other commands
        assertFalse results.scheduledExecution.workflow.commands[1].errors.hasErrors()
        assertFalse results.scheduledExecution.workflow.commands[2].errors.hasErrors()
        assertFalse results.scheduledExecution.workflow.commands[3].errors.hasErrors()

    }

    public void testDoValidateWorkflowOptions() {
        def testService = new ScheduledExecutionService()

        if (true) {//test job with options
            def fwkControl = mockFor(FrameworkService, true)
            fwkControl.demand.getFrameworkFromUserSession { session, request -> return null }
            fwkControl.demand.existsFrameworkProject { project, framework ->
                assertEquals 'testProject', project
                return true
            }
            testService.frameworkService = fwkControl.createMock()

            def params = [jobName: 'monkey1', project: 'testProject', description: 'blah', adhocExecution: false, type: '', name: '', command: '',
                    workflow: [threadcount: 1, keepgoing: true,
                            "commands[0]": [adhocExecution: true, adhocRemoteString: "do something"],
                            "commands[1]": [adhocExecution: true, adhocLocalString: "test dodah"],
                            "commands[2]": [jobName: 'test1', jobGroup: 'a/test'],
                    ],
                    options: ["options[0]": [name: 'test3', defaultValue: 'val3', enforced: false, valuesUrl: "http://test.com/test3"]]
            ]
            def results = testService._dovalidate(params, 'test', 'test', null)
            if (results.scheduledExecution.errors.hasErrors()) {
                results.scheduledExecution.errors.allErrors.each {
                    System.err.println(it);
                }
            }
            assertFalse results.failed
            def ScheduledExecution scheduledExecution = results.scheduledExecution
            assertNotNull(scheduledExecution)
            assertTrue(scheduledExecution instanceof ScheduledExecution)
            final ScheduledExecution execution = scheduledExecution
            assertNotNull(execution)
            assertNotNull(execution.errors)
            assertFalse(execution.errors.hasErrors())
            final Workflow wf = execution.workflow
            assertNotNull(wf)
            assertEquals(3, wf.commands.size())
            final Iterator iterator = wf.commands.iterator()
            assert iterator.hasNext()
            final CommandExec next = iterator.next()
            assertNotNull(next)
            assertFalse(next instanceof JobExec)
            assertEquals('do something', next.adhocRemoteString)
            final CommandExec next2 = iterator.next()
            assertNotNull(next2)
            assertFalse(next2 instanceof JobExec)
            assertEquals('test dodah', next2.adhocLocalString)
            assertTrue('adhocExecution', next2.adhocExecution)
            final JobExec next3 = iterator.next()
            assertNotNull(next3)
            assertTrue(next3 instanceof JobExec)
            assertEquals('test1', next3.jobName)
            assertEquals('a/test', next3.jobGroup)

            //check options
            assertNotNull execution.options
            assertLength(1, execution.options as Object[])
            final Iterator it2 = execution.options.iterator()
            assert it2.hasNext()
            final Option opt1 = it2.next()
            assertNotNull(opt1)
            assertEquals("wrong option name", "test3", opt1.name)
            assertEquals("wrong option name", "val3", opt1.defaultValue)
            assertNotNull("wrong option name", opt1.realValuesUrl)
            assertEquals("wrong option name", "http://test.com/test3", opt1.realValuesUrl.toExternalForm())
            assertFalse("wrong option name", opt1.enforced)
        }
    }

    public void testDoValidateScheduledWithNotifications() {
        def testService = new ScheduledExecutionService()
        def fwkControl = mockFor(FrameworkService, true)
        fwkControl.demand.getFrameworkFromUserSession { session, request -> return null }
        fwkControl.demand.existsFrameworkProject { project, framework ->
            assertEquals 'testProject', project
            return true
        }
        fwkControl.demand.isClusterModeEnabled {->
            return false
        }
        testService.frameworkService = fwkControl.createMock()

        def crontabString = '0 0 2 ? 12 1975'
        def params = [jobName: 'monkey1', project: 'testProject', description: 'blah',
                scheduled: true, crontabString: crontabString, useCrontabString: 'true',
                workflow: new Workflow(commands: [new CommandExec(adhocRemoteString: 'asdf')]),
                notifications: [[eventTrigger: ScheduledExecutionController.ONSUCCESS_TRIGGER_NAME, type: 'email', content: 'c@example.com,d@example.com']]
        ]
        def results = testService._dovalidate(params, 'test', 'test', null)

        assertTrue(results.failed)
        assertNotNull(results.scheduledExecution)
        assertTrue(results.scheduledExecution instanceof ScheduledExecution)
        final ScheduledExecution execution = results.scheduledExecution
        assertNotNull(execution)
        assertNotNull(execution.errors)

        assertTrue(execution.errors.hasErrors())
        assertTrue(execution.errors.hasFieldErrors('crontabString'))
        assertNotNull execution.workflow
        assertNotNull execution.workflow.commands
        assertEquals 1, execution.workflow.commands.size()
    }
    public void testDoValidateScheduledCrontabString() {
        def testService = new ScheduledExecutionService()
        def fwkControl = mockFor(FrameworkService, true)
        fwkControl.demand.getFrameworkFromUserSession { session, request -> return null }
        fwkControl.demand.existsFrameworkProject { project, framework ->
            assertEquals 'testProject', project
            return true
        }
        fwkControl.demand.isClusterModeEnabled {->
            return false
        }
        testService.frameworkService = fwkControl.createMock()

        def crontabString = '13 23 5 9 3 ? *'
        def params = [jobName: 'monkey1', project: 'testProject', description: 'blah',
                scheduled: true, crontabString: crontabString, useCrontabString:'true',
                workflow: new Workflow(commands: [new CommandExec(adhocRemoteString: 'asdf')]),
        ]
        def results = testService._dovalidate(params, 'test', 'test', null)

        assertFalse(results.failed)
        assertNotNull(results.scheduledExecution)
        assertTrue(results.scheduledExecution instanceof ScheduledExecution)
        final ScheduledExecution execution = results.scheduledExecution
        assertNotNull(execution)
        assertNotNull(execution.errors)
        assertFalse(execution.errors.getFieldError('crontabString').toString(),execution.errors.hasFieldErrors
                ('crontabString'))
        assertFalse(execution.errors.hasErrors())
        assertTrue(execution.scheduled)
//        assertEquals(crontabString, execution.crontabString)
        assertEquals('13', execution.seconds)
        assertEquals('23', execution.minute)
        assertEquals('5', execution.hour)
        assertEquals('9', execution.dayOfMonth)
        assertEquals('3', execution.month)
        assertEquals('?', execution.dayOfWeek)
        assertEquals('*', execution.year)

    }

    public void testDoValidateScheduled() {
        def testService = new ScheduledExecutionService()
        def fwkControl = mockFor(FrameworkService, true)
        fwkControl.demand.getFrameworkFromUserSession { session, request -> return null }
        fwkControl.demand.existsFrameworkProject { project, framework ->
            assertEquals 'testProject', project
            return true
        }
        fwkControl.demand.isClusterModeEnabled {->
            return false
        }
        testService.frameworkService = fwkControl.createMock()

        def job = new ScheduledExecution(
                jobName: 'monkey1',
                project: 'testProject',
                description: 'blah',
                scheduled: true,
                seconds: '13',
                minute: '23',
                hour: '5',
                dayOfMonth: '9',
                month: '3',
                dayOfWeek: '?',
                year: '*/2',
                workflow: new Workflow(commands: [new CommandExec(adhocRemoteString: 'asdf')]),
        )
        def params = new HashMap(job.properties)
        params.crontabString=job.generateCrontabExression()
        params.useCrontabString='true'
        def results = testService._dovalidate(params, 'test', 'test', null)

        assertFalse(results.scheduledExecution.errors.allErrors.collect{it.toString()}.join('; '),results.failed)
        assertNotNull(results.scheduledExecution)
        assertTrue(results.scheduledExecution instanceof ScheduledExecution)
        final ScheduledExecution execution = results.scheduledExecution
        assertNotNull(execution)
        assertNotNull(execution.errors)
        assertFalse(execution.errors.getFieldError('crontabString').toString(),execution.errors.hasFieldErrors
                ('crontabString'))
        assertFalse(execution.errors.hasErrors())
        assertTrue(execution.scheduled)
        assertEquals('13', execution.seconds)
        assertEquals('23', execution.minute)
        assertEquals('5', execution.hour)
        assertEquals('9', execution.dayOfMonth)
        assertEquals('3', execution.month)
        assertEquals('?', execution.dayOfWeek)
        assertEquals('*/2', execution.year)

    }

    public void testDoValidateAdhoc() {
        def testService = new ScheduledExecutionService()

        if (true) {//failure on missing adhoc script props
            def fwkControl = mockFor(FrameworkService, true)
            fwkControl.demand.getFrameworkFromUserSession { session, request -> return null }
            fwkControl.demand.existsFrameworkProject { project, framework ->
                assertEquals 'testProject', project
                return true
            }
            testService.frameworkService = fwkControl.createMock()

            def params = [jobName: 'monkey1', project: 'testProject', description: 'blah',
                    workflow: [threadcount: 1, keepgoing: true, "commands[0]": [adhocExecution: true]],
            ]
            def results = testService._dovalidate(params, 'test', 'test', null)

            assertTrue(results.failed)
            assertNotNull(results.scheduledExecution)
            assertTrue(results.scheduledExecution instanceof ScheduledExecution)
            final ScheduledExecution execution = results.scheduledExecution
            assertNotNull(execution)
            assertNotNull(execution.errors)

            assertTrue(execution.errors.hasErrors())
            assertTrue(execution.errors.hasFieldErrors('workflow'))
            assertNotNull execution.workflow
            assertNotNull execution.workflow.commands
            assertEquals 1, execution.workflow.commands.size()
            def exec = execution.workflow.commands[0]
            assertTrue exec.errors.hasErrors()
            assertTrue exec.errors.hasFieldErrors('adhocExecution')

        }
    }

    public void testDoValidateAdhocValidation1() {
        def testService = new ScheduledExecutionService()
        if (true) {//both adhocRemote/adhocLocal should result in validation error
            def fwkControl = mockFor(FrameworkService, true)
            fwkControl.demand.getFrameworkFromUserSession { session, request -> return null }
            fwkControl.demand.existsFrameworkProject { project, framework ->
                assertEquals 'testProject', project
                return true
            }
            fwkControl.demand.getCommand { project, type, command, framework ->
                assertEquals 'testProject', project
                assertEquals 'aType', type
                assertEquals 'aCommand', command
                return null
            }
            testService.frameworkService = fwkControl.createMock()

            def params = [jobName: 'monkey1', project: 'testProject', description: 'blah',
                    workflow: [threadcount: 1, keepgoing: true, "commands[0]": [adhocExecution: true, adhocRemoteString: 'test1', adhocLocalString: 'test2']],
            ]
            def results = testService._dovalidate(params, 'test', 'test', null)

            if (results.scheduledExecution.errors.hasErrors()) {
                results.scheduledExecution.errors.allErrors.each {
                    System.err.println(it);
                }
            }
            assertTrue(results.failed)
            assertNotNull(results.scheduledExecution)
            assertTrue(results.scheduledExecution instanceof ScheduledExecution)
            final ScheduledExecution execution = results.scheduledExecution
            assertNotNull(execution)
            assertNotNull(execution.errors)

            assertTrue(execution.errors.hasErrors())
            assertTrue(execution.errors.hasFieldErrors('workflow'))
            assertNotNull execution.workflow
            assertNotNull execution.workflow.commands
            assertEquals 1, execution.workflow.commands.size()
            def exec = execution.workflow.commands[0]
            assertTrue exec.errors.hasErrors()
            assertTrue exec.errors.hasFieldErrors('adhocRemoteString')

        }
    }

    public void testDoValidateAdhocValid() {
        def testService = new ScheduledExecutionService()
        if (true) {//test basic passing input (adhocRemoteString)
            def fwkControl = mockFor(FrameworkService, true)
            fwkControl.demand.getFrameworkFromUserSession { session, request -> return null }
            fwkControl.demand.existsFrameworkProject { project, framework ->
                assertEquals 'testProject', project
                return true
            }
            fwkControl.demand.getCommand { project, type, command, framework ->
                assertEquals 'testProject', project
                assertEquals 'aType', type
                assertEquals 'aCommand', command
                return null
            }
            testService.frameworkService = fwkControl.createMock()

            def params = [jobName: 'monkey1', project: 'testProject', description: 'blah',
                    workflow: [threadcount: 1, keepgoing: true, "commands[0]": [adhocExecution: true, adhocRemoteString: 'test what']],
            ]
            def results = testService._dovalidate(params, 'test', 'test', null)
            assertFalse(results.failed)
            assertNotNull(results.scheduledExecution)
            assertTrue(results.scheduledExecution instanceof ScheduledExecution)
            final ScheduledExecution execution = results.scheduledExecution
            assertNotNull(execution)
            assertNotNull(execution.errors)
            assertFalse(execution.errors.hasErrors())
            assertFalse(execution.errors.hasFieldErrors())
            assertFalse(execution.errors.hasFieldErrors('name'))
            assertFalse(execution.errors.hasFieldErrors('project'))
            assertFalse(execution.errors.hasFieldErrors('description'))
            assertFalse(execution.errors.hasFieldErrors('jobName'))
            assertFalse(execution.errors.hasFieldErrors('type'))
            assertFalse(execution.errors.hasFieldErrors('command'))
            assertEquals 'monkey1', execution.jobName
            assertEquals 'testProject', execution.project
            assertEquals 'blah', execution.description
            assertNotNull execution.workflow
            assertNotNull execution.workflow.commands
            assertEquals 1, execution.workflow.commands.size()
            def CommandExec cexec = execution.workflow.commands[0]
            assertTrue cexec.adhocExecution
            assertEquals 'test what', cexec.adhocRemoteString
            assertNull execution.argString
        }
    }

    public void testDoValidateAdhocValidLocalString() {
        def testService = new ScheduledExecutionService()
        if (true) {//test basic passing input (adhocLocalString)
            def fwkControl = mockFor(FrameworkService, true)
            fwkControl.demand.getFrameworkFromUserSession { session, request -> return null }
            fwkControl.demand.existsFrameworkProject { project, framework ->
                assertEquals 'testProject', project
                return true
            }
            fwkControl.demand.getCommand { project, type, command, framework ->
                assertEquals 'testProject', project
                assertEquals 'aType', type
                assertEquals 'aCommand', command
                return null
            }
            testService.frameworkService = fwkControl.createMock()

            def params = [jobName: 'monkey1', project: 'testProject', description: 'blah',
                    workflow: [threadcount: 1, keepgoing: true, "commands[0]": [adhocExecution: true, adhocLocalString: 'test what']],
            ]
            def results = testService._dovalidate(params, 'test', 'test', null)
            assertFalse(results.failed)
            assertNotNull(results.scheduledExecution)
            assertTrue(results.scheduledExecution instanceof ScheduledExecution)
            final ScheduledExecution execution = results.scheduledExecution
            assertNotNull(execution)
            assertNotNull(execution.errors)
            assertFalse(execution.errors.hasErrors())
            assertFalse(execution.errors.hasFieldErrors())
            assertFalse(execution.errors.hasFieldErrors('name'))
            assertFalse(execution.errors.hasFieldErrors('project'))
            assertFalse(execution.errors.hasFieldErrors('description'))
            assertFalse(execution.errors.hasFieldErrors('jobName'))
            assertFalse(execution.errors.hasFieldErrors('type'))
            assertFalse(execution.errors.hasFieldErrors('command'))
            assertEquals 'monkey1', execution.jobName
            assertEquals 'testProject', execution.project
            assertEquals 'blah', execution.description
            assertNotNull execution.workflow
            assertNotNull execution.workflow.commands
            assertEquals 1, execution.workflow.commands.size()
            def CommandExec cexec = execution.workflow.commands[0]
            assertTrue cexec.adhocExecution
            assertEquals 'test what', cexec.adhocLocalString
            assertNull execution.argString
        }
    }
    public void testDoValidateAdhocValidFilePath() {
        def testService = new ScheduledExecutionService()
        if (true) {//test basic passing input (adhocFilepath)
            def fwkControl = mockFor(FrameworkService, true)
            fwkControl.demand.getFrameworkFromUserSession { session, request -> return null }
            fwkControl.demand.existsFrameworkProject { project, framework ->
                assertEquals 'testProject', project
                return true
            }
            fwkControl.demand.getCommand { project, type, command, framework ->
                assertEquals 'testProject', project
                assertEquals 'aType', type
                assertEquals 'aCommand', command
                return null
            }
            testService.frameworkService = fwkControl.createMock()

            def params = [jobName: 'monkey1', project: 'testProject', description: 'blah',
                    workflow: [threadcount: 1, keepgoing: true, "commands[0]": [adhocExecution: true, adhocFilepath: 'test what']],
                    ]
            def results = testService._dovalidate(params, 'test', 'test', null)
            assertFalse(results.failed)
            assertNotNull(results.scheduledExecution)
            assertTrue(results.scheduledExecution instanceof ScheduledExecution)
            final ScheduledExecution execution = results.scheduledExecution
            assertNotNull(execution)
            assertNotNull(execution.errors)
            assertFalse(execution.errors.hasErrors())
            assertFalse(execution.errors.hasFieldErrors())
            assertFalse(execution.errors.hasFieldErrors('name'))
            assertFalse(execution.errors.hasFieldErrors('project'))
            assertFalse(execution.errors.hasFieldErrors('description'))
            assertFalse(execution.errors.hasFieldErrors('jobName'))
            assertFalse(execution.errors.hasFieldErrors('type'))
            assertFalse(execution.errors.hasFieldErrors('command'))
            assertEquals 'monkey1', execution.jobName
            assertEquals 'testProject', execution.project
            assertEquals 'blah', execution.description
            assertNotNull execution.workflow
            assertNotNull execution.workflow.commands
            assertEquals 1, execution.workflow.commands.size()
            def CommandExec cexec = execution.workflow.commands[0]
            assertTrue cexec.adhocExecution
            assertEquals 'test what', cexec.adhocFilepath
            assertNull execution.argString
        }
    }

    public void testDoValidateAdhocValidFilePathArgString() {
        def testService = new ScheduledExecutionService()
        if (true) {//test argString input for adhocFilepath
            def fwkControl = mockFor(FrameworkService, true)
            fwkControl.demand.getFrameworkFromUserSession { session, request -> return null }
            fwkControl.demand.existsFrameworkProject { project, framework ->
                assertEquals 'testProject', project
                return true
            }
            fwkControl.demand.getCommand { project, type, command, framework ->
                assertEquals 'testProject', project
                assertEquals 'aType', type
                assertEquals 'aCommand', command
                return null
            }
            testService.frameworkService = fwkControl.createMock()

            def params = [jobName: 'monkey1', project: 'testProject', description: 'blah',
                    workflow: [threadcount: 1, keepgoing: true, "commands[0]": [
                            adhocExecution: true,
                            adhocFilepath: 'test file',
                            argString: 'test args']],

            ]
            def results = testService._dovalidate(params, 'test', 'test', null)
            assertFalse(results.failed)
            assertNotNull(results.scheduledExecution)
            assertTrue(results.scheduledExecution instanceof ScheduledExecution)
            final ScheduledExecution execution = results.scheduledExecution
            assertNotNull(execution)
            assertNotNull(execution.errors)
            assertFalse(execution.errors.hasErrors())
            assertFalse(execution.errors.hasFieldErrors())
            assertFalse(execution.errors.hasFieldErrors('name'))
            assertFalse(execution.errors.hasFieldErrors('project'))
            assertFalse(execution.errors.hasFieldErrors('description'))
            assertFalse(execution.errors.hasFieldErrors('jobName'))
            assertFalse(execution.errors.hasFieldErrors('type'))
            assertFalse(execution.errors.hasFieldErrors('command'))
            assertEquals 'monkey1', execution.jobName
            assertEquals 'testProject', execution.project
            assertEquals 'blah', execution.description
            assertNotNull execution.workflow
            assertNotNull execution.workflow.commands
            assertEquals 1, execution.workflow.commands.size()
            def CommandExec cexec = execution.workflow.commands[0]
            assertTrue cexec.adhocExecution
            assertEquals 'test file', cexec.adhocFilepath
            assertEquals 'test args', cexec.argString
        }
    }

    public void testDoValidateAdhocValidAdhocRemoteStringArgString() {
        def testService = new ScheduledExecutionService()
        if (true) {//test argString input for adhocRemoteString argString should be set
            def fwkControl = mockFor(FrameworkService, true)
            fwkControl.demand.getFrameworkFromUserSession { session, request -> return null }
            fwkControl.demand.existsFrameworkProject { project, framework ->
                assertEquals 'testProject', project
                return true
            }
            fwkControl.demand.getCommand { project, type, command, framework ->
                assertEquals 'testProject', project
                assertEquals 'aType', type
                assertEquals 'aCommand', command
                return null
            }
            testService.frameworkService = fwkControl.createMock()

            def params = [jobName: 'monkey1', project: 'testProject', description: 'blah',
                    workflow: [threadcount: 1, keepgoing: true, "commands[0]": [
                            adhocExecution: true,
                            adhocRemoteString: 'test remote',
                            argString: 'test args'
                    ]],

            ]
            def results = testService._dovalidate(params, 'test', 'test', null)
            assertFalse(results.failed)
            assertNotNull(results.scheduledExecution)
            assertTrue(results.scheduledExecution instanceof ScheduledExecution)
            final ScheduledExecution execution = results.scheduledExecution
            assertNotNull(execution)
            assertNotNull(execution.errors)
            assertFalse(execution.errors.hasErrors())
            assertFalse(execution.errors.hasFieldErrors())
            assertFalse(execution.errors.hasFieldErrors('name'))
            assertFalse(execution.errors.hasFieldErrors('project'))
            assertFalse(execution.errors.hasFieldErrors('description'))
            assertFalse(execution.errors.hasFieldErrors('jobName'))
            assertFalse(execution.errors.hasFieldErrors('type'))
            assertFalse(execution.errors.hasFieldErrors('command'))
            assertEquals 'monkey1', execution.jobName
            assertEquals 'testProject', execution.project
            assertEquals 'blah', execution.description
            assertNotNull execution.workflow
            assertNotNull execution.workflow.commands
            assertEquals 1, execution.workflow.commands.size()
            def CommandExec cexec = execution.workflow.commands[0]
            assertTrue cexec.adhocExecution
            assertEquals 'test remote', cexec.adhocRemoteString
            assertEquals 'test args', cexec.argString
        }
    }

    public void testDoValidateAdhocAdhocLocalStringArgString() {
        def testService = new ScheduledExecutionService()
        if (true) {//test argString input for adhocLocalString
            def fwkControl = mockFor(FrameworkService, true)
            fwkControl.demand.getFrameworkFromUserSession { session, request -> return null }
            fwkControl.demand.existsFrameworkProject { project, framework ->
                assertEquals 'testProject', project
                return true
            }
            fwkControl.demand.getCommand { project, type, command, framework ->
                assertEquals 'testProject', project
                assertEquals 'aType', type
                assertEquals 'aCommand', command
                return null
            }
            testService.frameworkService = fwkControl.createMock()

            def params = [jobName: 'monkey1', project: 'testProject', description: 'blah',
                    workflow: [threadcount: 1, keepgoing: true, "commands[0]":[
                            adhocExecution: true,
                            adhocLocalString: 'test local',
                            argString: 'test args'
                    ]
                    ],

            ]
            def results = testService._dovalidate(params, 'test', 'test', null)
            assertFalse(results.failed)
            assertNotNull(results.scheduledExecution)
            assertTrue(results.scheduledExecution instanceof ScheduledExecution)
            final ScheduledExecution execution = results.scheduledExecution
            assertNotNull(execution)
            assertNotNull(execution.errors)
            assertFalse(execution.errors.hasErrors())
            assertFalse(execution.errors.hasFieldErrors())
            assertFalse(execution.errors.hasFieldErrors('name'))
            assertFalse(execution.errors.hasFieldErrors('project'))
            assertFalse(execution.errors.hasFieldErrors('description'))
            assertFalse(execution.errors.hasFieldErrors('jobName'))
            assertFalse(execution.errors.hasFieldErrors('type'))
            assertFalse(execution.errors.hasFieldErrors('command'))
            assertEquals 'monkey1', execution.jobName
            assertEquals 'testProject', execution.project
            assertEquals 'blah', execution.description
            assertNotNull execution.workflow
            assertNotNull execution.workflow.commands
            assertEquals 1, execution.workflow.commands.size()
            def CommandExec cexec = execution.workflow.commands[0]
            assertTrue cexec.adhocExecution
            assertEquals 'test local', cexec.adhocLocalString
            assertEquals 'test args', cexec.argString
        }
    }

    public void testDoValidateNotifications() {

        def sec = service
        //test job with notifications
            def fwkControl = mockFor(FrameworkService, true)
            fwkControl.demand.getFrameworkFromUserSession { session, request -> return null }
            fwkControl.demand.existsFrameworkProject { project, framework ->
                assertEquals 'testProject', project
                return true
            }
            fwkControl.demand.getCommand { project, type, command, framework ->
                assertEquals 'testProject', project
                assertEquals 'aType', type
                assertEquals 'aCommand', command
                return null
            }
            sec.frameworkService = fwkControl.createMock()

            def ms = mockFor(MessageSource)
            ms.demand.getMessage { key, data, locale -> key + ":" + data.toString() + ":" + locale.toString() }
            ms.demand.getMessage { error, locale -> error.toString() + ":" + locale.toString() }
            sec.messageSource = ms.createMock()

            def params = [jobName: 'monkey1', project: 'testProject', description: 'blah',
                    workflow: [threadcount: 1, keepgoing: true, "commands[0]": [adhocExecution: true, adhocRemoteString: 'test command']],
                    notifications: [[eventTrigger: ScheduledExecutionController.ONSUCCESS_TRIGGER_NAME, type: 'email', content: 'c@example.com,d@example.com']]
            ]
            def results = sec._dovalidate(params, 'test', 'test', null)
            if (results.scheduledExecution.errors.hasErrors()) {
                results.scheduledExecution.errors.allErrors.each {
                    System.err.println(it);
                }
            }
            assertFalse results.failed
            assertNotNull(results.scheduledExecution)
            assertTrue(results.scheduledExecution instanceof ScheduledExecution)
            final ScheduledExecution execution = results.scheduledExecution
            assertNotNull(execution)
            assertNotNull(execution.errors)
            assertFalse(execution.errors.hasErrors())
            assertEquals 'monkey1', execution.jobName
            assertEquals 'testProject', execution.project
            assertEquals 'blah', execution.description
            assertNotNull execution.workflow
            assertNotNull execution.workflow.commands
            assertEquals 1, execution.workflow.commands.size()
            def CommandExec cexec = execution.workflow.commands[0]
            assertTrue cexec.adhocExecution
            assertEquals 'test command', cexec.adhocRemoteString
            assertNull cexec.adhocFilepath
            assertNull execution.argString

            assertNotNull execution.notifications
            assertEquals 1, execution.notifications.size()
            final def not1 = execution.notifications.iterator().next()
            assertTrue(not1 instanceof Notification)
            def Notification n = not1
            assertEquals ScheduledExecutionController.ONSUCCESS_TRIGGER_NAME, n.eventTrigger
            assertEquals "email", n.type
            assertEquals "c@example.com,d@example.com", n.mailConfiguration().recipients
        }

    public void testDoValidateNotifications_2() {

        def sec = service//test job with notifications
            def fwkControl = mockFor(FrameworkService, true)
            fwkControl.demand.getFrameworkFromUserSession { session, request -> return null }
            fwkControl.demand.existsFrameworkProject { project, framework ->
                assertEquals 'testProject', project
                return true
            }
            fwkControl.demand.getCommand { project, type, command, framework ->
                assertEquals 'testProject', project
                assertEquals 'aType', type
                assertEquals 'aCommand', command
                return null
            }
            sec.frameworkService = fwkControl.createMock()

            def params = [jobName: 'monkey1', project: 'testProject', description: 'blah',
                    workflow: [threadcount: 1, keepgoing: true, "commands[0]": [adhocExecution: true, adhocRemoteString: 'test command']],
                    notifications: [[eventTrigger: ScheduledExecutionController.ONSUCCESS_TRIGGER_NAME, type: 'email', content: 'c@example.com,d@example.com'], [eventTrigger: ScheduledExecutionController.ONFAILURE_TRIGGER_NAME, type: 'email', content: 'monkey@example.com']]
            ]
            def results = sec._dovalidate(params, 'test', 'test', null)
            if (results.scheduledExecution.errors.hasErrors()) {
                results.scheduledExecution.errors.allErrors.each {
                    System.err.println(it);
                }
            }
            assertFalse results.failed
            assertNotNull(results.scheduledExecution)
            assertTrue(results.scheduledExecution instanceof ScheduledExecution)
            final ScheduledExecution execution = results.scheduledExecution
            assertNotNull(execution)
            assertNotNull(execution.errors)
            assertFalse(execution.errors.hasErrors())
            assertEquals 'monkey1', execution.jobName
            assertEquals 'testProject', execution.project
            assertEquals 'blah', execution.description
            assertNotNull execution.workflow
            assertNotNull execution.workflow.commands
            assertEquals 1, execution.workflow.commands.size()
            def CommandExec cexec = execution.workflow.commands[0]
            assertTrue cexec.adhocExecution
            assertEquals 'test command', cexec.adhocRemoteString
            assertNull cexec.adhocFilepath
            assertNull execution.argString

            assertNotNull execution.notifications
            assertEquals 2, execution.notifications.size()
            def nmap = [:]
            execution.notifications.each { not1 ->
                nmap[not1.eventTrigger] = not1
            }
            assertNotNull(nmap[ScheduledExecutionController.ONSUCCESS_TRIGGER_NAME])
            assertNotNull(nmap[ScheduledExecutionController.ONFAILURE_TRIGGER_NAME])
            assertTrue(nmap[ScheduledExecutionController.ONSUCCESS_TRIGGER_NAME] instanceof Notification)
            assertTrue(nmap[ScheduledExecutionController.ONFAILURE_TRIGGER_NAME] instanceof Notification)
            def Notification n = nmap[ScheduledExecutionController.ONSUCCESS_TRIGGER_NAME]
            assertEquals ScheduledExecutionController.ONSUCCESS_TRIGGER_NAME, n.eventTrigger
            assertEquals "email", n.type
            assertEquals "c@example.com,d@example.com", n.mailConfiguration().recipients
            def Notification n2 = nmap[ScheduledExecutionController.ONFAILURE_TRIGGER_NAME]
            assertEquals ScheduledExecutionController.ONFAILURE_TRIGGER_NAME, n2.eventTrigger
            assertEquals "email", n2.type
            assertEquals "monkey@example.com", n2.mailConfiguration().recipients
        }

    public void testDoValidateNotifications_formfields() {

        def sec = service//test job with notifications, using form input fields for params
            def fwkControl = mockFor(FrameworkService, true)
            fwkControl.demand.getFrameworkFromUserSession { session, request -> return null }
            fwkControl.demand.existsFrameworkProject { project, framework ->
                assertEquals 'testProject', project
                return true
            }
            fwkControl.demand.getCommand { project, type, command, framework ->
                assertEquals 'testProject', project
                assertEquals 'aType', type
                assertEquals 'aCommand', command
                return null
            }
            sec.frameworkService = fwkControl.createMock()

            def params = [jobName: 'monkey1', project: 'testProject', description: 'blah',
                    workflow: [threadcount: 1, keepgoing: true, "commands[0]": [
                            adhocExecution: true, adhocRemoteString: 'test command'
                    ]],
                    (ScheduledExecutionController.NOTIFY_ONSUCCESS_EMAIL): 'true', (ScheduledExecutionController.NOTIFY_SUCCESS_RECIPIENTS): 'c@example.com,d@example.com',
                    (ScheduledExecutionController.NOTIFY_ONFAILURE_EMAIL): 'true', (ScheduledExecutionController.NOTIFY_FAILURE_RECIPIENTS): 'monkey@example.com',

//                notifications:[onsuccess:[email:'c@example.com,d@example.com'],onfailure:[email:'monkey@example.com']]
            ]
            def results = sec._dovalidate(params, 'test', 'test', null)
            if (results.scheduledExecution.errors.hasErrors()) {
                results.scheduledExecution.errors.allErrors.each {
                    System.err.println(it);
                }
            }
            assertFalse results.failed
            assertNotNull(results.scheduledExecution)
            assertTrue(results.scheduledExecution instanceof ScheduledExecution)
            final ScheduledExecution execution = results.scheduledExecution
            assertNotNull(execution)
            assertNotNull(execution.errors)
            assertFalse(execution.errors.hasErrors())
            assertEquals 'monkey1', execution.jobName
            assertEquals 'testProject', execution.project
            assertEquals 'blah', execution.description
            assertNotNull execution.workflow
            assertNotNull execution.workflow.commands
            assertEquals 1, execution.workflow.commands.size()
            def CommandExec cexec = execution.workflow.commands[0]
            assertTrue cexec.adhocExecution
            assertEquals 'test command', cexec.adhocRemoteString
            assertNull cexec.adhocFilepath
            assertNull execution.argString

            assertNotNull execution.notifications
            assertEquals 2, execution.notifications.size()
            def nmap = [:]
            execution.notifications.each { not1 ->
                nmap[not1.eventTrigger] = not1
            }
            assertNotNull(nmap[ScheduledExecutionController.ONSUCCESS_TRIGGER_NAME])
            assertNotNull(nmap[ScheduledExecutionController.ONFAILURE_TRIGGER_NAME])
            assertTrue(nmap[ScheduledExecutionController.ONSUCCESS_TRIGGER_NAME] instanceof Notification)
            assertTrue(nmap[ScheduledExecutionController.ONFAILURE_TRIGGER_NAME] instanceof Notification)
            def Notification n = nmap[ScheduledExecutionController.ONSUCCESS_TRIGGER_NAME]
            assertEquals ScheduledExecutionController.ONSUCCESS_TRIGGER_NAME, n.eventTrigger
            assertEquals "email", n.type
            assertEquals "c@example.com,d@example.com", n.mailConfiguration().recipients
            def Notification n2 = nmap[ScheduledExecutionController.ONFAILURE_TRIGGER_NAME]
            assertEquals ScheduledExecutionController.ONFAILURE_TRIGGER_NAME, n2.eventTrigger
            assertEquals "email", n2.type
            assertEquals "monkey@example.com", n2.mailConfiguration().recipients
        }

    public void testDoValidateNotifications_invalidemail3() {

        def sec = service//test job with notifications, invalid email addresses
            def fwkControl = mockFor(FrameworkService, true)
            fwkControl.demand.getFrameworkFromUserSession { session, request -> return null }
            fwkControl.demand.existsFrameworkProject { project, framework ->
                assertEquals 'testProject', project
                return true
            }
            fwkControl.demand.getCommand { project, type, command, framework ->
                assertEquals 'testProject', project
                assertEquals 'aType', type
                assertEquals 'aCommand', command
                return null
            }
            sec.frameworkService = fwkControl.createMock()

            def params = [jobName: 'monkey1', project: 'testProject', description: 'blah', adhocExecution: false, name: 'aResource', type: 'aType', command: 'aCommand',
                    (ScheduledExecutionController.NOTIFY_ONSUCCESS_EMAIL): 'true', (ScheduledExecutionController.NOTIFY_SUCCESS_RECIPIENTS): 'c@example.',
            ]
            def results = sec._dovalidate(params, 'test', 'test', null)
            if (results.scheduledExecution.errors.hasErrors()) {
                results.scheduledExecution.errors.allErrors.each {
                    System.err.println(it);
                }
            }
            assertTrue results.failed
            assertNotNull(results.scheduledExecution)
            assertTrue(results.scheduledExecution instanceof ScheduledExecution)
            final ScheduledExecution execution = results.scheduledExecution
            assertNotNull(execution)
            assertNotNull(execution.errors)
            assertTrue(execution.errors.hasErrors())
            assertTrue(execution.errors.hasFieldErrors(ScheduledExecutionController.NOTIFY_SUCCESS_RECIPIENTS))
            assertFalse(execution.errors.hasFieldErrors(ScheduledExecutionController.NOTIFY_FAILURE_RECIPIENTS))
        }

    public void testDoValidateNotifications_invalidemail2() {

        def sec = service//test job with notifications, invalid email addresses
            def fwkControl = mockFor(FrameworkService, true)
            fwkControl.demand.getFrameworkFromUserSession { session, request -> return null }
            fwkControl.demand.existsFrameworkProject { project, framework ->
                assertEquals 'testProject', project
                return true
            }
            fwkControl.demand.getCommand { project, type, command, framework ->
                assertEquals 'testProject', project
                assertEquals 'aType', type
                assertEquals 'aCommand', command
                return null
            }
            sec.frameworkService = fwkControl.createMock()

            def params = [jobName: 'monkey1', project: 'testProject', description: 'blah', adhocExecution: false, name: 'aResource', type: 'aType', command: 'aCommand',
                    (ScheduledExecutionController.NOTIFY_ONFAILURE_EMAIL): 'true', (ScheduledExecutionController.NOTIFY_FAILURE_RECIPIENTS): '@example.com',
            ]
            def results = sec._dovalidate(params, 'test', 'test', null)
            if (results.scheduledExecution.errors.hasErrors()) {
                results.scheduledExecution.errors.allErrors.each {
                    System.err.println(it);
                }
            }
            assertTrue results.failed
            assertNotNull(results.scheduledExecution)
            assertTrue(results.scheduledExecution instanceof ScheduledExecution)
            final ScheduledExecution execution = results.scheduledExecution
            assertNotNull(execution)
            assertNotNull(execution.errors)
            assertTrue(execution.errors.hasErrors())
            assertTrue(execution.errors.hasFieldErrors(ScheduledExecutionController.NOTIFY_FAILURE_RECIPIENTS))
            assertFalse(execution.errors.hasFieldErrors(ScheduledExecutionController.NOTIFY_SUCCESS_RECIPIENTS))
        }

    public void testDoValidateNotifications_invalidemail() {

        def sec = service//test job with notifications, invalid email addresses using map based notifications definition
            def fwkControl = mockFor(FrameworkService, true)
            fwkControl.demand.getFrameworkFromUserSession { session, request -> return null }
            fwkControl.demand.existsFrameworkProject { project, framework ->
                assertEquals 'testProject', project
                return true
            }
            fwkControl.demand.getCommand { project, type, command, framework ->
                assertEquals 'testProject', project
                assertEquals 'aType', type
                assertEquals 'aCommand', command
                return null
            }
            sec.frameworkService = fwkControl.createMock()

            def params = [jobName: 'monkey1', project: 'testProject', description: 'blah', adhocExecution: false, name: 'aResource', type: 'aType', command: 'aCommand',
                    notifications: [[eventTrigger: ScheduledExecutionController.ONSUCCESS_TRIGGER_NAME, type: 'email', content: 'c@example.comd@example.com'], [eventTrigger: ScheduledExecutionController.ONFAILURE_TRIGGER_NAME, type: 'email', content: 'monkey@ example.com']]
            ]
            def results = sec._dovalidate(params, 'test', 'test', null)
            if (results.scheduledExecution.errors.hasErrors()) {
                results.scheduledExecution.errors.allErrors.each {
                    System.err.println(it);
                }
            }
            assertTrue results.failed
            assertNotNull(results.scheduledExecution)
            assertTrue(results.scheduledExecution instanceof ScheduledExecution)
            final ScheduledExecution execution = results.scheduledExecution
            assertNotNull(execution)
            assertNotNull(execution.errors)
            assertTrue(execution.errors.hasErrors())
            assertTrue(execution.errors.hasFieldErrors(ScheduledExecutionController.NOTIFY_FAILURE_RECIPIENTS))
            assertTrue(execution.errors.hasFieldErrors(ScheduledExecutionController.NOTIFY_SUCCESS_RECIPIENTS))
        }

    public void testDoValidateNotifications_invalidurls() {

        def sec = service//test job with notifications, invalid urls
            def fwkControl = mockFor(FrameworkService, true)
            fwkControl.demand.getFrameworkFromUserSession { session, request -> return null }
            fwkControl.demand.existsFrameworkProject { project, framework ->
                assertEquals 'testProject', project
                return true
            }
            fwkControl.demand.getCommand { project, type, command, framework ->
                assertEquals 'testProject', project
                assertEquals 'aType', type
                assertEquals 'aCommand', command
                return null
            }
            sec.frameworkService = fwkControl.createMock()

            def params = [jobName: 'monkey1', project: 'testProject', description: 'blah2', adhocExecution: false, name: 'aResource', type: 'aType', command: 'aCommand',
                    notifications: [[eventTrigger: ScheduledExecutionController.ONSUCCESS_TRIGGER_NAME, type: 'url', content: 'c@example.comd@example.com'], [eventTrigger: ScheduledExecutionController.ONFAILURE_TRIGGER_NAME, type: 'url', content: 'monkey@ example.com']]
            ]
            def results = sec._dovalidate(params, 'test', 'test', null)
            if (results.scheduledExecution.errors.hasErrors()) {
                results.scheduledExecution.errors.allErrors.each {
                    System.err.println(it);
                }
            }
            assertTrue results.failed
            assertNotNull(results.scheduledExecution)
            assertTrue(results.scheduledExecution instanceof ScheduledExecution)
            final ScheduledExecution execution = results.scheduledExecution
            assertNotNull(execution)
            assertNotNull(execution.errors)
            assertTrue(execution.errors.hasErrors())
            assertTrue(execution.errors.hasFieldErrors(ScheduledExecutionController.NOTIFY_FAILURE_URL))
            assertTrue(execution.errors.hasFieldErrors(ScheduledExecutionController.NOTIFY_SUCCESS_URL))
    }

    public void testInvalidNotificationsEmailsAllowEmbeddedProps() {

        def sec = new ScheduledExecutionService()
        //test job with notifications, invalid email addresses using map based notifications definition
        def fwkControl = mockFor(FrameworkService, true)
        fwkControl.demand.getFrameworkFromUserSession { session, request -> return null }
        fwkControl.demand.existsFrameworkProject { project, framework ->
            assertEquals 'testProject', project
            return true
        }
        fwkControl.demand.getCommand { project, type, command, framework ->
            assertEquals 'testProject', project
            assertEquals 'aType', type
            assertEquals 'aCommand', command
            return null
        }
        sec.frameworkService = fwkControl.createMock()

        def params = [jobName: 'monkey1', project: 'testProject', description: 'blah',
                workflow: [threadcount: 1, keepgoing: true, "commands[0]": [adhocExecution: true, adhocRemoteString: 'a remote string']],
                notifications: [[eventTrigger: ScheduledExecutionController.ONSUCCESS_TRIGGER_NAME, type: 'email', content: '${job.user.name}@something.org'],
                        [eventTrigger: ScheduledExecutionController.ONFAILURE_TRIGGER_NAME, type: 'email', content: '${job.user.email}']]
        ]
        def results = sec._dovalidate(params, 'test', 'test', null)
        if (results.scheduledExecution.errors.hasErrors()) {
            results.scheduledExecution.errors.allErrors.each {
                System.err.println(it);
            }
        }

        assertFalse results.failed
        assertNotNull(results.scheduledExecution)
        assertTrue(results.scheduledExecution instanceof ScheduledExecution)
        final ScheduledExecution execution = results.scheduledExecution
        assertNotNull(execution)
        assertNotNull(execution.errors)
        assertFalse(execution.errors.hasErrors())
        assertNotNull execution.notifications
        assertEquals 2, execution.notifications.size()
        def notifications = execution.notifications.groupBy { it.eventTrigger }
        final def not0 = notifications[ScheduledExecutionController.ONSUCCESS_TRIGGER_NAME][0]
        assertTrue(not0 instanceof Notification)
        def Notification n0 = not0
        assertEquals ScheduledExecutionController.ONSUCCESS_TRIGGER_NAME, n0.eventTrigger
        assertEquals "email", n0.type
        assertEquals '${job.user.name}@something.org', n0.mailConfiguration().recipients
        final def not1 = notifications[ScheduledExecutionController.ONFAILURE_TRIGGER_NAME][0]
        assertTrue(not1 instanceof Notification)
        def Notification n1 = not1
        assertEquals ScheduledExecutionController.ONFAILURE_TRIGGER_NAME, n1.eventTrigger
        assertEquals "email", n1.type
        assertEquals '${job.user.email}', n1.mailConfiguration().recipients
    }

    public void testDoValidateOptionsBasic() {

        def sec = new ScheduledExecutionService()
        if (true) {//test job with options
            def fwkControl = mockFor(FrameworkService, true)
            fwkControl.demand.getFrameworkFromUserSession { session, request -> return null }
            fwkControl.demand.existsFrameworkProject { project, framework ->
                assertEquals 'testProject', project
                return true
            }
            fwkControl.demand.getCommand { project, type, command, framework ->
                assertEquals 'testProject', project
                assertEquals 'aType', type
                assertEquals 'aCommand', command
                return null
            }
            sec.frameworkService = fwkControl.createMock()

            def params = [jobName: 'monkey1', project: 'testProject', description: 'blah',
                    workflow: [threadcount: 1, keepgoing: true, "commands[0]": [adhocExecution: true, adhocRemoteString: 'a remote string']],
                    options: ["options[0]": [name: 'test3', defaultValue: 'val3', enforced: false, valuesUrl: "http://test.com/test3"]]
            ]
            def results = sec._dovalidate(params, 'test', 'test', null)
            if (results.scheduledExecution.errors.hasErrors()) {
                results.scheduledExecution.errors.allErrors.each {
                    System.err.println(it);
                }
            }
            assertFalse results.failed
            def ScheduledExecution scheduledExecution = results.scheduledExecution
            assertNotNull(scheduledExecution)
            assertTrue(scheduledExecution instanceof ScheduledExecution)
            final ScheduledExecution execution = scheduledExecution
            assertNotNull(execution)
            assertNotNull(execution.errors)
            assertFalse(execution.errors.hasErrors())
            assertNotNull execution.options
            assertEquals(1, execution.options.size())
            final Iterator iterator = execution.options.iterator()
            assert iterator.hasNext()
            final Option next = iterator.next()
            assertNotNull(next)
            assertEquals("wrong option name", "test3", next.name)
            assertEquals("wrong option name", "val3", next.defaultValue)
            assertNotNull("wrong option name", next.realValuesUrl)
            assertEquals("wrong option name", "http://test.com/test3", next.realValuesUrl.toExternalForm())
            assertFalse("wrong option name", next.enforced)
        }
    }
    public void testDoValidateOptionsX() {

        def sec = new ScheduledExecutionService()
        if (true) {//test job with old-style options
            def fwkControl = mockFor(FrameworkService, true)
            fwkControl.demand.getFrameworkFromUserSession { session, request -> return null }
            fwkControl.demand.existsFrameworkProject { project, framework ->
                assertEquals 'testProject', project
                return true
            }
            fwkControl.demand.getCommand { project, type, command, framework ->
                assertEquals 'testProject', project
                assertEquals 'aType', type
                assertEquals 'aCommand', command
                return null
            }
            sec.frameworkService = fwkControl.createMock()

            def params = [jobName: 'monkey1', project: 'testProject', description: 'blah',
                    workflow: [threadcount: 1, keepgoing: true, "commands[0]": [adhocExecution: true, adhocRemoteString: 'test command',
                            argString: '-test3 ${option.test3}'
                    ]],
                    'option.test3': 'val3', options: ["options[0]": [name: 'test3', defaultValue: 'val3', enforced: false, valuesUrl: "http://test.com/test3"]]
            ]
            def results = sec._dovalidate(params, 'test', 'test', null)
            if (results.scheduledExecution.errors.hasErrors()) {
                results.scheduledExecution.errors.allErrors.each {
                    System.err.println(it);
                }
            }
            assertFalse results.failed
            def ScheduledExecution scheduledExecution = results.scheduledExecution
            assertNotNull(scheduledExecution)
            assertTrue(scheduledExecution instanceof ScheduledExecution)
            final ScheduledExecution execution = scheduledExecution
            assertNotNull(execution)
            assertNotNull(execution.errors)
            assertFalse(execution.errors.hasErrors())
            assertNotNull execution.options
            assertLength(1, execution.options as Object[])
            final Iterator iterator = execution.options.iterator()
            assert iterator.hasNext()
            final Option next = iterator.next()
            assertNotNull(next)
            assertEquals("wrong option name", "test3", next.name)
            assertEquals("wrong option name", "val3", next.defaultValue)
            assertNotNull("wrong option name", next.realValuesUrl)
            assertEquals("wrong option name", "http://test.com/test3", next.realValuesUrl.toExternalForm())
            assertFalse("wrong option name", next.enforced)

            //test argString of defined command workflow item
            final Workflow wf = execution.workflow
            assertNotNull(wf)
            assertLength(1, wf.commands as Object[])
            final Iterator wfiter = wf.commands.iterator()
            assert wfiter.hasNext()
            final CommandExec wfitem = wfiter.next()
            assertNotNull(wfitem)
            assertFalse(wfitem instanceof JobExec)
            assertEquals('test command', wfitem.adhocRemoteString)
            assertEquals('-test3 ${option.test3}', wfitem.argString)
        }

    }

    public void testDoValidateOptionsInvalidNoName() {

        def sec = new ScheduledExecutionService()
        if (true) {//invalid options: no name
            def fwkControl = mockFor(FrameworkService, true)
            fwkControl.demand.getFrameworkFromUserSession { session, request -> return null }
            fwkControl.demand.existsFrameworkProject { project, framework ->
                assertEquals 'testProject', project
                return true
            }
            fwkControl.demand.getCommand { project, type, command, framework ->
                assertEquals 'testProject', project
                assertEquals 'aType', type
                assertEquals 'aCommand', command
                return null
            }
            sec.frameworkService = fwkControl.createMock()

            def ms = mockFor(MessageSource)
            ms.demand.getMessage { key, data, locale -> 'message' }
            ms.demand.getMessage { error, locale -> 'message' }
            sec.messageSource = ms.createMock()

            def params = [jobName: 'monkey1', project: 'testProject', description: 'blah', adhocExecution: false, name: 'aResource', type: 'aType', command: 'aCommand',
                    options: ["options[0]": [defaultValue: 'val3', enforced: false, valuesUrl: "http://test.com/test3"]]
            ]
            def results = sec._dovalidate(params, 'test', 'test', null)
            if (results.scheduledExecution.errors.hasErrors()) {
                results.scheduledExecution.errors.allErrors.each {
                    System.err.println(it);
                }
            }
            assertTrue results.failed
            assertNotNull(results.scheduledExecution)
            assertTrue(results.scheduledExecution instanceof ScheduledExecution)
            final ScheduledExecution execution = results.scheduledExecution
            assertNotNull(execution)
            final def org.springframework.validation.Errors errors = execution.errors
            assertNotNull(errors)
            assertTrue(errors.hasErrors())
            assertTrue(errors.hasFieldErrors('options'))
            final Object rejset = errors.getFieldError('options').getRejectedValue()
            assertNotNull(rejset)
            assertLength(1, rejset as Object[])
            final Option rejopt = rejset.iterator().next()
            assertTrue(rejopt.errors.hasErrors())
            assertTrue(rejopt.errors.hasFieldErrors('name'))
        }

    }

    public void testDoValidateOptionsValidationInvalidValuesUrl() {

        def sec = new ScheduledExecutionService()
        if (true) {//invalid options: invalid valuesUrl
            def fwkControl = mockFor(FrameworkService, true)
            fwkControl.demand.getFrameworkFromUserSession { session, request -> return null }
            fwkControl.demand.existsFrameworkProject { project, framework ->
                assertEquals 'testProject', project
                return true
            }
            fwkControl.demand.getCommand { project, type, command, framework ->
                assertEquals 'testProject', project
                assertEquals 'aType', type
                assertEquals 'aCommand', command
                return null
            }
            sec.frameworkService = fwkControl.createMock()

            def ms = mockFor(MessageSource)
            ms.demand.getMessage { key, data, locale -> 'message' }
            ms.demand.getMessage { error, locale -> 'message' }
            sec.messageSource = ms.createMock()

            def params = [jobName: 'monkey1', project: 'testProject', description: 'blah',
                    workflow: [threadcount: 1, keepgoing: true, "commands[0]": [adhocExecution: true, adhocRemoteString: 'a remote string']],
                    options: ["options[0]": [name: 'opt3', defaultValue: 'val3', enforced: false, valuesUrl: "hzttp://test.com/test3"]]
            ]
            def results = sec._dovalidate(params, 'test', 'test', null)
            if (results.scheduledExecution.errors.hasErrors()) {
                results.scheduledExecution.errors.allErrors.each {
                    System.err.println(it);
                }
            }
            assertTrue results.failed
            assertNotNull(results.scheduledExecution)
            assertTrue(results.scheduledExecution instanceof ScheduledExecution)
            final ScheduledExecution execution = results.scheduledExecution
            assertNotNull(execution)
            final def org.springframework.validation.Errors errors = execution.errors
            assertNotNull(errors)
            assertTrue(errors.hasErrors())
            assertTrue(errors.hasFieldErrors('options'))
            final Object rejset = errors.getFieldError('options').getRejectedValue()
            assertNotNull(rejset)
            assertLength(1, rejset as Object[])
            final Option rejopt = rejset.iterator().next()
            assertTrue(rejopt.errors.hasErrors())
            assertTrue(rejopt.errors.hasFieldErrors('valuesUrl'))
        }

    }

    public void testDoValidateOptionsMultivalued() {

        def sec = new ScheduledExecutionService()
      
            def fwkControl = mockFor(FrameworkService, true)
            fwkControl.demand.getFrameworkFromUserSession { session, request -> return null }
            fwkControl.demand.existsFrameworkProject { project, framework ->
                assertEquals 'testProject', project
                return true
            }
            fwkControl.demand.getCommand { project, type, command, framework ->
                assertEquals 'testProject', project
                assertEquals 'aType', type
                assertEquals 'aCommand', command
                return null
            }
            sec.frameworkService = fwkControl.createMock()

            def params = [jobName: 'monkey1', project: 'testProject', description: 'blah',
                    workflow: [threadcount: 1, keepgoing: true, "commands[0]": [adhocExecution: true, adhocRemoteString: 'a remote string']],
                    options: ["options[0]": [name: 'opt3', defaultValue: 'val3', enforced: false, multivalued: true, delimiter: ' ']]
            ]
            def results = sec._dovalidate(params, 'test', 'test', null)
            if (results.scheduledExecution.errors.hasErrors()) {
                results.scheduledExecution.errors.allErrors.each {
                    System.err.println(it);
                }
            }
            assertFalse results.failed
            assertNotNull(results.scheduledExecution)
            assertTrue(results.scheduledExecution instanceof ScheduledExecution)
            ScheduledExecution execution = results.scheduledExecution
            assertNotNull(execution)
            assertNotNull(execution.errors)
            assertFalse(execution.errors.hasErrors())
            assertNotNull execution.options
            assertLength(1, execution.options as Object[])
            final Iterator iterator = execution.options.iterator()
            assert iterator.hasNext()
            final Option next = iterator.next()
            assertNotNull(next)
            assertEquals("wrong option name", "opt3", next.name)
            assertEquals("wrong option name", "val3", next.defaultValue)
            assertNull("wrong option name", next.realValuesUrl)
            assertFalse("wrong option name", next.enforced)
            assertTrue("wrong option name", next.multivalued)
            assertEquals("wrong option name", ' ', next.delimiter)
        

    }

    public void testDoValidateOptionsValidationMultivalued() {

        def sec = new ScheduledExecutionService()
        //invalid multi option, no delimiter
            def fwkControl = mockFor(FrameworkService, true)
            fwkControl.demand.getFrameworkFromUserSession { session, request -> return null }
            fwkControl.demand.existsFrameworkProject { project, framework ->
                assertEquals 'testProject', project
                return true
            }
            fwkControl.demand.getCommand { project, type, command, framework ->
                assertEquals 'testProject', project
                assertEquals 'aType', type
                assertEquals 'aCommand', command
                return null
            }
            sec.frameworkService = fwkControl.createMock()

            def ms = mockFor(MessageSource)
            ms.demand.getMessage { key, data, locale -> 'message' }
            ms.demand.getMessage { error, locale -> 'message' }
            sec.messageSource = ms.createMock()

            def params = [jobName: 'monkey1', project: 'testProject', description: 'blah', adhocExecution: false, name: 'aResource', type: 'aType', command: 'aCommand',
                    options: ["options[0]": [name: 'opt3', defaultValue: 'val3', enforced: false, multivalued: true]]
            ]
            def results = sec._dovalidate(params, 'test', 'test', null)
            if (results.scheduledExecution.errors.hasErrors()) {
                results.scheduledExecution.errors.allErrors.each {
                    System.err.println(it);
                }
            }
            assertTrue results.failed
            assertNotNull(results.scheduledExecution)
            assertTrue(results.scheduledExecution instanceof ScheduledExecution)
            final ScheduledExecution execution = results.scheduledExecution
            assertNotNull(execution)
            final def org.springframework.validation.Errors errors = execution.errors
            assertNotNull(errors)
            assertTrue(errors.hasErrors())
            assertTrue(errors.hasFieldErrors('options'))
            final Object rejset = errors.getFieldError('options').getRejectedValue()
            assertNotNull(rejset)
            assertLength(1, rejset as Object[])
            final Option rejopt = rejset.iterator().next()
            assertTrue(rejopt.errors.hasErrors())
            assertTrue(rejopt.errors.hasFieldErrors('delimiter'))

    }
    /**
     * Multivalued option with default values list and enforced:
     * the default values list should allow a delimiter separated list of valid values
     */
    public void testDoValidateOptions_MultivaluedEnforcedMultipleDefaults_valid() {

        def sec = new ScheduledExecutionService()

        def fwkControl = mockFor(FrameworkService, true)
        fwkControl.demand.getFrameworkFromUserSession { session, request -> return null }
        fwkControl.demand.existsFrameworkProject { project, framework ->
            assertEquals 'testProject', project
            return true
        }
        fwkControl.demand.getCommand { project, type, command, framework ->
            assertEquals 'testProject', project
            assertEquals 'aType', type
            assertEquals 'aCommand', command
            return null
        }
        sec.frameworkService = fwkControl.createMock()

        def ms = mockFor(MessageSource)
        ms.demand.getMessage { key, data, locale -> 'message' }
        ms.demand.getMessage { error, locale -> 'message' }
        sec.messageSource = ms.createMock()

        def optDef = [
                name: 'opt3',
                defaultValue: 'val1,val3',
                enforced: true,
                multivalued: true,
                delimiter: ',',
                values: ['val1', 'val2', 'val3']
        ]

        def params = [jobName: 'monkey1', project: 'testProject', description: 'blah', adhocExecution: false,
                workflow: [threadcount: 1, keepgoing: true, "commands[0]":
                        [adhocExecution: true, adhocRemoteString: 'a remote string']
                ],
                options: ["options[0]": optDef]
        ]
        def results = sec._dovalidate(params, 'test', 'test', null)
        if (results.scheduledExecution.errors.hasErrors()) {
            results.scheduledExecution.errors.allErrors.each {
                System.err.println(it);
            }
        }
        if(results.scheduledExecution.options?.any{it.hasErrors()}){
            results.scheduledExecution.options*.errors?.allErrors?.each {
                System.err.println(it);
            }
        }
        assertFalse results.failed
        assertNotNull(results.scheduledExecution)
        assertTrue(results.scheduledExecution instanceof ScheduledExecution)
        final ScheduledExecution execution = results.scheduledExecution
        assertNotNull(execution)
        final def org.springframework.validation.Errors errors = execution.errors
        assertNotNull(errors)
        assertFalse(errors.hasErrors())
        assertFalse(errors.hasFieldErrors('options'))
    }
    /**
     * multivalued option, default values, enforced values.
     * the default value should be rejected if not all values in it are in the allowed values
     */
    public void testDoValidateOptions_MultivaluedEnforcedMultipleDefaults_invalid() {

        def sec = new ScheduledExecutionService()

        def fwkControl = mockFor(FrameworkService, true)
        fwkControl.demand.getFrameworkFromUserSession { session, request -> return null }
        fwkControl.demand.existsFrameworkProject { project, framework ->
            assertEquals 'testProject', project
            return true
        }
        fwkControl.demand.getCommand { project, type, command, framework ->
            assertEquals 'testProject', project
            assertEquals 'aType', type
            assertEquals 'aCommand', command
            return null
        }
        sec.frameworkService = fwkControl.createMock()

        def ms = mockFor(MessageSource)
        ms.demand.getMessage { key, data, locale -> 'message' }
        ms.demand.getMessage { error, locale -> 'message' }
        sec.messageSource = ms.createMock()

        def optDef = [
                name: 'opt3',
                defaultValue: 'val1,val3,val4', //val4 not in allowed values
                enforced: true,
                multivalued: true,
                delimiter: ',',
                values: ['val1', 'val2', 'val3']
        ]

        def params = [jobName: 'monkey1', project: 'testProject', description: 'blah', adhocExecution: false,
                workflow: [threadcount: 1, keepgoing: true, "commands[0]":
                        [adhocExecution: true, adhocRemoteString: 'a remote string']
                ],
                options: ["options[0]": optDef]
        ]
        def results = sec._dovalidate(params, 'test', 'test', null)
        if (results.scheduledExecution.errors.hasErrors()) {
            results.scheduledExecution.errors.allErrors.each {
                System.err.println(it);
            }
        }
        if (results.scheduledExecution.options?.any { it.hasErrors() }) {
            results.scheduledExecution.options*.errors?.allErrors?.each {
                System.err.println(it);
            }
        }
        assertTrue results.failed
        assertNotNull(results.scheduledExecution)
        assertTrue(results.scheduledExecution instanceof ScheduledExecution)
        final ScheduledExecution execution = results.scheduledExecution
        assertNotNull(execution)
        final def org.springframework.validation.Errors errors = execution.errors
        assertNotNull(errors)
        assertTrue(errors.hasErrors())
        assertTrue(errors.hasFieldErrors('options'))
        final Object rejset = errors.getFieldError('options').getRejectedValue()
        assertNotNull(rejset)
        assertLength(1, rejset as Object[])
        final Option rejopt = rejset.iterator().next()
        assertTrue(rejopt.errors.hasErrors())
        assertTrue(rejopt.errors.hasFieldErrors('defaultValue'))
    }

    public void testDoValidateOptionsInvalidSecureMultivalued() {

        def sec = new ScheduledExecutionService()
        if (true) {//secure option with multi-valued is invalid
            def fwkControl = mockFor(FrameworkService, true)
            fwkControl.demand.getFrameworkFromUserSession { session, request -> return null }
            fwkControl.demand.existsFrameworkProject { project, framework ->
                assertEquals 'testProject', project
                return true
            }
            fwkControl.demand.getCommand { project, type, command, framework ->
                assertEquals 'testProject', project
                assertEquals 'aType', type
                assertEquals 'aCommand', command
                return null
            }
            sec.frameworkService = fwkControl.createMock()

            def ms = mockFor(MessageSource)
            ms.demand.getMessage { key, data, locale -> 'message' }
            ms.demand.getMessage { error, locale -> 'message' }
            sec.messageSource = ms.createMock()

            def params = [jobName: 'monkey1', project: 'testProject', description: 'blah', adhocExecution: false, name: 'aResource', type: 'aType', command: 'aCommand',
                    options: ["options[0]": [name: 'opt3', defaultValue: 'val3', enforced: false, multivalued: true, secureInput: true]]
            ]
            def results = sec._dovalidate(params, 'test', 'test', null)
            if (results.scheduledExecution.errors.hasErrors()) {
                results.scheduledExecution.errors.allErrors.each {
                    System.err.println(it);
                }
            }
            assertTrue results.failed
            assertNotNull(results.scheduledExecution)
            assertTrue(results.scheduledExecution instanceof ScheduledExecution)
            final ScheduledExecution execution = results.scheduledExecution
            assertNotNull(execution)
            final def org.springframework.validation.Errors errors = execution.errors
            assertNotNull(errors)
            assertTrue(errors.hasErrors())
            assertTrue(errors.hasFieldErrors('options'))
            final Object rejset = errors.getFieldError('options').getRejectedValue()
            assertNotNull(rejset)
            assertLength(1, rejset as Object[])
            final Option rejopt = rejset.iterator().next()
            assertTrue(rejopt.errors.hasErrors())
            assertTrue(rejopt.errors.hasFieldErrors('multivalued'))
        }
    }

    public void testDoValidateClusterModeNotEnabledShouldSetNotServerUUID() {
        def testService = new ScheduledExecutionService()
        def fwkControl = mockFor(FrameworkService, true)
        fwkControl.demand.getFrameworkFromUserSession { session, request -> return null }
        fwkControl.demand.existsFrameworkProject { project, framework ->
            assertEquals 'testProject', project
            return true
        }
        fwkControl.demand.getCommand { project, type, command, framework ->
            assertEquals 'testProject', project
            assertEquals 'aType', type
            assertEquals 'aCommand', command
            return null
        }
        testService.frameworkService = fwkControl.createMock()
        testService.frameworkService.metaClass.isClusterModeEnabled = {
            return false
        }
        def params = [jobName: 'monkey1', project: 'testProject', description: 'blah',
                workflow: [threadcount: 1, keepgoing: true, "commands[0]":
                        [adhocExecution: true, adhocRemoteString: 'a remote string']
                ],
                scheduled: true,
                crontabString: '0 21 */4 */4 */6 ? 2010-2040'
        ]
        def results = testService._dovalidate(params, 'test', 'test', null)
        assertFalse(results.failed)
        assertNotNull(results.scheduledExecution)
        assertTrue(results.scheduledExecution instanceof ScheduledExecution)
        final ScheduledExecution execution = results.scheduledExecution
        assertNotNull(execution)
        assertNotNull(execution.errors)
        assertFalse(execution.errors.hasErrors())
        assertNull(execution.serverNodeUUID)
    }

    public void testDoValidateClusterModeIsEnabledShouldSetServerUUID() {
        def testService = new ScheduledExecutionService()
        def fwkControl = mockFor(FrameworkService, true)
        fwkControl.demand.getFrameworkFromUserSession { session, request -> return null }
        fwkControl.demand.existsFrameworkProject { project, framework ->
            assertEquals 'testProject', project
            return true
        }
        fwkControl.demand.getCommand { project, type, command, framework ->
            assertEquals 'testProject', project
            assertEquals 'aType', type
            assertEquals 'aCommand', command
            return null
        }
        testService.frameworkService = fwkControl.createMock()
        testService.frameworkService.metaClass.isClusterModeEnabled = {
            return true
        }
        def uuid = UUID.randomUUID().toString()
        testService.frameworkService.metaClass.getServerUUID = {
            return uuid
        }
        def params = [jobName: 'monkey1', project: 'testProject', description: 'blah',
                workflow: [threadcount: 1, keepgoing: true, "commands[0]":
                        [adhocExecution: true, adhocRemoteString: 'a remote string']
                ],
                scheduled: true,
                crontabString: '0 21 */4 */4 */6 ? 2010-2040'
        ]
        def results = testService._dovalidate(params, 'test', 'test', null)
        assertFalse(results.failed)
        assertNotNull(results.scheduledExecution)
        assertTrue(results.scheduledExecution instanceof ScheduledExecution)
        final ScheduledExecution execution = results.scheduledExecution
        assertNotNull(execution)
        assertNotNull(execution.errors)
        assertFalse(execution.errors.hasErrors())
        assertEquals(uuid, execution.serverNodeUUID)
    }

    public void testDoUpdate() {
        def sec = new ScheduledExecutionService()
            def se = new ScheduledExecution(jobName: 'monkey1', project: 'testProject', description: 'blah')
            se.save()

            assertNotNull se.id

            //try to do update of the ScheduledExecution
            def fwkControl = mockFor(FrameworkService, true)
            fwkControl.demand.getFrameworkFromUserSession { session, request -> return null }
            fwkControl.demand.existsFrameworkProject { project, framework ->
                assertEquals 'testProject2', project
                return true
            }
            fwkControl.demand.getCommand { project, type, command, framework ->
                assertEquals 'testProject2', project
                assertEquals 'aType2', type
                assertEquals 'aCommand2', command
                return null
            }
            fwkControl.demand.authorizeProjectJobAll { framework, resource, actions, project -> return true }
            fwkControl.demand.authorizeProjectJobAll { framework, resource, actions, project -> return true }
            fwkControl.demand.getFrameworkFromUserSession { session, request -> return null }
            fwkControl.demand.getFrameworkFromUserSession { session, request -> return null }
            sec.frameworkService = fwkControl.createMock()
//            def sesControl = mockFor(ScheduledExecutionService, true)
//            sesControl.demand.getByIDorUUID {id -> return se }
//            sec.scheduledExecutionService = sesControl.createMock()

            def params = [id: se.id.toString(), jobName: 'monkey2', project: 'testProject2', description: 'blah',
                    _workflow_data: true,
                    workflow: [threadcount: 1, keepgoing: true, "commands[0]": [adhocExecution: true, adhocRemoteString: 'test command']],
            ]
            def results = sec._doupdate(params, 'test', 'test', null, null)
            def succeeded = results.success
            def scheduledExecution = results.scheduledExecution
            if (scheduledExecution && scheduledExecution.errors.hasErrors()) {
                scheduledExecution.errors.allErrors.each {
                    System.err.println(it);
                }
            }
            assertTrue succeeded
            assertNotNull(scheduledExecution)
            assertTrue(scheduledExecution instanceof ScheduledExecution)
            final ScheduledExecution execution = scheduledExecution
            assertNotNull(execution)
            assertNotNull(execution.errors)
            assertFalse(execution.errors.hasErrors())
            assertEquals 'monkey2', execution.jobName
            assertEquals 'testProject2', execution.project
            assertEquals 'blah', execution.description

            assertNotNull execution.workflow
            assertNotNull execution.workflow.commands
            assertEquals 1, execution.workflow.commands.size()
            def CommandExec cexec = execution.workflow.commands[0]
            assertTrue cexec.adhocExecution
            assertEquals 'test command', cexec.adhocRemoteString
            assertNull cexec.adhocFilepath
            assertNull execution.argString

            assertNull execution.notifications
            assertNull execution.options
    }

    public void testDoUpdateJobNameInvalid() {
        def sec = new ScheduledExecutionService()
        if (true) {//test update job name invalid
            def se = new ScheduledExecution(jobName: 'monkey1', project: 'testProject', description: 'blah', adhocExecution: true, adhocRemoteString: 'test command',)
            se.save()

            assertNotNull se.id

            //try to do update of the ScheduledExecution
            def fwkControl = mockFor(FrameworkService, true)
            fwkControl.demand.getFrameworkFromUserSession { session, request -> return null }
            fwkControl.demand.existsFrameworkProject { project, framework ->
                assertEquals 'testProject2', project
                return true
            }
            fwkControl.demand.getCommand { project, type, command, framework ->
                assertEquals 'testProject2', project
                assertEquals 'aType2', type
                assertEquals 'aCommand2', command
                return null
            }
            fwkControl.demand.authorizeProjectJobAll { framework, resource, actions, project -> return true }
            fwkControl.demand.authorizeProjectJobAll { framework, resource, actions, project -> return true }
            fwkControl.demand.getFrameworkFromUserSession { session, request -> return null }
            fwkControl.demand.getFrameworkFromUserSession { session, request -> return null }
            sec.frameworkService = fwkControl.createMock()
//            def sesControl = mockFor(ScheduledExecutionService, true)
//            sesControl.demand.getByIDorUUID {id -> return se }
//            sec.scheduledExecutionService = sesControl.createMock()

            def params = [id: se.id.toString(), jobName: 'test/monkey2', project: 'testProject2', description: 'blah', adhocExecution: true, adhocRemoteString: 'test command',]
            def results = sec._doupdate(params, 'test', 'test', null, null)
            def succeeded = results.success
            def scheduledExecution = results.scheduledExecution
            if (scheduledExecution && scheduledExecution.errors.hasErrors()) {
                scheduledExecution.errors.allErrors.each {
                    System.err.println(it);
                }
            }
            assertFalse succeeded
            assertNotNull(scheduledExecution)
            assertTrue(scheduledExecution instanceof ScheduledExecution)
            final ScheduledExecution sce = scheduledExecution
            assertNotNull(sce)
            assertNotNull(sce.errors)
            assertTrue sce.errors.hasErrors()
            assertTrue sce.errors.hasFieldErrors('jobName')
        }
    }

    public void testDoUpdateJob() {
        def sec = new ScheduledExecutionService()
        if (true) {//test update basic job details
            def se = new ScheduledExecution(jobName: 'monkey1', project: 'testProject', description: 'blah', adhocExecution: true, adhocRemoteString: 'test command',)
            se.save()

            assertNotNull se.id

            //try to do update of the ScheduledExecution
            def fwkControl = mockFor(FrameworkService, true)
            fwkControl.demand.getFrameworkFromUserSession { session, request -> return null }
            fwkControl.demand.existsFrameworkProject { project, framework ->
                assertEquals 'testProject2', project
                return true
            }
            fwkControl.demand.authorizeProjectJobAll { framework, resource, actions, project -> return true }
            sec.frameworkService = fwkControl.createMock()

            def params = new ScheduledExecution(jobName: 'monkey2', project: 'testProject2', description: 'blah', adhocExecution: true, adhocRemoteString: 'test command',
                    workflow: new Workflow(commands: [new CommandExec(adhocRemoteString: 'test command', adhocExecution: true)])
            )
            def results = sec._doupdateJob(se.id.toString(), params, 'test', 'test', null, null)
            def succeeded = results[0]
            def scheduledExecution = results[1]
            if (scheduledExecution && scheduledExecution.errors.hasErrors()) {
                scheduledExecution.errors.allErrors.each {
                    System.err.println(it);
                }
            }
            assertTrue succeeded
            assertNotNull(scheduledExecution)
            assertTrue(scheduledExecution instanceof ScheduledExecution)
            final ScheduledExecution execution = scheduledExecution
            assertNotNull(execution)
            assertNotNull(execution.errors)
            assertFalse(execution.errors.hasErrors())
            assertEquals 'monkey2', execution.jobName
            assertEquals 'testProject2', execution.project
            assertEquals 'blah', execution.description

            assertNotNull execution.workflow
            assertNotNull execution.workflow.commands
            assertEquals 1, execution.workflow.commands.size()
            def CommandExec cexec = execution.workflow.commands[0]
            assertTrue cexec.adhocExecution
            assertEquals 'test command', cexec.adhocRemoteString
            assertNull cexec.adhocFilepath
            assertNull execution.argString

            assertNull execution.notifications
            assertNull execution.options
        }
        if (true) {//test update basic job details
            def se = new ScheduledExecution(jobName: 'monkey1', project: 'testProject', description: 'blah',)
            se.save()

            assertNotNull se.id

            //try to do update of the ScheduledExecution
            def fwkControl = mockFor(FrameworkService, true)
            fwkControl.demand.getFrameworkFromUserSession { session, request -> return null }
            fwkControl.demand.existsFrameworkProject { project, framework ->
                assertEquals 'testProject2', project
                return true
            }
            fwkControl.demand.authorizeProjectJobAll { framework, resource, actions, project -> return true }
            sec.frameworkService = fwkControl.createMock()

            def params = new ScheduledExecution(jobName: 'test/monkey2', project: 'testProject2', description: 'blah', adhocExecution: true, adhocRemoteString: 'test command',
                    workflow: new Workflow(commands: [new CommandExec(adhocRemoteString: 'test command', adhocExecution: true)])
            )
            def results = sec._doupdateJob(se.id.toString(), params, 'test', 'test', null, null)
            def succeeded = results[0]
            def scheduledExecution = results[1]
            if (scheduledExecution && scheduledExecution.errors.hasErrors()) {
                scheduledExecution.errors.allErrors.each {
                    System.err.println(it);
                }
            }
            assertFalse succeeded
            assertNotNull(scheduledExecution)
            assertTrue(scheduledExecution instanceof ScheduledExecution)
            final ScheduledExecution sce = scheduledExecution
            assertNotNull(sce)
            assertNotNull(sce.errors)
            assertTrue sce.errors.hasErrors()
            assertTrue sce.errors.hasFieldErrors('jobName')
        }
    }


    public void testDoUpdateJobErrorHandlers() {
        def sec = new ScheduledExecutionService()
        if (true) {//test update basic job details
            def se = new ScheduledExecution(jobName: 'monkey1', project: 'testProject', description: 'blah', adhocExecution: true, adhocRemoteString: 'test command',
                    workflow: new Workflow(commands: [new CommandExec(adhocRemoteString: 'test command', adhocExecution: true)]))
            se.save()

            assertNotNull se.id

            //try to do update of the ScheduledExecution
            def fwkControl = mockFor(FrameworkService, true)
            fwkControl.demand.getFrameworkFromUserSession { session, request -> return null }
            fwkControl.demand.existsFrameworkProject { project, framework ->
                assertEquals 'testProject2', project
                return true
            }
            fwkControl.demand.authorizeProjectJobAll { framework, resource, actions, project -> return true }
            sec.frameworkService = fwkControl.createMock()

            def eh1 = new CommandExec(adhocRemoteString: 'err command')
            def params = new ScheduledExecution(jobName: 'monkey2', project: 'testProject2', description: 'blah',
                    workflow: new Workflow(commands: [new CommandExec(adhocRemoteString: 'test command', errorHandler: eh1)])
            )
            def results = sec._doupdateJob(se.id.toString(), params, 'test', 'test', null,null)
            def succeeded = results[0]
            def scheduledExecution = results[1]
            if (scheduledExecution && scheduledExecution.errors.hasErrors()) {
                scheduledExecution.errors.allErrors.each {
                    System.err.println(it);
                }
            }
            assertTrue succeeded
            assertNotNull(scheduledExecution)
            assertTrue(scheduledExecution instanceof ScheduledExecution)
            final ScheduledExecution execution = scheduledExecution
            assertNotNull(execution)
            assertNotNull(execution.errors)
            assertFalse(execution.errors.hasErrors())
            assertEquals 'monkey2', execution.jobName
            assertEquals 'testProject2', execution.project
            assertEquals 'blah', execution.description

            assertNotNull execution.workflow
            assertNotNull execution.workflow.commands
            assertEquals 1, execution.workflow.commands.size()
            def CommandExec cexec = execution.workflow.commands[0]
            assertEquals 'test command', cexec.adhocRemoteString
            assertNotNull cexec.errorHandler

            def CommandExec ehexec = cexec.errorHandler
            assertEquals 'err command', ehexec.adhocRemoteString
        }
    }

    public void testDoUpdateJobErrorHandlersPlugins() {
        def sec = new ScheduledExecutionService()
        if (true) {//test update basic job details
            def se = new ScheduledExecution(jobName: 'monkey1', project: 'testProject', description: 'blah', adhocExecution: true, adhocRemoteString: 'test command',
                    workflow: new Workflow(commands: [new CommandExec(adhocRemoteString: 'test command', adhocExecution: true)]))
            se.save()

            assertNotNull se.id

            //try to do update of the ScheduledExecution
            def fwkControl = mockFor(FrameworkService, true)
            fwkControl.demand.getFrameworkFromUserSession { session, request -> return null }
            fwkControl.demand.existsFrameworkProject { project, framework ->
                assertEquals 'testProject2', project
                return true
            }
            fwkControl.demand.authorizeProjectJobAll { framework, resource, actions, project -> return true }
            sec.frameworkService = fwkControl.createMock()

            def eh1 = new PluginStep(keepgoingOnSuccess: true, type: 'asdf', nodeStep: true, configuration: ["blah": "value"])
            def params = new ScheduledExecution(jobName: 'monkey2', project: 'testProject2', description: 'blah', adhocExecution: true, adhocRemoteString: 'test command',
                    workflow: new Workflow(commands: [new CommandExec(adhocRemoteString: 'test command', errorHandler: eh1)])
            )
            def results = sec._doupdateJob(se.id.toString(), params, 'test', 'test', null, null)
            def succeeded = results[0]
            def scheduledExecution = results[1]
            if (scheduledExecution && scheduledExecution.errors.hasErrors()) {
                scheduledExecution.errors.allErrors.each {
                    System.out.println(it);
                }
            }
            assertTrue succeeded
            assertNotNull(scheduledExecution)
            assertTrue(scheduledExecution instanceof ScheduledExecution)
            final ScheduledExecution execution = scheduledExecution
            assertNotNull(execution)
            assertNotNull(execution.errors)
            assertFalse(execution.errors.hasErrors())
            assertEquals 'monkey2', execution.jobName
            assertEquals 'testProject2', execution.project
            assertEquals 'blah', execution.description

            assertNotNull execution.workflow
            assertNotNull execution.workflow.commands
            assertEquals 1, execution.workflow.commands.size()
            def CommandExec cexec = execution.workflow.commands[0]
            assertEquals 'test command', cexec.adhocRemoteString
            assertNotNull cexec.errorHandler

            def PluginStep ehexec = cexec.errorHandler
            assertTrue("should be true", !!ehexec.keepgoingOnSuccess)
            assertEquals 'asdf', ehexec.type
            assertEquals true, ehexec.nodeStep
            assertEquals([blah: 'value'], ehexec.configuration)
        }
    }

    public void testDoUpdateJobErrorHandlersStepFirst() {
        def sec = new ScheduledExecutionService()
        if (true) {//test update basic job details
            def se = new ScheduledExecution(jobName: 'monkey1', project: 'testProject', description: 'blah', adhocExecution: true, adhocRemoteString: 'test command',
                    workflow: new Workflow(strategy: 'step-first',
                            commands: [
                                    new CommandExec(adhocRemoteString: 'test command', adhocExecution: true),
                                    new CommandExec(adhocRemoteString: 'test command', adhocExecution: true),
                                    new JobExec(jobName: 'test1', jobGroup: 'test'),
                                    new JobExec(jobName: 'test1', jobGroup: 'test'),
                            ]))
            se.save()

            assertNotNull se.id

            //try to do update of the ScheduledExecution
            def fwkControl = mockFor(FrameworkService, true)
            fwkControl.demand.getFrameworkFromUserSession { session, request -> return null }
            fwkControl.demand.existsFrameworkProject { project, framework ->
                assertEquals 'testProject2', project
                return true
            }
            fwkControl.demand.authorizeProjectJobAll { framework, resource, actions, project -> return true }
            sec.frameworkService = fwkControl.createMock()

            def eh1 = new CommandExec(adhocRemoteString: 'err command')
            def eh2 = new CommandExec(adhocRemoteString: 'err command')
            def eh3 = new JobExec(jobGroup: 'eh', jobName: 'eh1')
            def eh4 = new JobExec(jobGroup: 'eh', jobName: 'eh2')
            def params = new ScheduledExecution(jobName: 'monkey2', project: 'testProject2', description: 'blah', adhocExecution: true, adhocRemoteString: 'test command',
                    workflow: new Workflow(strategy: 'step-first',
                            commands: [
                                    new CommandExec(adhocRemoteString: 'test command', adhocExecution: true, errorHandler: eh1),
                                    new CommandExec(adhocRemoteString: 'test command', adhocExecution: true, errorHandler: eh3),
                                    new JobExec(jobName: 'test1', jobGroup: 'test', errorHandler: eh2),
                                    new JobExec(jobName: 'test1', jobGroup: 'test', errorHandler: eh4),
                            ]
                    )
            )
            def results = sec._doupdateJob(se.id.toString(), params, 'test', 'test', null, null)
            def succeeded = results[0]
            assertTrue succeeded

        }
    }

    public void testDoUpdateJobErrorHandlersNodeFirst() {
        def sec = new ScheduledExecutionService()
        if (true) {//test update basic job details
            def se = new ScheduledExecution(jobName: 'monkey1', project: 'testProject', description: 'blah', adhocExecution: true, adhocRemoteString: 'test command',
                    workflow: new Workflow(strategy: 'node-first',
                            commands: [
                                    new CommandExec(adhocRemoteString: 'test command', adhocExecution: true),
                                    new CommandExec(adhocRemoteString: 'test command', adhocExecution: true),
                                    new JobExec(jobName: 'test1', jobGroup: 'test'),
                                    new JobExec(jobName: 'test1', jobGroup: 'test'),
                            ]))
            se.save()

            assertNotNull se.id

            //try to do update of the ScheduledExecution
            def fwkControl = mockFor(FrameworkService, true)
            fwkControl.demand.getFrameworkFromUserSession { session, request -> return null }
            fwkControl.demand.existsFrameworkProject { project, framework ->
                assertEquals 'testProject2', project
                return true
            }
            fwkControl.demand.authorizeProjectJobAll { framework, resource, actions, project -> return true }
            sec.frameworkService = fwkControl.createMock()

            def eh1 = new CommandExec(adhocRemoteString: 'err command')
            def eh2 = new CommandExec(adhocRemoteString: 'err command')
            def eh3 = new JobExec(jobGroup: 'eh', jobName: 'eh1')
            def eh4 = new JobExec(jobGroup: 'eh', jobName: 'eh2')
            def params = new ScheduledExecution(jobName: 'monkey2', project: 'testProject2', description: 'blah', adhocExecution: true, adhocRemoteString: 'test command',
                    workflow: new Workflow(strategy: 'node-first',
                            commands: [
                                    new CommandExec(adhocRemoteString: 'test command', adhocExecution: true, errorHandler: eh1),
                                    new CommandExec(adhocRemoteString: 'test command', adhocExecution: true, errorHandler: eh3),
                                    new JobExec(jobName: 'test1', jobGroup: 'test', errorHandler: eh2),
                                    new JobExec(jobName: 'test1', jobGroup: 'test', errorHandler: eh4),
                            ]
                    )
            )
            def results = sec._doupdateJob(se.id.toString(), params, 'test', 'test', null, null)
            def succeeded = results[0]
            def ScheduledExecution rese = results[1]
            assertFalse succeeded

            assertFalse rese.workflow.commands[0].errors.hasErrors()

            assertTrue rese.workflow.commands[1].errors.hasErrors()
            assertTrue rese.workflow.commands[1].errors.hasFieldErrors('errorHandler')
            assertFalse rese.workflow.commands[2].errors.hasErrors()
            assertFalse rese.workflow.commands[3].errors.hasErrors()

        }
    }

    public void testDoUpdateJobShouldReplaceFilters() {
        def sec = new ScheduledExecutionService()
        if (true) {//test update basic job details
            def se = new ScheduledExecution(jobName: 'monkey1', project: 'testProject', description: 'blah',
                    doNodedispatch: true, nodeInclude: "hostname",
                    workflow: new Workflow(commands: [new CommandExec(adhocRemoteString: 'test command', adhocExecution: true)])
            )
            se.save()

            assertNotNull se.id

            //try to do update of the ScheduledExecution
            def fwkControl = mockFor(FrameworkService, true)
            fwkControl.demand.getFrameworkFromUserSession { session, request -> return null }
            fwkControl.demand.existsFrameworkProject { project, framework ->
                assertEquals 'testProject2', project
                return true
            }
            fwkControl.demand.authorizeProjectJobAll { framework, resource, actions, project -> return true }
            sec.frameworkService = fwkControl.createMock()

            def params = new ScheduledExecution(jobName: 'monkey2', project: 'testProject2', description: 'blah',
                    doNodedispatch: true, nodeIncludeName: "nodename",
                    workflow: new Workflow(commands: [new CommandExec(adhocRemoteString: 'test command', adhocExecution: true)])
            )
            def results = sec._doupdateJob(se.id.toString(), params, 'test', 'test', null, null)
            def succeeded = results[0]
            def scheduledExecution = results[1]
            if (scheduledExecution && scheduledExecution.errors.hasErrors()) {
                scheduledExecution.errors.allErrors.each {
                    System.err.println(it);
                }
            }
            assertTrue succeeded
            assertNotNull(scheduledExecution)
            assertTrue(scheduledExecution instanceof ScheduledExecution)
            final ScheduledExecution execution = scheduledExecution
            assertNotNull(execution)
            assertNotNull(execution.errors)
            assertFalse(execution.errors.hasErrors())
            assertTrue execution.doNodedispatch
            assertEquals "nodename", execution.nodeIncludeName
            assertNull "Filters should have been replaced, but hostname was: ${execution.nodeInclude}", execution.nodeInclude
        }
    }

    public void testDoUpdateJobShouldReplaceNodeThreadcount() {
        def sec = new ScheduledExecutionService()
        if (true) {//test update basic job details
            def se = new ScheduledExecution(jobName: 'monkey1', project: 'testProject', description: 'blah',
                    doNodedispatch: true, nodeInclude: "hostname",
                    workflow: new Workflow(commands: [new CommandExec(adhocRemoteString: 'test command', adhocExecution: true)])
            )
            se.save()

            assertNotNull se.id

            //try to do update of the ScheduledExecution
            def fwkControl = mockFor(FrameworkService, true)
            fwkControl.demand.getFrameworkFromUserSession { session, request -> return null }
            fwkControl.demand.existsFrameworkProject { project, framework ->
                assertEquals 'testProject2', project
                return true
            }
            fwkControl.demand.authorizeProjectJobAll { framework, resource, actions, project -> return true }
            sec.frameworkService = fwkControl.createMock()

            def params = new ScheduledExecution(jobName: 'monkey2', project: 'testProject2', description: 'blah',
                    doNodedispatch: true, nodeIncludeName: "nodename",
                    nodeThreadcount: 3,
                    workflow: new Workflow(commands: [new CommandExec(adhocRemoteString: 'test command', adhocExecution: true)])
            )
            def results = sec._doupdateJob(se.id.toString(), params, 'test', 'test', null, null)
            def succeeded = results[0]
            def scheduledExecution = results[1]
            if (scheduledExecution && scheduledExecution.errors.hasErrors()) {
                scheduledExecution.errors.allErrors.each {
                    System.err.println(it);
                }
            }
            assertTrue succeeded
            assertNotNull(scheduledExecution)
            assertTrue(scheduledExecution instanceof ScheduledExecution)
            final ScheduledExecution execution = scheduledExecution
            assertNotNull(execution)
            assertNotNull(execution.errors)
            assertFalse(execution.errors.hasErrors())
            assertTrue execution.doNodedispatch
            assertEquals 3, execution.nodeThreadcount
            assertEquals "nodename", execution.nodeIncludeName
            assertNull "Filters should have been replaced, but hostname was: ${execution.nodeInclude}", execution.nodeInclude
        }
    }


    public void testDoUpdateJobShouldNotBeBlankNodeThreadcount() {
        def sec = new ScheduledExecutionService()
        if (true) {//test update basic job details
            def se = new ScheduledExecution(jobName: 'monkey1', project: 'testProject', description: 'blah',
                    doNodedispatch: true, nodeInclude: "hostname",
                    nodeThreadcount: 1,
                    workflow: new Workflow(commands: [new CommandExec(adhocRemoteString: 'test command', adhocExecution: true)])
            )
            se.save()

            assertNotNull se.id
            assertNotNull se.nodeThreadcount

            //try to do update of the ScheduledExecution
            def fwkControl = mockFor(FrameworkService, true)
            fwkControl.demand.getFrameworkFromUserSession { session, request -> return null }
            fwkControl.demand.existsFrameworkProject { project, framework ->
                assertEquals 'testProject2', project
                return true
            }
            fwkControl.demand.authorizeProjectJobAll { framework, resource, actions, project -> return true }
            sec.frameworkService = fwkControl.createMock()

            def params = new ScheduledExecution(jobName: 'monkey2', project: 'testProject2', description: 'blah',
                    doNodedispatch: true, nodeIncludeName: "nodename",
                    nodeThreadcount: '',
                    workflow: new Workflow(commands: [new CommandExec(adhocRemoteString: 'test command', adhocExecution: true)])
            )
            def results = sec._doupdateJob(se.id.toString(), params, 'test', 'test', null, null)
            def succeeded = results[0]
            def scheduledExecution = results[1]
            if (scheduledExecution && scheduledExecution.errors.hasErrors()) {
                scheduledExecution.errors.allErrors.each {
                    System.err.println(it);
                }
            }
            assertTrue succeeded
            assertNotNull(scheduledExecution)
            assertTrue(scheduledExecution instanceof ScheduledExecution)
            final ScheduledExecution execution = scheduledExecution
            assertNotNull(execution)
            assertNotNull(execution.errors)
            assertFalse(execution.errors.hasErrors())
            assertTrue execution.doNodedispatch
            assertEquals 1, execution.nodeThreadcount
            assertEquals "nodename", execution.nodeIncludeName
            assertNull "Filters should have been replaced, but hostname was: ${execution.nodeInclude}", execution.nodeInclude
        }
    }


    public void testDoUpdateScheduledOk() {
        //test set scheduled with crontabString
        LinkedHashMap<String, Object> results = assertUpdateCrontabSuccess('0 21 */4 */4 */6 ? 2010-2040')
        final ScheduledExecution execution = results.scheduledExecution
        assertEquals '0', execution.seconds
        assertEquals '21', execution.minute
        assertEquals '*/4', execution.hour
        assertEquals '*/4', execution.dayOfMonth
        assertEquals '*/6', execution.month
        assertEquals '?', execution.dayOfWeek
        assertEquals '2010-2040', execution.year
        assertTrue execution.userRoles.contains('userrole')
        assertTrue execution.userRoles.contains('test')

    }

    public void testDoUpdateScheduledInvalidDayOfMonth() {
        //test set scheduled with invalid crontabString (invalid dayOfMonth/dayOfWeek combo)
        def results = assertUpdateCrontabFailure('0 21 */4 */4 */6 3 2010-2040')

    }

    public void testDoUpdateScheduledInvalidDayOfMonth2() {
        //test set scheduled with invalid crontabString (invalid dayOfMonth/dayOfWeek combo, two ?)
        def results = assertUpdateCrontabFailure('0 21 */4 ? */6 ? 2010-2040')

    }

    public void testDoUpdateScheduledInvalidYearChar() {
        //test set scheduled with invalid crontabString (invalid year char)
        def results = assertUpdateCrontabFailure('0 21 */4 */4 */6 ? z2010-2040')

    }

    public void testDoUpdateScheduledInvalidTooFewComponents() {
        //test set scheduled with invalid crontabString  (too few components)
        def results = assertUpdateCrontabFailure('0 21 */4 */4 */6')

    }

    public void testDoUpdateScheduledInvalidWrongSeconds() {
        //test set scheduled with invalid crontabString  (wrong seconds value)
        def results = assertUpdateCrontabFailure('70 21 */4 */4 */6 ?')

    }

    public void testDoUpdateScheduledInvalidWrongMinutes() {
        //test set scheduled with invalid crontabString  (wrong minutes value)
        def results = assertUpdateCrontabFailure('0 70 */4 */4 */6 ?')

    }

    public void testDoUpdateScheduledInvalidWrongHour() {
        //test set scheduled with invalid crontabString  (wrong hour value)
        def results = assertUpdateCrontabFailure('0 0 25 */4 */6 ?')

    }

    public void testDoUpdateScheduledInvalidWrongDayOfMonth() {
        //test set scheduled with invalid crontabString  (wrong day of month value)
        def results = assertUpdateCrontabFailure('0 0 2 32 */6 ?')

    }

    public void testDoUpdateScheduledInvalidWrongMonth() {
        //test set scheduled with invalid crontabString  (wrong month value)
        def results = assertUpdateCrontabFailure('0 0 2 3 13 ?')

    }

    public void testDoUpdateScheduledInvalidWrongDayOfWeek() {
        //test set scheduled with invalid crontabString  (wrong day of week value)
        def results = assertUpdateCrontabFailure('0 0 2 ? 12 8')
    }

    public void testDoUpdateScheduledInvalidTriggerInPast() {

        //test set scheduled with invalid crontabString  will not fire in future
        def results = assertUpdateCrontabFailure('0 0 2 ? 12 1975')

    }

    public void testDoUpdateScheduledInvalidTriggerInPastNotification() {

        //test set scheduled with invalid crontabString  will not fire in future
        LinkedHashMap<String, Object> results = assertUpdateCrontabFailure('0 0 2 ? 12 1975') { ScheduledExecution se ->
            def na1 = new Notification(eventTrigger: ScheduledExecutionController.ONSUCCESS_TRIGGER_NAME, type: 'email', content: 'c@example.com,d@example.com')
            def na2 = new Notification(eventTrigger: ScheduledExecutionController.ONFAILURE_TRIGGER_NAME, type: 'email', content: 'monkey@example.com')
            se.addToNotifications(na1)
            se.addToNotifications(na2)
            [notifications: [[eventTrigger: ScheduledExecutionController.ONSUCCESS_TRIGGER_NAME, type: 'email', content: 'spaghetti@nowhere.com'], [eventTrigger: ScheduledExecutionController.ONFAILURE_TRIGGER_NAME, type: 'email', content: 'milk@store.com']]]
        }
        ScheduledExecution se = results.scheduledExecution
        assertNotNull(se.notifications)
        assertEquals(2, se.notifications.size())

    }

    public void testDoUpdateScheduledInvalidTriggerInPastRemoveNotification() {

        //test set scheduled with invalid crontabString  will not fire in future
        LinkedHashMap<String, Object> results = assertUpdateCrontabFailure('0 0 2 ? 12 1975') { ScheduledExecution se ->
            def na1 = new Notification(eventTrigger: ScheduledExecutionController.ONSUCCESS_TRIGGER_NAME, type: 'email', content: 'c@example.com,d@example.com')
            def na2 = new Notification(eventTrigger: ScheduledExecutionController.ONFAILURE_TRIGGER_NAME, type: 'email', content: 'monkey@example.com')
            se.addToNotifications(na1)
            se.addToNotifications(na2)
            [notified: 'false']
        }
        ScheduledExecution se = results.scheduledExecution
        assertNotNull(se.notifications)
        assertEquals(2, se.notifications.size())
    }

    private LinkedHashMap<String, Object> assertUpdateCrontabSuccess(String crontabString) {
        def sec = new ScheduledExecutionService()
        def se = new ScheduledExecution(jobName: 'monkey1', project: 'testProject', description: 'blah',
                adhocExecution: false, name: 'aResource', type: 'aType', command: 'aCommand')
        se.save()

        assertNotNull se.id
        assertFalse se.scheduled

        //try to do update of the ScheduledExecution
        def fwkControl = mockFor(FrameworkService, true)
        fwkControl.demand.getFrameworkFromUserSession { session, request -> return null }
        fwkControl.demand.existsFrameworkProject { project, framework ->
            assertEquals 'testProject', project
            return true
        }
        fwkControl.demand.authorizeProjectJobAll { framework, resource, actions, project -> return true }
        fwkControl.demand.getFrameworkFromUserSession { session, request -> return null }
        fwkControl.demand.getFrameworkFromUserSession { session, request -> return null }
        fwkControl.demand.getRundeckBase {-> 'test-base' }
        sec.frameworkService = fwkControl.createMock()
        sec.frameworkService.metaClass.isClusterModeEnabled = {
            return false
        }

        def qtzControl = mockFor(FakeScheduler, true)
        qtzControl.demand.checkExists { key -> false }
        qtzControl.demand.getListenerManager { -> [addJobListener:{a,b->}] }
        qtzControl.demand.scheduleJob { jobDetail, trigger -> new Date() }
        sec.quartzScheduler = qtzControl.createMock()
        def params = [id: se.id.toString(), jobName: 'monkey1', project: 'testProject', description: 'blah',
                workflow: [threadcount: 1, keepgoing: true, "commands[0]": [adhocExecution: true, adhocRemoteString: 'a remote string']],
                _workflow_data: true,
                scheduled: true,
                crontabString: crontabString, useCrontabString: 'true']
        def results = sec._doupdate(params, 'test', 'userrole,test', null, null)
        def succeeded = results.success
        def scheduledExecution = results.scheduledExecution
        if (scheduledExecution && scheduledExecution.errors.hasErrors()) {
            scheduledExecution.errors.allErrors.each {
                System.err.println(it);
            }
        }
        assertTrue succeeded
        assertNotNull(scheduledExecution)
        assertTrue(scheduledExecution instanceof ScheduledExecution)
        final ScheduledExecution execution = scheduledExecution
        assertNotNull(execution)
        assertNotNull(execution.errors)
        assertFalse(execution.errors.hasErrors())
        assertTrue execution.scheduled
        results
    }


    private LinkedHashMap<String, Object> assertUpdateCrontabFailure(String crontabString, Closure jobConfigure = null) {
        def sec = new ScheduledExecutionService()
        def jobMap = [jobName: 'monkey1', project: 'testProject', description: 'blah',]
        def se = new ScheduledExecution(jobMap)
        def extraParams = [:]
        if (jobConfigure) {
            extraParams = jobConfigure.call(se)
        }
        assertNotNull se.save()

        assertNotNull se.id
        assertFalse se.scheduled

        //try to do update of the ScheduledExecution
        def fwkControl = mockFor(FrameworkService, true)
        fwkControl.demand.getFrameworkFromUserSession { session, request -> return null }
        fwkControl.demand.existsFrameworkProject { project, framework ->
            assertEquals 'testProject', project
            return true
        }
        fwkControl.demand.authorizeProjectJobAll { framework, resource, actions, project -> return true }
        fwkControl.demand.getFrameworkFromUserSession { session, request -> return null }
        fwkControl.demand.getFrameworkFromUserSession { session, request -> return null }
        sec.frameworkService = fwkControl.createMock()

        sec.frameworkService.metaClass.isClusterModeEnabled = {
            return false
        }

        def params = [id: se.id.toString(), scheduled: true, crontabString: crontabString, useCrontabString: 'true', jobName: 'monkey1', project: 'testProject', description: 'blah', adhocExecution: false,]
        def results = sec._doupdate(params + (extraParams ?: [:]), 'test', 'test', null, null)

        def succeeded = results.success
        def scheduledExecution = results.scheduledExecution
        if (scheduledExecution && scheduledExecution.errors.hasErrors()) {
            scheduledExecution.errors.allErrors.each {
                System.err.println(it);
            }
        }
        assertFalse succeeded
        assertTrue scheduledExecution.errors.hasErrors()
        assertTrue scheduledExecution.errors.hasFieldErrors('crontabString')

        results
    }

    public void testDoUpdateClusterModeNotEnabledShouldNotSetServerUUID() {
        def sec = new ScheduledExecutionService()
        def se = new ScheduledExecution(jobName: 'monkey1', project: 'testProject', description: 'blah',
                workflow: new Workflow(commands: [new CommandExec(adhocRemoteString: "blah")])
        )
        se.save()

        assertNotNull se.id
        assertFalse se.scheduled

        //try to do update of the ScheduledExecution
        def fwkControl = mockFor(FrameworkService, true)
        fwkControl.demand.getFrameworkFromUserSession { session, request -> return null }
        fwkControl.demand.existsFrameworkProject { project, framework ->
            assertEquals 'testProject', project
            return true
        }
        fwkControl.demand.authorizeProjectJobAll { framework, resource, actions, project -> return true }
        fwkControl.demand.getFrameworkFromUserSession { session, request -> return null }
        fwkControl.demand.getFrameworkFromUserSession { session, request -> return null }
        fwkControl.demand.getRundeckBase {-> 'test-base' }
        def uuid = UUID.randomUUID().toString()
        fwkControl.demand.getServerUUID { uuid }
        sec.frameworkService = fwkControl.createMock()
        sec.frameworkService.metaClass.isClusterModeEnabled = {
            return false
        }

        def qtzControl = mockFor(FakeScheduler, true)
        qtzControl.demand.checkExists { key -> false }
        qtzControl.demand.getListenerManager { -> [addJobListener:{a,b->}] }
        qtzControl.demand.scheduleJob { jobDetail, trigger -> new Date() }
        sec.quartzScheduler = qtzControl.createMock()

        def params = [id: se.id.toString(), jobName: 'monkey1', project: 'testProject', description: 'blah',
                scheduled: true, crontabString: '0 21 */4 */4 */6 ? 2010-2040', useCrontabString: 'true']
        def results = sec._doupdate(params, 'test', 'userrole,test', null, null)
        def succeeded = results.success
        def scheduledExecution = results.scheduledExecution
        if (scheduledExecution && scheduledExecution.errors.hasErrors()) {
            scheduledExecution.errors.allErrors.each {
                System.err.println(it);
            }
        }
        assertTrue succeeded
        assertNotNull(scheduledExecution)
        assertTrue(scheduledExecution instanceof ScheduledExecution)
        final ScheduledExecution execution = scheduledExecution
        assertNotNull(execution)
        assertNotNull(execution.errors)
        assertFalse(execution.errors.hasErrors())
        assertTrue execution.scheduled
        assertNull execution.serverNodeUUID
        assertTrue execution.serverNodeUUID != uuid
    }

    void testDoUpdateClusterModeIsEnabledShouldSetServerUUID() {
        def sec = new ScheduledExecutionService()
        def se = new ScheduledExecution(jobName: 'monkey1', project: 'testProject', description: 'blah',
                workflow: new Workflow(commands: [new CommandExec(adhocRemoteString: "blah")]))
        se.save()

        assertNotNull se.id
        assertFalse se.scheduled

        //try to do update of the ScheduledExecution
        def fwkControl = mockFor(FrameworkService, true)
        fwkControl.demand.getFrameworkFromUserSession { session, request -> return null }
        fwkControl.demand.existsFrameworkProject { project, framework ->
            assertEquals 'testProject', project
            return true
        }
        fwkControl.demand.authorizeProjectJobAll { framework, resource, actions, project -> return true }
        fwkControl.demand.getFrameworkFromUserSession { session, request -> return null }
        fwkControl.demand.getFrameworkFromUserSession { session, request -> return null }
        fwkControl.demand.getRundeckBase {-> 'test-base' }
        def uuid = UUID.randomUUID().toString()
        sec.frameworkService = fwkControl.createMock()
        sec.frameworkService.metaClass.isClusterModeEnabled = {
            return true
        }
        sec.frameworkService.metaClass.getServerUUID = {
            return uuid
        }

        def qtzControl = mockFor(FakeScheduler, true)
        qtzControl.demand.checkExists { key -> false }
        qtzControl.demand.getListenerManager { -> [addJobListener:{a,b->}] }
        qtzControl.demand.scheduleJob { jobDetail, trigger -> new Date() }
        sec.quartzScheduler = qtzControl.createMock()

        def params = [id: se.id.toString(), jobName: 'monkey1', project: 'testProject', description: 'blah',
                scheduled: true, crontabString: '0 21 */4 */4 */6 ? 2010-2040', useCrontabString: 'true']
        def results = sec._doupdate(params, 'test', 'userrole,test', null, null)
        def succeeded = results.success
        def scheduledExecution = results.scheduledExecution
        if (scheduledExecution && scheduledExecution.errors.hasErrors()) {
            scheduledExecution.errors.allErrors.each {
                System.err.println(it);
            }
        }
        println results
        assertTrue succeeded
        assertNotNull(scheduledExecution)
        assertTrue(scheduledExecution instanceof ScheduledExecution)
        final ScheduledExecution execution = scheduledExecution
        assertNotNull(execution)
        assertNotNull(execution.errors)
        assertFalse(execution.errors.hasErrors())
        assertTrue execution.scheduled
        assertNotNull(execution.serverNodeUUID)
        assertTrue execution.serverNodeUUID == uuid
    }

    public void testDoUpdateJobShouldSetCrontabString() {
        def sec = new ScheduledExecutionService()
        //test set scheduled with crontabString
        def se = new ScheduledExecution(jobName: 'monkey1', project: 'testProject', description: 'blah',)
        assertNotNull se.save()

        assertNotNull se.id
        assertFalse se.scheduled

        //try to do update of the ScheduledExecution
        def fwkControl = mockFor(FrameworkService, true)
        fwkControl.demand.getFrameworkFromUserSession { session, request -> return null }
        fwkControl.demand.existsFrameworkProject { project, framework ->
            assertEquals 'testProject', project
            return true
        }
        fwkControl.demand.getCommand { project, type, command, framework ->
            assertEquals 'testProject', project
            assertEquals 'aType', type
            assertEquals 'aCommand', command
            return null
        }
        fwkControl.demand.getRundeckBase {-> 'test-base' }
        sec.frameworkService = fwkControl.createMock()
        sec.frameworkService.metaClass.isClusterModeEnabled = {
            return false
        }
//        def sesControl = mockFor(ScheduledExecutionService, true)
//        sesControl.demand.scheduleJob {schedEx, oldname, oldgroup ->
        //scheduledExecution, renamed ? oldjobname : null, renamed ? oldjobgroup : null
//            assertNotNull(schedEx)
//        }
//        sec.scheduledExecutionService = sesControl.createMock()
//        final subject = new Subject()
//        subject.principals << new Username('test')
//        ['user'].each { group ->
//            subject.principals << new Group(group);
//        }
//        sec.request.setAttribute("subject", subject)

        def qtzControl = mockFor(FakeScheduler, true)
        qtzControl.demand.checkExists { key -> false }
        qtzControl.demand.getListenerManager { -> [addJobListener:{a,b->}] }
        qtzControl.demand.scheduleJob { jobDetail, trigger -> new Date() }
        sec.quartzScheduler = qtzControl.createMock()
        def params = new ScheduledExecution(jobName: 'monkey1', project: 'testProject', description: 'blah',
                workflow: new Workflow(commands: [new CommandExec(adhocRemoteString: 'test command', adhocExecution: true)]),
                scheduled: true, crontabString: '0 21 */4 */4 */6 ? 2010-2040', useCrontabString: 'true')
        assert params.parseCrontabString('0 21 */4 */4 */6 ? 2010-2040')
        def results = sec._doupdateJob(se.id.toString(), params, 'test', 'test', null, null)
        def succeeded = results[0]
        def scheduledExecution = results[1]
        if (scheduledExecution && scheduledExecution.errors.hasErrors()) {
            scheduledExecution.errors.allErrors.each {
                System.err.println(it);
            }
        }
        assertTrue succeeded
        assertNotNull(scheduledExecution)
        assertTrue(scheduledExecution instanceof ScheduledExecution)
        final ScheduledExecution execution = scheduledExecution
        assertNotNull(execution)
        assertNotNull(execution.errors)
        assertFalse(execution.errors.hasErrors())
        assertTrue execution.scheduled
        assertEquals '0', execution.seconds
        assertEquals '21', execution.minute
        assertEquals '*/4', execution.hour
        assertEquals '*/4', execution.dayOfMonth
        assertEquals '*/6', execution.month
        assertEquals '?', execution.dayOfWeek
        assertEquals '2010-2040', execution.year
    }

    public void testDoUpdateJobClusterModeNotEnabledShouldNotSetServerUUID() {
        def sec = new ScheduledExecutionService()
        //test set scheduled with crontabString
        def se = new ScheduledExecution(jobName: 'monkey1', project: 'testProject', description: 'blah',

        )
        se.save()

        assertNotNull se.id
        assertFalse se.scheduled

        //try to do update of the ScheduledExecution
        def fwkControl = mockFor(FrameworkService, true)
        fwkControl.demand.getFrameworkFromUserSession { session, request -> return null }
        fwkControl.demand.existsFrameworkProject { project, framework ->
            assertEquals 'testProject', project
            return true
        }
        fwkControl.demand.getRundeckBase {-> 'test-base' }
        sec.frameworkService = fwkControl.createMock()
        sec.frameworkService.metaClass.isClusterModeEnabled = {
            return false
        }

        def qtzControl = mockFor(FakeScheduler, true)
        qtzControl.demand.checkExists { key -> false }
        qtzControl.demand.getListenerManager { -> [addJobListener:{a,b->}] }
        qtzControl.demand.scheduleJob { jobDetail, trigger -> new Date() }
        sec.quartzScheduler = qtzControl.createMock()
        def params = new ScheduledExecution(jobName: 'monkey1', project: 'testProject', description: 'blah',
                workflow: new Workflow(commands: [new CommandExec(adhocRemoteString: 'test command', adhocExecution: true)]),
                scheduled: true, crontabString: '0 21 */4 */4 */6 ? 2010-2040',
                useCrontabString: 'true')
        def results = sec._doupdateJob(se.id.toString(), params, 'test', 'test', null, null)
        def succeeded = results[0]
        def scheduledExecution = results[1]
        if (scheduledExecution && scheduledExecution.errors.hasErrors()) {
            scheduledExecution.errors.allErrors.each {
                System.err.println(it);
            }
        }
        assertTrue succeeded
        assertNotNull(scheduledExecution)
        assertTrue(scheduledExecution instanceof ScheduledExecution)
        final ScheduledExecution execution = scheduledExecution
        assertNotNull(execution)
        assertNotNull(execution.errors)
        assertFalse(execution.errors.hasErrors())
        assertTrue execution.scheduled
        assertEquals null, execution.serverNodeUUID
    }

    public void testDoUpdateJobClusterModeIsEnabledShouldSetServerUUID() {
        def sec = new ScheduledExecutionService()
        //test set scheduled with crontabString
        def se = new ScheduledExecution(jobName: 'monkey1', project: 'testProject', description: 'blah',

        )
        se.save()

        assertNotNull se.id
        assertFalse se.scheduled

        //try to do update of the ScheduledExecution
        def fwkControl = mockFor(FrameworkService, true)
        fwkControl.demand.getFrameworkFromUserSession { session, request -> return null }
        fwkControl.demand.existsFrameworkProject { project, framework ->
            assertEquals 'testProject', project
            return true
        }
        fwkControl.demand.getRundeckBase {-> 'test-base' }
        sec.frameworkService = fwkControl.createMock()
        sec.frameworkService.metaClass.isClusterModeEnabled = {
            return true
        }
        def uuid = UUID.randomUUID().toString()
        sec.frameworkService.metaClass.getServerUUID = {
            return uuid
        }

        def qtzControl = mockFor(FakeScheduler, true)
        qtzControl.demand.checkExists { key -> false }
        qtzControl.demand.getListenerManager { -> [addJobListener:{a,b->}] }
        qtzControl.demand.scheduleJob { jobDetail, trigger -> new Date() }
        sec.quartzScheduler = qtzControl.createMock()
        def params = new ScheduledExecution(jobName: 'monkey1', project: 'testProject', description: 'blah',
                workflow: new Workflow(commands: [new CommandExec(adhocRemoteString: 'test command', adhocExecution: true)]),
                scheduled: true, crontabString: '0 21 */4 */4 */6 ? 2010-2040',
                useCrontabString: 'true')
        def results = sec._doupdateJob(se.id.toString(), params, 'test', 'test', null, null)
        def succeeded = results[0]
        def scheduledExecution = results[1]
        if (scheduledExecution && scheduledExecution.errors.hasErrors()) {
            scheduledExecution.errors.allErrors.each {
                System.err.println(it);
            }
        }
        assertTrue succeeded
        assertNotNull(scheduledExecution)
        assertTrue(scheduledExecution instanceof ScheduledExecution)
        final ScheduledExecution execution = scheduledExecution
        assertNotNull(execution)
        assertNotNull(execution.errors)
        assertFalse(execution.errors.hasErrors())
        assertTrue execution.scheduled
        assertEquals uuid, execution.serverNodeUUID
    }

    public void testDoUpdateJobShouldNotSetInvalidCrontabString() {
        def sec = new ScheduledExecutionService()
        if (true) {//test set scheduled with invalid crontabString (invalid dayOfMonth/dayOfWeek combo)
            def se = new ScheduledExecution(jobName: 'monkey1', project: 'testProject', description: 'blah', adhocExecution: false, name: 'aResource', type: 'aType', command: 'aCommand')
            se.save()

            assertNotNull se.id
            assertFalse se.scheduled

            //try to do update of the ScheduledExecution
            def fwkControl = mockFor(FrameworkService, true)
            fwkControl.demand.getFrameworkFromUserSession { session, request -> return null }
            fwkControl.demand.existsFrameworkProject { project, framework ->
                assertEquals 'testProject', project
                return true
            }
            fwkControl.demand.getCommand { project, type, command, framework ->
                assertEquals 'testProject', project
                assertEquals 'aType', type
                assertEquals 'aCommand', command
                return null
            }
            sec.frameworkService = fwkControl.createMock()
            sec.frameworkService.metaClass.isClusterModeEnabled = {
                return false
            }
//            def sesControl = mockFor(ScheduledExecutionService, true)
//            sesControl.demand.scheduleJob {schedEx, oldname, oldgroup ->
            //scheduledExecution, renamed ? oldjobname : null, renamed ? oldjobgroup : null
//                assertNotNull(schedEx)
//            }
//            sec.scheduledExecutionService = sesControl.createMock()

            def qtzControl = mockFor(FakeScheduler, true)
            qtzControl.demand.getJobNames { name -> [] }
            qtzControl.demand.scheduleJob { jobDetail, trigger -> new Date() }
            sec.quartzScheduler = qtzControl.createMock()
            def params = new ScheduledExecution(scheduled: true, crontabString: '0 21 */4 */4 */6 3 2010-2040', useCrontabString: 'true', jobName: 'monkey1', project: 'testProject', description: 'blah',
                    dayOfMonth: '*/4', dayOfWeek: '3',
                    workflow: new Workflow(commands: [new CommandExec(adhocRemoteString: 'test command', adhocExecution: true)]))
            params.crontabString= '0 21 */4 */4 */6 3 2010-2040'
            assert null!=params.crontabString
            def results = sec._doupdateJob(se.id.toString(), params, 'test', 'test', null, null)
            def succeeded = results[0]
            def scheduledExecution = results[1]
            if (scheduledExecution && scheduledExecution.errors.hasErrors()) {
                scheduledExecution.errors.allErrors.each {
                    System.err.println(it);
                }
            }
            assertFalse succeeded
            assertTrue scheduledExecution.errors.hasErrors()
            assertTrue scheduledExecution.errors.hasFieldErrors('crontabString')

        }
        if (true) {//test set scheduled with invalid crontabString (invalid dayOfMonth/dayOfWeek combo, two ?)
            def se = new ScheduledExecution(jobName: 'monkey1', project: 'testProject', description: 'blah', adhocExecution: false, name: 'aResource', type: 'aType', command: 'aCommand')
            se.save()

            assertNotNull se.id
            assertFalse se.scheduled

            //try to do update of the ScheduledExecution
            def fwkControl = mockFor(FrameworkService, true)
            fwkControl.demand.getFrameworkFromUserSession { session, request -> return null }
            fwkControl.demand.existsFrameworkProject { project, framework ->
                assertEquals 'testProject', project
                return true
            }
            fwkControl.demand.getCommand { project, type, command, framework ->
                assertEquals 'testProject', project
                assertEquals 'aType', type
                assertEquals 'aCommand', command
                return null
            }
            sec.frameworkService = fwkControl.createMock()
            sec.frameworkService.metaClass.isClusterModeEnabled = {
                return false
            }
//            def sesControl = mockFor(ScheduledExecutionService, true)
//            sesControl.demand.scheduleJob {schedEx, oldname, oldgroup ->
            //scheduledExecution, renamed ? oldjobname : null, renamed ? oldjobgroup : null
//                assertNotNull(schedEx)
//            }
//            sec.scheduledExecutionService = sesControl.createMock()

            def params = new ScheduledExecution(scheduled: true, crontabString: '0 21 */4 ? */6 ? 2010-2040', useCrontabString: 'true', jobName: 'monkey1', project: 'testProject', description: 'blah',
                    workflow: new Workflow(commands: [new CommandExec(adhocRemoteString: 'test command', adhocExecution: true)]))
            def results = sec._doupdateJob(se.id.toString(), params, 'test', 'test', null, null)
            def succeeded = results[0]
            def scheduledExecution = results[1]
            if (scheduledExecution && scheduledExecution.errors.hasErrors()) {
                scheduledExecution.errors.allErrors.each {
                    System.err.println(it);
                }
            }
            assertFalse succeeded
            assertTrue scheduledExecution.errors.hasErrors()
            assertTrue scheduledExecution.errors.hasFieldErrors('crontabString')

        }
        if (true) {//test set scheduled with invalid crontabString (invalid year char)
            def se = new ScheduledExecution(jobName: 'monkey1', project: 'testProject', description: 'blah', adhocExecution: false, name: 'aResource', type: 'aType', command: 'aCommand')
            se.save()

            assertNotNull se.id
            assertFalse se.scheduled

            //try to do update of the ScheduledExecution
            def fwkControl = mockFor(FrameworkService, true)
            fwkControl.demand.getFrameworkFromUserSession { session, request -> return null }
            fwkControl.demand.existsFrameworkProject { project, framework ->
                assertEquals 'testProject', project
                return true
            }
            fwkControl.demand.getCommand { project, type, command, framework ->
                assertEquals 'testProject', project
                assertEquals 'aType', type
                assertEquals 'aCommand', command
                return null
            }
            sec.frameworkService = fwkControl.createMock()
            sec.frameworkService.metaClass.isClusterModeEnabled = {
                return false
            }

            def params = new ScheduledExecution(scheduled: true, crontabString: '0 21 */4 */4 */6 ? z2010-2040', useCrontabString: 'true',
                    workflow: new Workflow(commands: [new CommandExec(adhocRemoteString: 'test command', adhocExecution: true)]))
            def results = sec._doupdateJob(se.id.toString(), params, 'test', 'test', null, null)
            def succeeded = results[0]
            def scheduledExecution = results[1]
            if (scheduledExecution && scheduledExecution.errors.hasErrors()) {
                scheduledExecution.errors.allErrors.each {
                    System.err.println(it);
                }
            }
            assertFalse succeeded
            assertTrue scheduledExecution.errors.hasErrors()
            assertTrue scheduledExecution.errors.hasFieldErrors('crontabString')

        }
        if (true) {//test set scheduled with invalid crontabString  (too few components)
            def se = new ScheduledExecution(jobName: 'monkey1', project: 'testProject', description: 'blah', adhocExecution: false, name: 'aResource', type: 'aType', command: 'aCommand')
            se.save()

            assertNotNull se.id
            assertFalse se.scheduled

            //try to do update of the ScheduledExecution
            def fwkControl = mockFor(FrameworkService, true)
            fwkControl.demand.getFrameworkFromUserSession { session, request -> return null }
            fwkControl.demand.existsFrameworkProject { project, framework ->
                assertEquals 'testProject', project
                return true
            }
            fwkControl.demand.getCommand { project, type, command, framework ->
                assertEquals 'testProject', project
                assertEquals 'aType', type
                assertEquals 'aCommand', command
                return null
            }
            sec.frameworkService = fwkControl.createMock()
            sec.frameworkService.metaClass.isClusterModeEnabled = {
                return false
            }

            def params = new ScheduledExecution(scheduled: true, crontabString: '0 21 */4 */4 */6', useCrontabString: 'true', jobName: 'monkey1', project: 'testProject', description: 'blah',
                    workflow: new Workflow(commands: [new CommandExec(adhocRemoteString: 'test command', adhocExecution: true)]))
            def results = sec._doupdateJob(se.id.toString(), params, 'test', 'test', null, null)
            def succeeded = results[0]
            def scheduledExecution = results[1]
            if (scheduledExecution && scheduledExecution.errors.hasErrors()) {
                scheduledExecution.errors.allErrors.each {
                    System.err.println(it);
                }
            }
            assertFalse succeeded
            assertTrue scheduledExecution.errors.hasErrors()
            assertTrue scheduledExecution.errors.hasFieldErrors('crontabString')

        }
        if (true) {//test set scheduled with invalid crontabString  (wrong seconds value)
            def se = new ScheduledExecution(jobName: 'monkey1', project: 'testProject', description: 'blah', adhocExecution: false, name: 'aResource', type: 'aType', command: 'aCommand')
            se.save()

            assertNotNull se.id
            assertFalse se.scheduled

            //try to do update of the ScheduledExecution
            def fwkControl = mockFor(FrameworkService, true)
            fwkControl.demand.getFrameworkFromUserSession { session, request -> return null }
            fwkControl.demand.existsFrameworkProject { project, framework ->
                assertEquals 'testProject', project
                return true
            }
            fwkControl.demand.getCommand { project, type, command, framework ->
                assertEquals 'testProject', project
                assertEquals 'aType', type
                assertEquals 'aCommand', command
                return null
            }
            sec.frameworkService = fwkControl.createMock()
            sec.frameworkService.metaClass.isClusterModeEnabled = {
                return false
            }

            def params = new ScheduledExecution(scheduled: true, crontabString: '70 21 */4 */4 */6 ?', useCrontabString: 'true', jobName: 'monkey1', project: 'testProject', description: 'blah',
                    workflow: new Workflow(commands: [new CommandExec(adhocRemoteString: 'test command', adhocExecution: true)]))
            def results = sec._doupdateJob(se.id.toString(), params, 'test', 'test', null, null)
            def succeeded = results[0]
            def scheduledExecution = results[1]
            if (scheduledExecution && scheduledExecution.errors.hasErrors()) {
                scheduledExecution.errors.allErrors.each {
                    System.err.println(it);
                }
            }
            assertFalse succeeded
            assertTrue scheduledExecution.errors.hasErrors()
            assertTrue scheduledExecution.errors.hasFieldErrors('crontabString')

        }
        if (true) {//test set scheduled with invalid crontabString  (wrong minutes value)
            def se = new ScheduledExecution(jobName: 'monkey1', project: 'testProject', description: 'blah', adhocExecution: false, name: 'aResource', type: 'aType', command: 'aCommand')
            se.save()

            assertNotNull se.id
            assertFalse se.scheduled

            //try to do update of the ScheduledExecution
            def fwkControl = mockFor(FrameworkService, true)
            fwkControl.demand.getFrameworkFromUserSession { session, request -> return null }
            fwkControl.demand.existsFrameworkProject { project, framework ->
                assertEquals 'testProject', project
                return true
            }
            fwkControl.demand.getCommand { project, type, command, framework ->
                assertEquals 'testProject', project
                assertEquals 'aType', type
                assertEquals 'aCommand', command
                return null
            }
            sec.frameworkService = fwkControl.createMock()
            sec.frameworkService.metaClass.isClusterModeEnabled = {
                return false
            }

            def params = new ScheduledExecution(scheduled: true, crontabString: '0 70 */4 */4 */6 ?', useCrontabString: 'true', jobName: 'monkey1', project: 'testProject', description: 'blah',
                    workflow: new Workflow(commands: [new CommandExec(adhocRemoteString: 'test command', adhocExecution: true)]))
            def results = sec._doupdateJob(se.id.toString(), params, 'test', 'test', null, null)
            def succeeded = results[0]
            def scheduledExecution = results[1]
            if (scheduledExecution && scheduledExecution.errors.hasErrors()) {
                scheduledExecution.errors.allErrors.each {
                    System.err.println(it);
                }
            }
            assertFalse succeeded
            assertTrue scheduledExecution.errors.hasErrors()
            assertTrue scheduledExecution.errors.hasFieldErrors('crontabString')

        }
        if (true) {//test set scheduled with invalid crontabString  (wrong hour value)
            def se = new ScheduledExecution(jobName: 'monkey1', project: 'testProject', description: 'blah', adhocExecution: false, name: 'aResource', type: 'aType', command: 'aCommand')
            se.save()

            assertNotNull se.id
            assertFalse se.scheduled

            //try to do update of the ScheduledExecution
            def fwkControl = mockFor(FrameworkService, true)
            fwkControl.demand.getFrameworkFromUserSession { session, request -> return null }
            fwkControl.demand.existsFrameworkProject { project, framework ->
                assertEquals 'testProject', project
                return true
            }
            fwkControl.demand.getCommand { project, type, command, framework ->
                assertEquals 'testProject', project
                assertEquals 'aType', type
                assertEquals 'aCommand', command
                return null
            }
            sec.frameworkService = fwkControl.createMock()
            sec.frameworkService.metaClass.isClusterModeEnabled = {
                return false
            }

            def params = new ScheduledExecution(scheduled: true, crontabString: '0 0 25 */4 */6 ?', useCrontabString: 'true', jobName: 'monkey1', project: 'testProject', description: 'blah',
                    workflow: new Workflow(commands: [new CommandExec(adhocRemoteString: 'test command', adhocExecution: true)]))
            def results = sec._doupdateJob(se.id.toString(), params, 'test', 'test', null, null)
            def succeeded = results[0]
            def scheduledExecution = results[1]
            if (scheduledExecution && scheduledExecution.errors.hasErrors()) {
                scheduledExecution.errors.allErrors.each {
                    System.err.println(it);
                }
            }
            assertFalse succeeded
            assertTrue scheduledExecution.errors.hasErrors()
            assertTrue scheduledExecution.errors.hasFieldErrors('crontabString')

        }
        if (true) {//test set scheduled with invalid crontabString  (wrong day of month value)
            def se = new ScheduledExecution(jobName: 'monkey1', project: 'testProject', description: 'blah', adhocExecution: false, name: 'aResource', type: 'aType', command: 'aCommand')
            se.save()

            assertNotNull se.id
            assertFalse se.scheduled

            //try to do update of the ScheduledExecution
            def fwkControl = mockFor(FrameworkService, true)
            fwkControl.demand.getFrameworkFromUserSession { session, request -> return null }
            fwkControl.demand.existsFrameworkProject { project, framework ->
                assertEquals 'testProject', project
                return true
            }
            fwkControl.demand.getCommand { project, type, command, framework ->
                assertEquals 'testProject', project
                assertEquals 'aType', type
                assertEquals 'aCommand', command
                return null
            }
            sec.frameworkService = fwkControl.createMock()
            sec.frameworkService.metaClass.isClusterModeEnabled = {
                return false
            }

            def params = new ScheduledExecution(scheduled: true, crontabString: '0 0 2 32 */6 ?', useCrontabString: 'true', jobName: 'monkey1', project: 'testProject', description: 'blah',
                    workflow: new Workflow(commands: [new CommandExec(adhocRemoteString: 'test command', adhocExecution: true)]))
            def results = sec._doupdateJob(se.id.toString(), params, 'test', 'test', null, null)
            def succeeded = results[0]
            def scheduledExecution = results[1]
            if (scheduledExecution && scheduledExecution.errors.hasErrors()) {
                scheduledExecution.errors.allErrors.each {
                    System.err.println(it);
                }
            }
            assertFalse succeeded
            assertTrue scheduledExecution.errors.hasErrors()
            assertTrue scheduledExecution.errors.hasFieldErrors('crontabString')

        }
        if (true) {//test set scheduled with invalid crontabString  (wrong month value)
            def se = new ScheduledExecution(jobName: 'monkey1', project: 'testProject', description: 'blah', adhocExecution: false, name: 'aResource', type: 'aType', command: 'aCommand')
            se.save()

            assertNotNull se.id
            assertFalse se.scheduled

            //try to do update of the ScheduledExecution
            def fwkControl = mockFor(FrameworkService, true)
            fwkControl.demand.getFrameworkFromUserSession { session, request -> return null }
            fwkControl.demand.existsFrameworkProject { project, framework ->
                assertEquals 'testProject', project
                return true
            }
            fwkControl.demand.getCommand { project, type, command, framework ->
                assertEquals 'testProject', project
                assertEquals 'aType', type
                assertEquals 'aCommand', command
                return null
            }
            sec.frameworkService = fwkControl.createMock()
            sec.frameworkService.metaClass.isClusterModeEnabled = {
                return false
            }

            def params = new ScheduledExecution(scheduled: true, crontabString: '0 0 2 3 13 ?', useCrontabString: 'true', jobName: 'monkey1', project: 'testProject', description: 'blah',
                    workflow: new Workflow(commands: [new CommandExec(adhocRemoteString: 'test command', adhocExecution: true)]))
            def results = sec._doupdateJob(se.id.toString(), params, 'test', 'test', null, null)
            def succeeded = results[0]
            def scheduledExecution = results[1]
            if (scheduledExecution && scheduledExecution.errors.hasErrors()) {
                scheduledExecution.errors.allErrors.each {
                    System.err.println(it);
                }
            }
            assertFalse succeeded
            assertTrue scheduledExecution.errors.hasErrors()
            assertTrue scheduledExecution.errors.hasFieldErrors('crontabString')

        }
        if (true) {//test set scheduled with invalid crontabString  (wrong day of week value)
            def se = new ScheduledExecution(jobName: 'monkey1', project: 'testProject', description: 'blah', adhocExecution: false, name: 'aResource', type: 'aType', command: 'aCommand')
            se.save()

            assertNotNull se.id
            assertFalse se.scheduled

            //try to do update of the ScheduledExecution
            def fwkControl = mockFor(FrameworkService, true)
            fwkControl.demand.getFrameworkFromUserSession { session, request -> return null }
            fwkControl.demand.existsFrameworkProject { project, framework ->
                assertEquals 'testProject', project
                return true
            }
            fwkControl.demand.getCommand { project, type, command, framework ->
                assertEquals 'testProject', project
                assertEquals 'aType', type
                assertEquals 'aCommand', command
                return null
            }
            sec.frameworkService = fwkControl.createMock()
            sec.frameworkService.metaClass.isClusterModeEnabled = {
                return false
            }

            def params = new ScheduledExecution(scheduled: true, crontabString: '0 0 2 ? 12 8', useCrontabString: 'true', jobName: 'monkey1', project: 'testProject', description: 'blah',
                    workflow: new Workflow(commands: [new CommandExec(adhocRemoteString: 'test command', adhocExecution: true)]))
            def results = sec._doupdateJob(se.id.toString(), params, 'test', 'test', null, null)
            def succeeded = results[0]
            def scheduledExecution = results[1]
            if (scheduledExecution && scheduledExecution.errors.hasErrors()) {
                scheduledExecution.errors.allErrors.each {
                    System.err.println(it);
                }
            }
            assertFalse succeeded
            assertTrue scheduledExecution.errors.hasErrors()
            assertTrue scheduledExecution.errors.hasFieldErrors('crontabString')

        }
    }

    public void testDoUpdateAdhocFailureEmptyAdhocParams() {
        def sec = new ScheduledExecutionService()
        if (true) {//test failure on empty adhoc params
            def se = new ScheduledExecution(jobName: 'monkey1', project: 'testProject', description: 'blah',
                    adhocExecution: true, adhocRemoteString: 'test remote',
                    command: '', type: '',)
            se.save()

            assertNotNull se.id

            //try to do update of the ScheduledExecution
            def fwkControl = mockFor(FrameworkService, true)
            fwkControl.demand.getFrameworkFromUserSession { session, request -> return null }
            fwkControl.demand.existsFrameworkProject { project, framework ->
                assertEquals 'testProject2', project
                return true
            }
            fwkControl.demand.authorizeProjectJobAll { framework, resource, actions, project -> return true }
            fwkControl.demand.authorizeProjectJobAll { framework, resource, actions, project -> return true }
            fwkControl.demand.getFrameworkFromUserSession { session, request -> return null }
            fwkControl.demand.getFrameworkFromUserSession { session, request -> return null }
            sec.frameworkService = fwkControl.createMock()

//            def sesControl = mockFor(ScheduledExecutionService, true)
//            sesControl.demand.getByIDorUUID {id -> return se }
//            sec.scheduledExecutionService = sesControl.createMock()

            def params = [id: se.id.toString(), jobName: 'monkey2', project: 'testProject2', description: 'blah',
                    workflow: [threadcount: 1, keepgoing: true, "commands[0]": [adhocExecution: true, adhocRemoteString: '']],
                    _workflow_data: true,
                    ]
            def results = sec._doupdate(params, 'test', 'test', null, null)
            def succeeded = results.success
            def scheduledExecution = results.scheduledExecution
            if (scheduledExecution && scheduledExecution.errors.hasErrors()) {
                scheduledExecution.errors.allErrors.each {
                    System.err.println(it);
                }
            }
            assertFalse succeeded
            assertNotNull(scheduledExecution)
            assertTrue(scheduledExecution instanceof ScheduledExecution)
            final ScheduledExecution execution = scheduledExecution
            assertNotNull(execution)
            assertNotNull(execution.errors)
            assertTrue(execution.errors.hasErrors())
            assertTrue(execution.errors.hasFieldErrors())
            assertTrue(execution.errors.hasFieldErrors('workflow'))
            assertNotNull execution.workflow
            assertNotNull execution.workflow.commands
            assertEquals 1, execution.workflow.commands.size()
            def exec = execution.workflow.commands[0]
            assertTrue exec.errors.hasErrors()
            assertTrue exec.errors.hasFieldErrors('adhocExecution')

        }

    }

    public void testDoUpdateChangeWorkflow() {
        def sec = new ScheduledExecutionService()
        if (true) {//test update from one adhoc type to another; remote -> local
            def se = new ScheduledExecution(jobName: 'monkey1', project: 'testProject', description: 'blah',
                    workflow: new Workflow(threadcount: 1, keepgoing: true, commands: [
                            new CommandExec(adhocExecution: true, adhocRemoteString: 'test remote')
                    ]),
                )
            se.save()

            assertNotNull se.id

            //try to do update of the ScheduledExecution
            def fwkControl = mockFor(FrameworkService, true)
            fwkControl.demand.getFrameworkFromUserSession { session, request -> return null }
            fwkControl.demand.existsFrameworkProject { project, framework ->
                assertEquals 'testProject2', project
                return true
            }
            fwkControl.demand.authorizeProjectJobAll { framework, resource, actions, project -> return true }
            fwkControl.demand.authorizeProjectJobAll { framework, resource, actions, project -> return true }
            fwkControl.demand.getFrameworkFromUserSession { session, request -> return null }
            fwkControl.demand.getFrameworkFromUserSession { session, request -> return null }
            sec.frameworkService = fwkControl.createMock()

//            def sesControl = mockFor(ScheduledExecutionService, true)
//            sesControl.demand.getByIDorUUID {id -> return se }
//            sec.scheduledExecutionService = sesControl.createMock()

            def params = [id: se.id.toString(), jobName: 'monkey2', project: 'testProject2', description: 'blah',
                    workflow: [threadcount: 1, keepgoing: true, "commands[0]": [adhocExecution: true, adhocLocalString: 'test local']],
                    _workflow_data: true,
            ]
            def results = sec._doupdate(params, 'test', 'test', null, null)
            def succeeded = results.success
            def scheduledExecution = results.scheduledExecution
            if (scheduledExecution && scheduledExecution.errors.hasErrors()) {
                scheduledExecution.errors.allErrors.each {
                    System.err.println(it);
                }
            }
            assertTrue succeeded
            assertNotNull(scheduledExecution)
            assertTrue(scheduledExecution instanceof ScheduledExecution)
            final ScheduledExecution execution = scheduledExecution
            assertNotNull(execution)
            assertNotNull(execution.errors)
            assertFalse(execution.errors.hasErrors())
            assertEquals 'monkey2', execution.jobName
            assertEquals 'testProject2', execution.project
            assertEquals 'blah', execution.description
            assertNotNull execution.workflow
            assertNotNull execution.workflow.commands
            assertEquals 1, execution.workflow.commands.size()
            def CommandExec cexec = execution.workflow.commands[0]
            assertTrue cexec.adhocExecution
            assertNull cexec.adhocFilepath
            assertEquals 'test local', cexec.adhocLocalString
            assertNull cexec.adhocRemoteString
            assertNull cexec.argString

            assertNull execution.notifications
            assertNull execution.options
        }

    }

    public void testDoUpdateJobShouldntSetEmptyAdhocItem() {
        def sec = new ScheduledExecutionService()
        //test failure on empty adhoc params
        def se = new ScheduledExecution(jobName: 'monkey1', project: 'testProject', description: 'blah',
                adhocExecution: true, adhocRemoteString: 'test remote',
                command: '', type: '',)
        se.save()

        assertNotNull se.id

        //try to do update of the ScheduledExecution
        def fwkControl = mockFor(FrameworkService, true)
        fwkControl.demand.getFrameworkFromUserSession { session, request -> return null }
        fwkControl.demand.existsFrameworkProject { project, framework ->
            assertEquals 'testProject2', project
            return true
        }
        fwkControl.demand.authorizeProjectJobAll { framework, resource, actions, project -> return true }
        sec.frameworkService = fwkControl.createMock()

        def params = new ScheduledExecution(jobName: 'monkey2', project: 'testProject2', description: 'blah',
                workflow: new Workflow(commands: [new CommandExec(adhocExecution: 'true', adhocRemoteString: '')])
        )
        def results = sec._doupdateJob(se.id.toString(), params, 'test', 'test', null, null)
        def succeeded = results[0]
        def scheduledExecution = results[1]
        if (scheduledExecution && scheduledExecution.errors.hasErrors()) {
            scheduledExecution.errors.allErrors.each {
                System.err.println(it);
            }
        }
        assertFalse succeeded
        assertNotNull(scheduledExecution)
        assertTrue(scheduledExecution instanceof ScheduledExecution)
        final ScheduledExecution execution = scheduledExecution
        assertNotNull(execution)
        assertNotNull(execution.errors)
        assertTrue(execution.errors.hasErrors())
        assertTrue(execution.errors.hasFieldErrors())
        assertTrue(execution.errors.hasFieldErrors('workflow'))
        assertNotNull execution.workflow
        assertNotNull execution.workflow.commands
        assertEquals 1, execution.workflow.commands.size()
        def exec = execution.workflow.commands[0]
        assertTrue exec.errors.hasErrors()
        assertTrue exec.errors.hasFieldErrors('adhocExecution')

    }

    public void testDoUpdateJobShouldChangeWorkflow() {
        def sec = new ScheduledExecutionService()
        //test update from one adhoc type to another; file -> remote
        def se = new ScheduledExecution(jobName: 'monkey1', project: 'testProject', description: 'blah',
                workflow: new Workflow(commands: [new CommandExec(adhocExecution: 'true', adhocLocalString: 'test local'),
                        new CommandExec(adhocExecution: 'true', adhocFilepath: 'test file'),
                        new JobExec(jobName: 'a name', jobGroup: 'a group', argString: 'whatever')])
        )
        se.save()

        assertNotNull se.id

        //try to do update of the ScheduledExecution
        def fwkControl = mockFor(FrameworkService, true)
        fwkControl.demand.getFrameworkFromUserSession { session, request -> return null }
        fwkControl.demand.existsFrameworkProject { project, framework ->
            assertEquals 'testProject2', project
            return true
        }
        fwkControl.demand.authorizeProjectJobAll { framework, resource, actions, project -> return true }
        sec.frameworkService = fwkControl.createMock()

        def params = new ScheduledExecution(jobName: 'monkey2', project: 'testProject2', description: 'blah',
                workflow: new Workflow(commands: [new CommandExec(adhocExecution: 'true', adhocRemoteString: 'test remote')])
        )
        def results = sec._doupdateJob(se.id.toString(), params, 'test', 'test', null, null)
        def succeeded = results[0]
        def scheduledExecution = results[1]
        if (scheduledExecution && scheduledExecution.errors.hasErrors()) {
            scheduledExecution.errors.allErrors.each {
                System.err.println(it);
            }
        }
        assertTrue succeeded
        assertNotNull(scheduledExecution)
        assertTrue(scheduledExecution instanceof ScheduledExecution)
        final ScheduledExecution execution = scheduledExecution
        assertNotNull(execution)
        assertNotNull(execution.errors)
        assertFalse(execution.errors.hasErrors())
        assertEquals 'monkey2', execution.jobName
        assertEquals 'testProject2', execution.project
        assertEquals 'blah', execution.description
        assertNotNull execution.workflow
        assertNotNull execution.workflow.commands
        assertEquals 1, execution.workflow.commands.size()
        def CommandExec cexec = execution.workflow.commands[0]
        assertTrue cexec.adhocExecution
        assertNull cexec.adhocFilepath
        assertEquals 'test remote', cexec.adhocRemoteString
        assertNull cexec.adhocLocalString
        assertNull cexec.argString
        assertNull execution.argString
        assertNull execution.notifications
        assertNull execution.options
    }

    public void testDoUpdateNotificationsAddOnsuccess() {
        def sec = new ScheduledExecutionService()
        //test update job, add onsuccess
        def se = new ScheduledExecution(jobName: 'monkey1', project: 'testProject', description: 'blah2', adhocExecution: true, adhocRemoteString: 'test command',)
        def na1 = new Notification(eventTrigger: ScheduledExecutionController.ONSUCCESS_TRIGGER_NAME, type: 'email', content: 'c@example.com,d@example.com')
        def na2 = new Notification(eventTrigger: ScheduledExecutionController.ONFAILURE_TRIGGER_NAME, type: 'email', content: 'monkey@example.com')
        se.addToNotifications(na1)
        se.addToNotifications(na2)
        se.save()

        assertNotNull se.id

        //try to do update of the ScheduledExecution
        def fwkControl = mockFor(FrameworkService, true)
        fwkControl.demand.getFrameworkFromUserSession { session, request -> return null }
        fwkControl.demand.existsFrameworkProject { project, framework ->
            assertEquals 'testProject', project
            return true
        }
        fwkControl.demand.getCommand { project, type, command, framework ->
            assertEquals 'testProject', project
            assertEquals 'aType', type
            assertEquals 'aCommand', command
            return null
        }
        fwkControl.demand.authorizeProjectJobAll { framework, resource, actions, project -> return true }
        fwkControl.demand.getFrameworkFromUserSession { session, request -> return null }
        fwkControl.demand.getFrameworkFromUserSession { session, request -> return null }
        sec.frameworkService = fwkControl.createMock()

//            def sesControl = mockFor(ScheduledExecutionService, true)
//            sesControl.demand.getByIDorUUID {id -> return se }
//            sec.scheduledExecutionService = sesControl.createMock()

        def params = [id: se.id.toString(), jobName: 'monkey1', project: 'testProject', description: 'blah2',
                workflow: [threadcount: 1, keepgoing: true, "commands[0]": [adhocExecution: true, adhocRemoteString: 'test command']],
                _workflow_data: true,
                notified: 'true',
                (ScheduledExecutionController.NOTIFY_ONSUCCESS_EMAIL): 'true',
                (ScheduledExecutionController.NOTIFY_SUCCESS_RECIPIENTS): 'spaghetti@nowhere.com',
        ]
        def results = sec._doupdate(params, 'test', 'test', null, null)
        def succeeded = results.success
        def scheduledExecution = results.scheduledExecution
        if (scheduledExecution && scheduledExecution.errors.hasErrors()) {
            scheduledExecution.errors.allErrors.each {
                System.err.println(it);
            }
        }
        assertTrue succeeded
        assertNotNull(scheduledExecution)
        assertTrue(scheduledExecution instanceof ScheduledExecution)
        final ScheduledExecution execution = scheduledExecution
        assertNotNull(execution)
        assertNotNull(execution.errors)
        assertFalse(execution.errors.hasErrors())
        assertEquals 'monkey1', execution.jobName
        assertEquals 'testProject', execution.project
        assertEquals 'blah2', execution.description
        assertNotNull execution.workflow
        assertNotNull execution.workflow.commands
        assertEquals 1, execution.workflow.commands.size()
        def CommandExec cexec = execution.workflow.commands[0]
        assertTrue cexec.adhocExecution
        assertEquals 'test command', cexec.adhocRemoteString
        assertNull cexec.adhocFilepath
        assertNull execution.argString

        assertNotNull execution.notifications
        assertEquals 1, execution.notifications.size()
        def nmap = [:]
        execution.notifications.each { not1 ->
            nmap[not1.eventTrigger] = not1
        }
        assertNotNull(nmap[ScheduledExecutionController.ONSUCCESS_TRIGGER_NAME])
        assertNull(nmap[ScheduledExecutionController.ONFAILURE_TRIGGER_NAME])
        assertTrue(nmap[ScheduledExecutionController.ONSUCCESS_TRIGGER_NAME] instanceof Notification)
        def Notification n2 = nmap[ScheduledExecutionController.ONSUCCESS_TRIGGER_NAME]
        assertEquals ScheduledExecutionController.ONSUCCESS_TRIGGER_NAME, n2.eventTrigger
        assertEquals "email", n2.type
        assertEquals "spaghetti@nowhere.com", n2.mailConfiguration().recipients
    }

    public void testDoUpdateNotificationsShouldUpdateOnSuccess() {
        assertUpdateNotifications([notified: 'true',
                (ScheduledExecutionController.NOTIFY_SUCCESS_RECIPIENTS): 'spaghetti@nowhere.com',
                (ScheduledExecutionController.NOTIFY_ONSUCCESS_EMAIL): 'true',
                (ScheduledExecutionController.NOTIFY_FAILURE_RECIPIENTS): 'milk@store.com'
        ]) { ScheduledExecution execution ->

            assertEquals 1, execution.notifications.size()
            def nmap = [:]
            execution.notifications.each { not1 ->
                nmap[not1.eventTrigger] = not1
            }
            assertNotNull(nmap[ScheduledExecutionController.ONSUCCESS_TRIGGER_NAME])
            assertNull(nmap[ScheduledExecutionController.ONFAILURE_TRIGGER_NAME])
            assertNull(nmap[ScheduledExecutionController.ONSTART_TRIGGER_NAME])
            assertTrue(nmap[ScheduledExecutionController.ONSUCCESS_TRIGGER_NAME] instanceof Notification)
            def Notification n2 = nmap[ScheduledExecutionController.ONSUCCESS_TRIGGER_NAME]
            assertEquals ScheduledExecutionController.ONSUCCESS_TRIGGER_NAME, n2.eventTrigger
            assertEquals "email", n2.type
            assertEquals "spaghetti@nowhere.com", n2.mailConfiguration().recipients
        }
    }

    public void testDoUpdateNotificationsShouldUpdateOnFailure() {
        assertUpdateNotifications([notified: 'true',
                (ScheduledExecutionController.NOTIFY_ONFAILURE_EMAIL): 'true',
                (ScheduledExecutionController.NOTIFY_FAILURE_RECIPIENTS): 'milk@store.com'
        ]) { ScheduledExecution execution ->

            assertEquals 1, execution.notifications.size()
            def nmap = [:]
            execution.notifications.each { not1 ->
                nmap[not1.eventTrigger] = not1
            }
            assertNotNull(nmap[ScheduledExecutionController.ONFAILURE_TRIGGER_NAME])
            assertNull(nmap[ScheduledExecutionController.ONSUCCESS_TRIGGER_NAME])
            assertNull(nmap[ScheduledExecutionController.ONSTART_TRIGGER_NAME])
            assertTrue(nmap[ScheduledExecutionController.ONFAILURE_TRIGGER_NAME] instanceof Notification)
            def Notification n2 = nmap[ScheduledExecutionController.ONFAILURE_TRIGGER_NAME]
            assertEquals ScheduledExecutionController.ONFAILURE_TRIGGER_NAME, n2.eventTrigger
            assertEquals "email", n2.type
            assertEquals "milk@store.com", n2.mailConfiguration().recipients
        }
    }

    public void testDoUpdateNotificationsShouldUpdateOnStart() {
        assertUpdateNotifications([notified: 'true',
                (ScheduledExecutionController.NOTIFY_ONSTART_EMAIL): 'true',
                (ScheduledExecutionController.NOTIFY_START_RECIPIENTS): 'avbdf@zzdf.com'
        ]) { ScheduledExecution execution ->

            assertEquals 1, execution.notifications.size()
            def nmap = [:]
            execution.notifications.each { not1 ->
                nmap[not1.eventTrigger] = not1
            }
            assertNotNull(nmap[ScheduledExecutionController.ONSTART_TRIGGER_NAME])
            assertNull(nmap[ScheduledExecutionController.ONSUCCESS_TRIGGER_NAME])
            assertNull(nmap[ScheduledExecutionController.ONFAILURE_TRIGGER_NAME])
            assertTrue(nmap[ScheduledExecutionController.ONSTART_TRIGGER_NAME] instanceof Notification)
            def Notification n2 = nmap[ScheduledExecutionController.ONSTART_TRIGGER_NAME]
            assertEquals ScheduledExecutionController.ONSTART_TRIGGER_NAME, n2.eventTrigger
            assertEquals "email", n2.type
            assertEquals "avbdf@zzdf.com", n2.mailConfiguration().recipients
        }
    }

    private void assertUpdateNotifications(LinkedHashMap<String, String> inputParams, Closure tests = null) {
        def sec = new ScheduledExecutionService()
        //test update job  notifications, disabling onsuccess
        def se = new ScheduledExecution(jobName: 'monkey1', project: 'testProject', description: 'blah2',
                adhocExecution: true, adhocRemoteString: 'test command',)
        se.save()

        assertNotNull se.id

        //try to do update of the ScheduledExecution
        def fwkControl = mockFor(FrameworkService, true)
        fwkControl.demand.getFrameworkFromUserSession { session, request -> return null }
        fwkControl.demand.existsFrameworkProject { project, framework ->
            assertEquals 'testProject', project
            return true
        }
        fwkControl.demand.getCommand { project, type, command, framework ->
            assertEquals 'testProject', project
            assertEquals 'aType', type
            assertEquals 'aCommand', command
            return null
        }
        fwkControl.demand.authorizeProjectJobAll { framework, resource, actions, project -> return true }
        fwkControl.demand.getFrameworkFromUserSession { session, request -> return null }
        fwkControl.demand.getFrameworkFromUserSession { session, request -> return null }
        sec.frameworkService = fwkControl.createMock()

        def params = [id: se.id.toString(), jobName: 'monkey1', project: 'testProject', description: 'blah2',
                workflow: [threadcount: 1, keepgoing: true, "commands[0]": [adhocExecution: true, adhocRemoteString: 'test command']],
                _workflow_data: true,
        ] + inputParams
        def results = sec._doupdate(params, 'test', 'test', null, null)
        def succeeded = results.success
        def scheduledExecution = results.scheduledExecution
        if (scheduledExecution && scheduledExecution.errors.hasErrors()) {
            scheduledExecution.errors.allErrors.each {
                System.err.println(it);
            }
        }
        assertTrue succeeded
        assertNotNull(scheduledExecution)
        assertTrue(scheduledExecution instanceof ScheduledExecution)
        final ScheduledExecution execution = scheduledExecution
        assertNotNull(execution)
        assertNotNull(execution.errors)
        assertFalse(execution.errors.hasErrors())
        assertEquals 'monkey1', execution.jobName
        assertEquals 'testProject', execution.project
        assertEquals 'blah2', execution.description
        assertNotNull execution.workflow
        assertNotNull execution.workflow.commands
        assertEquals 1, execution.workflow.commands.size()
        def CommandExec cexec = execution.workflow.commands[0]
        assertTrue cexec.adhocExecution
        assertEquals 'test command', cexec.adhocRemoteString
        assertNull cexec.adhocFilepath
        assertNull execution.argString

        assertNotNull execution.notifications
        if (tests) {
            tests.call(execution)
        }
    }

    public void testDoUpdateNotificationsReplace() {
        def sec = new ScheduledExecutionService()
        if (true) {//test update job  notifications, replacing onsuccess, and removing onfailure
            def se = new ScheduledExecution(jobName: 'monkey1', project: 'testProject', description: 'blah2', adhocExecution: true, adhocRemoteString: 'test command',)
            def na1 = new Notification(eventTrigger: ScheduledExecutionController.ONSUCCESS_TRIGGER_NAME, type: 'email', content: 'c@example.com,d@example.com')
            def na2 = new Notification(eventTrigger: ScheduledExecutionController.ONFAILURE_TRIGGER_NAME, type: 'email', content: 'monkey@example.com')
            se.addToNotifications(na1)
            se.addToNotifications(na2)
            se.save()

            assertNotNull se.id

            //try to do update of the ScheduledExecution
            def fwkControl = mockFor(FrameworkService, true)
            fwkControl.demand.getFrameworkFromUserSession { session, request -> return null }
            fwkControl.demand.existsFrameworkProject { project, framework ->
                assertEquals 'testProject', project
                return true
            }
            fwkControl.demand.getCommand { project, type, command, framework ->
                assertEquals 'testProject', project
                assertEquals 'aType', type
                assertEquals 'aCommand', command
                return null
            }
            fwkControl.demand.authorizeProjectJobAll { framework, resource, actions, project -> return true }
            fwkControl.demand.getFrameworkFromUserSession { session, request -> return null }
            fwkControl.demand.getFrameworkFromUserSession { session, request -> return null }
            sec.frameworkService = fwkControl.createMock()

//            def sesControl = mockFor(ScheduledExecutionService, true)
//            sesControl.demand.getByIDorUUID {id -> return se }
//            sec.scheduledExecutionService = sesControl.createMock()

            def params = [id: se.id.toString(), jobName: 'monkey1', project: 'testProject', description: 'blah2',
                    workflow: [threadcount: 1, keepgoing: true, "commands[0]": [adhocExecution: true, adhocRemoteString: 'test command']],
                    _workflow_data: true,
                    notified: 'true',
                    (ScheduledExecutionController.NOTIFY_ONSUCCESS_URL): 'true', (ScheduledExecutionController.NOTIFY_SUCCESS_URL): 'http://example.com',
                    (ScheduledExecutionController.NOTIFY_FAILURE_RECIPIENTS): 'milk@store.com',
            ]
            def results = sec._doupdate(params, 'test', 'test', null, null)
            def succeeded = results.success
            def scheduledExecution = results.scheduledExecution
            if (scheduledExecution && scheduledExecution.errors.hasErrors()) {
                scheduledExecution.errors.allErrors.each {
                    System.err.println(it);
                }
            }
            assertTrue succeeded
            assertNotNull(scheduledExecution)
            assertTrue(scheduledExecution instanceof ScheduledExecution)
            final ScheduledExecution execution = scheduledExecution
            assertNotNull(execution)
            assertNotNull(execution.errors)
            assertFalse(execution.errors.hasErrors())
            assertEquals 'monkey1', execution.jobName
            assertEquals 'testProject', execution.project
            assertEquals 'blah2', execution.description
            assertNotNull execution.workflow
            assertNotNull execution.workflow.commands
            assertEquals 1, execution.workflow.commands.size()
            def CommandExec cexec = execution.workflow.commands[0]
            assertTrue cexec.adhocExecution
            assertEquals 'test command', cexec.adhocRemoteString
            assertNull cexec.adhocFilepath
            assertNull execution.argString

            assertNotNull execution.notifications
            assertEquals 1, execution.notifications.size()
            def nmap = [:]
            execution.notifications.each { not1 ->
                nmap[not1.eventTrigger] = not1
            }
            assertNotNull(nmap[ScheduledExecutionController.ONSUCCESS_TRIGGER_NAME])
            assertNull(nmap[ScheduledExecutionController.ONFAILURE_TRIGGER_NAME])
            assertTrue(nmap[ScheduledExecutionController.ONSUCCESS_TRIGGER_NAME] instanceof Notification)
            def Notification n2 = nmap[ScheduledExecutionController.ONSUCCESS_TRIGGER_NAME]
            assertEquals ScheduledExecutionController.ONSUCCESS_TRIGGER_NAME, n2.eventTrigger
            assertEquals "url", n2.type
            assertEquals "http://example.com", n2.content
        }

    }

    public void testDoUpdateNotificationsReplaceRemoveAll() {
        def sec = new ScheduledExecutionService()
        if (true) {//test update job  notifications, removing all notifications
            def se = new ScheduledExecution(jobName: 'monkey1', project: 'testProject', description: 'blah2',
                    workflow: new Workflow(threadcount: 1, keepgoing: true, commands:
                            [new CommandExec(adhocExecution: true, adhocRemoteString: 'a remote string')]
                    )
            )
            def na1 = new Notification(eventTrigger: ScheduledExecutionController.ONSUCCESS_TRIGGER_NAME, type: 'email', content: 'c@example.com,d@example.com')
            def na2 = new Notification(eventTrigger: ScheduledExecutionController.ONFAILURE_TRIGGER_NAME, type: 'email', content: 'monkey@example.com')
            se.addToNotifications(na1)
            se.addToNotifications(na2)
            se.save()

            assertNotNull se.id

            //try to do update of the ScheduledExecution
            def fwkControl = mockFor(FrameworkService, true)
            fwkControl.demand.getFrameworkFromUserSession { session, request -> return null }
            fwkControl.demand.existsFrameworkProject { project, framework ->
                assertEquals 'testProject', project
                return true
            }
            fwkControl.demand.getCommand { project, type, command, framework ->
                assertEquals 'testProject', project
                assertEquals 'aType', type
                assertEquals 'aCommand', command
                return null
            }
            fwkControl.demand.authorizeProjectJobAll { framework, resource, actions, project -> return true }
            fwkControl.demand.getFrameworkFromUserSession { session, request -> return null }
            fwkControl.demand.getFrameworkFromUserSession { session, request -> return null }
            sec.frameworkService = fwkControl.createMock()

//            def sesControl = mockFor(ScheduledExecutionService, true)
//            sesControl.demand.getByIDorUUID {id -> return se }
//            sec.scheduledExecutionService = sesControl.createMock()

            def params = [id: se.id.toString(), jobName: 'monkey1', project: 'testProject', description: 'blah2',
                    workflow: [threadcount: 1, keepgoing: true, "commands[0]": [adhocExecution: true, adhocRemoteString: 'test command']],
                    _workflow_data: true,
                    notified: 'false',
                    (ScheduledExecutionController.NOTIFY_ONSUCCESS_URL): 'true', (ScheduledExecutionController.NOTIFY_SUCCESS_URL): 'http://example.com',
                    (ScheduledExecutionController.NOTIFY_FAILURE_RECIPIENTS): 'milk@store.com',
            ]
            def results = sec._doupdate(params, 'test', 'test', null, null)
            def succeeded = results.success
            def scheduledExecution = results.scheduledExecution
            if (scheduledExecution && scheduledExecution.errors.hasErrors()) {
                scheduledExecution.errors.allErrors.each {
                    System.err.println(it);
                }
            }
            assertTrue succeeded
            assertNotNull(scheduledExecution)
            assertTrue(scheduledExecution instanceof ScheduledExecution)
            final ScheduledExecution execution = scheduledExecution
            assertNotNull(execution)
            assertNotNull(execution.errors)
            assertFalse(execution.errors.hasErrors())
            assertEquals 'monkey1', execution.jobName
            assertEquals 'testProject', execution.project
            assertEquals 'blah2', execution.description
            assertNotNull execution.workflow
            assertNotNull execution.workflow.commands
            assertEquals 1, execution.workflow.commands.size()
            def CommandExec cexec = execution.workflow.commands[0]
            assertTrue cexec.adhocExecution
            assertEquals 'test command', cexec.adhocRemoteString
            assertNull cexec.adhocFilepath
            assertNull execution.argString

            assertTrue(execution.notifications == null || execution.notifications.size() == 0)
        }

    }

    public void testDoUpdateNotificationsReplaceRemoveAll2() {
        def sec = new ScheduledExecutionService()
        if (true) {//test update job  notifications, removing all notifications by unchecking
            def se = new ScheduledExecution(jobName: 'monkey1', project: 'testProject', description: 'blah2',
                    workflow: new Workflow(threadcount: 1, keepgoing: true, commands:
                            [new CommandExec(adhocExecution: true, adhocRemoteString: 'a remote string')]
                    )
            )
            def na1 = new Notification(eventTrigger: ScheduledExecutionController.ONSUCCESS_TRIGGER_NAME, type: 'email', content: 'c@example.com,d@example.com')
            def na2 = new Notification(eventTrigger: ScheduledExecutionController.ONFAILURE_TRIGGER_NAME, type: 'email', content: 'monkey@example.com')
            se.addToNotifications(na1)
            se.addToNotifications(na2)
            se.save()

            assertNotNull se.id

            //try to do update of the ScheduledExecution
            def fwkControl = mockFor(FrameworkService, true)
            fwkControl.demand.getFrameworkFromUserSession { session, request -> return null }
            fwkControl.demand.existsFrameworkProject { project, framework ->
                assertEquals 'testProject', project
                return true
            }
            fwkControl.demand.getCommand { project, type, command, framework ->
                assertEquals 'testProject', project
                assertEquals 'aType', type
                assertEquals 'aCommand', command
                return null
            }
            fwkControl.demand.authorizeProjectJobAll { framework, resource, actions, project -> return true }
            fwkControl.demand.getFrameworkFromUserSession { session, request -> return null }
            fwkControl.demand.getFrameworkFromUserSession { session, request -> return null }
            sec.frameworkService = fwkControl.createMock()

//            def sesControl = mockFor(ScheduledExecutionService, true)
//            sesControl.demand.getByIDorUUID {id -> return se }
//            sec.scheduledExecutionService = sesControl.createMock()

            def params = [id: se.id.toString(), jobName: 'monkey1', project: 'testProject', description: 'blah2',
                    workflow: [threadcount: 1, keepgoing: true, "commands[0]": [adhocExecution: true, adhocRemoteString: 'test command']],
                    _workflow_data: true,
                    notified: 'true',
                    (ScheduledExecutionController.NOTIFY_SUCCESS_URL): 'http://example.com',
                    (ScheduledExecutionController.NOTIFY_FAILURE_RECIPIENTS): 'milk@store.com',
            ]
            def results = sec._doupdate(params, 'test', 'test', null, null)
            def succeeded = results.success
            def scheduledExecution = results.scheduledExecution
            if (scheduledExecution && scheduledExecution.errors.hasErrors()) {
                scheduledExecution.errors.allErrors.each {
                    System.err.println(it);
                }
            }
            assertTrue succeeded
            assertNotNull(scheduledExecution)
            assertTrue(scheduledExecution instanceof ScheduledExecution)
            final ScheduledExecution execution = scheduledExecution
            assertNotNull(execution)
            assertNotNull(execution.errors)
            assertFalse(execution.errors.hasErrors())
            assertEquals 'monkey1', execution.jobName
            assertEquals 'testProject', execution.project
            assertEquals 'blah2', execution.description
            assertNotNull execution.workflow
            assertNotNull execution.workflow.commands
            assertEquals 1, execution.workflow.commands.size()
            def CommandExec cexec = execution.workflow.commands[0]
            assertTrue cexec.adhocExecution
            assertEquals 'test command', cexec.adhocRemoteString
            assertNull cexec.adhocFilepath
            assertNull execution.argString

            assertTrue(execution.notifications == null || execution.notifications.size() == 0)
        }
    }

    public void testDoUpdateNotifications() {
        def sec = new ScheduledExecutionService()
        if (true) {//test update job  notifications
            def se = new ScheduledExecution(jobName: 'monkey1', project: 'testProject', description: 'blah',
                    workflow: new Workflow(threadcount: 1, keepgoing: true, commands:
                            [new CommandExec(adhocExecution: true, adhocRemoteString: 'a remote string')]
                    )
            )
            def na1 = new Notification(eventTrigger: ScheduledExecutionController.ONSUCCESS_TRIGGER_NAME, type: 'email', content: 'c@example.com,d@example.com')
            def na2 = new Notification(eventTrigger: ScheduledExecutionController.ONFAILURE_TRIGGER_NAME, type: 'email', content: 'monkey@example.com')
            se.addToNotifications(na1)
            se.addToNotifications(na2)
            se.save()

            assertNotNull se.id

            //try to do update of the ScheduledExecution
            def fwkControl = mockFor(FrameworkService, true)
            fwkControl.demand.getFrameworkFromUserSession { session, request -> return null }
            fwkControl.demand.existsFrameworkProject { project, framework ->
                assertEquals 'testProject', project
                return true
            }
            fwkControl.demand.getCommand { project, type, command, framework ->
                assertEquals 'testProject', project
                assertEquals 'aType', type
                assertEquals 'aCommand', command
                return null
            }
            fwkControl.demand.authorizeProjectJobAll { framework, resource, actions, project -> return true }
            fwkControl.demand.getFrameworkFromUserSession { session, request -> return null }
            fwkControl.demand.getFrameworkFromUserSession { session, request -> return null }
            sec.frameworkService = fwkControl.createMock()

//            def sesControl = mockFor(ScheduledExecutionService, true)
//            sesControl.demand.getByIDorUUID {id -> return se }
//            sec.scheduledExecutionService = sesControl.createMock()

            def params = [id: se.id.toString(), jobName: 'monkey1', project: 'testProject', description: 'blah',
                    workflow: [threadcount: 1, keepgoing: true, "commands[0]": [adhocExecution: true, adhocRemoteString: 'test command']],
                    _workflow_data: true,
                    notifications: [[eventTrigger: ScheduledExecutionController.ONSUCCESS_TRIGGER_NAME, type: 'email', content: 'spaghetti@nowhere.com'], [eventTrigger: ScheduledExecutionController.ONFAILURE_TRIGGER_NAME, type: 'email', content: 'milk@store.com']]
            ]
            def results = sec._doupdate(params, 'test', 'test', null, null)
            def succeeded = results.success
            def scheduledExecution = results.scheduledExecution
            if (scheduledExecution && scheduledExecution.errors.hasErrors()) {
                scheduledExecution.errors.allErrors.each {
                    System.err.println(it);
                }
            }
            assertTrue succeeded
            assertNotNull(scheduledExecution)
            assertTrue(scheduledExecution instanceof ScheduledExecution)
            final ScheduledExecution execution = scheduledExecution
            assertNotNull(execution)
            assertNotNull(execution.errors)
            assertFalse(execution.errors.hasErrors())
            assertEquals 'monkey1', execution.jobName
            assertEquals 'testProject', execution.project
            assertEquals 'blah', execution.description

            assertNotNull execution.workflow
            assertNotNull execution.workflow.commands
            assertEquals 1, execution.workflow.commands.size()
            def CommandExec cexec = execution.workflow.commands[0]
            assertTrue cexec.adhocExecution
            assertEquals 'test command', cexec.adhocRemoteString
            assertNull cexec.adhocFilepath
            assertNull execution.argString

            assertNotNull execution.notifications
            assertEquals 2, execution.notifications.size()
            def nmap = [:]
            execution.notifications.each { not1 ->
                nmap[not1.eventTrigger] = not1
            }
            assertNotNull(nmap[ScheduledExecutionController.ONSUCCESS_TRIGGER_NAME])
            assertNotNull(nmap[ScheduledExecutionController.ONFAILURE_TRIGGER_NAME])
            assertTrue(nmap[ScheduledExecutionController.ONSUCCESS_TRIGGER_NAME] instanceof Notification)
            assertTrue(nmap[ScheduledExecutionController.ONFAILURE_TRIGGER_NAME] instanceof Notification)
            def Notification n = nmap[ScheduledExecutionController.ONSUCCESS_TRIGGER_NAME]
            assertEquals ScheduledExecutionController.ONSUCCESS_TRIGGER_NAME, n.eventTrigger
            assertEquals "email", n.type
            assertEquals "spaghetti@nowhere.com", n.mailConfiguration().recipients
            def Notification n2 = nmap[ScheduledExecutionController.ONFAILURE_TRIGGER_NAME]
            assertEquals ScheduledExecutionController.ONFAILURE_TRIGGER_NAME, n2.eventTrigger
            assertEquals "email", n2.type
            assertEquals "milk@store.com", n2.mailConfiguration().recipients
        }

        if (true) {//test update job  notifications, using form input parameters
            def se = new ScheduledExecution(jobName: 'monkey1', project: 'testProject', description: 'blah', adhocExecution: true, adhocRemoteString: 'test command',)
            def na1 = new Notification(eventTrigger: ScheduledExecutionController.ONSUCCESS_TRIGGER_NAME, type: 'email', content: 'c@example.com,d@example.com')
            def na2 = new Notification(eventTrigger: ScheduledExecutionController.ONFAILURE_TRIGGER_NAME, type: 'email', content: 'monkey@example.com')
            se.addToNotifications(na1)
            se.addToNotifications(na2)
            se.save()

            assertNotNull se.id

            //try to do update of the ScheduledExecution
            def fwkControl = mockFor(FrameworkService, true)
            fwkControl.demand.getFrameworkFromUserSession { session, request -> return null }
            fwkControl.demand.existsFrameworkProject { project, framework ->
                assertEquals 'testProject', project
                return true
            }
            fwkControl.demand.getCommand { project, type, command, framework ->
                assertEquals 'testProject', project
                assertEquals 'aType', type
                assertEquals 'aCommand', command
                return null
            }
            fwkControl.demand.authorizeProjectJobAll { framework, resource, actions, project -> return true }
            fwkControl.demand.getFrameworkFromUserSession { session, request -> return null }
            fwkControl.demand.getFrameworkFromUserSession { session, request -> return null }
            sec.frameworkService = fwkControl.createMock()

//            def sesControl = mockFor(ScheduledExecutionService, true)
//            sesControl.demand.getByIDorUUID {id -> return se }
//            sec.scheduledExecutionService = sesControl.createMock()

            def params = [id: se.id.toString(), jobName: 'monkey1', project: 'testProject', description: 'blah',
                    workflow: [threadcount: 1, keepgoing: true, "commands[0]": [adhocExecution: true, adhocRemoteString: 'test command']],
                    _workflow_data: true,
                    notified: 'true',
                    (ScheduledExecutionController.NOTIFY_ONSUCCESS_EMAIL): 'true', (ScheduledExecutionController.NOTIFY_SUCCESS_RECIPIENTS): 'spaghetti@nowhere.com',
                    (ScheduledExecutionController.NOTIFY_ONFAILURE_EMAIL): 'true', (ScheduledExecutionController.NOTIFY_FAILURE_RECIPIENTS): 'milk@store.com',
            ]
            def results = sec._doupdate(params, 'test', 'test', null, null)
            def succeeded = results.success
            def scheduledExecution = results.scheduledExecution
            if (scheduledExecution && scheduledExecution.errors.hasErrors()) {
                scheduledExecution.errors.allErrors.each {
                    System.err.println(it);
                }
            }
            assertTrue succeeded
            assertNotNull(scheduledExecution)
            assertTrue(scheduledExecution instanceof ScheduledExecution)
            final ScheduledExecution execution = scheduledExecution
            assertNotNull(execution)
            assertNotNull(execution.errors)
            assertFalse(execution.errors.hasErrors())
            assertEquals 'monkey1', execution.jobName
            assertEquals 'testProject', execution.project
            assertEquals 'blah', execution.description
            assertNotNull execution.workflow
            assertNotNull execution.workflow.commands
            assertEquals 1, execution.workflow.commands.size()
            def CommandExec cexec = execution.workflow.commands[0]
            assertTrue cexec.adhocExecution
            assertEquals 'test command', cexec.adhocRemoteString
            assertNull cexec.adhocFilepath
            assertNull execution.argString

            assertNotNull execution.notifications
            assertEquals 2, execution.notifications.size()
            def nmap = [:]
            execution.notifications.each { not1 ->
                nmap[not1.eventTrigger] = not1
            }
            assertNotNull(nmap[ScheduledExecutionController.ONSUCCESS_TRIGGER_NAME])
            assertNotNull(nmap[ScheduledExecutionController.ONFAILURE_TRIGGER_NAME])
            assertTrue(nmap[ScheduledExecutionController.ONSUCCESS_TRIGGER_NAME] instanceof Notification)
            assertTrue(nmap[ScheduledExecutionController.ONFAILURE_TRIGGER_NAME] instanceof Notification)
            def Notification n = nmap[ScheduledExecutionController.ONSUCCESS_TRIGGER_NAME]
            assertEquals ScheduledExecutionController.ONSUCCESS_TRIGGER_NAME, n.eventTrigger
            assertEquals "email", n.type
            assertEquals "spaghetti@nowhere.com", n.mailConfiguration().recipients
            def Notification n2 = nmap[ScheduledExecutionController.ONFAILURE_TRIGGER_NAME]
            assertEquals ScheduledExecutionController.ONFAILURE_TRIGGER_NAME, n2.eventTrigger
            assertEquals "email", n2.type
            assertEquals "milk@store.com", n2.mailConfiguration().recipients
        }
        if (true) {//test update job  notifications, using form input parameters, invalid email addresses
            def se = new ScheduledExecution(jobName: 'monkey1', project: 'testProject', description: 'blah', adhocExecution: true, adhocRemoteString: 'test command',)
            def na1 = new Notification(eventTrigger: ScheduledExecutionController.ONSUCCESS_TRIGGER_NAME, type: 'email', content: 'c@example.com,d@example.com')
            def na2 = new Notification(eventTrigger: ScheduledExecutionController.ONFAILURE_TRIGGER_NAME, type: 'email', content: 'monkey@example.com')
            se.addToNotifications(na1)
            se.addToNotifications(na2)
            se.save(flush: true)

            assertNotNull se.id

            //try to do update of the ScheduledExecution
            def fwkControl = mockFor(FrameworkService, true)
            fwkControl.demand.getFrameworkFromUserSession { session, request -> return null }
            fwkControl.demand.existsFrameworkProject { project, framework ->
                assertEquals 'testProject', project
                return true
            }
            fwkControl.demand.getCommand { project, type, command, framework ->
                assertEquals 'testProject', project
                assertEquals 'aType', type
                assertEquals 'aCommand', command
                return null
            }
            fwkControl.demand.authorizeProjectJobAll { framework, resource, actions, project -> return true }
            fwkControl.demand.getFrameworkFromUserSession { session, request -> return null }
            fwkControl.demand.getFrameworkFromUserSession { session, request -> return null }
            sec.frameworkService = fwkControl.createMock()

//            def sesControl = mockFor(ScheduledExecutionService, true)
//            sesControl.demand.getByIDorUUID {id -> return se }
//            sec.scheduledExecutionService = sesControl.createMock()

            def params = [id: se.id.toString(), jobName: 'monkey1', project: 'testProject', description: 'blah',
                    (ScheduledExecutionController.NOTIFY_ONSUCCESS_EMAIL): 'true', (ScheduledExecutionController.NOTIFY_SUCCESS_RECIPIENTS): 'spaghetti@ nowhere.com',
                    (ScheduledExecutionController.NOTIFY_ONFAILURE_EMAIL): 'true', (ScheduledExecutionController.NOTIFY_FAILURE_RECIPIENTS): 'milkstore.com',
            ]
            def results = sec._doupdate(params, 'test', 'test', null, null)
            def succeeded = results.success
            def scheduledExecution = results.scheduledExecution
            if (scheduledExecution && scheduledExecution.errors.hasErrors()) {
                scheduledExecution.errors.allErrors.each {
                    System.err.println(it);
                }
            }
            assertFalse succeeded
            assertNotNull(scheduledExecution)
            assertTrue(scheduledExecution instanceof ScheduledExecution)
            final ScheduledExecution executionErr = scheduledExecution
            assertNotNull executionErr
            assertNotNull(executionErr.errors)
            assertTrue(executionErr.errors.hasErrors())
            assertTrue(executionErr.errors.hasFieldErrors(ScheduledExecutionController.NOTIFY_FAILURE_RECIPIENTS))
            assertTrue(executionErr.errors.hasFieldErrors(ScheduledExecutionController.NOTIFY_SUCCESS_RECIPIENTS))

            final ScheduledExecution execution = ScheduledExecution.get(scheduledExecution.id)

            assertEquals 'monkey1', execution.jobName
            assertEquals 'testProject', execution.project
            assertEquals 'blah', execution.description
            assertNotNull execution.notifications
            assertEquals 2, execution.notifications.size()
            def nmap = [:]
            execution.notifications.each { not1 ->
                nmap[not1.eventTrigger] = not1
            }
            assertNotNull(nmap[ScheduledExecutionController.ONSUCCESS_TRIGGER_NAME])
            assertNotNull(nmap[ScheduledExecutionController.ONFAILURE_TRIGGER_NAME])
            assertTrue(nmap[ScheduledExecutionController.ONSUCCESS_TRIGGER_NAME] instanceof Notification)
            assertTrue(nmap[ScheduledExecutionController.ONFAILURE_TRIGGER_NAME] instanceof Notification)
            def Notification n = nmap[ScheduledExecutionController.ONSUCCESS_TRIGGER_NAME]
            assertEquals ScheduledExecutionController.ONSUCCESS_TRIGGER_NAME, n.eventTrigger
            assertEquals "email", n.type
            assertEquals "c@example.com,d@example.com", n.mailConfiguration().recipients
            def Notification n2 = nmap[ScheduledExecutionController.ONFAILURE_TRIGGER_NAME]
            assertEquals ScheduledExecutionController.ONFAILURE_TRIGGER_NAME, n2.eventTrigger
            assertEquals "email", n2.type
            assertEquals "monkey@example.com", n2.mailConfiguration().recipients
        }
    }

    public void testDoUpdateJobShouldUpdateNotifications() {
        def sec = new ScheduledExecutionService()
        if (true) {//test update job  notifications
            def se = new ScheduledExecution(jobName: 'monkey1', project: 'testProject', description: 'blah',
                    workflow: new Workflow(commands: [new CommandExec(adhocExecution: true, adhocRemoteString: 'test command',)])
            )
            def na1 = new Notification(eventTrigger: ScheduledExecutionController.ONSUCCESS_TRIGGER_NAME, type: 'email', content: 'c@example.com,d@example.com')
            def na2 = new Notification(eventTrigger: ScheduledExecutionController.ONFAILURE_TRIGGER_NAME, type: 'email', content: 'monkey@example.com')
            se.addToNotifications(na1)
            se.addToNotifications(na2)
            se.save()

            assertNotNull se.id

            //try to do update of the ScheduledExecution
            def fwkControl = mockFor(FrameworkService, true)
            fwkControl.demand.getFrameworkFromUserSession { session, request -> return null }
            fwkControl.demand.existsFrameworkProject { project, framework ->
                assertEquals 'testProject', project
                return true
            }
            fwkControl.demand.getCommand { project, type, command, framework ->
                assertEquals 'testProject', project
                assertEquals 'aType', type
                assertEquals 'aCommand', command
                return null
            }
            fwkControl.demand.authorizeProjectJobAll { framework, resource, actions, project -> return true }
            fwkControl.demand.getFrameworkFromUserSession { session, request -> return null }
            fwkControl.demand.getFrameworkFromUserSession { session, request -> return null }
            sec.frameworkService = fwkControl.createMock()

            def params = new ScheduledExecution(jobName: 'monkey1', project: 'testProject', description: 'blah', adhocExecution: true, adhocRemoteString: 'test command',
                    notifications: [new Notification(eventTrigger: ScheduledExecutionController.ONSUCCESS_TRIGGER_NAME, type: 'email', content: 'spaghetti@nowhere.com'),
                            new Notification(eventTrigger: ScheduledExecutionController.ONFAILURE_TRIGGER_NAME, type: 'email', content: 'milk@store.com')
                    ])
            def results = sec._doupdateJob(se.id, params, 'test', 'test', null, null)
            def succeeded = results[0]
            def scheduledExecution = results[1]
            if (scheduledExecution && scheduledExecution.errors.hasErrors()) {
                scheduledExecution.errors.allErrors.each {
                    System.err.println(it);
                }
            }
            assertTrue succeeded
            assertNotNull(scheduledExecution)
            assertTrue(scheduledExecution instanceof ScheduledExecution)
            final ScheduledExecution execution = scheduledExecution
            assertNotNull(execution)
            assertNotNull(execution.errors)
            assertFalse(execution.errors.hasErrors())
            assertEquals 'monkey1', execution.jobName
            assertEquals 'testProject', execution.project
            assertEquals 'blah', execution.description

            assertNotNull execution.workflow
            assertNotNull execution.workflow.commands
            assertEquals 1, execution.workflow.commands.size()
            def CommandExec cexec = execution.workflow.commands[0]
            assertTrue cexec.adhocExecution
            assertEquals 'test command', cexec.adhocRemoteString
            assertNull cexec.adhocFilepath
            assertNull execution.argString

            assertNotNull execution.notifications
            assertEquals 2, execution.notifications.size()
            def nmap = [:]
            execution.notifications.each { not1 ->
                nmap[not1.eventTrigger] = not1
            }
            assertNotNull(nmap[ScheduledExecutionController.ONSUCCESS_TRIGGER_NAME])
            assertNotNull(nmap[ScheduledExecutionController.ONFAILURE_TRIGGER_NAME])
            assertTrue(nmap[ScheduledExecutionController.ONSUCCESS_TRIGGER_NAME] instanceof Notification)
            assertTrue(nmap[ScheduledExecutionController.ONFAILURE_TRIGGER_NAME] instanceof Notification)
            def Notification n = nmap[ScheduledExecutionController.ONSUCCESS_TRIGGER_NAME]
            assertEquals ScheduledExecutionController.ONSUCCESS_TRIGGER_NAME, n.eventTrigger
            assertEquals "email", n.type
            assertEquals "spaghetti@nowhere.com", n.mailConfiguration().recipients
            def Notification n2 = nmap[ScheduledExecutionController.ONFAILURE_TRIGGER_NAME]
            assertEquals ScheduledExecutionController.ONFAILURE_TRIGGER_NAME, n2.eventTrigger
            assertEquals "email", n2.type
            assertEquals "milk@store.com", n2.mailConfiguration().recipients
        }
    }

    public void testDoUpdateJobShouldFailBadNotifications() {
        def sec = new ScheduledExecutionService()
        if (true) {//test update job  notifications, invalid email addresses
            def se = new ScheduledExecution(jobName: 'monkey1', project: 'testProject', description: 'blah', adhocExecution: true, adhocRemoteString: 'test command',)
            def na1 = new Notification(eventTrigger: ScheduledExecutionController.ONSUCCESS_TRIGGER_NAME, type: 'email', content: 'c@example.com,d@example.com')
            def na2 = new Notification(eventTrigger: ScheduledExecutionController.ONFAILURE_TRIGGER_NAME, type: 'email', content: 'monkey@example.com')
            se.addToNotifications(na1)
            se.addToNotifications(na2)
            se.save(flush: true)

            assertNotNull se.id

            //try to do update of the ScheduledExecution
            def fwkControl = mockFor(FrameworkService, true)
            fwkControl.demand.getFrameworkFromUserSession { session, request -> return null }
            fwkControl.demand.existsFrameworkProject { project, framework ->
                assertEquals 'testProject', project
                return true
            }
            fwkControl.demand.getCommand { project, type, command, framework ->
                assertEquals 'testProject', project
                assertEquals 'aType', type
                assertEquals 'aCommand', command
                return null
            }
            fwkControl.demand.authorizeProjectJobAll { framework, resource, actions, project -> return true }
            fwkControl.demand.getFrameworkFromUserSession { session, request -> return null }
            fwkControl.demand.getFrameworkFromUserSession { session, request -> return null }
            sec.frameworkService = fwkControl.createMock()

            def params = new ScheduledExecution(jobName: 'monkey1', project: 'testProject', description: 'blah', adhocExecution: true, adhocRemoteString: 'test command',
                    notifications: [new Notification(eventTrigger: ScheduledExecutionController.ONSUCCESS_TRIGGER_NAME, type: 'email', content: 'spaghetti@ nowhere.com'),
                            new Notification(eventTrigger: ScheduledExecutionController.ONFAILURE_TRIGGER_NAME, type: 'email', content: 'milkstore.com')
                    ])
            def results = sec._doupdateJob(se.id, params, 'test', 'test', null, null)
            def succeeded = results[0]
            def scheduledExecution = results[1]
            if (scheduledExecution && scheduledExecution.errors.hasErrors()) {
                scheduledExecution.errors.allErrors.each {
                    System.err.println(it);
                }
            }
            assertFalse succeeded
            assertNotNull(scheduledExecution)
            assertTrue(scheduledExecution instanceof ScheduledExecution)
            final ScheduledExecution executionErr = scheduledExecution
            assertNotNull executionErr
            assertNotNull(executionErr.errors)
            assertTrue(executionErr.errors.hasErrors())
            assertTrue(executionErr.errors.hasFieldErrors(ScheduledExecutionController.NOTIFY_FAILURE_RECIPIENTS))
            assertTrue(executionErr.errors.hasFieldErrors(ScheduledExecutionController.NOTIFY_SUCCESS_RECIPIENTS))

            final ScheduledExecution execution = ScheduledExecution.get(scheduledExecution.id)

            assertEquals 'monkey1', execution.jobName
            assertEquals 'testProject', execution.project
            assertEquals 'blah', execution.description
            assertNotNull execution.notifications
            assertEquals 2, execution.notifications.size()
            def nmap = [:]
            execution.notifications.each { not1 ->
                nmap[not1.eventTrigger] = not1
            }
            assertNotNull(nmap[ScheduledExecutionController.ONSUCCESS_TRIGGER_NAME])
            assertNotNull(nmap[ScheduledExecutionController.ONFAILURE_TRIGGER_NAME])
            assertTrue(nmap[ScheduledExecutionController.ONSUCCESS_TRIGGER_NAME] instanceof Notification)
            assertTrue(nmap[ScheduledExecutionController.ONFAILURE_TRIGGER_NAME] instanceof Notification)
            def Notification n = nmap[ScheduledExecutionController.ONSUCCESS_TRIGGER_NAME]
            assertEquals ScheduledExecutionController.ONSUCCESS_TRIGGER_NAME, n.eventTrigger
            assertEquals "email", n.type
            assertEquals "c@example.com,d@example.com", n.mailConfiguration().recipients
            def Notification n2 = nmap[ScheduledExecutionController.ONFAILURE_TRIGGER_NAME]
            assertEquals ScheduledExecutionController.ONFAILURE_TRIGGER_NAME, n2.eventTrigger
            assertEquals "email", n2.type
            assertEquals "monkey@example.com", n2.mailConfiguration().recipients
        }
    }

    public void testDoUpdateWorkflow() {
        def sec = new ScheduledExecutionService()
        if (true) {//test update workflow 

            def se = new ScheduledExecution(jobName: 'monkey1', project: 'testProject', description: 'blah3')
            def workflow = new Workflow(threadcount: 1, keepgoing: true)
            def wfitem = new CommandExec(adhocExecution: true, adhocRemoteString: 'test command',)
            workflow.addToCommands(wfitem)
            se.workflow = workflow
            se.save()

            assertNotNull se.id

            //try to do update of the ScheduledExecution
            def fwkControl = mockFor(FrameworkService, true)
            fwkControl.demand.getFrameworkFromUserSession { session, request -> return null }
            fwkControl.demand.existsFrameworkProject { project, framework ->
                return true
            }
            fwkControl.demand.getCommand { project, type, command, framework ->
                return null
            }
            fwkControl.demand.authorizeProjectJobAll { framework, resource, actions, project -> return true }
            fwkControl.demand.getFrameworkFromUserSession { session, request -> return null }
            fwkControl.demand.getFrameworkFromUserSession { session, request -> return null }
            sec.frameworkService = fwkControl.createMock()

//            def sesControl = mockFor(ScheduledExecutionService, true)
//            sesControl.demand.getByIDorUUID {id -> return se }
//            sec.scheduledExecutionService = sesControl.createMock()

            def params = [id: se.id.toString(), description: 'changed description', workflow: ['commands[0]': [adhocExecution: true, adhocRemoteString: 'test command2',]], '_workflow_data': true]
            def results = sec._doupdate(params, 'test', 'test', null, null)
            def succeeded = results.success
            def scheduledExecution = results.scheduledExecution
            if (scheduledExecution && scheduledExecution.errors.hasErrors()) {
                scheduledExecution.errors.allErrors.each {
                    System.err.println(it);
                }
            }
            if (scheduledExecution.workflow) {
                if (scheduledExecution.workflow.errors.hasErrors()) {
                    scheduledExecution.workflow.errors.allErrors.each {
                        System.err.println(it);
                    }
                }
                if (scheduledExecution.workflow.commands) {
                    scheduledExecution.workflow.commands.each { cexec ->
                        if (cexec.errors.hasErrors()) {
                            cexec.errors.allErrors.each {
                                System.err.println(it);
                            }
                        }
                    }
                }
            }

            assertTrue succeeded
            assertNotNull(scheduledExecution)
            assertTrue(scheduledExecution instanceof ScheduledExecution)
            final ScheduledExecution execution = scheduledExecution
            assertNotNull(execution)
            assertNotNull(execution.errors)
            assertFalse(execution.errors.hasErrors())
            assertEquals('changed description', execution.description)
            assertNotNull(execution.workflow)
            assertLength(1, execution.workflow.commands as Object[])
            def CommandExec cexec = execution.workflow.commands[0]
            assertEquals 'test command2', cexec.adhocRemoteString
        }
    }

    public void testDoUpdateNodeThreadcountShouldNotBeBlank() {
        def sec = new ScheduledExecutionService()
        if (true) {//test update workflow

            def se = new ScheduledExecution(jobName: 'monkey1', project: 'testProject', description: 'blah3', nodeThreadcount: 1)
            def workflow = new Workflow(threadcount: 1, keepgoing: true)
            def wfitem = new CommandExec(adhocExecution: true, adhocRemoteString: 'test command',)
            workflow.addToCommands(wfitem)
            se.workflow = workflow
            se.save()

            assertNotNull se.id

            //try to do update of the ScheduledExecution
            def fwkControl = mockFor(FrameworkService, true)
            fwkControl.demand.getFrameworkFromUserSession { session, request -> return null }
            fwkControl.demand.existsFrameworkProject { project, framework ->
                return true
            }
            fwkControl.demand.getCommand { project, type, command, framework ->
                return null
            }
            fwkControl.demand.authorizeProjectJobAll { framework, resource, actions, project -> return true }
            fwkControl.demand.getFrameworkFromUserSession { session, request -> return null }
            fwkControl.demand.getFrameworkFromUserSession { session, request -> return null }
            sec.frameworkService = fwkControl.createMock()

//            def sesControl = mockFor(ScheduledExecutionService, true)
//            sesControl.demand.getByIDorUUID {id -> return se }
//            sec.scheduledExecutionService = sesControl.createMock()

            def params = [id: se.id.toString(), description: 'changed description',
                    //set nodeThreadcount to blank
                    nodeThreadcount: '',
                    workflow: ['commands[0]': [adhocExecution: true, adhocRemoteString: 'test command2',]], '_workflow_data': true]
            def results = sec._doupdate(params, 'test', 'test', null, null)
            def succeeded = results.success
            def scheduledExecution = results.scheduledExecution
            if (scheduledExecution && scheduledExecution.errors.hasErrors()) {
                scheduledExecution.errors.allErrors.each {
                    System.out.println(it);
                }
            }
            if (scheduledExecution.workflow) {
                if (scheduledExecution.workflow.errors.hasErrors()) {
                    scheduledExecution.workflow.errors.allErrors.each {
                        System.out.println(it);
                    }
                }
                if (scheduledExecution.workflow.commands) {
                    scheduledExecution.workflow.commands.each { cexec ->
                        if (cexec.errors.hasErrors()) {
                            cexec.errors.allErrors.each {
                                System.out.println(it);
                            }
                        }
                    }
                }
            }

            assertTrue succeeded
            assertNotNull(scheduledExecution)
            assertTrue(scheduledExecution instanceof ScheduledExecution)
            final ScheduledExecution execution = scheduledExecution
            assertNotNull(execution)
            assertNotNull(execution.errors)
            assertFalse(execution.errors.hasErrors())
            assertEquals('changed description', execution.description)
            assertNotNull(execution.nodeThreadcount)
            assertEquals(1, execution.nodeThreadcount)
        }
    }

    public void testDoUpdateWorkflowOptions() {
        def sec = new ScheduledExecutionService()
        if (true) {//test update: update workflow by adding Options

            def se = new ScheduledExecution(jobName: 'monkey1', project: 'testProject', description: 'blah3')
            def workflow = new Workflow(threadcount: 1, keepgoing: true)
            def wfitem = new CommandExec(name: 'aResource', type: 'aType', command: 'aCommand')
            workflow.addToCommands(wfitem)
            se.workflow = workflow
            se.save()

            assertNotNull se.id

            //try to do update of the ScheduledExecution
            def fwkControl = mockFor(FrameworkService, true)
            fwkControl.demand.getFrameworkFromUserSession { session, request -> return null }
            fwkControl.demand.existsFrameworkProject { project, framework ->
                return true
            }
            fwkControl.demand.getCommand { project, type, command, framework ->
                return null
            }
            fwkControl.demand.authorizeProjectJobAll { framework, resource, actions, project -> return true }
            fwkControl.demand.getFrameworkFromUserSession { session, request -> return null }
            fwkControl.demand.getFrameworkFromUserSession { session, request -> return null }
            sec.frameworkService = fwkControl.createMock()

//            def sesControl = mockFor(ScheduledExecutionService, true)
//            sesControl.demand.getByIDorUUID {id -> return se }
//            sec.scheduledExecutionService = sesControl.createMock()

            def params = [id: se.id.toString(), description: 'changed description',
                    options: ["options[0]": [name: 'test3', defaultValue: 'val3', enforced: false, valuesUrl: "http://test.com/test3"]]
            ]
            def results = sec._doupdate(params, 'test', 'test', null, null)
            def succeeded = results.success
            def scheduledExecution = results.scheduledExecution
            if (scheduledExecution && scheduledExecution.errors.hasErrors()) {
                scheduledExecution.errors.allErrors.each {
                    System.err.println(it);
                }
            }
            assertTrue succeeded
            assertNotNull(scheduledExecution)
            assertTrue(scheduledExecution instanceof ScheduledExecution)
            final ScheduledExecution execution = scheduledExecution
            assertNotNull(execution)
            assertNotNull(execution.errors)
            assertFalse(execution.errors.hasErrors())
            assertEquals('changed description', execution.description)
            assertNotNull(execution.workflow)
            assertLength(1, execution.workflow.commands as Object[])

            assertFalse(execution.errors.hasErrors())
            assertNotNull execution.options
            assertLength(1, execution.options as Object[])
            final Iterator iterator = execution.options.iterator()
            assert iterator.hasNext()
            final Option next = iterator.next()
            assertNotNull(next)
            assertEquals("wrong option name", "test3", next.name)
            assertEquals("wrong option name", "val3", next.defaultValue)
            assertNotNull("wrong option name", next.realValuesUrl)
            assertEquals("wrong option name", "http://test.com/test3", next.realValuesUrl.toExternalForm())
            assertFalse("wrong option name", next.enforced)

        }
        if (true) {//test update: update workflow by removing Options

            def se = new ScheduledExecution(jobName: 'monkey1', project: 'testProject', description: 'blah2', adhocExecution: false, name: '', type: '', command: '')
            def workflow = new Workflow(threadcount: 1, keepgoing: true)
            def wfitem = new CommandExec(name: 'aResource', type: 'aType', command: 'aCommand')
            workflow.addToCommands(wfitem)
            se.workflow = workflow
            def opt1 = new Option(name: 'test1', defaultValue: 'val1', enforced: false, valuesUrl: "http://test.com/test")
            def opt2 = new Option(name: 'test2', defaultValue: 'val2', enforced: true, values: ['a', 'b', 'c'])
            se.addToOptions(opt1)
            se.addToOptions(opt2)
            se.save()

            assertNotNull se.id

            //try to do update of the ScheduledExecution
            def fwkControl = mockFor(FrameworkService, true)
            fwkControl.demand.getFrameworkFromUserSession { session, request -> return null }
            fwkControl.demand.existsFrameworkProject { project, framework ->
                return true
            }
            fwkControl.demand.getCommand { project, type, command, framework ->
                return null
            }
            fwkControl.demand.authorizeProjectJobAll { framework, resource, actions, project -> return true }
            fwkControl.demand.getFrameworkFromUserSession { session, request -> return null }
            fwkControl.demand.getFrameworkFromUserSession { session, request -> return null }
            sec.frameworkService = fwkControl.createMock()

//            def sesControl = mockFor(ScheduledExecutionService, true)
//            sesControl.demand.getByIDorUUID {id -> return se }
//            sec.scheduledExecutionService = sesControl.createMock()

            def params = [id: se.id.toString(), description: 'changed description', _nooptions: true]
            def results = sec._doupdate(params, 'test', 'test', null, null)
            def succeeded = results.success
            def scheduledExecution = results.scheduledExecution
            if (scheduledExecution && scheduledExecution.errors.hasErrors()) {
                scheduledExecution.errors.allErrors.each {
                    System.err.println(it);
                }
            }
            assertTrue succeeded
            assertNotNull(scheduledExecution)
            assertTrue(scheduledExecution instanceof ScheduledExecution)
            final ScheduledExecution execution = scheduledExecution
            assertNotNull(execution)
            assertNotNull(execution.errors)
            assertFalse(execution.errors.hasErrors())
            assertEquals('changed description', execution.description)
            assertNotNull(execution.workflow)
            assertLength(1, execution.workflow.commands as Object[])

            assertFalse(execution.errors.hasErrors())
            assertNull execution.options

        }
    }

    public void testDoUpdateOptionsUnmodified() {

        def sec = new ScheduledExecutionService()
        if (true) {//test update: don't modify existing option

            def se = new ScheduledExecution(jobName: 'monkey1', project: 'testProject', description: 'blah2', adhocExecution: false, name: 'aResource', type: 'aType', command: 'aCommand')
            def opt1 = new Option(name: 'test1', defaultValue: 'val1', enforced: false, valuesUrl: "http://test.com/test")
            def opt2 = new Option(name: 'test2', defaultValue: 'val2', enforced: true, values: ['a', 'b', 'c'])
            se.addToOptions(opt1)
            se.addToOptions(opt2)
            se.save()

            assertNotNull se.id

            //try to do update of the ScheduledExecution
            def fwkControl = mockFor(FrameworkService, true)
            fwkControl.demand.getFrameworkFromUserSession { session, request -> return null }
            fwkControl.demand.existsFrameworkProject { project, framework ->
                return true
            }
            fwkControl.demand.getCommand { project, type, command, framework ->
                return null
            }
            fwkControl.demand.authorizeProjectJobAll { framework, resource, actions, project -> return true }
            fwkControl.demand.getFrameworkFromUserSession { session, request -> return null }
            fwkControl.demand.getFrameworkFromUserSession { session, request -> return null }
            sec.frameworkService = fwkControl.createMock()

//            def sesControl = mockFor(ScheduledExecutionService, true)
//            sesControl.demand.getByIDorUUID {id -> return se }
//            sec.scheduledExecutionService = sesControl.createMock()

            def params = [id: se.id.toString(), jobName: 'monkey1', project: 'testProject', description: 'blah2',
                    workflow: [threadcount: 1, keepgoing: true, "commands[0]": [adhocExecution: true, adhocRemoteString: 'a remote string']],
                    _workflow_data: true,
            ]
            def results = sec._doupdate(params, 'test', 'test', null, null)
            def succeeded = results.success
            def scheduledExecution = results.scheduledExecution
            if (scheduledExecution && scheduledExecution.errors.hasErrors()) {
                scheduledExecution.errors.allErrors.each {
                    System.err.println(it);
                }
            }
            assertTrue succeeded
            assertNotNull(scheduledExecution)
            assertTrue(scheduledExecution instanceof ScheduledExecution)
            final ScheduledExecution execution = scheduledExecution
            assertNotNull(execution)
            assertNotNull(execution.errors)
            assertFalse(execution.errors.hasErrors())
            assertNotNull execution.options
            assertLength(2, execution.options as Object[])
            final Iterator iterator = execution.options.iterator()
            assert iterator.hasNext()
            final Option next = iterator.next()
            assertNotNull(next)
            assertEquals("wrong option name", "test1", next.name)
            assertEquals("wrong option name", "val1", next.defaultValue)
            assertNotNull("wrong option name", next.realValuesUrl)
            assertEquals("wrong option name", "http://test.com/test", next.realValuesUrl.toExternalForm())
            assertFalse("wrong option name", next.enforced)
            assert iterator.hasNext()
            final Option next2 = iterator.next()
            assertNotNull(next2)
            assertEquals("wrong option name", "test2", next2.name)
            assertEquals("wrong option name", "val2", next2.defaultValue)
            assertNull("wrong option name", next2.realValuesUrl)
            assertTrue("wrong option name", next2.enforced)
            assertNotNull("wrong option name", next2.values)
            assertLength(3, next2.values as Object[])
            Assert.assertArrayEquals(['a', 'b', 'c'] as String[], next2.values as String[])

        }

    }

    public void testDoUpdateOptionsRemoveAll() {

        def sec = new ScheduledExecutionService()
        if (true) {//test update: set _nooptions to delete options

            def se = new ScheduledExecution(jobName: 'monkey1', project: 'testProject', description: 'blah2',
                    workflow: new Workflow(commands: [new CommandExec(adhocRemoteString: 'test command', adhocExecution: true)]),
            )
            def opt1 = new Option(name: 'test1', defaultValue: 'val1', enforced: false, valuesUrl: "http://test.com/test")
            def opt2 = new Option(name: 'test2', defaultValue: 'val2', enforced: true, values: ['a', 'b', 'c'])
            se.addToOptions(opt1)
            se.addToOptions(opt2)
            se.save()

            assertNotNull se.id

            //try to do update of the ScheduledExecution
            def fwkControl = mockFor(FrameworkService, true)
            fwkControl.demand.getFrameworkFromUserSession { session, request -> return null }
            fwkControl.demand.existsFrameworkProject { project, framework ->
                return true
            }
            fwkControl.demand.getCommand { project, type, command, framework ->
                return null
            }
            fwkControl.demand.authorizeProjectJobAll { framework, resource, actions, project -> return true }
            fwkControl.demand.getFrameworkFromUserSession { session, request -> return null }
            fwkControl.demand.getFrameworkFromUserSession { session, request -> return null }
            sec.frameworkService = fwkControl.createMock()

//            def sesControl = mockFor(ScheduledExecutionService, true)
//            sesControl.demand.getByIDorUUID {id -> return se }
//            sec.scheduledExecutionService = sesControl.createMock()

            def params = [id: se.id.toString(), jobName: 'monkey1', project: 'testProject', description: 'blah2',
                    workflow: new Workflow(commands: [new CommandExec(adhocRemoteString: 'test command', adhocExecution: true)]),
                    _nooptions: true]
            def results = sec._doupdate(params, 'test', 'test', null, null)
            def succeeded = results.success
            def scheduledExecution = results.scheduledExecution
            if (scheduledExecution && scheduledExecution.errors.hasErrors()) {
                scheduledExecution.errors.allErrors.each {
                    System.err.println(it);
                }
            }
            assertTrue succeeded
            assertNotNull(scheduledExecution)
            assertTrue(scheduledExecution instanceof ScheduledExecution)
            final ScheduledExecution execution = scheduledExecution
            assertNotNull(execution)
            assertNotNull(execution.errors)
            assertFalse(execution.errors.hasErrors())
            assertNull execution.options
        }

    }

    public void testDoUpdateOptionsReplace() {

        def sec = new ScheduledExecutionService()
        if (true) {//test update: set options to replace options

            def se = new ScheduledExecution(jobName: 'monkey1', project: 'testProject', description: 'blah2',
                    workflow: new Workflow(commands: [new CommandExec(adhocRemoteString: 'test command', adhocExecution: true)]),
            )
            def opt1 = new Option(name: 'test1', defaultValue: 'val1', enforced: false, valuesUrl: "http://test.com/test")
            def opt2 = new Option(name: 'test2', defaultValue: 'val2', enforced: true, values: ['a', 'b', 'c'])
            se.addToOptions(opt1)
            se.addToOptions(opt2)
            se.save()

            assertNotNull se.id

            //try to do update of the ScheduledExecution
            def fwkControl = mockFor(FrameworkService, true)
            fwkControl.demand.getFrameworkFromUserSession { session, request -> return null }
            fwkControl.demand.existsFrameworkProject { project, framework ->
                return true
            }
            fwkControl.demand.getCommand { project, type, command, framework ->
                return null
            }
            fwkControl.demand.authorizeProjectJobAll { framework, resource, actions, project -> return true }
            fwkControl.demand.getFrameworkFromUserSession { session, request -> return null }
            fwkControl.demand.getFrameworkFromUserSession { session, request -> return null }
            sec.frameworkService = fwkControl.createMock()

//            def sesControl = mockFor(ScheduledExecutionService, true)
//            sesControl.demand.getByIDorUUID {id -> return se }
//            sec.scheduledExecutionService = sesControl.createMock()

            def params = [id: se.id.toString(), jobName: 'monkey1', project: 'testProject', description: 'blah2',
                    workflow: new Workflow(commands: [new CommandExec(adhocRemoteString: 'test command', adhocExecution: true)]),
                    options: ["options[0]": [name: 'test3', defaultValue: 'val3', enforced: false, valuesUrl: "http://test.com/test3"]]
            ]
            def results = sec._doupdate(params, 'test', 'test', null, null)
            def succeeded = results.success
            def scheduledExecution = results.scheduledExecution
            if (scheduledExecution && scheduledExecution.errors.hasErrors()) {
                scheduledExecution.errors.allErrors.each {
                    System.err.println(it);
                }
            }
            assertTrue succeeded
            assertNotNull(scheduledExecution)
            assertTrue(scheduledExecution instanceof ScheduledExecution)
            final ScheduledExecution execution = scheduledExecution
            assertNotNull(execution)
            assertNotNull(execution.errors)
            assertFalse(execution.errors.hasErrors())
            assertNotNull execution.options
            assertLength(1, execution.options as Object[])
            final Iterator iterator = execution.options.iterator()
            assert iterator.hasNext()
            final Option next = iterator.next()
            assertNotNull(next)
            assertEquals("wrong option name", "test3", next.name)
            assertEquals("wrong option name", "val3", next.defaultValue)
            assertNotNull("wrong option name", next.realValuesUrl)
            assertEquals("wrong option name", "http://test.com/test3", next.realValuesUrl.toExternalForm())
            assertFalse("wrong option name", next.enforced)

        }

    }


    public void testDoUpdateOptionsShouldModify() {
        def sec = new ScheduledExecutionService()
        if (true) {//test update: set options to modify existing options

            def se = new ScheduledExecution(jobName: 'monkey1', project: 'testProject', description: 'blah2', adhocExecution: false, name: 'aResource', type: 'aType', command: 'aCommand')
            def opt1 = new Option(name: 'test1', defaultValue: 'val1', enforced: true, values: ['a', 'b', 'c'])
            def opt2 = new Option(name: 'test2', enforced: false, valuesUrl: "http://test.com/test2")
            se.addToOptions(opt1)
            se.addToOptions(opt2)
            se.save()

            assertNotNull se.id

            //try to do update of the ScheduledExecution
            def fwkControl = mockFor(FrameworkService, true)
            fwkControl.demand.getFrameworkFromUserSession { session, request -> return null }
            fwkControl.demand.existsFrameworkProject { project, framework ->
                return true
            }
            fwkControl.demand.getCommand { project, type, command, framework ->
                return null
            }
            fwkControl.demand.authorizeProjectJobAll { framework, resource, actions, project -> return true }
            fwkControl.demand.getFrameworkFromUserSession { session, request -> return null }
            fwkControl.demand.getFrameworkFromUserSession { session, request -> return null }
            sec.frameworkService = fwkControl.createMock()

//            def sesControl = mockFor(ScheduledExecutionService, true)
//            sesControl.demand.getByIDorUUID {id -> return se }
//            sec.scheduledExecutionService = sesControl.createMock()

            def params = [id: se.id.toString(), jobName: 'monkey1', project: 'testProject', description: 'blah2',
                    workflow: [threadcount: 1, keepgoing: true, "commands[0]": [adhocExecution: true, adhocRemoteString: 'a remote string']],
                    _workflow_data: true,
                    options: ["options[0]": [name: 'test1', defaultValue: 'val3', enforced: false, valuesUrl: "http://test.com/test3"],
                            "options[1]": [name: 'test2', defaultValue: 'd', enforced: true, values: ['a', 'b', 'c', 'd']]]
            ]
            def results = sec._doupdate(params, 'test', 'test', null, null)
            def succeeded = results.success
            def scheduledExecution = results.scheduledExecution
            if (scheduledExecution && scheduledExecution.errors.hasErrors()) {
                scheduledExecution.errors.allErrors.each {
                    System.err.println(it);
                }
            }
            assertTrue succeeded
            assertNotNull(scheduledExecution)
            assertTrue(scheduledExecution instanceof ScheduledExecution)
            final ScheduledExecution execution = scheduledExecution
            assertNotNull(execution)
            assertNotNull(execution.errors)
            assertFalse(execution.errors.hasErrors())
            assertNotNull execution.options
            assertLength(2, execution.options as Object[])
            final Iterator iterator = execution.options.iterator()
            assert iterator.hasNext()
            final Option next = iterator.next()
            assertNotNull(next)
            assertEquals("wrong option name", "test1", next.name)
            assertEquals("wrong option name", "val3", next.defaultValue)
            assertNotNull("wrong option name", next.realValuesUrl)
            assertEquals("wrong option name", "http://test.com/test3", next.realValuesUrl.toExternalForm())
            assertFalse("wrong option name", next.enforced)
            final Option next2 = iterator.next()
            assertNotNull(next2)
            assertEquals("wrong option name", "test2", next2.name)
            assertEquals("wrong option name", "d", next2.defaultValue)
            assertNull("wrong option name", next2.realValuesUrl)
            assertTrue("wrong option name", next2.enforced)

        }
    }

    public void testDoUpdateShouldValidateOption() {

        def sec = new ScheduledExecutionService()
        if (true) {//test update: set options to validate multivalued options

            def se = new ScheduledExecution(jobName: 'monkey1', project: 'testProject', description: 'blah2', adhocExecution: false, name: 'aResource', type: 'aType', command: 'aCommand')
            def opt1 = new Option(name: 'test1', defaultValue: 'val1', enforced: true, values: ['a', 'b', 'c'])
            def opt2 = new Option(name: 'test2', enforced: false, valuesUrl: "http://test.com/test2")
            se.addToOptions(opt1)
            se.addToOptions(opt2)
            se.save()

            assertNotNull se.id

            //try to do update of the ScheduledExecution
            def fwkControl = mockFor(FrameworkService, true)
            fwkControl.demand.getFrameworkFromUserSession { session, request -> return null }
            fwkControl.demand.existsFrameworkProject { project, framework ->
                return true
            }
            fwkControl.demand.getCommand { project, type, command, framework ->
                return null
            }
            fwkControl.demand.authorizeProjectJobAll { framework, resource, actions, project -> return true }
            fwkControl.demand.getFrameworkFromUserSession { session, request -> return null }
            fwkControl.demand.getFrameworkFromUserSession { session, request -> return null }
            sec.frameworkService = fwkControl.createMock()

//            def sesControl = mockFor(ScheduledExecutionService, true)
//            sesControl.demand.getByIDorUUID {id -> return se }
//            sec.scheduledExecutionService = sesControl.createMock()

            def ms = mockFor(MessageSource)
            ms.demand.getMessage { key, data, locale -> 'message' }
            ms.demand.getMessage { error, locale -> 'message' }
            sec.messageSource = ms.createMock()


            def params = [id: se.id.toString(), jobName: 'monkey1', project: 'testProject', description: 'blah2', adhocExecution: false, name: 'aResource', type: 'aType', command: 'aCommand',
                    options: ["options[0]": [name: 'test1', defaultValue: 'val3', enforced: false, multivalued: true],
                            "options[1]": [name: 'test2', defaultValue: 'val2', enforced: false, values: ['a', 'b', 'c', 'd'], multivalued: true, delimiter: "testdelim"]]
            ]
            def results = sec._doupdate(params, 'test', 'test', null, null)
            def succeeded = results.success
            def scheduledExecution = results.scheduledExecution
            if (scheduledExecution && scheduledExecution.errors.hasErrors()) {
                scheduledExecution.errors.allErrors.each {
                    System.err.println(it);
                }
            }
            assertFalse succeeded
            assertNotNull(scheduledExecution)
            assertTrue(scheduledExecution instanceof ScheduledExecution)
            final ScheduledExecution execution = scheduledExecution
            assertNotNull(execution)
            assertNotNull(execution.errors)
            assertTrue(execution.errors.hasErrors())
            assertNotNull execution.options
            assertLength(2, execution.options as Object[])
            final Iterator iterator = execution.options.iterator()
            assert iterator.hasNext()
            final Option next = iterator.next()
            assertNotNull(next)
            assertEquals("wrong option name", "test1", next.name)
            assertTrue(next.errors.hasFieldErrors())
            assertTrue(next.errors.hasFieldErrors('delimiter'))
            final Option next2 = iterator.next()
            assertNotNull(next2)
            assertFalse(next2.errors.hasFieldErrors())
            assertEquals("wrong option name", "test2", next2.name)
            assertTrue(next2.multivalued)
            assertEquals("testdelim", next2.delimiter)

        }
    }

    public void testDoUpdateJobShouldRemoveOptions() {

        def sec = new ScheduledExecutionService()
        if (true) {//test updateJob: no options in input job should remove existing options

            def se = new ScheduledExecution(jobName: 'monkey1', project: 'testProject', description: 'blah2',
                    workflow: new Workflow(commands: [new CommandExec(adhocRemoteString: 'test command', adhocExecution: true)]),
            )
            def opt1 = new Option(name: 'test1', defaultValue: 'val1', enforced: false, valuesUrl: "http://test.com/test")
            def opt2 = new Option(name: 'test2', defaultValue: 'val2', enforced: true, values: ['a', 'b', 'c'])
            se.addToOptions(opt1)
            se.addToOptions(opt2)
            se.save()

            assertNotNull se.id

            //try to do update of the ScheduledExecution
            def fwkControl = mockFor(FrameworkService, true)
            fwkControl.demand.getFrameworkFromUserSession { session, request -> return null }
            fwkControl.demand.existsFrameworkProject { project, framework ->
                return true
            }
            fwkControl.demand.getCommand { project, type, command, framework ->
                return null
            }
            sec.frameworkService = fwkControl.createMock()

            def params = new ScheduledExecution(jobName: 'monkey1', project: 'testProject', description: 'blah2',
                    workflow: new Workflow(commands: [new CommandExec(adhocRemoteString: 'test command', adhocExecution: true)]),
            )
            def results = sec._doupdateJob(se.id, params, 'test', 'test', null, null)
            def succeeded = results[0]
            def scheduledExecution = results[1]
            if (scheduledExecution && scheduledExecution.errors.hasErrors()) {
                scheduledExecution.errors.allErrors.each {
                    System.err.println(it);
                }
            }
            assertTrue succeeded
            assertNotNull(scheduledExecution)
            assertTrue(scheduledExecution instanceof ScheduledExecution)
            final ScheduledExecution execution = scheduledExecution
            assertNotNull(execution)
            assertNotNull(execution.errors)
            assertFalse(execution.errors.hasErrors())
            assertNull execution.options
        }
    }

    public void testDoUpdateSessionOptsShouldRemoveOptions() {

        def sec = new ScheduledExecutionService()
        if (true) {//test updateJob: no options in input job should remove existing options

            def se = new ScheduledExecution(jobName: 'monkey1', project: 'testProject', description: 'blah2',
                    workflow: new Workflow(commands: [new CommandExec(adhocRemoteString: 'test command', adhocExecution: true)]),
            )
            def opt1 = new Option(name: 'test1', defaultValue: 'val1', enforced: false, valuesUrl: "http://test.com/test")
            def opt2 = new Option(name: 'test2', defaultValue: 'val2', enforced: true, values: ['a', 'b', 'c'])
            se.addToOptions(opt1)
            se.addToOptions(opt2)
            se.save()

            assertNotNull se.id

            //try to do update of the ScheduledExecution
            def fwkControl = mockFor(FrameworkService, true)
            fwkControl.demand.getFrameworkFromUserSession { session, request -> return null }
            fwkControl.demand.existsFrameworkProject { project, framework ->
                return true
            }
            fwkControl.demand.authorizeProjectJobAll { framework, resource, actions, project -> return true }
            fwkControl.demand.authorizeProjectJobAll { framework, resource, actions, project -> return true }
            fwkControl.demand.getFrameworkFromUserSession { session, request -> return null }
            fwkControl.demand.getFrameworkFromUserSession { session, request -> return null }
            sec.frameworkService = fwkControl.createMock()

            def params = [
                    id: se.id.toString(),
                    jobName: 'monkey2',
                    project: 'testProject',
                    description: 'blah',
                    workflow: [threadcount: 1, keepgoing: true, "commands[0]": [adhocExecution: true, adhocRemoteString: 'a remote string']],
                    _sessionopts: true,
                    _sessionEditOPTSObject: [:] //empty map to clear options
            ]

            def results = sec._doupdate(params, 'test', 'test', null, null)
            def succeeded = results.success
            def scheduledExecution = results.scheduledExecution
            if (scheduledExecution && scheduledExecution.errors.hasErrors()) {
                scheduledExecution.errors.allErrors.each {
                    System.err.println(it);
                }
            }
            assertTrue succeeded
            assertNotNull(scheduledExecution)
            assertTrue(scheduledExecution instanceof ScheduledExecution)
            final ScheduledExecution execution = scheduledExecution
            assertNotNull(execution)
            assertNotNull(execution.errors)
            assertFalse(execution.errors.hasErrors())
            assertNull "should not have options", execution.options
        }
    }

    public void testDoUpdateJobShouldReplaceOptions() {

        def sec = new ScheduledExecutionService()
        if (true) {//test update: set options to replace options

            def se = new ScheduledExecution(jobName: 'monkey1', project: 'testProject', description: 'blah2', adhocExecution: false, name: 'aResource', type: 'aType', command: 'aCommand')
            def opt1 = new Option(name: 'test1', defaultValue: 'val1', enforced: false, valuesUrl: "http://test.com/test")
            def opt2 = new Option(name: 'test2', defaultValue: 'val2', enforced: true, values: ['a', 'b', 'c'])
            se.addToOptions(opt1)
            se.addToOptions(opt2)
            se.save()

            assertNotNull se.id

            //try to do update of the ScheduledExecution
            def fwkControl = mockFor(FrameworkService, true)
            fwkControl.demand.getFrameworkFromUserSession { session, request -> return null }
            fwkControl.demand.existsFrameworkProject { project, framework ->
                return true
            }
            fwkControl.demand.getCommand { project, type, command, framework ->
                return null
            }
            sec.frameworkService = fwkControl.createMock()

            def params = new ScheduledExecution(jobName: 'monkey1', project: 'testProject', description: 'blah2',
                    workflow: new Workflow(commands: [new CommandExec(adhocRemoteString: 'test command', adhocExecution: true)]),
                    options: [
                            new Option(name: 'test3', defaultValue: 'val3', enforced: false, valuesUrl: "http://test.com/test3"),
                    ]
            )
            def results = sec._doupdateJob(se.id, params, 'test', 'test', null, null)
            def succeeded = results[0]
            def scheduledExecution = results[1]
            if (scheduledExecution && scheduledExecution.errors.hasErrors()) {
                scheduledExecution.errors.allErrors.each {
                    System.err.println(it);
                }
            }
            assertTrue succeeded
            assertNotNull(scheduledExecution)
            assertTrue(scheduledExecution instanceof ScheduledExecution)
            final ScheduledExecution execution = scheduledExecution
            assertNotNull(execution)
            assertNotNull(execution.errors)
            assertFalse(execution.errors.hasErrors())
            assertNotNull execution.options
            assertLength(1, execution.options as Object[])
            final Iterator iterator = execution.options.iterator()
            assert iterator.hasNext()
            final Option next = iterator.next()
            assertNotNull(next)
            assertEquals("wrong option name", "test3", next.name)
            assertEquals("wrong option name", "val3", next.defaultValue)
            assertNotNull("wrong option name", next.realValuesUrl)
            assertEquals("wrong option name", "http://test.com/test3", next.realValuesUrl.toExternalForm())
            assertFalse("wrong option name", next.enforced)

        }
        if (true) {//test update: set options to modify existing options

            def se = new ScheduledExecution(jobName: 'monkey1', project: 'testProject', description: 'blah2', adhocExecution: false, name: 'aResource', type: 'aType', command: 'aCommand')
            def opt1 = new Option(name: 'test1', defaultValue: 'val1', enforced: true, values: ['a', 'b', 'c'])
            def opt2 = new Option(name: 'test2', enforced: false, valuesUrl: "http://test.com/test2")
            se.addToOptions(opt1)
            se.addToOptions(opt2)
            se.save()

            assertNotNull se.id

            //try to do update of the ScheduledExecution
            def fwkControl = mockFor(FrameworkService, true)
            fwkControl.demand.getFrameworkFromUserSession { session, request -> return null }
            fwkControl.demand.existsFrameworkProject { project, framework ->
                return true
            }
            fwkControl.demand.getCommand { project, type, command, framework ->
                return null
            }
            sec.frameworkService = fwkControl.createMock()

            def params = new ScheduledExecution(jobName: 'monkey1', project: 'testProject', description: 'blah2',
                    workflow: new Workflow(commands: [new CommandExec(adhocRemoteString: 'test command', adhocExecution: true)]),
                    options: [
                            new Option(name: 'test1', defaultValue: 'val3', enforced: false, valuesUrl: "http://test.com/test3"),
                            new Option(name: 'test2', defaultValue: 'd', enforced: true, values: ['a', 'b', 'c', 'd'])
                    ]
            )
            def results = sec._doupdateJob(se.id, params, 'test', 'test', null, null)
            def succeeded = results[0]
            def scheduledExecution = results[1]
            if (scheduledExecution && scheduledExecution.errors.hasErrors()) {
                scheduledExecution.errors.allErrors.each {
                    System.err.println(it);
                }
            }
            assertTrue succeeded
            assertNotNull(scheduledExecution)
            assertTrue(scheduledExecution instanceof ScheduledExecution)
            final ScheduledExecution execution = scheduledExecution
            assertNotNull(execution)
            assertNotNull(execution.errors)
            assertFalse(execution.errors.hasErrors())
            assertNotNull execution.options
            assertLength(2, execution.options as Object[])
            final Iterator iterator = execution.options.iterator()
            assert iterator.hasNext()
            final Option next = iterator.next()
            assertNotNull(next)
            assertEquals("wrong option name", "test1", next.name)
            assertEquals("wrong option name", "val3", next.defaultValue)
            assertNotNull("wrong option name", next.realValuesUrl)
            assertEquals("wrong option name", "http://test.com/test3", next.realValuesUrl.toExternalForm())
            assertFalse("wrong option name", next.enforced)
            final Option next2 = iterator.next()
            assertNotNull(next2)
            assertEquals("wrong option name", "test2", next2.name)
            assertEquals("wrong option name", "d", next2.defaultValue)
            assertNull("wrong option name", next2.realValuesUrl)
            assertTrue("wrong option name", next2.enforced)

        }
        if (true) {//test update invalid: multivalued true without delimiter

            def se = new ScheduledExecution(jobName: 'monkey1', project: 'testProject', description: 'blah2', adhocExecution: false, name: 'aResource', type: 'aType', command: 'aCommand')
            def opt1 = new Option(name: 'test1', defaultValue: 'val1', enforced: true, values: ['a', 'b', 'c'])
            def opt2 = new Option(name: 'test2', enforced: false, valuesUrl: "http://test.com/test2")
            se.addToOptions(opt1)
            se.addToOptions(opt2)
            se.save()

            assertNotNull se.id

            //try to do update of the ScheduledExecution
            def fwkControl = mockFor(FrameworkService, true)
            fwkControl.demand.getFrameworkFromUserSession { session, request -> return null }
            fwkControl.demand.existsFrameworkProject { project, framework ->
                return true
            }
            fwkControl.demand.getCommand { project, type, command, framework ->
                return null
            }
            sec.frameworkService = fwkControl.createMock()

            def ms = mockFor(MessageSource)
            ms.demand.getMessage { key, data, locale -> 'message' }
            ms.demand.getMessage { error, locale -> 'message' }
            sec.messageSource = ms.createMock()

            def params = new ScheduledExecution(jobName: 'monkey1', project: 'testProject', description: 'blah2',
                    workflow: new Workflow(commands: [new CommandExec(adhocRemoteString: 'test command', adhocExecution: true)]),
                    options: [
                            new Option(name: 'test1', defaultValue: 'val3', enforced: false, valuesUrl: "http://test.com/test3", multivalued: true),
                            new Option(name: 'test2', defaultValue: 'd', enforced: true, values: ['a', 'b', 'c', 'd'], multivalued: true, delimiter: "testdelim")
                    ]
            )
            def results = sec._doupdateJob(se.id, params, 'test', 'test', null, null)
            def succeeded = results[0]
            def scheduledExecution = results[1]
            if (scheduledExecution && scheduledExecution.errors.hasErrors()) {
                scheduledExecution.errors.allErrors.each {
                    System.err.println(it);
                }
            }
            assertFalse succeeded
            assertNotNull(scheduledExecution)
            assertTrue(scheduledExecution instanceof ScheduledExecution)
            final ScheduledExecution execution = scheduledExecution
            assertNotNull(execution)
            assertNotNull(execution.errors)
            assertTrue(execution.errors.hasErrors())
            assertNotNull execution.options
            assertLength(2, execution.options as Object[])
            final Iterator iterator = execution.options.iterator()
            assert iterator.hasNext()
            final Option next = iterator.next()
            assertNotNull(next)
            assertTrue next.errors.hasFieldErrors('delimiter')
            final Option next2 = iterator.next()
            assertNotNull(next2)
            assertFalse next2.errors.hasFieldErrors()
            assertTrue next2.multivalued
            assertEquals "testdelim", next2.delimiter

        }
    }

    /**
     * Import job with same UUID, and different project, should fail due to uniqueness of uuid
     */
    public void testLoadJobsShouldNotUpdateUUIDWrongProjectDupeOptionUpdate() {

        def sec = new ScheduledExecutionService()

        //create mock of FrameworkService
        def fwkControl = mockFor(FrameworkService, true)
        fwkControl.demand.getFrameworkFromUserSession { session, request -> return null }
        fwkControl.demand.getFrameworkFromUserSession { session, request -> return null }
        fwkControl.demand.existsFrameworkProject { project, framework -> return true }
        fwkControl.demand.authorizeProjectJobAll { framework, job, actions, project -> return true }
        fwkControl.demand.getFrameworkFromUserSession { session, request -> return null }
        fwkControl.demand.getFrameworkFromUserSession { session, request -> return null }
        fwkControl.demand.authorizeProjectResourceAll { framework, job, actions, project -> return true }
        sec.frameworkService = fwkControl.createMock()
        def ms = mockFor(MessageSource)
        ms.demand.getMessage { key, data, locale -> key }
        ms.demand.getMessage { error, locale -> error.toString() }
        sec.messageSource = ms.createMock()

        //create original job
        final CommandExec exec = new CommandExec(adhocExecution: true, adhocRemoteString: "echo original test")
        exec.save()
        def wf = new Workflow(commands: [exec])
        wf.save()
        def se = new ScheduledExecution(jobName: 'testUploadShouldUpdateSameNameDupeOptionUpdate',
                groupPath: "testgroup", project: 'project1', description: 'new desc',
                workflow: wf,
                uuid: UUID.randomUUID().toString()
        )
        if (!se.validate()) {
            se.errors.allErrors.each {
                println it.toString()
            }
        }
        assertNotNull(se.save())
        assertNotNull(se.id)


        def upload = new ScheduledExecution(
                jobName: 'testUploadShouldUpdateSameNameDupeOptionUpdate',
                groupPath: "testgroup",
                project: 'project2',
                description: 'desc',
                workflow: new Workflow(commands: [new CommandExec(adhocExecution: true, adhocRemoteString: "echo test")]),
                uuid: se.uuid
        )
        def result = sec.loadJobs([upload], 'update', 'test', 'userrole,test', [:], null,null)
        assertNotNull result
        assertNotNull result.jobs
        assertNotNull result.errjobs
        assertNotNull result.skipjobs
        assertEquals "shouldn't have error jobs: ${result.errjobs}", 1, result.errjobs.size()
        ScheduledExecution errorjob = result.errjobs[0].scheduledExecution
        assertTrue(errorjob.hasErrors())
        assertTrue(errorjob.errors.hasFieldErrors('uuid'))
        assertEquals "shouldn't have skipped jobs: ${result.skipjobs}", 0, result.skipjobs.size()
        assertEquals 0, result.jobs.size()

        se.delete()
    }
    /**
     * Import job with same UUID, and different project, should fail due to uniqueness of uuid
     */
    public void testLoadJobsUUIDOptionNull() {

        def uuid1 = UUID.randomUUID().toString()
        assertLoadJobs('create', null, uuid1, uuid1, 'project1', 'project2', { Map result ->
            assertEquals "should have error jobs: ${result.errjobs}", 1, result.errjobs.size()
            ScheduledExecution errorjob = result.errjobs[0].scheduledExecution
            assertTrue(errorjob.hasErrors())
            assertTrue(errorjob.errors.hasFieldErrors('uuid'))
            assertEquals "shouldn't have skipped jobs: ${result.skipjobs}", 0, result.skipjobs.size()
            assertEquals 0, result.jobs.size()
        })
    }
    /**
     * Import job with same UUID, and different project, should fail due to uniqueness of uuid
     */
    public void testLoadJobsUUIDOptionPreserve() {

        def uuid1 = UUID.randomUUID().toString()
        assertLoadJobs('create', 'preserve', uuid1, uuid1, 'project1', 'project2', { Map result ->
            assertEquals "should have error jobs: ${result.errjobs}", 1, result.errjobs.size()
            ScheduledExecution errorjob = result.errjobs[0].scheduledExecution
            assertTrue(errorjob.hasErrors())
            assertTrue(errorjob.errors.hasFieldErrors('uuid'))
            assertEquals "shouldn't have skipped jobs: ${result.skipjobs}", 0, result.skipjobs.size()
            assertEquals 0, result.jobs.size()
        })
    }
    /**
     * Import job with same UUID, and different project, should fail due to uniqueness of uuid
     */
    public void testLoadJobsUUIDOptionRemove() {

        def uuid1 = UUID.randomUUID().toString()
        assertLoadJobs('create', 'remove', uuid1, uuid1, 'project1', 'project2', { Map result ->
            assertEquals "shouldn't have error jobs: ${result.errjobs}", 0, result.errjobs.size()
            assertEquals "shouldn't have skipped jobs: ${result.skipjobs}", 0, result.skipjobs.size()
            assertEquals 1, result.jobs.size()
        })
    }
    /**
     * Import job with same UUID, and different project, should fail due to uniqueness of uuid
     */
    public void testLoadJobsUpdateUUIDOptionPreserve() {

        def uuid1 = UUID.randomUUID().toString()
        assertLoadJobs('update', 'preserve', uuid1, uuid1, 'project1', 'project2', { Map result ->
            assertEquals "should have error jobs: ${result.errjobs}", 1, result.errjobs.size()
            ScheduledExecution errorjob = result.errjobs[0].scheduledExecution
            assertTrue(errorjob.hasErrors())
            assertTrue(errorjob.errors.hasFieldErrors('uuid'))
            assertEquals "shouldn't have skipped jobs: ${result.skipjobs}", 0, result.skipjobs.size()
            assertEquals 0, result.jobs.size()
        })
    }
    /**
     * Import job with same UUID, and different project, should fail due to uniqueness of uuid
     */
    public void testLoadJobsUpdateUUIDOptionRemove() {

        def uuid1 = UUID.randomUUID().toString()
        assertLoadJobs('update', 'remove', uuid1, uuid1, 'project1', 'project2', { Map result ->
            assertEquals "shouldn't have error jobs: ${result.errjobs}", 0, result.errjobs.size()
            assertEquals "shouldn't have skipped jobs: ${result.skipjobs}", 0, result.skipjobs.size()
            assertEquals 1, result.jobs.size()
        })
    }

    private void assertLoadJobs(String importOption, String uuidOption, String uuid1, String uuid2, String project1, String project2, Closure test) {
        def sec = new ScheduledExecutionService()

        //create mock of FrameworkService
        def fwkControl = mockFor(FrameworkService, true)
        fwkControl.demand.getFrameworkFromUserSession { session, request -> return null }
        fwkControl.demand.getFrameworkFromUserSession { session, request -> return null }
        fwkControl.demand.existsFrameworkProject { project, framework -> return true }
        fwkControl.demand.authorizeProjectJobAll { framework, job, actions, project -> return true }
        fwkControl.demand.getFrameworkFromUserSession { session, request -> return null }
        fwkControl.demand.getFrameworkFromUserSession { session, request -> return null }
        fwkControl.demand.authorizeProjectResourceAll { framework, job, actions, project -> return true }
        sec.frameworkService = fwkControl.createMock()
        def ms = mockFor(MessageSource)
        ms.demand.getMessage { key, data, locale -> key }
        ms.demand.getMessage { error, locale -> error.toString() }
        sec.messageSource = ms.createMock()

        //create original job
        final CommandExec exec = new CommandExec(adhocExecution: true, adhocRemoteString: "echo original test")
        exec.save()
        def wf = new Workflow(commands: [exec])
        wf.save()
        def se = new ScheduledExecution(jobName: 'testjob',
                groupPath: "testgroup", project: project1?: 'project1', description: 'new desc',
                workflow: wf,
                uuid: uuid1
        )
        if (!se.validate()) {
            se.errors.allErrors.each {
                println it.toString()
            }
        }
        assertNotNull(se.save())
        assertNotNull(se.id)


        def upload = new ScheduledExecution(
                jobName: 'testjob',
                groupPath: "testgroup",
                project: project2?: 'project2',
                description: 'desc',
                workflow: new Workflow(commands: [new CommandExec(adhocExecution: true, adhocRemoteString: "echo test")]),
                uuid: uuid2
        )
        def result = sec.loadJobs([upload], importOption,uuidOption, 'test', 'userrole,test', [:], null, null)
        assertNotNull result
        assertNotNull result.jobs
        assertNotNull result.errjobs
        assertNotNull result.skipjobs
        if(test){
            test.call(result)
        }

        se.delete()
    }

    public void testUploadShouldUpdateSameNameDupeOptionUpdate() {

        def sec = new ScheduledExecutionService()

        //create mock of FrameworkService
        def fwkControl = mockFor(FrameworkService, true)
        fwkControl.demand.getFrameworkFromUserSession { session, request -> return null }
        fwkControl.demand.getFrameworkFromUserSession { session, request -> return null }
        fwkControl.demand.existsFrameworkProject { project, framework -> return true }
        fwkControl.demand.authorizeProjectJobAll { framework, job, actions, project -> return true }
        fwkControl.demand.getFrameworkFromUserSession { session, request -> return null }
        fwkControl.demand.getFrameworkFromUserSession { session, request -> return null }
        sec.frameworkService = fwkControl.createMock()

        //create original job
        final CommandExec exec = new CommandExec(adhocExecution: true, adhocRemoteString: "echo original test")
        exec.save()
        def wf = new Workflow(commands: [exec])
        wf.save()
        def se = new ScheduledExecution(jobName: 'testUploadShouldUpdateSameNameDupeOptionUpdate', groupPath: "testgroup", project: 'project1', description: 'new desc',
                workflow: wf
        )
        se.save()
        assertNotNull se.id

        //mock the scheduledExecutionService
//        def mock2 = mockFor(ScheduledExecutionService, true)
//        mock2.demand.nextExecutionTimes {joblist -> return [] }
//        mock2.demand.loadJobs {jobset, dupeOption, user, roleList, changeinfo, framework ->
//            [
//                    jobs: [expectedJob],
//                    jobsi: [scheduledExecution: expectedJob, entrynum: 0],
//                    errjobs: [],
//                    skipjobs: []
//            ]
//        }
//        sec.scheduledExecutionService = mock2.createMock()

        def xml = '''
<joblist>
    <job>
        <name>testUploadShouldUpdateSameNameDupeOptionUpdate</name>
        <group>testgroup</group>
        <description>desc</description>
        <context>
            <project>project1</project>
        </context>
        <sequence>
            <command><exec>echo test</exec></command>
        </sequence>
    </job>
</joblist>
'''
        def upload = new ScheduledExecution(
                jobName: 'testUploadShouldUpdateSameNameDupeOptionUpdate',
                groupPath: "testgroup",
                project: 'project1',
                description: 'desc',
                workflow: new Workflow(commands: [new CommandExec(adhocExecution: true, adhocRemoteString: "echo test")])
        )
//        sec.request.addFile(new MockMultipartFile('xmlBatch', 'test.xml', 'text/xml', xml as byte[]))
        //set update
//        sec.params.dupeOption = 'update'
        def result = sec.loadJobs([upload], 'update', 'test', 'userrole,test', [:], null, null)
//        final subject = new Subject()
//        subject.principals << new Username('test')
//        subject.principals.addAll(['userrole', 'test'].collect {new Group(it)})
//        sec.request.setAttribute("subject", subject)

//        def result = sec.upload()
        //[jobs: jobs, errjobs: errjobs, skipjobs: skipjobs, nextExecutions:scheduledExecutionService.nextExecutionTimes(jobs.grep{ it.scheduled }), messages: msgs, didupload: true]
        assertNotNull result
        assertNotNull result.jobs
        assertNotNull result.errjobs
        assertNotNull result.skipjobs
        assertEquals "shouldn't have error jobs: ${result.errjobs}", 0, result.errjobs.size()
        assertEquals "shouldn't have skipped jobs: ${result.skipjobs}", 0, result.skipjobs.size()
        assertEquals 1, result.jobs.size()
        assertTrue result.jobs[0] instanceof ScheduledExecution
        def ScheduledExecution job = result.jobs[0]
        assertEquals "testUploadShouldUpdateSameNameDupeOptionUpdate", job.jobName
        assertEquals "testgroup", job.groupPath
        assertEquals "desc", job.description
        assertEquals "project1", job.project
        assertEquals "echo test", job.workflow.commands[0].adhocRemoteString
        assertEquals se.id, job.id
        se.delete()
    }

    public void testUploadShouldSkipSameNameDupeOptionSkip() {
        def sec = new ScheduledExecutionService()

        //create mock of FrameworkService
        def fwkControl = mockFor(FrameworkService, true)
        fwkControl.demand.getFrameworkFromUserSession { session, request -> return null }
        fwkControl.demand.getFrameworkFromUserSession { session, request -> return null }
        fwkControl.demand.existsFrameworkProject { project, framework -> return true }
        sec.frameworkService = fwkControl.createMock()
        //mock the scheduledExecutionService
//        def mock2 = mockFor(ScheduledExecutionService, true)
//        mock2.demand.nextExecutionTimes {joblist -> return [] }
//        sec.scheduledExecutionService = mock2.createMock()

        //create original job
        final CommandExec exec = new CommandExec(adhocExecution: true, adhocRemoteString: "echo original test")
        exec.save()
        def wf = new Workflow(commands: [exec])
        wf.save()
        def se = new ScheduledExecution(jobName: 'testUploadShouldSkipSameNameDupeOptionSkip', groupPath: "testgroup", project: 'project1', description: 'original desc',
                workflow: wf
        )
        se.save()
        assertNotNull se.id

        def xml = '''
<joblist>
    <job>
        <name>testUploadShouldSkipSameNameDupeOptionSkip</name>
        <group>testgroup</group>
        <description>desc</description>
        <context>
            <project>project1</project>
        </context>
        <sequence>
            <command><exec>echo test</exec></command>
        </sequence>
    </job>
</joblist>
'''
        def upload = new ScheduledExecution(
                jobName: 'testUploadShouldSkipSameNameDupeOptionSkip',
                groupPath: "testgroup",
                project: 'project1',
                description: 'desc',
                workflow: new Workflow(commands: [new CommandExec(adhocExecution: true, adhocRemoteString: "echo test")])
        )
//        sec.request.addFile(new MockMultipartFile('xmlBatch', 'test.xml', 'text/xml', xml as byte[]))
        //set update
//        sec.params.dupeOption = 'skip'
        def result = sec.loadJobs([upload], 'skip', 'test', 'test,userrole', [:], null, null)
        //[jobs: jobs, errjobs: errjobs, skipjobs: skipjobs, nextExecutions:scheduledExecutionService.nextExecutionTimes(jobs.grep{ it.scheduled }), messages: msgs, didupload: true]
        assertNotNull result
        assertNotNull result.jobs
        assertNotNull result.errjobs
        assertNotNull result.skipjobs
        assertEquals "shouldn't have error jobs: ${result.errjobs}", 0, result.errjobs.size()
        assertEquals "shouldn't have success jobs: ${result.jobs}", 0, result.jobs.size()
        assertEquals 1, result.skipjobs.size()

        //get original job and test values
        ScheduledExecution test = ScheduledExecution.get(se.id)
        assertNotNull test
        assertEquals "original desc", test.description
        assertEquals "echo original test", test.workflow.commands[0].adhocRemoteString
        se.delete()
    }

    public void testUploadShouldCreateSameNameDupeOptionCreate() {
        def sec = new ScheduledExecutionService()
        //create mock of FrameworkService
        def fwkControl = mockFor(FrameworkService, true)
        fwkControl.demand.getFrameworkFromUserSession { session, request -> return null }
        fwkControl.demand.getFrameworkFromUserSession { session, request -> return null }
        fwkControl.demand.existsFrameworkProject { project, framework -> return true }
        fwkControl.demand.authorizeProjectResourceAll { framework, resource, actions, project -> return true }
        fwkControl.demand.authorizeProjectJobAll { framework, scheduledExecution, actions, project -> return true }
        fwkControl.demand.getFrameworkFromUserSession { session, request -> return null }
        fwkControl.demand.getFrameworkFromUserSession { session, request -> return null }
        sec.frameworkService = fwkControl.createMock()
        //mock the scheduledExecutionService
//        def mock2 = mockFor(ScheduledExecutionService, true)
//        mock2.demand.nextExecutionTimes {joblist -> return [] }
//        sec.scheduledExecutionService = mock2.createMock()

        //create original job
        final CommandExec exec = new CommandExec(adhocExecution: true, adhocRemoteString: "echo original test")
        exec.save()
        def wf = new Workflow(commands: [exec])
        wf.save()
        def se = new ScheduledExecution(jobName: 'test1', groupPath: "testgroup", project: 'project1', description: 'original desc',
                workflow: wf
        )
        se.save()
        assertNotNull se.id

        def xml = '''
<joblist>
    <job>
        <name>test1</name>
        <group>testgroup</group>
        <description>desc</description>
        <context>
            <project>project1</project>
        </context>
        <sequence>
            <command><exec>echo test</exec></command>
        </sequence>
    </job>
</joblist>
'''

        def upload = new ScheduledExecution(
                jobName: 'test1',
                groupPath: "testgroup",
                project: 'project1',
                description: 'desc',
                workflow: new Workflow(commands: [new CommandExec(adhocExecution: true, adhocRemoteString: "echo test")])
        )
//        sec.request.addFile(new MockMultipartFile('xmlBatch', 'test.xml', 'text/xml', xml as byte[]))
        //set update
//        sec.params.dupeOption = 'create'
        def result = sec.loadJobs([upload], 'create', 'test', 'test,userrole', [:], null, null)
        //[jobs: jobs, errjobs: errjobs, skipjobs: skipjobs, nextExecutions:scheduledExecutionService.nextExecutionTimes(jobs.grep{ it.scheduled }), messages: msgs, didupload: true]
        assertNotNull result
        assertNotNull result.jobs
        assertNotNull result.errjobs
        assertNotNull result.skipjobs
        assertEquals "shouldn't have error jobs: ${result.errjobs}", 0, result.errjobs.size()
        assertEquals "shouldn't have skipped jobs: ${result.skipjobs}", 0, result.skipjobs.size()
        assertEquals 1, result.jobs.size()
        assertTrue result.jobs[0] instanceof ScheduledExecution
        def ScheduledExecution job = result.jobs[0]
        assertEquals "test1", job.jobName
        assertEquals "testgroup", job.groupPath
        assertEquals "desc", job.description
        assertEquals "project1", job.project
        assertEquals "echo test", job.workflow.commands[0].adhocRemoteString
        assertFalse se.id == job.id

        //get original job and test values
        ScheduledExecution test = ScheduledExecution.get(se.id)
        assertNotNull test
        assertEquals "original desc", test.description
        assertEquals "echo original test", test.workflow.commands[0].adhocRemoteString
    }


    public void testUploadJobIdentityShouldRequireJobName() {
        def sec = new ScheduledExecutionService()

        //create mock of FrameworkService
        def fwkControl = mockFor(FrameworkService, true)
        fwkControl.demand.getFrameworkFromUserSession { session, request -> return null }
        fwkControl.demand.getFrameworkFromUserSession { session, request -> return null }
        fwkControl.demand.existsFrameworkProject { project, framework -> return true }
        fwkControl.demand.authorizeProjectResourceAll { framework, resource, actions, project -> return true }
        fwkControl.demand.authorizeProjectJobAll { framework, scheduledExecution, actions, project -> return true }
        fwkControl.demand.getFrameworkFromUserSession { session, request -> return null }
        fwkControl.demand.getFrameworkFromUserSession { session, request -> return null }
        sec.frameworkService = fwkControl.createMock()
        //mock the scheduledExecutionService
//        def mock2 = mockFor(ScheduledExecutionService, true)
//        mock2.demand.nextExecutionTimes {joblist -> return [] }
//        sec.scheduledExecutionService = mock2.createMock()

        //create original job
        final CommandExec exec = new CommandExec(adhocExecution: true, adhocRemoteString: "echo original test")
        exec.save()
        def wf = new Workflow(commands: [exec])
        wf.save()
        def se = new ScheduledExecution(jobName: 'test1', groupPath: "testgroup", project: 'project1', description: 'original desc',
                workflow: wf
        )
        se.save()
        assertNotNull se.id

        //test different name

        def xml = '''
<joblist>
    <job>
        <name>test2</name>
        <group>testgroup</group>
        <description>desc</description>
        <context>
            <project>project1</project>
        </context>
        <sequence>
            <command><exec>echo test</exec></command>
        </sequence>
    </job>
</joblist>
'''
        def upload = new ScheduledExecution(
                jobName: 'test2',
                groupPath: "testgroup",
                project: 'project1',
                description: 'desc',
                workflow: new Workflow(commands: [new CommandExec(adhocExecution: true, adhocRemoteString: "echo test")])
        )
//        sec.request.addFile(new MockMultipartFile('xmlBatch', 'test.xml', 'text/xml', xml as byte[]))
        //set update
//        sec.params.dupeOption = 'skip'
        def result = sec.loadJobs([upload], 'skip', 'test', 'test,userrole', [:], null, null)
        //[jobs: jobs, errjobs: errjobs, skipjobs: skipjobs, nextExecutions:scheduledExecutionService.nextExecutionTimes(jobs.grep{ it.scheduled }), messages: msgs, didupload: true]
        assertNotNull result
        assertNotNull result.jobs
        assertNotNull result.errjobs
        assertNotNull result.skipjobs
        assertEquals "shouldn't have skipped jobs: ${result.skipjobs}", 0, result.skipjobs.size()
        assertEquals "shouldn't have error jobs: ${result.errjobs}", 0, result.errjobs.size()
        assertEquals "should have success jobs: ${result.jobs}", 1, result.jobs.size()
        assertFalse se.id == result.jobs[0].id

        //get original job and test values
        ScheduledExecution test = ScheduledExecution.get(se.id)
        assertNotNull test
        assertEquals "original desc", test.description
        assertEquals "echo original test", test.workflow.commands[0].adhocRemoteString
    }

    public void testUploadJobIdentityShouldRequireGroupPath() {
        def sec = new ScheduledExecutionService()

        //create mock of FrameworkService
        def fwkControl = mockFor(FrameworkService, true)
        fwkControl.demand.getFrameworkFromUserSession { session, request -> return null }
        fwkControl.demand.getFrameworkFromUserSession { session, request -> return null }
        fwkControl.demand.existsFrameworkProject { project, framework -> return true }
        fwkControl.demand.authorizeProjectResourceAll { framework, resource, actions, project -> return true }
        fwkControl.demand.authorizeProjectJobAll { framework, scheduledExecution, actions, project -> return true }
        fwkControl.demand.getFrameworkFromUserSession { session, request -> return null }
        fwkControl.demand.getFrameworkFromUserSession { session, request -> return null }
        sec.frameworkService = fwkControl.createMock()
        //mock the scheduledExecutionService
//        def mock2 = mockFor(ScheduledExecutionService, true)
//        mock2.demand.nextExecutionTimes {joblist -> return [] }
//        sec.scheduledExecutionService = mock2.createMock()

        //create original job
        final CommandExec exec = new CommandExec(adhocExecution: true, adhocRemoteString: "echo original test")
        exec.save()
        def wf = new Workflow(commands: [exec])
        wf.save()
        def se = new ScheduledExecution(jobName: 'test1', groupPath: "testgroup", project: 'project1', description: 'original desc',
                workflow: wf
        )
        se.save()
        assertNotNull se.id

        //test different group

        def xml = '''
<joblist>
    <job>
        <name>test1</name>
        <group>testgroup1</group>
        <description>desc</description>
        <context>
            <project>project1</project>
        </context>
        <sequence>
            <command><exec>echo test</exec></command>
        </sequence>
    </job>
</joblist>
'''

        def upload = new ScheduledExecution(
                jobName: 'test1',
                groupPath: "testgroup1",
                project: 'project1',
                description: 'desc',
                workflow: new Workflow(commands: [new CommandExec(adhocExecution: true, adhocRemoteString: "echo test")])
        )
//        sec.request.addFile(new MockMultipartFile('xmlBatch', 'test.xml', 'text/xml', xml as byte[]))
        //set update
//        sec.params.dupeOption = 'skip'

        def result = sec.loadJobs([upload], 'skip', 'test', 'test,userrole', [:], null, null)
        //[jobs: jobs, errjobs: errjobs, skipjobs: skipjobs, nextExecutions:scheduledExecutionService.nextExecutionTimes(jobs.grep{ it.scheduled }), messages: msgs, didupload: true]
        assertNotNull result
        assertNotNull result.jobs
        assertNotNull result.errjobs
        assertNotNull result.skipjobs
        assertEquals "shouldn't have skipped jobs: ${result.skipjobs}", 0, result.skipjobs.size()
        assertEquals "shouldn't have error jobs: ${result.errjobs}", 0, result.errjobs.size()
        assertEquals "should have success jobs: ${result.jobs}", 1, result.jobs.size()
        assertFalse se.id == result.jobs[0].id

        //get original job and test values
        ScheduledExecution test = ScheduledExecution.get(se.id)
        assertNotNull test
        assertEquals "original desc", test.description
        assertEquals "echo original test", test.workflow.commands[0].adhocRemoteString
    }

    public void testUploadJobIdentityShouldRequireProject() {
        def sec = new ScheduledExecutionService()

        //create mock of FrameworkService
        def fwkControl = mockFor(FrameworkService, true)
        fwkControl.demand.getFrameworkFromUserSession { session, request -> return null }
        fwkControl.demand.getFrameworkFromUserSession { session, request -> return null }
        fwkControl.demand.existsFrameworkProject { project, framework -> return true }
        fwkControl.demand.authorizeProjectResourceAll { framework, resource, actions, project -> return true }
        fwkControl.demand.authorizeProjectJobAll { framework, scheduledExecution, actions, project -> return true }
        fwkControl.demand.getFrameworkFromUserSession { session, request -> return null }
        fwkControl.demand.getFrameworkFromUserSession { session, request -> return null }
        sec.frameworkService = fwkControl.createMock()
        //mock the scheduledExecutionService
//        def mock2 = mockFor(ScheduledExecutionService, true)
//        mock2.demand.nextExecutionTimes {joblist -> return [] }
//        sec.scheduledExecutionService = mock2.createMock()

        //create original job
        final CommandExec exec = new CommandExec(adhocExecution: true, adhocRemoteString: "echo original test")
        exec.save()
        def wf = new Workflow(commands: [exec])
        wf.save()
        def se = new ScheduledExecution(jobName: 'test1', groupPath: "testgroup", project: 'project1', description: 'original desc',
                workflow: wf
        )
        se.save()
        assertNotNull se.id

        //test different project

        def xml = '''
<joblist>
    <job>
        <name>test1</name>
        <group>testgroup</group>
        <description>desc</description>
        <context>
            <project>project2</project>
        </context>
        <sequence>
            <command><exec>echo test</exec></command>
        </sequence>
    </job>
</joblist>
'''
        def upload = new ScheduledExecution(
                jobName: 'test1',
                groupPath: "testgroup",
                project: 'project2',
                description: 'desc',
                workflow: new Workflow(commands: [new CommandExec(adhocExecution: true, adhocRemoteString: "echo test")])
        )
//        sec.request.addFile(new MockMultipartFile('xmlBatch', 'test.xml', 'text/xml', xml as byte[]))
        //set update
//        sec.params.dupeOption = 'skip'

        def result = sec.loadJobs([upload], 'skip', 'test', 'test,userrole', [:], null, null)
        //[jobs: jobs, errjobs: errjobs, skipjobs: skipjobs, nextExecutions:scheduledExecutionService.nextExecutionTimes(jobs.grep{ it.scheduled }), messages: msgs, didupload: true]
        assertNotNull result
        assertNotNull result.jobs
        assertNotNull result.errjobs
        assertNotNull result.skipjobs
        assertEquals "shouldn't have skipped jobs: ${result.skipjobs}", 0, result.skipjobs.size()
        assertEquals "shouldn't have error jobs: ${result.errjobs}", 0, result.errjobs.size()
        assertEquals "should have success jobs: ${result.jobs}", 1, result.jobs.size()
        assertFalse se.id == result.jobs[0].id

        //get original job and test values
        ScheduledExecution test = ScheduledExecution.get(se.id)
        assertNotNull test
        assertEquals "original desc", test.description
        assertEquals "echo original test", test.workflow.commands[0].adhocRemoteString
    }

    public void testLoadJobs_JobShouldRequireProject() {
        def sec = new ScheduledExecutionService()

        //create mock of FrameworkService
        def fwkControl = mockFor(FrameworkService, true)
        fwkControl.demand.getFrameworkFromUserSession { session, request -> return null }
        fwkControl.demand.getFrameworkFromUserSession { session, request -> return null }
        fwkControl.demand.existsFrameworkProject { project, framework -> return true }
        fwkControl.demand.authorizeProjectResourceAll { framework, resource, actions, project -> return true }
        fwkControl.demand.authorizeProjectJobAll { framework, scheduledExecution, actions, project -> return true }
        fwkControl.demand.getFrameworkFromUserSession { session, request -> return null }
        fwkControl.demand.getFrameworkFromUserSession { session, request -> return null }
        sec.frameworkService = fwkControl.createMock()

        //null project

        def upload = new ScheduledExecution(
                jobName: 'test1',
                groupPath: "testgroup",
                project: null,
                description: 'desc',
                workflow: new Workflow(commands: [new CommandExec(adhocExecution: true, adhocRemoteString: "echo test")])
        )

        def result = sec.loadJobs([upload], 'create', 'test', 'test,userrole', [:], null, null)
        assertNotNull result
        assertNotNull result.jobs
        assertNotNull result.errjobs
        assertNotNull result.skipjobs
        assertEquals "shouldn't have skipped jobs: ${result.skipjobs}", 0, result.skipjobs.size()
        assertEquals "shouldn't have error jobs: ${result.errjobs}", 1, result.errjobs.size()
        assertEquals "should have success jobs: ${result.jobs}", 0, result.jobs.size()
        assertEquals "Project was not specified", result.errjobs[0].errmsg
    }

    public void testUploadShouldOverwriteFilters() {
        def sec = new ScheduledExecutionService()

        //create mock of FrameworkService
        def fwkControl = mockFor(FrameworkService, true)
        fwkControl.demand.getFrameworkFromUserSession { session, request -> return null }
        fwkControl.demand.getFrameworkFromUserSession { session, request -> return null }
        fwkControl.demand.existsFrameworkProject { project, framework -> return true }
        fwkControl.demand.authorizeProjectJobAll { framework, resource, actions, project -> return true }
        fwkControl.demand.getFrameworkFromUserSession { session, request -> return null }
        fwkControl.demand.getFrameworkFromUserSession { session, request -> return null }
        sec.frameworkService = fwkControl.createMock()
        //mock the scheduledExecutionService
//        def mock2 = mockFor(ScheduledExecutionService, true)
//        mock2.demand.nextExecutionTimes {joblist -> return [] }
//        sec.scheduledExecutionService = mock2.createMock()

        //create original job
        final CommandExec exec = new CommandExec(adhocExecution: true, adhocRemoteString: "echo original test")
        exec.save()
        def wf = new Workflow(commands: [exec])
        wf.save()
        def se = new ScheduledExecution(jobName: 'testUploadOverwritesFilters', groupPath: "testgroup", project: 'project1', description: 'original desc',
                doNodedispatch: true, nodeInclude: "monkey.*", nodeExcludeOsFamily: 'windows', nodeIncludeTags: 'something',
                workflow: wf
        )
        se.save()
        assertNotNull se.id

        //test updating filters

        def xml = '''
<joblist>
    <job>
        <name>testUploadOverwritesFilters</name>
        <group>testgroup</group>
        <description>desc</description>
        <context>
            <project>project1</project>
        </context>
        <dispatch threadcount="1" keepgoing="true"/>
        <nodefilters excludeprecedence="true">
          <include>
             <hostname>asuka</hostname>
             <name>test</name>
          </include>
          <exclude>
             <hostname>testo</hostname>
             <tags>dev</tags>
          </exclude>
        </nodefilters>
        <sequence>
            <command><exec>echo test</exec></command>
        </sequence>
    </job>
</joblist>
'''
        def upload = new ScheduledExecution(
                jobName: 'testUploadOverwritesFilters',
                groupPath: "testgroup",
                project: 'project1',
                description: 'desc',
                workflow: new Workflow(commands: [new CommandExec(adhocExecution: true, adhocRemoteString: "echo test")]),
                doNodedispatch: true,
                nodeThreadcount: 1,
                nodeKeepgoing: true,
                nodeExcludePrecedence: true,
                nodeInclude: 'asuka',
                nodeIncludeName: 'test',
                nodeExclude: 'testo',
                nodeExcludeTags: 'dev'
        )
//        sec.request.addFile(new MockMultipartFile('xmlBatch', 'test.xml', 'text/xml', xml as byte[]))
        //set update

        def result = sec.loadJobs([upload], 'update', 'test', 'test,userrole', [:], null, null)
        //[jobs: jobs, errjobs: errjobs, skipjobs: skipjobs, nextExecutions:scheduledExecutionService.nextExecutionTimes(jobs.grep{ it.scheduled }), messages: msgs, didupload: true]
//        assertNull sec.response.redirectedUrl
//        assertNull "Result had an error: ${sec.flash.error}", sec.flash.error
        assertNotNull result
        assertNotNull result.jobs
        assertNotNull result.errjobs
        assertNotNull result.skipjobs
        assertEquals "shouldn't have skipped jobs: ${result.skipjobs}", 0, result.skipjobs.size()
        assertEquals "shouldn't have error jobs: ${result.errjobs}", 0, result.errjobs.size()
        assertEquals "should have success jobs: ${result.jobs}", 1, result.jobs.size()
        assertEquals se.id, result.jobs[0].id

        //get original job and test values
        ScheduledExecution test = ScheduledExecution.get(se.id)
        assertNotNull test
        assertTrue test.doNodedispatch
        assertEquals "asuka", test.nodeInclude
        assertEquals "test", test.nodeIncludeName
        assertNull "wrong value", test.nodeIncludeTags
        assertEquals "testo", test.nodeExclude
        assertEquals "dev", test.nodeExcludeTags
        assertNull "wrong value", test.nodeExcludeOsFamily
    }

    public void testUploadShouldRemoveFilters() {
        def sec = new ScheduledExecutionService()

        //create mock of FrameworkService
        def fwkControl = mockFor(FrameworkService, true)
        fwkControl.demand.getFrameworkFromUserSession { session, request -> return null }
        fwkControl.demand.getFrameworkFromUserSession { session, request -> return null }
        fwkControl.demand.existsFrameworkProject { project, framework -> return true }
        fwkControl.demand.authorizeProjectJobAll { framework, resource, actions, project -> return true }
        fwkControl.demand.getFrameworkFromUserSession { session, request -> return null }
        fwkControl.demand.getFrameworkFromUserSession { session, request -> return null }
        sec.frameworkService = fwkControl.createMock()
        //mock the scheduledExecutionService
//        def mock2 = mockFor(ScheduledExecutionService, true)
//        mock2.demand.nextExecutionTimes {joblist -> return [] }
//        sec.scheduledExecutionService = mock2.createMock()

        //create original job
        final CommandExec exec = new CommandExec(adhocExecution: true, adhocRemoteString: "echo original test")
        exec.save()
        def wf = new Workflow(commands: [exec])
        wf.save()
        def se = new ScheduledExecution(jobName: 'testUploadShouldRemoveFilters', groupPath: "testgroup", project: 'project1', description: 'original desc',
                doNodedispatch: true, nodeInclude: "monkey.*", nodeExcludeOsFamily: 'windows', nodeIncludeTags: 'something',
                workflow: wf
        )
        se.save()
        assertNotNull se.id

        //test removing filters

        def xml = '''
<joblist>
    <job>
        <name>testUploadShouldRemoveFilters</name>
        <group>testgroup</group>
        <description>desc</description>
        <context>
            <project>project1</project>
        </context>


        <sequence>
            <command><exec>echo test</exec></command>
        </sequence>
    </job>
</joblist>
'''

        def upload = new ScheduledExecution(
                jobName: 'testUploadShouldRemoveFilters',
                groupPath: "testgroup",
                project: 'project1',
                description: 'desc',
                workflow: new Workflow(commands: [new CommandExec(adhocExecution: true, adhocRemoteString: "echo test")])
        )
//        sec.request.addFile(new MockMultipartFile('xmlBatch', 'test.xml', 'text/xml', xml as byte[]))
        //set update

        def result = sec.loadJobs([upload], 'update', 'test', 'test,userrole', [:], null, null)
        //[jobs: jobs, errjobs: errjobs, skipjobs: skipjobs, nextExecutions:scheduledExecutionService.nextExecutionTimes(jobs.grep{ it.scheduled }), messages: msgs, didupload: true]
//        assertNull sec.response.redirectedUrl
//        assertNull "Result had an error: ${sec.flash.error}", sec.flash.error
        assertNotNull result
        assertNotNull result.jobs
        assertNotNull result.errjobs
        assertNotNull result.skipjobs
        assertEquals "shouldn't have skipped jobs: ${result.skipjobs}", 0, result.skipjobs.size()
        assertEquals "shouldn't have error jobs: ${result.errjobs}", 0, result.errjobs.size()
        assertEquals "should have success jobs: ${result.jobs}", 1, result.jobs.size()
        assertEquals se.id, result.jobs[0].id

        //get original job and test values
        ScheduledExecution test = ScheduledExecution.get(se.id)
        assertNotNull test
        assertTrue(!test.doNodedispatch)
        assertNull "wrong value", test.nodeInclude
        assertNull "wrong value", test.nodeIncludeName
        assertNull "wrong value", test.nodeIncludeTags
        assertNull "wrong value", test.nodeExclude
        assertNull "wrong value", test.nodeExcludeTags
        assertNull "wrong value", test.nodeExcludeOsFamily
        se.delete()
    }

    public void testUploadShouldChangeThreadcount() {
        def sec = new ScheduledExecutionService()

        //create mock of FrameworkService
        def fwkControl = mockFor(FrameworkService, true)
        fwkControl.demand.getFrameworkFromUserSession { session, request -> return null }
        fwkControl.demand.getFrameworkFromUserSession { session, request -> return null }
        fwkControl.demand.existsFrameworkProject { project, framework -> return true }
        fwkControl.demand.authorizeProjectJobAll { framework, resource, actions, project -> return true }
        fwkControl.demand.getFrameworkFromUserSession { session, request -> return null }
        fwkControl.demand.getFrameworkFromUserSession { session, request -> return null }
        sec.frameworkService = fwkControl.createMock()
        //mock the scheduledExecutionService
//        def mock2 = mockFor(ScheduledExecutionService, true)
//        mock2.demand.nextExecutionTimes {joblist -> return [] }
//        sec.scheduledExecutionService = mock2.createMock()

        //create original job
        final CommandExec exec = new CommandExec(adhocExecution: true, adhocRemoteString: "echo original test")
        exec.save()
        def wf = new Workflow(commands: [exec])
        wf.save()
        def se = new ScheduledExecution(jobName: 'testUploadOverwritesFilters', groupPath: "testgroup", project: 'project1', description: 'original desc',
                doNodedispatch: true, nodeInclude: "monkey.*", nodeExcludeOsFamily: 'windows', nodeIncludeTags: 'something',
                nodeThreadcount: 1,
                workflow: wf
        )
        se.save()
        assertNotNull se.id
        assertEquals(1, se.nodeThreadcount)

        //test updating filters


        def upload = new ScheduledExecution(
                jobName: 'testUploadOverwritesFilters',
                groupPath: "testgroup",
                project: 'project1',
                description: 'desc',
                workflow: new Workflow(commands: [new CommandExec(adhocExecution: true, adhocRemoteString: "echo test")]),
                doNodedispatch: true,
                nodeThreadcount: 4,
                nodeKeepgoing: true,
                nodeExcludePrecedence: true,
                nodeInclude: 'asuka',
                nodeIncludeName: 'test',
                nodeExclude: 'testo',
                nodeExcludeTags: 'dev'
        )
//        sec.request.addFile(new MockMultipartFile('xmlBatch', 'test.xml', 'text/xml', xml as byte[]))
        //set update

        def result = sec.loadJobs([upload], 'update', 'test', 'test,userrole', [:], null, null)
        //[jobs: jobs, errjobs: errjobs, skipjobs: skipjobs, nextExecutions:scheduledExecutionService.nextExecutionTimes(jobs.grep{ it.scheduled }), messages: msgs, didupload: true]
//        assertNull sec.response.redirectedUrl
//        assertNull "Result had an error: ${sec.flash.error}", sec.flash.error
        assertNotNull result
        assertNotNull result.jobs
        assertNotNull result.errjobs
        assertNotNull result.skipjobs
        assertEquals "shouldn't have skipped jobs: ${result.skipjobs}", 0, result.skipjobs.size()
        assertEquals "shouldn't have error jobs: ${result.errjobs}", 0, result.errjobs.size()
        assertEquals "should have success jobs: ${result.jobs}", 1, result.jobs.size()
        assertEquals se.id, result.jobs[0].id

        //get original job and test values
        ScheduledExecution test = ScheduledExecution.get(se.id)
        assertNotNull test
        assertTrue test.doNodedispatch
        assertEquals 4, test.nodeThreadcount
        assertEquals "asuka", test.nodeInclude
        assertEquals "test", test.nodeIncludeName
        assertNull "wrong value", test.nodeIncludeTags
        assertEquals "testo", test.nodeExclude
        assertEquals "dev", test.nodeExcludeTags
        assertNull "wrong value", test.nodeExcludeOsFamily
    }

    public void testLoadJobsErrorHandlers() {
        def sec = new ScheduledExecutionService()

        //create mock of FrameworkService
        def fwkControl = mockFor(FrameworkService, true)
        fwkControl.demand.getFrameworkFromUserSession { session, request -> return null }
        fwkControl.demand.getFrameworkFromUserSession { session, request -> return null }
        fwkControl.demand.existsFrameworkProject { project, framework -> return true }
        fwkControl.demand.authorizeProjectResourceAll { framework, resource, actions, project -> return true }
        fwkControl.demand.authorizeProjectJobAll { framework, resource, actions, project -> return true }
        fwkControl.demand.getFrameworkFromUserSession { session, request -> return null }
        fwkControl.demand.getFrameworkFromUserSession { session, request -> return null }
        sec.frameworkService = fwkControl.createMock()
        def ms = mockFor(MessageSource)
        ms.demand.getMessage { key, data, locale -> key }
        ms.demand.getMessage { error, locale -> error.toString() }
        sec.messageSource = ms.createMock()
        //test upload job with error-handlers

        def upload = new ScheduledExecution(
                jobName: 'testUploadErrorHandlers',
                groupPath: "testgroup",
                project: 'project1',
                description: 'desc',
                workflow: new Workflow(commands: [
                        new CommandExec(adhocExecution: true, adhocRemoteString: "echo test",
                                errorHandler: new CommandExec(adhocExecution: true,
                                        adhocRemoteString: "echo this is an errorhandler")),
                        new CommandExec(argString: "blah blah", adhocLocalString: "test2",
                                errorHandler: new CommandExec(argString: "blah blah err",
                                        adhocLocalString: "test2err")),
                        new CommandExec(argString: "blah3 blah3", adhocFilepath: "test3",
                                errorHandler: new CommandExec(argString: "blah3 blah3 err",
                                        adhocFilepath: "test3err")),
                        new JobExec(jobGroup: "group", jobName: "test",
                                errorHandler: new JobExec(jobName: "testerr", jobGroup: "grouperr", argString: "line err")),

                ])
        )
//        sec.request.addFile(new MockMultipartFile('xmlBatch', 'test.xml', 'text/xml', xml as byte[]))
        //set update

        def result = sec.loadJobs([upload], 'update', 'test', 'test,userrole', [:], null, null)
        //[jobs: jobs, errjobs: errjobs, skipjobs: skipjobs, nextExecutions:scheduledExecutionService.nextExecutionTimes(jobs.grep{ it.scheduled }), messages: msgs, didupload: true]
        assertNotNull result
        assertNotNull result.jobs
        assertNotNull result.errjobs
        assertNotNull result.skipjobs
        assertEquals "shouldn't have skipped jobs: ${result.skipjobs}", 0, result.skipjobs.size()
        assertEquals "shouldn't have error jobs: ${result.errjobs}", 0, result.errjobs.size()
        assertEquals "should have success jobs: ${result.jobs}", 1, result.jobs.size()
        assertNotNull result.jobs[0].id

        //get original job and test values
        ScheduledExecution test = ScheduledExecution.get(result.jobs[0].id)
        assertNotNull test
        assertNotNull test.workflow
        assertNotNull test.workflow.commands
        assertEquals 4, test.workflow.commands.size()
        test.workflow.commands.each {
            assertNotNull(it.errorHandler)
            assertNotNull(it.id)
        }
        (0..2).each { ndx ->
            assertTrue(test.workflow.commands[ndx] instanceof CommandExec)
            assertTrue(test.workflow.commands[ndx].errorHandler instanceof CommandExec)
//            assertNotNull(test.workflow.commands[ndx].errorHandler.id)
        }
        [3].each { ndx ->
            assertTrue(test.workflow.commands[ndx] instanceof JobExec)
            assertTrue(test.workflow.commands[ndx].errorHandler instanceof JobExec)
//            assertNotNull(test.workflow.commands[ndx].errorHandler.id)
        }
        test.delete()
    }




}

class FakeScheduler implements Scheduler{
    String getSchedulerName() {
        return null
    }

    String getSchedulerInstanceId() {
        return null
    }

    SchedulerContext getContext() {
        return null
    }

    void start() {

    }

    void startDelayed(int i) {

    }

    boolean isStarted() {
        return false
    }

    void standby() {

    }

    boolean isInStandbyMode() {
        return false
    }

    void shutdown() {

    }

    void shutdown(boolean b) {

    }

    boolean isShutdown() {
        return false
    }

    SchedulerMetaData getMetaData() {
        return null
    }

    List getCurrentlyExecutingJobs() {
        return null
    }

    void setJobFactory(JobFactory factory) {

    }

    Date scheduleJob(JobDetail detail, Trigger trigger) {
        return null
    }

    Date scheduleJob(Trigger trigger) {
        return null
    }

    @Override
    boolean unscheduleJobs(List<TriggerKey> list) throws SchedulerException {
        return false
    }



    void addJob(JobDetail detail, boolean b) {

    }

    void pauseAll() {

    }

    void resumeAll() {

    }

    List getJobGroupNames() {
        return new String[0] as List
    }

    List getTriggerGroupNames() {
        return new String[0] as List
    }


    Set getPausedTriggerGroups() {
        return null
    }

    void addCalendar(String s, Calendar calendar, boolean b, boolean b1) {

    }

    boolean deleteCalendar(String s) {
        return false
    }

    Calendar getCalendar(String s) {
        return null
    }

    List getCalendarNames() {
        return new String[0] as List
    }

    @Override
    ListenerManager getListenerManager() throws SchedulerException {
        return null
    }

    @Override
    void scheduleJobs(Map<JobDetail, Set<? extends Trigger>> map, boolean b) throws SchedulerException {

    }

    @Override
    void scheduleJob(JobDetail jobDetail, Set<? extends Trigger> set, boolean b) throws SchedulerException {

    }

    @Override
    boolean unscheduleJob(TriggerKey triggerKey) throws SchedulerException {
        return false
    }

    @Override
    Date rescheduleJob(TriggerKey triggerKey, Trigger trigger) throws SchedulerException {
        return null
    }

    @Override
    void addJob(JobDetail jobDetail, boolean b, boolean b1) throws SchedulerException {

    }

    @Override
    boolean deleteJob(JobKey jobKey) throws SchedulerException {
        return false
    }

    @Override
    boolean deleteJobs(List<JobKey> list) throws SchedulerException {
        return false
    }

    @Override
    void triggerJob(JobKey jobKey) throws SchedulerException {

    }

    @Override
    void triggerJob(JobKey jobKey, JobDataMap jobDataMap) throws SchedulerException {

    }

    @Override
    void pauseJob(JobKey jobKey) throws SchedulerException {

    }

    @Override
    void pauseJobs(GroupMatcher<JobKey> groupMatcher) throws SchedulerException {

    }

    @Override
    void pauseTrigger(TriggerKey triggerKey) throws SchedulerException {

    }

    @Override
    void pauseTriggers(GroupMatcher<TriggerKey> groupMatcher) throws SchedulerException {

    }

    @Override
    void resumeJob(JobKey jobKey) throws SchedulerException {

    }

    @Override
    void resumeJobs(GroupMatcher<JobKey> groupMatcher) throws SchedulerException {

    }

    @Override
    void resumeTrigger(TriggerKey triggerKey) throws SchedulerException {

    }

    @Override
    void resumeTriggers(GroupMatcher<TriggerKey> groupMatcher) throws SchedulerException {

    }

    @Override
    Set<JobKey> getJobKeys(GroupMatcher<JobKey> groupMatcher) throws SchedulerException {
        return null
    }

    @Override
    List<? extends Trigger> getTriggersOfJob(JobKey jobKey) throws SchedulerException {
        return null
    }

    @Override
    Set<TriggerKey> getTriggerKeys(GroupMatcher<TriggerKey> groupMatcher) throws SchedulerException {
        return null
    }

    @Override
    JobDetail getJobDetail(JobKey jobKey) throws SchedulerException {
        return null
    }

    @Override
    Trigger getTrigger(TriggerKey triggerKey) throws SchedulerException {
        return null
    }

    @Override
    Trigger.TriggerState getTriggerState(TriggerKey triggerKey) throws SchedulerException {
        return null
    }

    @Override
    boolean interrupt(JobKey jobKey) throws UnableToInterruptJobException {
        return false
    }

    @Override
    boolean interrupt(String s) throws UnableToInterruptJobException {
        return false
    }

    @Override
    boolean checkExists(JobKey jobKey) throws SchedulerException {
        return false
    }

    @Override
    boolean checkExists(TriggerKey triggerKey) throws SchedulerException {
        return false
    }

    @Override
    void clear() throws SchedulerException {

    }
}
