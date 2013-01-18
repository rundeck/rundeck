import grails.test.GrailsUnitTestCase
import org.quartz.*
import org.quartz.spi.JobFactory
import org.springframework.context.MessageSource
import rundeck.*
import rundeck.services.FrameworkService
import rundeck.services.ScheduledExecutionService
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
class ScheduledExServiceTests extends GrailsUnitTestCase {

    protected void setUp() {
        super.setUp();
    }

    /**
     * Test getByIDorUUID method.
     */
    public void testGetByIDorUUID() {
        mockDomain(ScheduledExecution)
        mockDomain(Workflow)
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

        ScheduledExecution.metaClass.static.findByUuid = {uuid-> uuid=='testUUID'?se:null }

        def result = testService.getByIDorUUID('testUUID')
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
    public void testGetByIDorUUIDWithOverlap() {
        mockDomain(ScheduledExecution)
        mockDomain(Workflow)

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
        String idstr=id.toString()

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

        ScheduledExecution.metaClass.static.findByUuid = { uuid-> uuid=='testUUID'? se : uuid==idstr?se2:null }
        assertEquals(se,ScheduledExecution.findByUuid('testUUID'))

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

    public void testDoValidate() {
        mockDomain(ScheduledExecution)
        mockDomain(Workflow)
        def testService = new ScheduledExecutionService()

        if (true) {//failure on empty input
            def fwkControl = mockFor(FrameworkService, true)
            fwkControl.demand.getFrameworkFromUserSession {session, request -> return null }
            fwkControl.demand.existsFrameworkProject {project, framework -> return false }
            testService.frameworkService = fwkControl.createMock()

            def params = [:]
            def results = testService._dovalidate(params,'test','test',null)
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
        if (true) {//test basic passing input
            def fwkControl = mockFor(FrameworkService, true)
            fwkControl.demand.getFrameworkFromUserSession {session, request -> return null }
            fwkControl.demand.existsFrameworkProject {project, framework ->
                assertEquals 'testProject', project
                return true
            }
            fwkControl.demand.getCommand {project, type, command, framework ->
                assertEquals 'testProject', project
                assertEquals 'aType', type
                assertEquals 'aCommand', command
                return null
            }
            testService.frameworkService = fwkControl.createMock()

            def params = [jobName: 'monkey1', project: 'testProject', description: 'blah', adhocExecution: true, adhocRemoteString: 'a command']
            def results = testService._dovalidate(params,'test','test',null)
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
            assertFalse execution.adhocExecution
            assertNotNull execution.workflow
            assertEquals 1, execution.workflow.commands.size()
            final CommandExec exec = execution.workflow.commands[0]
            assertEquals 'a command', exec.adhocRemoteString
            assertTrue exec.adhocExecution
        }
        if (true) {//test invalid job name
            def fwkControl = mockFor(FrameworkService, true)
            fwkControl.demand.getFrameworkFromUserSession {session, request -> return null }
            fwkControl.demand.existsFrameworkProject {project, framework ->
                assertEquals 'testProject', project
                return true
            }
            fwkControl.demand.getCommand {project, type, command, framework ->
                assertEquals 'testProject', project
                assertEquals 'aType', type
                assertEquals 'aCommand', command
                return null
            }
            testService.frameworkService = fwkControl.createMock()

            def params = [jobName: 'test/monkey', project: 'testProject', description: 'blah', adhocExecution: true, adhocRemoteString: 'a command']
            def results = testService._dovalidate(params,'test','test',null)
            assertTrue(results.failed)
            def sce = results.scheduledExecution

            assertTrue sce.errors.hasErrors()
            assertTrue sce.errors.hasFieldErrors('jobName')

        }
    }

    public void testDoValidateNodedispatchIsBlank() {
        mockDomain(ScheduledExecution)
        mockDomain(Workflow)
        def testService = new ScheduledExecutionService()

        //test nodedispatch true, threadcount should default to 1 if input is blank
        def fwkControl = mockFor(FrameworkService, true)
        fwkControl.demand.getFrameworkFromUserSession {session, request -> return null }
        fwkControl.demand.existsFrameworkProject {project, framework ->
            assertEquals 'testProject', project
            return true
        }
        fwkControl.demand.getCommand {project, type, command, framework ->
            assertEquals 'testProject', project
            assertEquals 'aType', type
            assertEquals 'aCommand', command
            return null
        }
        testService.frameworkService = fwkControl.createMock()

        def params = [jobName: 'monkey1', project: 'testProject', description: 'blah', adhocExecution: true, adhocRemoteString: 'a command',doNodedispatch: 'true',nodeInclude: 'blah',nodeThreadcount: ""]
        def results = testService._dovalidate(params,'test','test',null)
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
        assertEquals(1,execution.nodeThreadcount)
    }

    public void testDoValidateWorkflow() {
        def testService = new ScheduledExecutionService()
        if (true) {//test job with options
            def fwkControl = mockFor(FrameworkService, true)
            fwkControl.demand.getFrameworkFromUserSession {session, request -> return null }
            fwkControl.demand.existsFrameworkProject {project, framework ->
                assertEquals 'testProject', project
                return true
            }
            testService.frameworkService = fwkControl.createMock()

            def params = [jobName: 'monkey1', project: 'testProject', description: 'blah',
                    workflow: [threadcount: 1, keepgoing: true, "commands[0]": [adhocExecution: true, adhocRemoteString: 'a remote string']]
            ]
            def results = testService._dovalidate(params,'test','test',null)
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
            assertLength(1, wf.commands as Object[])
            final Iterator iterator = wf.commands.iterator()
            assert iterator.hasNext()
            final CommandExec next = iterator.next()
            assertNotNull(next)
            assertFalse(next instanceof JobExec)
            assertEquals('a remote string', next.adhocRemoteString)
        }
        if (true) {//test job with options
            def fwkControl = mockFor(FrameworkService, true)
            fwkControl.demand.getFrameworkFromUserSession {session, request -> return null }
            fwkControl.demand.existsFrameworkProject {project, framework ->
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
            def results = testService._dovalidate(params,'test','test',null)
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
                workflow: new Workflow(keepgoing: true,strategy: 'step-first',commands: [
                        new CommandExec(adhocRemoteString: 'test command1', adhocExecution: true, errorHandler:
                                new JobExec(jobGroup: 'test1',jobName: 'blah')),
                        new CommandExec(adhocRemoteString: 'test command2', adhocExecution: true,errorHandler:
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
                workflow: new Workflow(keepgoing: true,strategy: 'node-first',commands: [
                        new CommandExec(adhocRemoteString: 'test command1', adhocExecution: true, errorHandler:
                                new JobExec(jobGroup: 'test1',jobName: 'blah')),
                        new CommandExec(adhocRemoteString: 'test command2', adhocExecution: true,errorHandler:
                                new CommandExec(adhocRemoteString: 'test ehcommand', adhocExecution: true)),
                        new JobExec(jobGroup: 'test1', jobName: 'blah2', errorHandler:
                                new CommandExec(adhocRemoteString: 'test command1', adhocExecution: true)),
                        new JobExec(jobGroup: 'test1', jobName: 'blah3', errorHandler:
                                new JobExec(jobGroup: 'test1', jobName: 'blah4')),
                ])
        ]
        def results = testService._dovalidate(params, 'test', 'test', null)
        results.scheduledExecution.workflow.commands.each{ cmd->
            if (cmd.errors.hasErrors()) {
                cmd.errors.allErrors.each {
                    System.out.println("command: "+cmd+", error: "+it);
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
            fwkControl.demand.getFrameworkFromUserSession {session, request -> return null }
            fwkControl.demand.existsFrameworkProject {project, framework ->
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
            def results = testService._dovalidate(params,'test','test',null)
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

    public void testDoValidateAdhoc() {
        def testService = new ScheduledExecutionService()

        if (true) {//failure on missing adhoc script props
            def fwkControl = mockFor(FrameworkService, true)
            fwkControl.demand.getFrameworkFromUserSession {session, request -> return null }
            fwkControl.demand.existsFrameworkProject {project, framework ->
                assertEquals 'testProject', project
                return true
            }
            fwkControl.demand.getCommand {project, type, command, framework ->
                assertEquals 'testProject', project
                assertEquals 'aType', type
                assertEquals 'aCommand', command
                return null
            }
            testService.frameworkService = fwkControl.createMock()

            def params = [jobName: 'monkey1', project: 'testProject', description: 'blah', adhocExecution: true]
            def results = testService._dovalidate(params,'test','test',null)

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

        if (true) {//both adhocRemote/adhocLocal should result in validation error
            def fwkControl = mockFor(FrameworkService, true)
            fwkControl.demand.getFrameworkFromUserSession {session, request -> return null }
            fwkControl.demand.existsFrameworkProject {project, framework ->
                assertEquals 'testProject', project
                return true
            }
            fwkControl.demand.getCommand {project, type, command, framework ->
                assertEquals 'testProject', project
                assertEquals 'aType', type
                assertEquals 'aCommand', command
                return null
            }
            testService.frameworkService = fwkControl.createMock()

            def params = [jobName: 'monkey1', project: 'testProject', description: 'blah', type: '', command: '', adhocExecution: true, adhocRemoteString: 'test1', adhocLocalString: 'test2']
            def results = testService._dovalidate(params,'test','test',null)

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
        if (true) {//test basic passing input (adhocRemoteString)
            def fwkControl = mockFor(FrameworkService, true)
            fwkControl.demand.getFrameworkFromUserSession {session, request -> return null }
            fwkControl.demand.existsFrameworkProject {project, framework ->
                assertEquals 'testProject', project
                return true
            }
            fwkControl.demand.getCommand {project, type, command, framework ->
                assertEquals 'testProject', project
                assertEquals 'aType', type
                assertEquals 'aCommand', command
                return null
            }
            testService.frameworkService = fwkControl.createMock()

            def params = [jobName: 'monkey1', project: 'testProject', description: 'blah', type: '', command: '', adhocExecution: true, adhocRemoteString: 'test what']
            def results = testService._dovalidate(params,'test','test',null)
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
            assertFalse execution.adhocExecution
            assertNotNull execution.workflow
            assertNotNull execution.workflow.commands
            assertEquals 1, execution.workflow.commands.size()
            def CommandExec cexec = execution.workflow.commands[0]
            assertTrue cexec.adhocExecution
            assertEquals 'test what', cexec.adhocRemoteString
            assertNull execution.adhocLocalString
            assertNull execution.adhocFilepath
            assertNull execution.argString
        }
        if (true) {//test basic passing input (adhocLocalString)
            def fwkControl = mockFor(FrameworkService, true)
            fwkControl.demand.getFrameworkFromUserSession {session, request -> return null }
            fwkControl.demand.existsFrameworkProject {project, framework ->
                assertEquals 'testProject', project
                return true
            }
            fwkControl.demand.getCommand {project, type, command, framework ->
                assertEquals 'testProject', project
                assertEquals 'aType', type
                assertEquals 'aCommand', command
                return null
            }
            testService.frameworkService = fwkControl.createMock()

            def params = [jobName: 'monkey1', project: 'testProject', description: 'blah', adhocExecution: true, adhocLocalString: 'test what']
            def results = testService._dovalidate(params,'test','test',null)
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
            assertFalse execution.adhocExecution
            assertNotNull execution.workflow
            assertNotNull execution.workflow.commands
            assertEquals 1, execution.workflow.commands.size()
            def CommandExec cexec = execution.workflow.commands[0]
            assertTrue cexec.adhocExecution
            assertEquals 'test what', cexec.adhocLocalString
            assertNull execution.adhocRemoteString
            assertNull execution.adhocFilepath
            assertNull execution.argString
        }
        if (true) {//test basic passing input (adhocFilepath)
            def fwkControl = mockFor(FrameworkService, true)
            fwkControl.demand.getFrameworkFromUserSession {session, request -> return null }
            fwkControl.demand.existsFrameworkProject {project, framework ->
                assertEquals 'testProject', project
                return true
            }
            fwkControl.demand.getCommand {project, type, command, framework ->
                assertEquals 'testProject', project
                assertEquals 'aType', type
                assertEquals 'aCommand', command
                return null
            }
            testService.frameworkService = fwkControl.createMock()

            def params = [jobName: 'monkey1', project: 'testProject', description: 'blah', type: '', command: '', adhocExecution: true, adhocFilepath: 'test what']
            def results = testService._dovalidate(params,'test','test',null)
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
            assertFalse execution.adhocExecution
            assertNotNull execution.workflow
            assertNotNull execution.workflow.commands
            assertEquals 1, execution.workflow.commands.size()
            def CommandExec cexec = execution.workflow.commands[0]
            assertTrue cexec.adhocExecution
            assertEquals 'test what', cexec.adhocFilepath
            assertNull execution.adhocLocalString
            assertNull execution.adhocRemoteString
            assertNull execution.argString
        }
        if (true) {//test argString input for adhocFilepath
            def fwkControl = mockFor(FrameworkService, true)
            fwkControl.demand.getFrameworkFromUserSession {session, request -> return null }
            fwkControl.demand.existsFrameworkProject {project, framework ->
                assertEquals 'testProject', project
                return true
            }
            fwkControl.demand.getCommand {project, type, command, framework ->
                assertEquals 'testProject', project
                assertEquals 'aType', type
                assertEquals 'aCommand', command
                return null
            }
            testService.frameworkService = fwkControl.createMock()

            def params = [jobName: 'monkey1', project: 'testProject', description: 'blah', type: '', command: '',
                    adhocExecution: true,
                    adhocFilepath: 'test file',
                    argString: 'test args'
            ]
            def results = testService._dovalidate(params,'test','test',null)
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
            assertFalse execution.adhocExecution
            assertNotNull execution.workflow
            assertNotNull execution.workflow.commands
            assertEquals 1, execution.workflow.commands.size()
            def CommandExec cexec = execution.workflow.commands[0]
            assertTrue cexec.adhocExecution
            assertEquals 'test file', cexec.adhocFilepath
            assertNull execution.adhocLocalString
            assertNull execution.adhocRemoteString
            assertEquals 'test args', execution.argString
        }
        if (true) {//test argString input for adhocRemoteString argString should be set
            def fwkControl = mockFor(FrameworkService, true)
            fwkControl.demand.getFrameworkFromUserSession {session, request -> return null }
            fwkControl.demand.existsFrameworkProject {project, framework ->
                assertEquals 'testProject', project
                return true
            }
            fwkControl.demand.getCommand {project, type, command, framework ->
                assertEquals 'testProject', project
                assertEquals 'aType', type
                assertEquals 'aCommand', command
                return null
            }
            testService.frameworkService = fwkControl.createMock()

            def params = [jobName: 'monkey1', project: 'testProject', description: 'blah', type: '', command: '',
                    adhocExecution: true,
                    adhocExecutionType: 'remote',
                    adhocRemoteString: 'test remote',
                    argString: 'test args'
            ]
            def results = testService._dovalidate(params,'test','test',null)
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
            assertFalse execution.adhocExecution
            assertNotNull execution.workflow
            assertNotNull execution.workflow.commands
            assertEquals 1, execution.workflow.commands.size()
            def CommandExec cexec = execution.workflow.commands[0]
            assertTrue cexec.adhocExecution
            assertEquals 'test remote', cexec.adhocRemoteString
            assertNull execution.adhocLocalString
            assertNull execution.adhocFilepath
            assertEquals 'test args', execution.argString
        }
        if (true) {//test argString input for adhocLocalString
            def fwkControl = mockFor(FrameworkService, true)
            fwkControl.demand.getFrameworkFromUserSession {session, request -> return null }
            fwkControl.demand.existsFrameworkProject {project, framework ->
                assertEquals 'testProject', project
                return true
            }
            fwkControl.demand.getCommand {project, type, command, framework ->
                assertEquals 'testProject', project
                assertEquals 'aType', type
                assertEquals 'aCommand', command
                return null
            }
            testService.frameworkService = fwkControl.createMock()

            def params = [jobName: 'monkey1', project: 'testProject', description: 'blah', type: '', command: '',
                    adhocExecution: true,
                    adhocExecutionType: 'local',
                    adhocLocalString: 'test local',
                    argString: 'test args'
            ]
            def results = testService._dovalidate(params,'test','test',null)
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
            assertFalse execution.adhocExecution
            assertNotNull execution.workflow
            assertNotNull execution.workflow.commands
            assertEquals 1, execution.workflow.commands.size()
            def CommandExec cexec = execution.workflow.commands[0]
            assertTrue cexec.adhocExecution
            assertEquals 'test local', cexec.adhocLocalString
            assertNull execution.adhocFilepath
            assertNull execution.adhocRemoteString
            assertEquals 'test args', execution.argString
        }
    }

    public void testDoValidateNotifications() {

        def sec = new ScheduledExecutionService()
        if (true) {//test job with notifications
            def fwkControl = mockFor(FrameworkService, true)
            fwkControl.demand.getFrameworkFromUserSession {session, request -> return null }
            fwkControl.demand.existsFrameworkProject {project, framework ->
                assertEquals 'testProject', project
                return true
            }
            fwkControl.demand.getCommand {project, type, command, framework ->
                assertEquals 'testProject', project
                assertEquals 'aType', type
                assertEquals 'aCommand', command
                return null
            }
            sec.frameworkService = fwkControl.createMock()

            def params = [jobName: 'monkey1', project: 'testProject', description: 'blah', adhocExecution: true, adhocRemoteString: "test command",
                    notifications: [[eventTrigger: 'onsuccess', type: 'email', content: 'c@example.com,d@example.com']]
            ]
            def results = sec._dovalidate(params,'test','test',null)
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
            assertFalse execution.adhocExecution
            assertNotNull execution.workflow
            assertNotNull execution.workflow.commands
            assertEquals 1, execution.workflow.commands.size()
            def CommandExec cexec = execution.workflow.commands[0]
            assertTrue cexec.adhocExecution
            assertEquals 'test command', cexec.adhocRemoteString
            assertNull cexec.adhocFilepath
            assertNull execution.adhocLocalString
            assertNull execution.adhocRemoteString
            assertNull execution.argString

            assertNotNull execution.notifications
            assertEquals 1, execution.notifications.size()
            final def not1 = execution.notifications.iterator().next()
            assertTrue(not1 instanceof Notification)
            def Notification n = not1
            assertEquals "onsuccess", n.eventTrigger
            assertEquals "email", n.type
            assertEquals "c@example.com,d@example.com", n.content
        }
        if (true) {//test job with notifications
            def fwkControl = mockFor(FrameworkService, true)
            fwkControl.demand.getFrameworkFromUserSession {session, request -> return null }
            fwkControl.demand.existsFrameworkProject {project, framework ->
                assertEquals 'testProject', project
                return true
            }
            fwkControl.demand.getCommand {project, type, command, framework ->
                assertEquals 'testProject', project
                assertEquals 'aType', type
                assertEquals 'aCommand', command
                return null
            }
            sec.frameworkService = fwkControl.createMock()

            def params = [jobName: 'monkey1', project: 'testProject', description: 'blah', adhocExecution: true, adhocRemoteString: 'test command',
                    notifications: [[eventTrigger: 'onsuccess', type: 'email', content: 'c@example.com,d@example.com'], [eventTrigger: 'onfailure', type: 'email', content: 'monkey@example.com']]
            ]
            def results = sec._dovalidate(params,'test','test',null)
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
            assertFalse execution.adhocExecution
            assertNotNull execution.workflow
            assertNotNull execution.workflow.commands
            assertEquals 1, execution.workflow.commands.size()
            def CommandExec cexec = execution.workflow.commands[0]
            assertTrue cexec.adhocExecution
            assertEquals 'test command', cexec.adhocRemoteString
            assertNull cexec.adhocFilepath
            assertNull execution.adhocLocalString
            assertNull execution.adhocRemoteString
            assertNull execution.argString

            assertNotNull execution.notifications
            assertEquals 2, execution.notifications.size()
            def nmap = [:]
            execution.notifications.each {not1 ->
                nmap[not1.eventTrigger] = not1
            }
            assertNotNull(nmap.onsuccess)
            assertNotNull(nmap.onfailure)
            assertTrue(nmap.onsuccess instanceof Notification)
            assertTrue(nmap.onfailure instanceof Notification)
            def Notification n = nmap.onsuccess
            assertEquals "onsuccess", n.eventTrigger
            assertEquals "email", n.type
            assertEquals "c@example.com,d@example.com", n.content
            def Notification n2 = nmap.onfailure
            assertEquals "onfailure", n2.eventTrigger
            assertEquals "email", n2.type
            assertEquals "monkey@example.com", n2.content
        }
        if (true) {//test job with notifications, using form input fields for params
            def fwkControl = mockFor(FrameworkService, true)
            fwkControl.demand.getFrameworkFromUserSession {session, request -> return null }
            fwkControl.demand.existsFrameworkProject {project, framework ->
                assertEquals 'testProject', project
                return true
            }
            fwkControl.demand.getCommand {project, type, command, framework ->
                assertEquals 'testProject', project
                assertEquals 'aType', type
                assertEquals 'aCommand', command
                return null
            }
            sec.frameworkService = fwkControl.createMock()

            def params = [jobName: 'monkey1', project: 'testProject', description: 'blah', adhocExecution: true, adhocRemoteString: 'test command',
                    notifyOnsuccess: 'true', notifySuccessRecipients: 'c@example.com,d@example.com',
                    notifyOnfailure: 'true', notifyFailureRecipients: 'monkey@example.com',

//                notifications:[onsuccess:[email:'c@example.com,d@example.com'],onfailure:[email:'monkey@example.com']]
            ]
            def results = sec._dovalidate(params,'test','test',null)
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
            assertFalse execution.adhocExecution
            assertNotNull execution.workflow
            assertNotNull execution.workflow.commands
            assertEquals 1, execution.workflow.commands.size()
            def CommandExec cexec = execution.workflow.commands[0]
            assertTrue cexec.adhocExecution
            assertEquals 'test command', cexec.adhocRemoteString
            assertNull cexec.adhocFilepath
            assertNull execution.adhocLocalString
            assertNull execution.adhocRemoteString
            assertNull execution.argString

            assertNotNull execution.notifications
            assertEquals 2, execution.notifications.size()
            def nmap = [:]
            execution.notifications.each {not1 ->
                nmap[not1.eventTrigger] = not1
            }
            assertNotNull(nmap.onsuccess)
            assertNotNull(nmap.onfailure)
            assertTrue(nmap.onsuccess instanceof Notification)
            assertTrue(nmap.onfailure instanceof Notification)
            def Notification n = nmap.onsuccess
            assertEquals "onsuccess", n.eventTrigger
            assertEquals "email", n.type
            assertEquals "c@example.com,d@example.com", n.content
            def Notification n2 = nmap.onfailure
            assertEquals "onfailure", n2.eventTrigger
            assertEquals "email", n2.type
            assertEquals "monkey@example.com", n2.content
        }

        if (true) {//test job with notifications, invalid email addresses
            def fwkControl = mockFor(FrameworkService, true)
            fwkControl.demand.getFrameworkFromUserSession {session, request -> return null }
            fwkControl.demand.existsFrameworkProject {project, framework ->
                assertEquals 'testProject', project
                return true
            }
            fwkControl.demand.getCommand {project, type, command, framework ->
                assertEquals 'testProject', project
                assertEquals 'aType', type
                assertEquals 'aCommand', command
                return null
            }
            sec.frameworkService = fwkControl.createMock()

            def params = [jobName: 'monkey1', project: 'testProject', description: 'blah', adhocExecution: false, name: 'aResource', type: 'aType', command: 'aCommand',
                    notifyOnsuccess: 'true', notifySuccessRecipients: 'c@example.',
            ]
            def results = sec._dovalidate(params,'test','test',null)
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
            assertTrue(execution.errors.hasFieldErrors('notifySuccessRecipients'))
            assertFalse(execution.errors.hasFieldErrors('notifyFailureRecipients'))
        }
        if (true) {//test job with notifications, invalid email addresses
            def fwkControl = mockFor(FrameworkService, true)
            fwkControl.demand.getFrameworkFromUserSession {session, request -> return null }
            fwkControl.demand.existsFrameworkProject {project, framework ->
                assertEquals 'testProject', project
                return true
            }
            fwkControl.demand.getCommand {project, type, command, framework ->
                assertEquals 'testProject', project
                assertEquals 'aType', type
                assertEquals 'aCommand', command
                return null
            }
            sec.frameworkService = fwkControl.createMock()

            def params = [jobName: 'monkey1', project: 'testProject', description: 'blah', adhocExecution: false, name: 'aResource', type: 'aType', command: 'aCommand',
                    notifyOnfailure: 'true', notifyFailureRecipients: '@example.com',
            ]
            def results = sec._dovalidate(params,'test','test',null)
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
            assertTrue(execution.errors.hasFieldErrors('notifyFailureRecipients'))
            assertFalse(execution.errors.hasFieldErrors('notifySuccessRecipients'))
        }
        if (true) {//test job with notifications, invalid email addresses using map based notifications definition
            def fwkControl = mockFor(FrameworkService, true)
            fwkControl.demand.getFrameworkFromUserSession {session, request -> return null }
            fwkControl.demand.existsFrameworkProject {project, framework ->
                assertEquals 'testProject', project
                return true
            }
            fwkControl.demand.getCommand {project, type, command, framework ->
                assertEquals 'testProject', project
                assertEquals 'aType', type
                assertEquals 'aCommand', command
                return null
            }
            sec.frameworkService = fwkControl.createMock()

            def params = [jobName: 'monkey1', project: 'testProject', description: 'blah', adhocExecution: false, name: 'aResource', type: 'aType', command: 'aCommand',
                    notifications: [[eventTrigger: 'onsuccess', type: 'email', content: 'c@example.comd@example.com'], [eventTrigger: 'onfailure', type: 'email', content: 'monkey@ example.com']]
            ]
            def results = sec._dovalidate(params,'test','test',null)
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
            assertTrue(execution.errors.hasFieldErrors('notifyFailureRecipients'))
            assertTrue(execution.errors.hasFieldErrors('notifySuccessRecipients'))
        }
        if (true) {//test job with notifications, invalid urls
            def fwkControl = mockFor(FrameworkService, true)
            fwkControl.demand.getFrameworkFromUserSession {session, request -> return null }
            fwkControl.demand.existsFrameworkProject {project, framework ->
                assertEquals 'testProject', project
                return true
            }
            fwkControl.demand.getCommand {project, type, command, framework ->
                assertEquals 'testProject', project
                assertEquals 'aType', type
                assertEquals 'aCommand', command
                return null
            }
            sec.frameworkService = fwkControl.createMock()

            def params = [jobName: 'monkey1', project: 'testProject', description: 'blah2', adhocExecution: false, name: 'aResource', type: 'aType', command: 'aCommand',
                    notifications: [[eventTrigger: 'onsuccess', type: 'url', content: 'c@example.comd@example.com'], [eventTrigger: 'onfailure', type: 'url', content: 'monkey@ example.com']]
            ]
            def results = sec._dovalidate(params,'test','test',null)
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
            assertTrue(execution.errors.hasFieldErrors('notifyFailureUrl'))
            assertTrue(execution.errors.hasFieldErrors('notifySuccessUrl'))
        }
    }

    public void testDoValidateOptions() {

        def sec = new ScheduledExecutionService()
        if (true) {//test job with options
            def fwkControl = mockFor(FrameworkService, true)
            fwkControl.demand.getFrameworkFromUserSession {session, request -> return null }
            fwkControl.demand.existsFrameworkProject {project, framework ->
                assertEquals 'testProject', project
                return true
            }
            fwkControl.demand.getCommand {project, type, command, framework ->
                assertEquals 'testProject', project
                assertEquals 'aType', type
                assertEquals 'aCommand', command
                return null
            }
            sec.frameworkService = fwkControl.createMock()

            def params = [jobName: 'monkey1', project: 'testProject', description: 'blah', adhocExecution: false, name: 'aResource', type: 'aType', command: 'aCommand',
                    options: ["options[0]": [name: 'test3', defaultValue: 'val3', enforced: false, valuesUrl: "http://test.com/test3"]]
            ]
            def results = sec._dovalidate(params,'test','test',null)
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
        }
        if (true) {//test job with old-style options
            def fwkControl = mockFor(FrameworkService, true)
            fwkControl.demand.getFrameworkFromUserSession {session, request -> return null }
            fwkControl.demand.existsFrameworkProject {project, framework ->
                assertEquals 'testProject', project
                return true
            }
            fwkControl.demand.getCommand {project, type, command, framework ->
                assertEquals 'testProject', project
                assertEquals 'aType', type
                assertEquals 'aCommand', command
                return null
            }
            sec.frameworkService = fwkControl.createMock()

            def params = [jobName: 'monkey1', project: 'testProject', description: 'blah', adhocExecution: true, adhocRemoteString: 'test command',
                    'option.test3': 'val3', options: ["options[0]": [name: 'test3', defaultValue: 'val3', enforced: false, valuesUrl: "http://test.com/test3"]]
            ]
            def results = sec._dovalidate(params,'test','test',null)
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
        if (true) {//invalid options: no name
            def fwkControl = mockFor(FrameworkService, true)
            fwkControl.demand.getFrameworkFromUserSession {session, request -> return null }
            fwkControl.demand.existsFrameworkProject {project, framework ->
                assertEquals 'testProject', project
                return true
            }
            fwkControl.demand.getCommand {project, type, command, framework ->
                assertEquals 'testProject', project
                assertEquals 'aType', type
                assertEquals 'aCommand', command
                return null
            }
            sec.frameworkService = fwkControl.createMock()

            def ms = mockFor(MessageSource)
            ms.demand.getMessage {key,data,locale -> 'message'}
            ms.demand.getMessage {error,locale -> 'message'}
            sec.messageSource = ms.createMock()

            def params = [jobName: 'monkey1', project: 'testProject', description: 'blah', adhocExecution: false, name: 'aResource', type: 'aType', command: 'aCommand',
                    options: ["options[0]": [defaultValue: 'val3', enforced: false, valuesUrl: "http://test.com/test3"]]
            ]
            def results = sec._dovalidate(params,'test','test',null)
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
        if (true) {//invalid options: invalid valuesUrl
            def fwkControl = mockFor(FrameworkService, true)
            fwkControl.demand.getFrameworkFromUserSession {session, request -> return null }
            fwkControl.demand.existsFrameworkProject {project, framework ->
                assertEquals 'testProject', project
                return true
            }
            fwkControl.demand.getCommand {project, type, command, framework ->
                assertEquals 'testProject', project
                assertEquals 'aType', type
                assertEquals 'aCommand', command
                return null
            }
            sec.frameworkService = fwkControl.createMock()

            def params = [jobName: 'monkey1', project: 'testProject', description: 'blah', adhocExecution: false, name: 'aResource', type: 'aType', command: 'aCommand',
                    options: ["options[0]": [name: 'opt3', defaultValue: 'val3', enforced: false, valuesUrl: "hzttp://test.com/test3"]]
            ]
            def results = sec._dovalidate(params,'test','test',null)
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
        if (true) {//valid multi option
            def fwkControl = mockFor(FrameworkService, true)
            fwkControl.demand.getFrameworkFromUserSession {session, request -> return null }
            fwkControl.demand.existsFrameworkProject {project, framework ->
                assertEquals 'testProject', project
                return true
            }
            fwkControl.demand.getCommand {project, type, command, framework ->
                assertEquals 'testProject', project
                assertEquals 'aType', type
                assertEquals 'aCommand', command
                return null
            }
            sec.frameworkService = fwkControl.createMock()

            def params = [jobName: 'monkey1', project: 'testProject', description: 'blah', adhocExecution: false, name: 'aResource', type: 'aType', command: 'aCommand',
                    options: ["options[0]": [name: 'opt3', defaultValue: 'val3', enforced: false, multivalued: true, delimiter: ' ']]
            ]
            def results = sec._dovalidate(params,'test','test',null)
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
        if (true) {//invalid multi option, no delimiter
            def fwkControl = mockFor(FrameworkService, true)
            fwkControl.demand.getFrameworkFromUserSession {session, request -> return null }
            fwkControl.demand.existsFrameworkProject {project, framework ->
                assertEquals 'testProject', project
                return true
            }
            fwkControl.demand.getCommand {project, type, command, framework ->
                assertEquals 'testProject', project
                assertEquals 'aType', type
                assertEquals 'aCommand', command
                return null
            }
            sec.frameworkService = fwkControl.createMock()

            def ms = mockFor(MessageSource)
            ms.demand.getMessage {key, data, locale -> 'message'}
            ms.demand.getMessage {error, locale -> 'message'}
            sec.messageSource = ms.createMock()

            def params = [jobName: 'monkey1', project: 'testProject', description: 'blah', adhocExecution: false, name: 'aResource', type: 'aType', command: 'aCommand',
                    options: ["options[0]": [name: 'opt3', defaultValue: 'val3', enforced: false, multivalued: true]]
            ]
            def results = sec._dovalidate(params,'test','test',null)
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
        if (true) {//secure option with multi-valued is invalid
            def fwkControl = mockFor(FrameworkService, true)
            fwkControl.demand.getFrameworkFromUserSession {session, request -> return null }
            fwkControl.demand.existsFrameworkProject {project, framework ->
                assertEquals 'testProject', project
                return true
            }
            fwkControl.demand.getCommand {project, type, command, framework ->
                assertEquals 'testProject', project
                assertEquals 'aType', type
                assertEquals 'aCommand', command
                return null
            }
            sec.frameworkService = fwkControl.createMock()

            def ms = mockFor(MessageSource)
            ms.demand.getMessage {key, data, locale -> 'message'}
            ms.demand.getMessage {error, locale -> 'message'}
            sec.messageSource = ms.createMock()

            def params = [jobName: 'monkey1', project: 'testProject', description: 'blah', adhocExecution: false, name: 'aResource', type: 'aType', command: 'aCommand',
                    options: ["options[0]": [name: 'opt3', defaultValue: 'val3', enforced: false, multivalued: true, secureInput: true]]
            ]
            def results = sec._dovalidate(params,'test','test',null)
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

    public void testDoUpdate() {
        def sec = new ScheduledExecutionService()
        if (true) {//test update basic job details
            def se = new ScheduledExecution(jobName: 'monkey1', project: 'testProject', description: 'blah', adhocExecution: true, adhocRemoteString: 'test command',)
            se.save()

            assertNotNull se.id

            //try to do update of the ScheduledExecution
            def fwkControl = mockFor(FrameworkService, true)
            fwkControl.demand.getFrameworkFromUserSession {session, request -> return null }
            fwkControl.demand.existsFrameworkProject {project, framework ->
                assertEquals 'testProject2', project
                return true
            }
            fwkControl.demand.getCommand {project, type, command, framework ->
                assertEquals 'testProject2', project
                assertEquals 'aType2', type
                assertEquals 'aCommand2', command
                return null
            }
            fwkControl.demand.authorizeProjectJobAll {framework, resource, actions, project -> return true}
            fwkControl.demand.getFrameworkFromUserSession {session, request -> return null }
            fwkControl.demand.getFrameworkFromUserSession {session, request -> return null }
            sec.frameworkService = fwkControl.createMock()
//            def sesControl = mockFor(ScheduledExecutionService, true)
//            sesControl.demand.getByIDorUUID {id -> return se }
//            sec.scheduledExecutionService = sesControl.createMock()

            def params = [id: se.id.toString(), jobName: 'monkey2', project: 'testProject2', description: 'blah', adhocExecution: true, adhocRemoteString: 'test command',]
            def results = sec._doupdate(params,'test','test',null)
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

            assertFalse execution.adhocExecution
            assertNotNull execution.workflow
            assertNotNull execution.workflow.commands
            assertEquals 1, execution.workflow.commands.size()
            def CommandExec cexec = execution.workflow.commands[0]
            assertTrue cexec.adhocExecution
            assertEquals 'test command', cexec.adhocRemoteString
            assertNull cexec.adhocFilepath
            assertNull execution.adhocLocalString
            assertNull execution.adhocRemoteString
            assertNull execution.argString

            assertNull execution.notifications
            assertNull execution.options
        }
        if (true) {//test update job name invalid
            def se = new ScheduledExecution(jobName: 'monkey1', project: 'testProject', description: 'blah', adhocExecution: true, adhocRemoteString: 'test command',)
            se.save()

            assertNotNull se.id

            //try to do update of the ScheduledExecution
            def fwkControl = mockFor(FrameworkService, true)
            fwkControl.demand.getFrameworkFromUserSession {session, request -> return null }
            fwkControl.demand.existsFrameworkProject {project, framework ->
                assertEquals 'testProject2', project
                return true
            }
            fwkControl.demand.getCommand {project, type, command, framework ->
                assertEquals 'testProject2', project
                assertEquals 'aType2', type
                assertEquals 'aCommand2', command
                return null
            }
            fwkControl.demand.authorizeProjectJobAll {framework, resource, actions, project -> return true}
            fwkControl.demand.getFrameworkFromUserSession {session, request -> return null }
            fwkControl.demand.getFrameworkFromUserSession {session, request -> return null }
            sec.frameworkService = fwkControl.createMock()
//            def sesControl = mockFor(ScheduledExecutionService, true)
//            sesControl.demand.getByIDorUUID {id -> return se }
//            sec.scheduledExecutionService = sesControl.createMock()

            def params = [id: se.id.toString(), jobName: 'test/monkey2', project: 'testProject2', description: 'blah', adhocExecution: true, adhocRemoteString: 'test command',]
            def results = sec._doupdate(params,'test','test',null)
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
            fwkControl.demand.getFrameworkFromUserSession {session, request -> return null }
            fwkControl.demand.existsFrameworkProject {project, framework ->
                assertEquals 'testProject2', project
                return true
            }
            fwkControl.demand.getCommand {project, type, command, framework ->
                assertEquals 'testProject2', project
                assertEquals 'aType2', type
                assertEquals 'aCommand2', command
                return null
            }
            sec.frameworkService = fwkControl.createMock()

            def params = new ScheduledExecution(jobName: 'monkey2', project: 'testProject2', description: 'blah', adhocExecution: true, adhocRemoteString: 'test command',
                                                workflow: new Workflow(commands: [new CommandExec(adhocRemoteString: 'test command', adhocExecution: true)])
            )
            def results = sec._doupdateJob(se.id.toString(), params,'test','test',null)
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

            assertFalse execution.adhocExecution
            assertNotNull execution.workflow
            assertNotNull execution.workflow.commands
            assertEquals 1, execution.workflow.commands.size()
            def CommandExec cexec = execution.workflow.commands[0]
            assertTrue cexec.adhocExecution
            assertEquals 'test command', cexec.adhocRemoteString
            assertNull cexec.adhocFilepath
            assertNull execution.adhocLocalString
            assertNull execution.adhocRemoteString
            assertNull execution.argString

            assertNull execution.notifications
            assertNull execution.options
        }
        if (true) {//test update basic job details
            def se = new ScheduledExecution(jobName: 'monkey1', project: 'testProject', description: 'blah', adhocExecution: true, adhocRemoteString: 'test command',)
            se.save()

            assertNotNull se.id

            //try to do update of the ScheduledExecution
            def fwkControl = mockFor(FrameworkService, true)
            fwkControl.demand.getFrameworkFromUserSession {session, request -> return null }
            fwkControl.demand.existsFrameworkProject {project, framework ->
                assertEquals 'testProject2', project
                return true
            }
            fwkControl.demand.getCommand {project, type, command, framework ->
                assertEquals 'testProject2', project
                assertEquals 'aType2', type
                assertEquals 'aCommand2', command
                return null
            }
            sec.frameworkService = fwkControl.createMock()

            def params = new ScheduledExecution(jobName: 'test/monkey2', project: 'testProject2', description: 'blah', adhocExecution: true, adhocRemoteString: 'test command',
                                                workflow: new Workflow(commands: [new CommandExec(adhocRemoteString: 'test command', adhocExecution: true)])
            )
            def results = sec._doupdateJob(se.id.toString(), params,'test','test',null)
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
            fwkControl.demand.getFrameworkFromUserSession {session, request -> return null }
            fwkControl.demand.existsFrameworkProject {project, framework ->
                assertEquals 'testProject2', project
                return true
            }
            fwkControl.demand.getCommand {project, type, command, framework ->
                assertEquals 'testProject2', project
                assertEquals 'aType2', type
                assertEquals 'aCommand2', command
                return null
            }
            sec.frameworkService = fwkControl.createMock()

            def eh1 = new CommandExec(adhocRemoteString: 'err command')
            def params = new ScheduledExecution(jobName: 'monkey2', project: 'testProject2', description: 'blah', adhocExecution: true, adhocRemoteString: 'test command',
                                                workflow: new Workflow(commands: [new CommandExec(adhocRemoteString: 'test command', errorHandler: eh1)])
            )
            def results = sec._doupdateJob(se.id.toString(), params, 'test', 'test', null)
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

            assertFalse execution.adhocExecution
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
    public void testDoUpdateJobErrorHandlersStepFirst() {
        def sec = new ScheduledExecutionService()
        if (true) {//test update basic job details
            def se = new ScheduledExecution(jobName: 'monkey1', project: 'testProject', description: 'blah', adhocExecution: true, adhocRemoteString: 'test command',
                    workflow: new Workflow(strategy: 'step-first',
                            commands: [
                                    new CommandExec(adhocRemoteString: 'test command', adhocExecution: true),
                                    new CommandExec(adhocRemoteString: 'test command', adhocExecution: true),
                                    new JobExec(jobName: 'test1',jobGroup: 'test'),
                                    new JobExec(jobName: 'test1', jobGroup: 'test'),
                            ]))
            se.save()

            assertNotNull se.id

            //try to do update of the ScheduledExecution
            def fwkControl = mockFor(FrameworkService, true)
            fwkControl.demand.getFrameworkFromUserSession {session, request -> return null }
            fwkControl.demand.existsFrameworkProject {project, framework ->
                assertEquals 'testProject2', project
                return true
            }
            fwkControl.demand.getCommand {project, type, command, framework ->
                assertEquals 'testProject2', project
                assertEquals 'aType2', type
                assertEquals 'aCommand2', command
                return null
            }
            sec.frameworkService = fwkControl.createMock()

            def eh1 = new CommandExec(adhocRemoteString: 'err command')
            def eh2 = new CommandExec(adhocRemoteString: 'err command')
            def eh3 = new JobExec(jobGroup: 'eh',jobName: 'eh1')
            def eh4 = new JobExec(jobGroup: 'eh',jobName: 'eh2')
            def params = new ScheduledExecution(jobName: 'monkey2', project: 'testProject2', description: 'blah', adhocExecution: true, adhocRemoteString: 'test command',
                workflow: new Workflow(strategy: 'step-first',
                       commands: [
                               new CommandExec(adhocRemoteString: 'test command', adhocExecution: true,errorHandler: eh1),
                               new CommandExec(adhocRemoteString: 'test command', adhocExecution: true,errorHandler:eh3),
                               new JobExec(jobName: 'test1', jobGroup: 'test',errorHandler: eh2),
                               new JobExec(jobName: 'test1', jobGroup: 'test',errorHandler: eh4),
                       ]
                )
            )
            def results = sec._doupdateJob(se.id.toString(), params, 'test', 'test', null)
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
                                    new JobExec(jobName: 'test1',jobGroup: 'test'),
                                    new JobExec(jobName: 'test1', jobGroup: 'test'),
                            ]))
            se.save()

            assertNotNull se.id

            //try to do update of the ScheduledExecution
            def fwkControl = mockFor(FrameworkService, true)
            fwkControl.demand.getFrameworkFromUserSession {session, request -> return null }
            fwkControl.demand.existsFrameworkProject {project, framework ->
                assertEquals 'testProject2', project
                return true
            }
            fwkControl.demand.getCommand {project, type, command, framework ->
                assertEquals 'testProject2', project
                assertEquals 'aType2', type
                assertEquals 'aCommand2', command
                return null
            }
            sec.frameworkService = fwkControl.createMock()

            def eh1 = new CommandExec(adhocRemoteString: 'err command')
            def eh2 = new CommandExec(adhocRemoteString: 'err command')
            def eh3 = new JobExec(jobGroup: 'eh',jobName: 'eh1')
            def eh4 = new JobExec(jobGroup: 'eh',jobName: 'eh2')
            def params = new ScheduledExecution(jobName: 'monkey2', project: 'testProject2', description: 'blah', adhocExecution: true, adhocRemoteString: 'test command',
                workflow: new Workflow(strategy: 'node-first',
                       commands: [
                               new CommandExec(adhocRemoteString: 'test command', adhocExecution: true,errorHandler: eh1),
                               new CommandExec(adhocRemoteString: 'test command', adhocExecution: true,errorHandler:eh3),
                               new JobExec(jobName: 'test1', jobGroup: 'test',errorHandler: eh2),
                               new JobExec(jobName: 'test1', jobGroup: 'test',errorHandler: eh4),
                       ]
                )
            )
            def results = sec._doupdateJob(se.id.toString(), params, 'test', 'test', null)
            def succeeded = results[0]
            def ScheduledExecution rese=results[1]
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
            fwkControl.demand.getFrameworkFromUserSession {session, request -> return null }
            fwkControl.demand.existsFrameworkProject {project, framework ->
                assertEquals 'testProject2', project
                return true
            }
            fwkControl.demand.getCommand {project, type, command, framework ->
                assertEquals 'testProject2', project
                assertEquals 'aType2', type
                assertEquals 'aCommand2', command
                return null
            }
            sec.frameworkService = fwkControl.createMock()

            def params = new ScheduledExecution(jobName: 'monkey2', project: 'testProject2', description: 'blah',
                                                doNodedispatch: true, nodeIncludeName: "nodename",
                                                workflow: new Workflow(commands: [new CommandExec(adhocRemoteString: 'test command', adhocExecution: true)])
            )
            def results = sec._doupdateJob(se.id.toString(), params,'test','test',null)
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
            fwkControl.demand.getCommand { project, type, command, framework ->
                assertEquals 'testProject2', project
                assertEquals 'aType2', type
                assertEquals 'aCommand2', command
                return null
            }
            sec.frameworkService = fwkControl.createMock()

            def params = new ScheduledExecution(jobName: 'monkey2', project: 'testProject2', description: 'blah',
                                                doNodedispatch: true, nodeIncludeName: "nodename",
                                                nodeThreadcount: 3,
                                                workflow: new Workflow(commands: [new CommandExec(adhocRemoteString: 'test command', adhocExecution: true)])
            )
            def results = sec._doupdateJob(se.id.toString(), params, 'test', 'test', null)
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
            fwkControl.demand.getCommand { project, type, command, framework ->
                assertEquals 'testProject2', project
                assertEquals 'aType2', type
                assertEquals 'aCommand2', command
                return null
            }
            sec.frameworkService = fwkControl.createMock()

            def params = new ScheduledExecution(jobName: 'monkey2', project: 'testProject2', description: 'blah',
                                                doNodedispatch: true, nodeIncludeName: "nodename",
                                                nodeThreadcount: '',
                                                workflow: new Workflow(commands: [new CommandExec(adhocRemoteString: 'test command', adhocExecution: true)])
            )
            def results = sec._doupdateJob(se.id.toString(), params, 'test', 'test', null)
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


    public void testDoUpdateScheduled() {
        if (true) {//test set scheduled with crontabString
            def sec = new ScheduledExecutionService()
            def se = new ScheduledExecution(jobName: 'monkey1', project: 'testProject', description: 'blah', adhocExecution: false, name: 'aResource', type: 'aType', command: 'aCommand')
            se.save()

            assertNotNull se.id
            assertFalse se.scheduled

            //try to do update of the ScheduledExecution
            def fwkControl = mockFor(FrameworkService, true)
            fwkControl.demand.getFrameworkFromUserSession {session, request -> return null }
            fwkControl.demand.existsFrameworkProject {project, framework ->
                assertEquals 'testProject', project
                return true
            }
            fwkControl.demand.getCommand {project, type, command, framework ->
                assertEquals 'testProject', project
                assertEquals 'aType', type
                assertEquals 'aCommand', command
                return null
            }
            fwkControl.demand.authorizeProjectJobAll {framework, resource, actions, project -> return true}
            fwkControl.demand.getFrameworkFromUserSession {session, request -> return null }
            fwkControl.demand.getFrameworkFromUserSession {session, request -> return null }
            fwkControl.demand.getRundeckBase {'test-base'}
            sec.frameworkService = fwkControl.createMock()
//            def sesControl = mockFor(ScheduledExecutionService, true)
//            sesControl.demand.getByIDorUUID {id -> return se }
//            sesControl.demand.scheduleJob {schedEx, oldname, oldgroup ->
//                //scheduledExecution, renamed ? oldjobname : null, renamed ? oldjobgroup : null
//                assertNotNull(schedEx)
//            }
//            sec.scheduledExecutionService = sesControl.createMock()
//            sec.metaClass.scheduleJob= {schedEx, oldname, oldgroup ->
                //scheduledExecution, renamed ? oldjobname : null, renamed ? oldjobgroup : null
//                assertNotNull(schedEx)
//            }

            def qtzControl = mockFor(FakeScheduler, true)
            qtzControl.demand.getJobNames{name->[]}
            qtzControl.demand.scheduleJob{jobDetail,trigger-> new Date()}
            sec.quartzScheduler=qtzControl.createMock()
//            final subject = new Subject()
//            subject.principals << new Username('test')
//            ['userrole', 'test'].each { group ->
//                subject.principals << new Group(group);
//            }
            //sec.request.setAttribute("subject", subject)
            def params = [id: se.id.toString(), jobName: 'monkey1', project: 'testProject', description: 'blah', adhocExecution: false, name: 'aResource', type: 'aType', command: 'aCommand', scheduled: true, crontabString: '0 21 */4 */4 */6 ? 2010-2040', useCrontabString: 'true']
            def results = sec._doupdate(params,'test','userrole,test',null)
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
        if (true) {//test set scheduled with invalid crontabString (invalid dayOfMonth/dayOfWeek combo)
            def sec = new ScheduledExecutionService()
            def se = new ScheduledExecution(jobName: 'monkey1', project: 'testProject', description: 'blah', adhocExecution: false, name: 'aResource', type: 'aType', command: 'aCommand')
            se.save()

            assertNotNull se.id
            assertFalse se.scheduled

            //try to do update of the ScheduledExecution
            def fwkControl = mockFor(FrameworkService, true)
            fwkControl.demand.getFrameworkFromUserSession {session, request -> return null }
            fwkControl.demand.existsFrameworkProject {project, framework ->
                assertEquals 'testProject', project
                return true
            }
            fwkControl.demand.getCommand {project, type, command, framework ->
                assertEquals 'testProject', project
                assertEquals 'aType', type
                assertEquals 'aCommand', command
                return null
            }
            fwkControl.demand.authorizeProjectJobAll {framework, resource, actions, project -> return true}
            fwkControl.demand.getFrameworkFromUserSession {session, request -> return null }
            fwkControl.demand.getFrameworkFromUserSession {session, request -> return null }
            sec.frameworkService = fwkControl.createMock()
//            def sesControl = mockFor(ScheduledExecutionService, true)
//            sesControl.demand.getByIDorUUID {id -> return se }
//            sesControl.demand.scheduleJob {schedEx, oldname, oldgroup ->
                //scheduledExecution, renamed ? oldjobname : null, renamed ? oldjobgroup : null
//                assertNotNull(schedEx)
//            }
//            sec.scheduledExecutionService = sesControl.createMock()

            def params = [id: se.id.toString(), scheduled: true, crontabString: '0 21 */4 */4 */6 3 2010-2040', useCrontabString: 'true', jobName: 'monkey1', project: 'testProject', description: 'blah', adhocExecution: false, name: 'aResource', type: 'aType', command: 'aCommand']
            def results = sec._doupdate(params,'test','test',null)
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

        }
        if (true) {//test set scheduled with invalid crontabString (invalid dayOfMonth/dayOfWeek combo, two ?)
            def sec = new ScheduledExecutionService()
            def se = new ScheduledExecution(jobName: 'monkey1', project: 'testProject', description: 'blah', adhocExecution: false, name: 'aResource', type: 'aType', command: 'aCommand')
            se.save()

            assertNotNull se.id
            assertFalse se.scheduled

            //try to do update of the ScheduledExecution
            def fwkControl = mockFor(FrameworkService, true)
            fwkControl.demand.getFrameworkFromUserSession {session, request -> return null }
            fwkControl.demand.existsFrameworkProject {project, framework ->
                assertEquals 'testProject', project
                return true
            }
            fwkControl.demand.getCommand {project, type, command, framework ->
                assertEquals 'testProject', project
                assertEquals 'aType', type
                assertEquals 'aCommand', command
                return null
            }
            fwkControl.demand.authorizeProjectJobAll {framework, resource, actions, project -> return true}
            fwkControl.demand.getFrameworkFromUserSession {session, request -> return null }
            fwkControl.demand.getFrameworkFromUserSession {session, request -> return null }
            sec.frameworkService = fwkControl.createMock()
//            def sesControl = mockFor(ScheduledExecutionService, true)
//            sesControl.demand.getByIDorUUID {id -> return se }
//            sesControl.demand.scheduleJob {schedEx, oldname, oldgroup ->
                //scheduledExecution, renamed ? oldjobname : null, renamed ? oldjobgroup : null
//                assertNotNull(schedEx)
//            }
//            sec.scheduledExecutionService = sesControl.createMock()

            def params = [id: se.id.toString(), scheduled: true, crontabString: '0 21 */4 ? */6 ? 2010-2040', useCrontabString: 'true', jobName: 'monkey1', project: 'testProject', description: 'blah', adhocExecution: false, name: 'aResource', type: 'aType', command: 'aCommand']
            def results = sec._doupdate(params,'test','test',null)
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

        }
        if (true) {//test set scheduled with invalid crontabString (invalid year char)
            def sec = new ScheduledExecutionService()
            def se = new ScheduledExecution(jobName: 'monkey1', project: 'testProject', description: 'blah', adhocExecution: false, name: 'aResource', type: 'aType', command: 'aCommand')
            se.save()

            assertNotNull se.id
            assertFalse se.scheduled

            //try to do update of the ScheduledExecution
            def fwkControl = mockFor(FrameworkService, true)
            fwkControl.demand.getFrameworkFromUserSession {session, request -> return null }
            fwkControl.demand.existsFrameworkProject {project, framework ->
                assertEquals 'testProject', project
                return true
            }
            fwkControl.demand.getCommand {project, type, command, framework ->
                assertEquals 'testProject', project
                assertEquals 'aType', type
                assertEquals 'aCommand', command
                return null
            }
            fwkControl.demand.authorizeProjectJobAll {framework, resource, actions, project -> return true}
            fwkControl.demand.getFrameworkFromUserSession {session, request -> return null }
            fwkControl.demand.getFrameworkFromUserSession {session, request -> return null }
            sec.frameworkService = fwkControl.createMock()

//            def sesControl = mockFor(ScheduledExecutionService, true)
//            sesControl.demand.getByIDorUUID {id -> return se }
//            sec.scheduledExecutionService = sesControl.createMock()

            def params = [id: se.id.toString(), scheduled: true, crontabString: '0 21 */4 */4 */6 ? z2010-2040', useCrontabString: 'true']
            def results = sec._doupdate(params,'test','test',null)
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

        }
        if (true) {//test set scheduled with invalid crontabString  (too few components)
            def sec = new ScheduledExecutionService()
            def se = new ScheduledExecution(jobName: 'monkey1', project: 'testProject', description: 'blah', adhocExecution: false, name: 'aResource', type: 'aType', command: 'aCommand')
            se.save()

            assertNotNull se.id
            assertFalse se.scheduled

            //try to do update of the ScheduledExecution
            def fwkControl = mockFor(FrameworkService, true)
            fwkControl.demand.getFrameworkFromUserSession {session, request -> return null }
            fwkControl.demand.existsFrameworkProject {project, framework ->
                assertEquals 'testProject', project
                return true
            }
            fwkControl.demand.getCommand {project, type, command, framework ->
                assertEquals 'testProject', project
                assertEquals 'aType', type
                assertEquals 'aCommand', command
                return null
            }
            fwkControl.demand.authorizeProjectJobAll {framework, resource, actions, project -> return true}
            fwkControl.demand.getFrameworkFromUserSession {session, request -> return null }
            fwkControl.demand.getFrameworkFromUserSession {session, request -> return null }
            sec.frameworkService = fwkControl.createMock()

//            def sesControl = mockFor(ScheduledExecutionService, true)
//            sesControl.demand.getByIDorUUID {id -> return se }
//            sec.scheduledExecutionService = sesControl.createMock()

            def params = [id: se.id.toString(), scheduled: true, crontabString: '0 21 */4 */4 */6', useCrontabString: 'true', jobName: 'monkey1', project: 'testProject', description: 'blah', adhocExecution: false, name: 'aResource', type: 'aType', command: 'aCommand']
            def results = sec._doupdate(params,'test','test',null)
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

        }
        if (true) {//test set scheduled with invalid crontabString  (wrong seconds value)
            def sec = new ScheduledExecutionService()
            def se = new ScheduledExecution(jobName: 'monkey1', project: 'testProject', description: 'blah', adhocExecution: false, name: 'aResource', type: 'aType', command: 'aCommand')
            se.save()

            assertNotNull se.id
            assertFalse se.scheduled

            //try to do update of the ScheduledExecution
            def fwkControl = mockFor(FrameworkService, true)
            fwkControl.demand.getFrameworkFromUserSession {session, request -> return null }
            fwkControl.demand.existsFrameworkProject {project, framework ->
                assertEquals 'testProject', project
                return true
            }
            fwkControl.demand.getCommand {project, type, command, framework ->
                assertEquals 'testProject', project
                assertEquals 'aType', type
                assertEquals 'aCommand', command
                return null
            }
            fwkControl.demand.authorizeProjectJobAll {framework, resource, actions, project -> return true}
            fwkControl.demand.getFrameworkFromUserSession {session, request -> return null }
            fwkControl.demand.getFrameworkFromUserSession {session, request -> return null }
            sec.frameworkService = fwkControl.createMock()

//            def sesControl = mockFor(ScheduledExecutionService, true)
//            sesControl.demand.getByIDorUUID {id -> return se }
//            sec.scheduledExecutionService = sesControl.createMock()

            def params = [id: se.id.toString(), scheduled: true, crontabString: '70 21 */4 */4 */6 ?', useCrontabString: 'true', jobName: 'monkey1', project: 'testProject', description: 'blah', adhocExecution: false, name: 'aResource', type: 'aType', command: 'aCommand']
            def results = sec._doupdate(params,'test','test',null)
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

        }
        if (true) {//test set scheduled with invalid crontabString  (wrong minutes value)
            def sec = new ScheduledExecutionService()
            def se = new ScheduledExecution(jobName: 'monkey1', project: 'testProject', description: 'blah', adhocExecution: false, name: 'aResource', type: 'aType', command: 'aCommand')
            se.save()

            assertNotNull se.id
            assertFalse se.scheduled

            //try to do update of the ScheduledExecution
            def fwkControl = mockFor(FrameworkService, true)
            fwkControl.demand.getFrameworkFromUserSession {session, request -> return null }
            fwkControl.demand.existsFrameworkProject {project, framework ->
                assertEquals 'testProject', project
                return true
            }
            fwkControl.demand.getCommand {project, type, command, framework ->
                assertEquals 'testProject', project
                assertEquals 'aType', type
                assertEquals 'aCommand', command
                return null
            }
            fwkControl.demand.authorizeProjectJobAll {framework, resource, actions, project -> return true}
            fwkControl.demand.getFrameworkFromUserSession {session, request -> return null }
            fwkControl.demand.getFrameworkFromUserSession {session, request -> return null }
            sec.frameworkService = fwkControl.createMock()

//            def sesControl = mockFor(ScheduledExecutionService, true)
//            sesControl.demand.getByIDorUUID {id -> return se }
//            sec.scheduledExecutionService = sesControl.createMock()

            def params = [id: se.id.toString(), scheduled: true, crontabString: '0 70 */4 */4 */6 ?', useCrontabString: 'true', jobName: 'monkey1', project: 'testProject', description: 'blah', adhocExecution: false, name: 'aResource', type: 'aType', command: 'aCommand']
            def results = sec._doupdate(params,'test','test',null)
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

        }
        if (true) {//test set scheduled with invalid crontabString  (wrong hour value)
            def sec = new ScheduledExecutionService()
            def se = new ScheduledExecution(jobName: 'monkey1', project: 'testProject', description: 'blah', adhocExecution: false, name: 'aResource', type: 'aType', command: 'aCommand')
            se.save()

            assertNotNull se.id
            assertFalse se.scheduled

            //try to do update of the ScheduledExecution
            def fwkControl = mockFor(FrameworkService, true)
            fwkControl.demand.getFrameworkFromUserSession {session, request -> return null }
            fwkControl.demand.existsFrameworkProject {project, framework ->
                assertEquals 'testProject', project
                return true
            }
            fwkControl.demand.getCommand {project, type, command, framework ->
                assertEquals 'testProject', project
                assertEquals 'aType', type
                assertEquals 'aCommand', command
                return null
            }
            fwkControl.demand.authorizeProjectJobAll {framework, resource, actions, project -> return true}
            fwkControl.demand.getFrameworkFromUserSession {session, request -> return null }
            fwkControl.demand.getFrameworkFromUserSession {session, request -> return null }
            sec.frameworkService = fwkControl.createMock()

//            def sesControl = mockFor(ScheduledExecutionService, true)
//            sesControl.demand.getByIDorUUID {id -> return se }
//            sec.scheduledExecutionService = sesControl.createMock()

            def params = [id: se.id.toString(), scheduled: true, crontabString: '0 0 25 */4 */6 ?', useCrontabString: 'true', jobName: 'monkey1', project: 'testProject', description: 'blah', adhocExecution: false, name: 'aResource', type: 'aType', command: 'aCommand']
            def results = sec._doupdate(params,'test','test',null)
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

        }
        if (true) {//test set scheduled with invalid crontabString  (wrong day of month value)
            def sec = new ScheduledExecutionService()
            def se = new ScheduledExecution(jobName: 'monkey1', project: 'testProject', description: 'blah', adhocExecution: false, name: 'aResource', type: 'aType', command: 'aCommand')
            se.save()

            assertNotNull se.id
            assertFalse se.scheduled

            //try to do update of the ScheduledExecution
            def fwkControl = mockFor(FrameworkService, true)
            fwkControl.demand.getFrameworkFromUserSession {session, request -> return null }
            fwkControl.demand.existsFrameworkProject {project, framework ->
                assertEquals 'testProject', project
                return true
            }
            fwkControl.demand.getCommand {project, type, command, framework ->
                assertEquals 'testProject', project
                assertEquals 'aType', type
                assertEquals 'aCommand', command
                return null
            }
            fwkControl.demand.authorizeProjectJobAll {framework, resource, actions, project -> return true}
            fwkControl.demand.getFrameworkFromUserSession {session, request -> return null }
            fwkControl.demand.getFrameworkFromUserSession {session, request -> return null }
            sec.frameworkService = fwkControl.createMock()

//            def sesControl = mockFor(ScheduledExecutionService, true)
//            sesControl.demand.getByIDorUUID {id -> return se }
//            sec.scheduledExecutionService = sesControl.createMock()

            def params = [id: se.id.toString(), scheduled: true, crontabString: '0 0 2 32 */6 ?', useCrontabString: 'true', jobName: 'monkey1', project: 'testProject', description: 'blah', adhocExecution: false, name: 'aResource', type: 'aType', command: 'aCommand']
            def results = sec._doupdate(params,'test','test',null)
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

        }
        if (true) {//test set scheduled with invalid crontabString  (wrong month value)
            def sec = new ScheduledExecutionService()
            def se = new ScheduledExecution(jobName: 'monkey1', project: 'testProject', description: 'blah', adhocExecution: false, name: 'aResource', type: 'aType', command: 'aCommand')
            se.save()

            assertNotNull se.id
            assertFalse se.scheduled

            //try to do update of the ScheduledExecution
            def fwkControl = mockFor(FrameworkService, true)
            fwkControl.demand.getFrameworkFromUserSession {session, request -> return null }
            fwkControl.demand.existsFrameworkProject {project, framework ->
                assertEquals 'testProject', project
                return true
            }
            fwkControl.demand.getCommand {project, type, command, framework ->
                assertEquals 'testProject', project
                assertEquals 'aType', type
                assertEquals 'aCommand', command
                return null
            }
            fwkControl.demand.authorizeProjectJobAll {framework, resource, actions, project -> return true}
            fwkControl.demand.getFrameworkFromUserSession {session, request -> return null }
            fwkControl.demand.getFrameworkFromUserSession {session, request -> return null }
            sec.frameworkService = fwkControl.createMock()

//            def sesControl = mockFor(ScheduledExecutionService, true)
//            sesControl.demand.getByIDorUUID {id -> return se }
//            sec.scheduledExecutionService = sesControl.createMock()

            def params = [id: se.id.toString(), scheduled: true, crontabString: '0 0 2 3 13 ?', useCrontabString: 'true', jobName: 'monkey1', project: 'testProject', description: 'blah', adhocExecution: false, name: 'aResource', type: 'aType', command: 'aCommand']
            def results = sec._doupdate(params,'test','test',null)
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

        }
        if (true) {//test set scheduled with invalid crontabString  (wrong day of week value)
            def sec = new ScheduledExecutionService()
            def se = new ScheduledExecution(jobName: 'monkey1', project: 'testProject', description: 'blah', adhocExecution: false, name: 'aResource', type: 'aType', command: 'aCommand')
            se.save()

            assertNotNull se.id
            assertFalse se.scheduled

            //try to do update of the ScheduledExecution
            def fwkControl = mockFor(FrameworkService, true)
            fwkControl.demand.getFrameworkFromUserSession {session, request -> return null }
            fwkControl.demand.existsFrameworkProject {project, framework ->
                assertEquals 'testProject', project
                return true
            }
            fwkControl.demand.getCommand {project, type, command, framework ->
                assertEquals 'testProject', project
                assertEquals 'aType', type
                assertEquals 'aCommand', command
                return null
            }
            fwkControl.demand.authorizeProjectJobAll {framework, resource, actions, project -> return true}
            fwkControl.demand.getFrameworkFromUserSession {session, request -> return null }
            fwkControl.demand.getFrameworkFromUserSession {session, request -> return null }
            sec.frameworkService = fwkControl.createMock()

//            def sesControl = mockFor(ScheduledExecutionService, true)
//            sesControl.demand.getByIDorUUID {id -> return se }
//            sec.scheduledExecutionService = sesControl.createMock()

            def params = [id: se.id.toString(), scheduled: true, crontabString: '0 0 2 ? 12 8', useCrontabString: 'true', jobName: 'monkey1', project: 'testProject', description: 'blah', adhocExecution: false, name: 'aResource', type: 'aType', command: 'aCommand']
            def results = sec._doupdate(params,'test','test',null)
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

        }
    }

    public void testDoUpdateJobShouldSetCrontabString() {
        def sec = new ScheduledExecutionService()
        //test set scheduled with crontabString
        def se = new ScheduledExecution(jobName: 'monkey1', project: 'testProject', description: 'blah', adhocExecution: false, name: 'aResource', type: 'aType', command: 'aCommand')
        se.save()

        assertNotNull se.id
        assertFalse se.scheduled

        //try to do update of the ScheduledExecution
        def fwkControl = mockFor(FrameworkService, true)
        fwkControl.demand.getFrameworkFromUserSession {session, request -> return null }
        fwkControl.demand.existsFrameworkProject {project, framework ->
            assertEquals 'testProject', project
            return true
        }
        fwkControl.demand.getCommand {project, type, command, framework ->
            assertEquals 'testProject', project
            assertEquals 'aType', type
            assertEquals 'aCommand', command
            return null
        }
        fwkControl.demand.getRundeckBase{'test-base'}
        sec.frameworkService = fwkControl.createMock()
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
        qtzControl.demand.getJobNames {name -> []}
        qtzControl.demand.scheduleJob {jobDetail, trigger -> new Date()}
        sec.quartzScheduler = qtzControl.createMock()
        def params = new ScheduledExecution(jobName: 'monkey1', project: 'testProject', description: 'blah',
                                            workflow: new Workflow(commands: [new CommandExec(adhocRemoteString: 'test command', adhocExecution: true)]),
                                            scheduled: true, crontabString: '0 21 */4 */4 */6 ? 2010-2040', useCrontabString: 'true')
        def results = sec._doupdateJob(se.id.toString(), params,'test','test',null)
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

    public void testDoUpdateJobShouldNotSetInvalidCrontabString() {
        def sec = new ScheduledExecutionService()
        if (true) {//test set scheduled with invalid crontabString (invalid dayOfMonth/dayOfWeek combo)
            def se = new ScheduledExecution(jobName: 'monkey1', project: 'testProject', description: 'blah', adhocExecution: false, name: 'aResource', type: 'aType', command: 'aCommand')
            se.save()

            assertNotNull se.id
            assertFalse se.scheduled

            //try to do update of the ScheduledExecution
            def fwkControl = mockFor(FrameworkService, true)
            fwkControl.demand.getFrameworkFromUserSession {session, request -> return null }
            fwkControl.demand.existsFrameworkProject {project, framework ->
                assertEquals 'testProject', project
                return true
            }
            fwkControl.demand.getCommand {project, type, command, framework ->
                assertEquals 'testProject', project
                assertEquals 'aType', type
                assertEquals 'aCommand', command
                return null
            }
            sec.frameworkService = fwkControl.createMock()
//            def sesControl = mockFor(ScheduledExecutionService, true)
//            sesControl.demand.scheduleJob {schedEx, oldname, oldgroup ->
                //scheduledExecution, renamed ? oldjobname : null, renamed ? oldjobgroup : null
//                assertNotNull(schedEx)
//            }
//            sec.scheduledExecutionService = sesControl.createMock()

            def qtzControl = mockFor(FakeScheduler, true)
            qtzControl.demand.getJobNames {name -> []}
            qtzControl.demand.scheduleJob {jobDetail, trigger -> new Date()}
            sec.quartzScheduler = qtzControl.createMock()
            def params = new ScheduledExecution(scheduled: true, crontabString: '0 21 */4 */4 */6 3 2010-2040', useCrontabString: 'true', jobName: 'monkey1', project: 'testProject', description: 'blah',
                                                dayOfMonth: '*/4', dayOfWeek: '3',
                                                workflow: new Workflow(commands: [new CommandExec(adhocRemoteString: 'test command', adhocExecution: true)]))
            def results = sec._doupdateJob(se.id.toString(), params,'test','test',null)
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
            fwkControl.demand.getFrameworkFromUserSession {session, request -> return null }
            fwkControl.demand.existsFrameworkProject {project, framework ->
                assertEquals 'testProject', project
                return true
            }
            fwkControl.demand.getCommand {project, type, command, framework ->
                assertEquals 'testProject', project
                assertEquals 'aType', type
                assertEquals 'aCommand', command
                return null
            }
            sec.frameworkService = fwkControl.createMock()
//            def sesControl = mockFor(ScheduledExecutionService, true)
//            sesControl.demand.scheduleJob {schedEx, oldname, oldgroup ->
                //scheduledExecution, renamed ? oldjobname : null, renamed ? oldjobgroup : null
//                assertNotNull(schedEx)
//            }
//            sec.scheduledExecutionService = sesControl.createMock()

            def params = new ScheduledExecution(scheduled: true, crontabString: '0 21 */4 ? */6 ? 2010-2040', useCrontabString: 'true', jobName: 'monkey1', project: 'testProject', description: 'blah',
                                                workflow: new Workflow(commands: [new CommandExec(adhocRemoteString: 'test command', adhocExecution: true)]))
            def results = sec._doupdateJob(se.id.toString(), params,'test','test',null)
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
            fwkControl.demand.getFrameworkFromUserSession {session, request -> return null }
            fwkControl.demand.existsFrameworkProject {project, framework ->
                assertEquals 'testProject', project
                return true
            }
            fwkControl.demand.getCommand {project, type, command, framework ->
                assertEquals 'testProject', project
                assertEquals 'aType', type
                assertEquals 'aCommand', command
                return null
            }
            sec.frameworkService = fwkControl.createMock()

            def params = new ScheduledExecution(scheduled: true, crontabString: '0 21 */4 */4 */6 ? z2010-2040', useCrontabString: 'true',
                                                workflow: new Workflow(commands: [new CommandExec(adhocRemoteString: 'test command', adhocExecution: true)]))
            def results = sec._doupdateJob(se.id.toString(), params,'test','test',null)
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
            fwkControl.demand.getFrameworkFromUserSession {session, request -> return null }
            fwkControl.demand.existsFrameworkProject {project, framework ->
                assertEquals 'testProject', project
                return true
            }
            fwkControl.demand.getCommand {project, type, command, framework ->
                assertEquals 'testProject', project
                assertEquals 'aType', type
                assertEquals 'aCommand', command
                return null
            }
            sec.frameworkService = fwkControl.createMock()

            def params = new ScheduledExecution(scheduled: true, crontabString: '0 21 */4 */4 */6', useCrontabString: 'true', jobName: 'monkey1', project: 'testProject', description: 'blah',
                                                workflow: new Workflow(commands: [new CommandExec(adhocRemoteString: 'test command', adhocExecution: true)]))
            def results = sec._doupdateJob(se.id.toString(), params,'test','test',null)
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
            fwkControl.demand.getFrameworkFromUserSession {session, request -> return null }
            fwkControl.demand.existsFrameworkProject {project, framework ->
                assertEquals 'testProject', project
                return true
            }
            fwkControl.demand.getCommand {project, type, command, framework ->
                assertEquals 'testProject', project
                assertEquals 'aType', type
                assertEquals 'aCommand', command
                return null
            }
            sec.frameworkService = fwkControl.createMock()

            def params = new ScheduledExecution(scheduled: true, crontabString: '70 21 */4 */4 */6 ?', useCrontabString: 'true', jobName: 'monkey1', project: 'testProject', description: 'blah',
                                                workflow: new Workflow(commands: [new CommandExec(adhocRemoteString: 'test command', adhocExecution: true)]))
            def results = sec._doupdateJob(se.id.toString(), params,'test','test',null)
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
            fwkControl.demand.getFrameworkFromUserSession {session, request -> return null }
            fwkControl.demand.existsFrameworkProject {project, framework ->
                assertEquals 'testProject', project
                return true
            }
            fwkControl.demand.getCommand {project, type, command, framework ->
                assertEquals 'testProject', project
                assertEquals 'aType', type
                assertEquals 'aCommand', command
                return null
            }
            sec.frameworkService = fwkControl.createMock()

            def params = new ScheduledExecution(scheduled: true, crontabString: '0 70 */4 */4 */6 ?', useCrontabString: 'true', jobName: 'monkey1', project: 'testProject', description: 'blah',
                                                workflow: new Workflow(commands: [new CommandExec(adhocRemoteString: 'test command', adhocExecution: true)]))
            def results = sec._doupdateJob(se.id.toString(), params,'test','test',null)
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
            fwkControl.demand.getFrameworkFromUserSession {session, request -> return null }
            fwkControl.demand.existsFrameworkProject {project, framework ->
                assertEquals 'testProject', project
                return true
            }
            fwkControl.demand.getCommand {project, type, command, framework ->
                assertEquals 'testProject', project
                assertEquals 'aType', type
                assertEquals 'aCommand', command
                return null
            }
            sec.frameworkService = fwkControl.createMock()

            def params = new ScheduledExecution(scheduled: true, crontabString: '0 0 25 */4 */6 ?', useCrontabString: 'true', jobName: 'monkey1', project: 'testProject', description: 'blah',
                                                workflow: new Workflow(commands: [new CommandExec(adhocRemoteString: 'test command', adhocExecution: true)]))
            def results = sec._doupdateJob(se.id.toString(), params,'test','test',null)
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
            fwkControl.demand.getFrameworkFromUserSession {session, request -> return null }
            fwkControl.demand.existsFrameworkProject {project, framework ->
                assertEquals 'testProject', project
                return true
            }
            fwkControl.demand.getCommand {project, type, command, framework ->
                assertEquals 'testProject', project
                assertEquals 'aType', type
                assertEquals 'aCommand', command
                return null
            }
            sec.frameworkService = fwkControl.createMock()

            def params = new ScheduledExecution(scheduled: true, crontabString: '0 0 2 32 */6 ?', useCrontabString: 'true', jobName: 'monkey1', project: 'testProject', description: 'blah',
                                                workflow: new Workflow(commands: [new CommandExec(adhocRemoteString: 'test command', adhocExecution: true)]))
            def results = sec._doupdateJob(se.id.toString(), params,'test','test',null)
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
            fwkControl.demand.getFrameworkFromUserSession {session, request -> return null }
            fwkControl.demand.existsFrameworkProject {project, framework ->
                assertEquals 'testProject', project
                return true
            }
            fwkControl.demand.getCommand {project, type, command, framework ->
                assertEquals 'testProject', project
                assertEquals 'aType', type
                assertEquals 'aCommand', command
                return null
            }
            sec.frameworkService = fwkControl.createMock()

            def params = new ScheduledExecution(scheduled: true, crontabString: '0 0 2 3 13 ?', useCrontabString: 'true', jobName: 'monkey1', project: 'testProject', description: 'blah',
                                                workflow: new Workflow(commands: [new CommandExec(adhocRemoteString: 'test command', adhocExecution: true)]))
            def results = sec._doupdateJob(se.id.toString(), params,'test','test',null)
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
            fwkControl.demand.getFrameworkFromUserSession {session, request -> return null }
            fwkControl.demand.existsFrameworkProject {project, framework ->
                assertEquals 'testProject', project
                return true
            }
            fwkControl.demand.getCommand {project, type, command, framework ->
                assertEquals 'testProject', project
                assertEquals 'aType', type
                assertEquals 'aCommand', command
                return null
            }
            sec.frameworkService = fwkControl.createMock()

            def params = new ScheduledExecution(scheduled: true, crontabString: '0 0 2 ? 12 8', useCrontabString: 'true', jobName: 'monkey1', project: 'testProject', description: 'blah',
                                                workflow: new Workflow(commands: [new CommandExec(adhocRemoteString: 'test command', adhocExecution: true)]))
            def results = sec._doupdateJob(se.id.toString(), params,'test','test',null)
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

    public void testDoUpdateAdhoc() {
        def sec = new ScheduledExecutionService()
        if (true) {//test failure on empty adhoc params
            def se = new ScheduledExecution(jobName: 'monkey1', project: 'testProject', description: 'blah',
                                            adhocExecution: true, adhocRemoteString: 'test remote',
                                            command: '', type: '',)
            se.save()

            assertNotNull se.id

            //try to do update of the ScheduledExecution
            def fwkControl = mockFor(FrameworkService, true)
            fwkControl.demand.getFrameworkFromUserSession {session, request -> return null }
            fwkControl.demand.existsFrameworkProject {project, framework ->
                assertEquals 'testProject2', project
                return true
            }
            fwkControl.demand.getCommand {project, type, command, framework ->
                assertEquals 'testProject2', project
                assertEquals '', type
                assertEquals '', command
                return null
            }
            fwkControl.demand.authorizeProjectJobAll {framework, resource, actions, project -> return true}
            fwkControl.demand.getFrameworkFromUserSession {session, request -> return null }
            fwkControl.demand.getFrameworkFromUserSession {session, request -> return null }
            sec.frameworkService = fwkControl.createMock()

//            def sesControl = mockFor(ScheduledExecutionService, true)
//            sesControl.demand.getByIDorUUID {id -> return se }
//            sec.scheduledExecutionService = sesControl.createMock()

            def params = [id: se.id.toString(), jobName: 'monkey2', project: 'testProject2', description: 'blah', adhocExecution: 'true', adhocRemoteString: '']
            def results = sec._doupdate(params,'test','test',null)
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
//        sessionFactory.currentSession.clear()
//        sessionFactory.currentSession.flush()
        if (true) {//test update from one adhoc type to another; remote -> local
            def se = new ScheduledExecution(jobName: 'monkey1', project: 'testProject', description: 'blah',
                                            adhocExecution: true, adhocRemoteString: 'test remote',
                                            command: '', type: '',)
            se.save()

            assertNotNull se.id

            //try to do update of the ScheduledExecution
            def fwkControl = mockFor(FrameworkService, true)
            fwkControl.demand.getFrameworkFromUserSession {session, request -> return null }
            fwkControl.demand.existsFrameworkProject {project, framework ->
                assertEquals 'testProject2', project
                return true
            }
            fwkControl.demand.getCommand {project, type, command, framework ->
                assertEquals 'testProject2', project
                assertEquals '', type
                assertEquals '', command
                return null
            }
            fwkControl.demand.authorizeProjectJobAll {framework, resource, actions, project -> return true}
            fwkControl.demand.getFrameworkFromUserSession {session, request -> return null }
            fwkControl.demand.getFrameworkFromUserSession {session, request -> return null }
            sec.frameworkService = fwkControl.createMock()

//            def sesControl = mockFor(ScheduledExecutionService, true)
//            sesControl.demand.getByIDorUUID {id -> return se }
//            sec.scheduledExecutionService = sesControl.createMock()

            def params = [id: se.id.toString(), jobName: 'monkey2', project: 'testProject2', description: 'blah', adhocExecution: 'true', adhocLocalString: 'test local']
            def results = sec._doupdate(params,'test','test',null)
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
            assertFalse execution.adhocExecution
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
        if (true) {//test update from one adhoc type to another; remote -> file
            def se = new ScheduledExecution(jobName: 'monkey1', project: 'testProject', description: 'blah',
                                            adhocExecution: true, adhocRemoteString: 'test remote',
                                            command: '', type: '',)
            se.save()

            assertNotNull se.id

            //try to do update of the ScheduledExecution
            def fwkControl = mockFor(FrameworkService, true)
            fwkControl.demand.getFrameworkFromUserSession {session, request -> return null }
            fwkControl.demand.existsFrameworkProject {project, framework ->
                assertEquals 'testProject2', project
                return true
            }
            fwkControl.demand.getCommand {project, type, command, framework ->
                assertEquals 'testProject2', project
                assertEquals '', type
                assertEquals '', command
                return null
            }
            fwkControl.demand.authorizeProjectJobAll {framework, resource, actions, project -> return true}
            fwkControl.demand.getFrameworkFromUserSession {session, request -> return null }
            fwkControl.demand.getFrameworkFromUserSession {session, request -> return null }
            sec.frameworkService = fwkControl.createMock()

//            def sesControl = mockFor(ScheduledExecutionService, true)
//            sesControl.demand.getByIDorUUID {id -> return se }
//            sec.scheduledExecutionService = sesControl.createMock()

            def params = [id: se.id.toString(), jobName: 'monkey2', project: 'testProject2', description: 'blah', adhocExecution: 'true', adhocFilepath: 'test file']
            def results = sec._doupdate(params,'test','test',null)
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
            assertFalse execution.adhocExecution
            assertNotNull execution.workflow
            assertNotNull execution.workflow.commands
            assertEquals 1, execution.workflow.commands.size()
            def CommandExec cexec = execution.workflow.commands[0]
            assertTrue cexec.adhocExecution
            assertEquals 'test file', cexec.adhocFilepath
            assertNull cexec.adhocRemoteString
            assertNull cexec.adhocLocalString
            assertNull cexec.argString

            assertNull execution.argString
            assertNull execution.notifications
            assertNull execution.options
        }
        if (true) {//test update from one adhoc type to another; local -> remote
            def se = new ScheduledExecution(jobName: 'monkey1', project: 'testProject', description: 'blah',
                                            adhocExecution: true, adhocLocalString: 'test local',
                                            command: '', type: '',)
            se.save()

            assertNotNull se.id

            //try to do update of the ScheduledExecution
            def fwkControl = mockFor(FrameworkService, true)
            fwkControl.demand.getFrameworkFromUserSession {session, request -> return null }
            fwkControl.demand.existsFrameworkProject {project, framework ->
                assertEquals 'testProject2', project
                return true
            }
            fwkControl.demand.getCommand {project, type, command, framework ->
                assertEquals 'testProject2', project
                assertEquals '', type
                assertEquals '', command
                return null
            }
            fwkControl.demand.authorizeProjectJobAll {framework, resource, actions, project -> return true}
            fwkControl.demand.getFrameworkFromUserSession {session, request -> return null }
            fwkControl.demand.getFrameworkFromUserSession {session, request -> return null }
            sec.frameworkService = fwkControl.createMock()

//            def sesControl = mockFor(ScheduledExecutionService, true)
//            sesControl.demand.getByIDorUUID {id -> return se }
//            sec.scheduledExecutionService = sesControl.createMock()

            def params = [id: se.id.toString(), jobName: 'monkey2', project: 'testProject2', description: 'blah', adhocExecution: 'true', adhocRemoteString: 'test remote']
            def results = sec._doupdate(params,'test','test',null)
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
            assertFalse execution.adhocExecution
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
        if (true) {//test update from one adhoc type to another; local -> file
            def se = new ScheduledExecution(jobName: 'monkey1', project: 'testProject', description: 'blah',
                                            adhocExecution: true, adhocLocalString: 'test local',
                                            command: '', type: '',)
            se.save()

            assertNotNull se.id

            //try to do update of the ScheduledExecution
            def fwkControl = mockFor(FrameworkService, true)
            fwkControl.demand.getFrameworkFromUserSession {session, request -> return null }
            fwkControl.demand.existsFrameworkProject {project, framework ->
                assertEquals 'testProject2', project
                return true
            }
            fwkControl.demand.getCommand {project, type, command, framework ->
                assertEquals 'testProject2', project
                assertEquals '', type
                assertEquals '', command
                return null
            }
            fwkControl.demand.authorizeProjectJobAll {framework, resource, actions, project -> return true}
            fwkControl.demand.getFrameworkFromUserSession {session, request -> return null }
            fwkControl.demand.getFrameworkFromUserSession {session, request -> return null }
            sec.frameworkService = fwkControl.createMock()

//            def sesControl = mockFor(ScheduledExecutionService, true)
//            sesControl.demand.getByIDorUUID {id -> return se }
//            sec.scheduledExecutionService = sesControl.createMock()

            def params = [id: se.id.toString(), jobName: 'monkey2', project: 'testProject2', description: 'blah', adhocExecution: 'true', adhocFilepath: 'test file']
            def results = sec._doupdate(params,'test','test',null)
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
            assertFalse execution.adhocExecution
            assertNotNull execution.workflow
            assertNotNull execution.workflow.commands
            assertEquals 1, execution.workflow.commands.size()
            def CommandExec cexec = execution.workflow.commands[0]
            assertTrue cexec.adhocExecution
            assertEquals 'test file', cexec.adhocFilepath
            assertNull cexec.adhocLocalString
            assertNull cexec.adhocRemoteString
            assertNull cexec.argString
            assertNull execution.argString
            assertNull execution.notifications
            assertNull execution.options
        }
        if (true) {//test update from one adhoc type to another; file -> remote
            def se = new ScheduledExecution(jobName: 'monkey1', project: 'testProject', description: 'blah',
                                            adhocExecution: true, adhocFilepath: 'test file',
                                            command: '', type: '',)
            se.save()

            assertNotNull se.id

            //try to do update of the ScheduledExecution
            def fwkControl = mockFor(FrameworkService, true)
            fwkControl.demand.getFrameworkFromUserSession {session, request -> return null }
            fwkControl.demand.existsFrameworkProject {project, framework ->
                assertEquals 'testProject2', project
                return true
            }
            fwkControl.demand.getCommand {project, type, command, framework ->
                assertEquals 'testProject2', project
                assertEquals '', type
                assertEquals '', command
                return null
            }
            fwkControl.demand.authorizeProjectJobAll {framework, resource, actions, project -> return true}
            fwkControl.demand.getFrameworkFromUserSession {session, request -> return null }
            fwkControl.demand.getFrameworkFromUserSession {session, request -> return null }
            sec.frameworkService = fwkControl.createMock()

//            def sesControl = mockFor(ScheduledExecutionService, true)
//            sesControl.demand.getByIDorUUID {id -> return se }
//            sec.scheduledExecutionService = sesControl.createMock()

            def params = [id: se.id.toString(), jobName: 'monkey2', project: 'testProject2', description: 'blah', adhocExecution: 'true', adhocRemoteString: 'test remote']
            def results = sec._doupdate(params,'test','test',null)
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
            assertFalse execution.adhocExecution
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
        if (true) {//test update from one adhoc type to another; file -> local
            def se = new ScheduledExecution(jobName: 'monkey1', project: 'testProject', description: 'blah',
                                            adhocExecution: true, adhocFilepath: 'test file',
                                            command: '', type: '',)
            se.save()

            assertNotNull se.id

            //try to do update of the ScheduledExecution
            def fwkControl = mockFor(FrameworkService, true)
            fwkControl.demand.getFrameworkFromUserSession {session, request -> return null }
            fwkControl.demand.existsFrameworkProject {project, framework ->
                assertEquals 'testProject2', project
                return true
            }
            fwkControl.demand.getCommand {project, type, command, framework ->
                assertEquals 'testProject2', project
                assertEquals '', type
                assertEquals '', command
                return null
            }
            fwkControl.demand.authorizeProjectJobAll {framework, resource, actions, project -> return true}
            fwkControl.demand.getFrameworkFromUserSession {session, request -> return null }
            fwkControl.demand.getFrameworkFromUserSession {session, request -> return null }
            sec.frameworkService = fwkControl.createMock()

//            def sesControl = mockFor(ScheduledExecutionService, true)
//            sesControl.demand.getByIDorUUID {id -> return se }
//            sec.scheduledExecutionService = sesControl.createMock()

            def params = [id: se.id.toString(), jobName: 'monkey2', project: 'testProject2', description: 'blah', adhocExecution: 'true', adhocLocalString: 'test local']
            def results = sec._doupdate(params,'test','test',null)
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
            assertFalse execution.adhocExecution
            assertNotNull execution.workflow
            assertNotNull execution.workflow.commands
            assertEquals 1, execution.workflow.commands.size()
            def CommandExec cexec = execution.workflow.commands[0]
            assertTrue cexec.adhocExecution
            assertNull cexec.adhocFilepath
            assertEquals 'test local', cexec.adhocLocalString
            assertNull cexec.adhocRemoteString
            assertNull cexec.argString
            assertNull execution.argString
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
        fwkControl.demand.getFrameworkFromUserSession {session, request -> return null }
        fwkControl.demand.existsFrameworkProject {project, framework ->
            assertEquals 'testProject2', project
            return true
        }
        fwkControl.demand.getCommand {project, type, command, framework ->
            assertEquals 'testProject2', project
            assertEquals '', type
            assertEquals '', command
            return null
        }
        sec.frameworkService = fwkControl.createMock()

        def params = new ScheduledExecution(jobName: 'monkey2', project: 'testProject2', description: 'blah',
                                            workflow: new Workflow(commands: [new CommandExec(adhocExecution: 'true', adhocRemoteString: '')])
        )
        def results = sec._doupdateJob(se.id.toString(), params,'test','test',null)
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
        fwkControl.demand.getFrameworkFromUserSession {session, request -> return null }
        fwkControl.demand.existsFrameworkProject {project, framework ->
            assertEquals 'testProject2', project
            return true
        }
        sec.frameworkService = fwkControl.createMock()

        def params = new ScheduledExecution(jobName: 'monkey2', project: 'testProject2', description: 'blah',
                                            workflow: new Workflow(commands: [new CommandExec(adhocExecution: 'true', adhocRemoteString: 'test remote')])
        )
        def results = sec._doupdateJob(se.id.toString(), params,'test','test',null)
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
        assertFalse execution.adhocExecution
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

    public void testDoUpdateNotificationsShouldUpdate() {
        def sec = new ScheduledExecutionService()
        if (true) {//test update job  notifications, disabling onsuccess
            def se = new ScheduledExecution(jobName: 'monkey1', project: 'testProject', description: 'blah2', adhocExecution: true, adhocRemoteString: 'test command',)
            def na1 = new Notification(eventTrigger: 'onsuccess', type: 'email', content: 'c@example.com,d@example.com')
            def na2 = new Notification(eventTrigger: 'onfailure', type: 'email', content: 'monkey@example.com')
            se.addToNotifications(na1)
            se.addToNotifications(na2)
            se.save()

            assertNotNull se.id

            //try to do update of the ScheduledExecution
            def fwkControl = mockFor(FrameworkService, true)
            fwkControl.demand.getFrameworkFromUserSession {session, request -> return null }
            fwkControl.demand.existsFrameworkProject {project, framework ->
                assertEquals 'testProject', project
                return true
            }
            fwkControl.demand.getCommand {project, type, command, framework ->
                assertEquals 'testProject', project
                assertEquals 'aType', type
                assertEquals 'aCommand', command
                return null
            }
            fwkControl.demand.authorizeProjectJobAll {framework, resource, actions, project -> return true}
            fwkControl.demand.getFrameworkFromUserSession {session, request -> return null }
            fwkControl.demand.getFrameworkFromUserSession {session, request -> return null }
            sec.frameworkService = fwkControl.createMock()

//            def sesControl = mockFor(ScheduledExecutionService, true)
//            sesControl.demand.getByIDorUUID {id -> return se }
//            sec.scheduledExecutionService = sesControl.createMock()

            def params = [id: se.id.toString(), jobName: 'monkey1', project: 'testProject', description: 'blah2', adhocExecution: true, adhocRemoteString: 'test command',
                    notified: 'true',
                    notifySuccessRecipients: 'spaghetti@nowhere.com',
                    notifyOnfailure: 'true', notifyFailureRecipients: 'milk@store.com',
            ]
            def results = sec._doupdate(params,'test','test',null)
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
            assertFalse execution.adhocExecution
            assertNotNull execution.workflow
            assertNotNull execution.workflow.commands
            assertEquals 1, execution.workflow.commands.size()
            def CommandExec cexec = execution.workflow.commands[0]
            assertTrue cexec.adhocExecution
            assertEquals 'test command', cexec.adhocRemoteString
            assertNull cexec.adhocFilepath
            assertNull execution.adhocLocalString
            assertNull execution.adhocRemoteString
            assertNull execution.argString

            assertNotNull execution.notifications
            assertEquals 1, execution.notifications.size()
            def nmap = [:]
            execution.notifications.each {not1 ->
                nmap[not1.eventTrigger] = not1
            }
            assertNull(nmap.onsuccess)
            assertNotNull(nmap.onfailure)
            assertTrue(nmap.onfailure instanceof Notification)
            def Notification n2 = nmap.onfailure
            assertEquals "onfailure", n2.eventTrigger
            assertEquals "email", n2.type
            assertEquals "milk@store.com", n2.content
        }
        if (true) {//test update job  notifications, replacing onsuccess, and removing onfailure
            def se = new ScheduledExecution(jobName: 'monkey1', project: 'testProject', description: 'blah2', adhocExecution: true, adhocRemoteString: 'test command',)
            def na1 = new Notification(eventTrigger: 'onsuccess', type: 'email', content: 'c@example.com,d@example.com')
            def na2 = new Notification(eventTrigger: 'onfailure', type: 'email', content: 'monkey@example.com')
            se.addToNotifications(na1)
            se.addToNotifications(na2)
            se.save()

            assertNotNull se.id

            //try to do update of the ScheduledExecution
            def fwkControl = mockFor(FrameworkService, true)
            fwkControl.demand.getFrameworkFromUserSession {session, request -> return null }
            fwkControl.demand.existsFrameworkProject {project, framework ->
                assertEquals 'testProject', project
                return true
            }
            fwkControl.demand.getCommand {project, type, command, framework ->
                assertEquals 'testProject', project
                assertEquals 'aType', type
                assertEquals 'aCommand', command
                return null
            }
            fwkControl.demand.authorizeProjectJobAll {framework, resource, actions, project -> return true}
            fwkControl.demand.getFrameworkFromUserSession {session, request -> return null }
            fwkControl.demand.getFrameworkFromUserSession {session, request -> return null }
            sec.frameworkService = fwkControl.createMock()

//            def sesControl = mockFor(ScheduledExecutionService, true)
//            sesControl.demand.getByIDorUUID {id -> return se }
//            sec.scheduledExecutionService = sesControl.createMock()

            def params = [id: se.id.toString(), jobName: 'monkey1', project: 'testProject', description: 'blah2', adhocExecution: true, adhocRemoteString: 'test command',
                    notified: 'true',
                    notifyOnsuccessUrl: 'true', notifySuccessUrl: 'http://example.com',
                    notifyFailureRecipients: 'milk@store.com',
            ]
            def results = sec._doupdate(params,'test','test',null)
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
            assertFalse execution.adhocExecution
            assertNotNull execution.workflow
            assertNotNull execution.workflow.commands
            assertEquals 1, execution.workflow.commands.size()
            def CommandExec cexec = execution.workflow.commands[0]
            assertTrue cexec.adhocExecution
            assertEquals 'test command', cexec.adhocRemoteString
            assertNull cexec.adhocFilepath
            assertNull execution.adhocLocalString
            assertNull execution.adhocRemoteString
            assertNull execution.argString

            assertNotNull execution.notifications
            assertEquals 1, execution.notifications.size()
            def nmap = [:]
            execution.notifications.each {not1 ->
                nmap[not1.eventTrigger] = not1
            }
            assertNotNull(nmap.onsuccess)
            assertNull(nmap.onfailure)
            assertTrue(nmap.onsuccess instanceof Notification)
            def Notification n2 = nmap.onsuccess
            assertEquals "onsuccess", n2.eventTrigger
            assertEquals "url", n2.type
            assertEquals "http://example.com", n2.content
        }

        if (true) {//test update job  notifications, removing all notifications
            def se = new ScheduledExecution(jobName: 'monkey1', project: 'testProject', description: 'blah2', adhocExecution: true, adhocRemoteString: 'test command',)
            def na1 = new Notification(eventTrigger: 'onsuccess', type: 'email', content: 'c@example.com,d@example.com')
            def na2 = new Notification(eventTrigger: 'onfailure', type: 'email', content: 'monkey@example.com')
            se.addToNotifications(na1)
            se.addToNotifications(na2)
            se.save()

            assertNotNull se.id

            //try to do update of the ScheduledExecution
            def fwkControl = mockFor(FrameworkService, true)
            fwkControl.demand.getFrameworkFromUserSession {session, request -> return null }
            fwkControl.demand.existsFrameworkProject {project, framework ->
                assertEquals 'testProject', project
                return true
            }
            fwkControl.demand.getCommand {project, type, command, framework ->
                assertEquals 'testProject', project
                assertEquals 'aType', type
                assertEquals 'aCommand', command
                return null
            }
            fwkControl.demand.authorizeProjectJobAll {framework, resource, actions, project -> return true}
            fwkControl.demand.getFrameworkFromUserSession {session, request -> return null }
            fwkControl.demand.getFrameworkFromUserSession {session, request -> return null }
            sec.frameworkService = fwkControl.createMock()

//            def sesControl = mockFor(ScheduledExecutionService, true)
//            sesControl.demand.getByIDorUUID {id -> return se }
//            sec.scheduledExecutionService = sesControl.createMock()

            def params = [id: se.id.toString(), jobName: 'monkey1', project: 'testProject', description: 'blah2', adhocExecution: true, adhocRemoteString: 'test command',
                    notified: 'false',
                    notifyOnsuccessUrl: 'true', notifySuccessUrl: 'http://example.com',
                    notifyFailureRecipients: 'milk@store.com',
            ]
            def results = sec._doupdate(params,'test','test',null)
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
            assertFalse execution.adhocExecution
            assertNotNull execution.workflow
            assertNotNull execution.workflow.commands
            assertEquals 1, execution.workflow.commands.size()
            def CommandExec cexec = execution.workflow.commands[0]
            assertTrue cexec.adhocExecution
            assertEquals 'test command', cexec.adhocRemoteString
            assertNull cexec.adhocFilepath
            assertNull execution.adhocLocalString
            assertNull execution.adhocRemoteString
            assertNull execution.argString

            assertNull execution.notifications
        }

        if (true) {//test update job  notifications, removing all notifications by unchecking
            def se = new ScheduledExecution(jobName: 'monkey1', project: 'testProject', description: 'blah2', adhocExecution: true, adhocRemoteString: 'test command',)
            def na1 = new Notification(eventTrigger: 'onsuccess', type: 'email', content: 'c@example.com,d@example.com')
            def na2 = new Notification(eventTrigger: 'onfailure', type: 'email', content: 'monkey@example.com')
            se.addToNotifications(na1)
            se.addToNotifications(na2)
            se.save()

            assertNotNull se.id

            //try to do update of the ScheduledExecution
            def fwkControl = mockFor(FrameworkService, true)
            fwkControl.demand.getFrameworkFromUserSession {session, request -> return null }
            fwkControl.demand.existsFrameworkProject {project, framework ->
                assertEquals 'testProject', project
                return true
            }
            fwkControl.demand.getCommand {project, type, command, framework ->
                assertEquals 'testProject', project
                assertEquals 'aType', type
                assertEquals 'aCommand', command
                return null
            }
            fwkControl.demand.authorizeProjectJobAll {framework, resource, actions, project -> return true}
            fwkControl.demand.getFrameworkFromUserSession {session, request -> return null }
            fwkControl.demand.getFrameworkFromUserSession {session, request -> return null }
            sec.frameworkService = fwkControl.createMock()

//            def sesControl = mockFor(ScheduledExecutionService, true)
//            sesControl.demand.getByIDorUUID {id -> return se }
//            sec.scheduledExecutionService = sesControl.createMock()

            def params = [id: se.id.toString(), jobName: 'monkey1', project: 'testProject', description: 'blah2', adhocExecution: true, adhocRemoteString: 'test command',
                    notified: 'true',
                    notifySuccessUrl: 'http://example.com',
                    notifyFailureRecipients: 'milk@store.com',
            ]
            def results = sec._doupdate(params,'test','test',null)
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
            assertFalse execution.adhocExecution
            assertNotNull execution.workflow
            assertNotNull execution.workflow.commands
            assertEquals 1, execution.workflow.commands.size()
            def CommandExec cexec = execution.workflow.commands[0]
            assertTrue cexec.adhocExecution
            assertEquals 'test command', cexec.adhocRemoteString
            assertNull cexec.adhocFilepath
            assertNull execution.adhocLocalString
            assertNull execution.adhocRemoteString
            assertNull execution.argString

            assertNull execution.notifications
        }
    }

    public void testDoUpdateNotifications() {
        def sec = new ScheduledExecutionService()
        if (true) {//test update job  notifications
            def se = new ScheduledExecution(jobName: 'monkey1', project: 'testProject', description: 'blah', adhocExecution: true, adhocRemoteString: 'test command',)
            def na1 = new Notification(eventTrigger: 'onsuccess', type: 'email', content: 'c@example.com,d@example.com')
            def na2 = new Notification(eventTrigger: 'onfailure', type: 'email', content: 'monkey@example.com')
            se.addToNotifications(na1)
            se.addToNotifications(na2)
            se.save()

            assertNotNull se.id

            //try to do update of the ScheduledExecution
            def fwkControl = mockFor(FrameworkService, true)
            fwkControl.demand.getFrameworkFromUserSession {session, request -> return null }
            fwkControl.demand.existsFrameworkProject {project, framework ->
                assertEquals 'testProject', project
                return true
            }
            fwkControl.demand.getCommand {project, type, command, framework ->
                assertEquals 'testProject', project
                assertEquals 'aType', type
                assertEquals 'aCommand', command
                return null
            }
            fwkControl.demand.authorizeProjectJobAll {framework, resource, actions, project -> return true}
            fwkControl.demand.getFrameworkFromUserSession {session, request -> return null }
            fwkControl.demand.getFrameworkFromUserSession {session, request -> return null }
            sec.frameworkService = fwkControl.createMock()

//            def sesControl = mockFor(ScheduledExecutionService, true)
//            sesControl.demand.getByIDorUUID {id -> return se }
//            sec.scheduledExecutionService = sesControl.createMock()

            def params = [id: se.id.toString(), jobName: 'monkey1', project: 'testProject', description: 'blah', adhocExecution: true, adhocRemoteString: 'test command',
                    notifications: [[eventTrigger: 'onsuccess', type: 'email', content: 'spaghetti@nowhere.com'], [eventTrigger: 'onfailure', type: 'email', content: 'milk@store.com']]
            ]
            def results = sec._doupdate(params,'test','test',null)
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

            assertFalse execution.adhocExecution
            assertNotNull execution.workflow
            assertNotNull execution.workflow.commands
            assertEquals 1, execution.workflow.commands.size()
            def CommandExec cexec = execution.workflow.commands[0]
            assertTrue cexec.adhocExecution
            assertEquals 'test command', cexec.adhocRemoteString
            assertNull cexec.adhocFilepath
            assertNull execution.adhocLocalString
            assertNull execution.adhocRemoteString
            assertNull execution.argString

            assertNotNull execution.notifications
            assertEquals 2, execution.notifications.size()
            def nmap = [:]
            execution.notifications.each {not1 ->
                nmap[not1.eventTrigger] = not1
            }
            assertNotNull(nmap.onsuccess)
            assertNotNull(nmap.onfailure)
            assertTrue(nmap.onsuccess instanceof Notification)
            assertTrue(nmap.onfailure instanceof Notification)
            def Notification n = nmap.onsuccess
            assertEquals "onsuccess", n.eventTrigger
            assertEquals "email", n.type
            assertEquals "spaghetti@nowhere.com", n.content
            def Notification n2 = nmap.onfailure
            assertEquals "onfailure", n2.eventTrigger
            assertEquals "email", n2.type
            assertEquals "milk@store.com", n2.content
        }

        if (true) {//test update job  notifications, using form input parameters
            def se = new ScheduledExecution(jobName: 'monkey1', project: 'testProject', description: 'blah', adhocExecution: true, adhocRemoteString: 'test command',)
            def na1 = new Notification(eventTrigger: 'onsuccess', type: 'email', content: 'c@example.com,d@example.com')
            def na2 = new Notification(eventTrigger: 'onfailure', type: 'email', content: 'monkey@example.com')
            se.addToNotifications(na1)
            se.addToNotifications(na2)
            se.save()

            assertNotNull se.id

            //try to do update of the ScheduledExecution
            def fwkControl = mockFor(FrameworkService, true)
            fwkControl.demand.getFrameworkFromUserSession {session, request -> return null }
            fwkControl.demand.existsFrameworkProject {project, framework ->
                assertEquals 'testProject', project
                return true
            }
            fwkControl.demand.getCommand {project, type, command, framework ->
                assertEquals 'testProject', project
                assertEquals 'aType', type
                assertEquals 'aCommand', command
                return null
            }
            fwkControl.demand.authorizeProjectJobAll {framework, resource, actions, project -> return true}
            fwkControl.demand.getFrameworkFromUserSession {session, request -> return null }
            fwkControl.demand.getFrameworkFromUserSession {session, request -> return null }
            sec.frameworkService = fwkControl.createMock()

//            def sesControl = mockFor(ScheduledExecutionService, true)
//            sesControl.demand.getByIDorUUID {id -> return se }
//            sec.scheduledExecutionService = sesControl.createMock()

            def params = [id: se.id.toString(), jobName: 'monkey1', project: 'testProject', description: 'blah', adhocExecution: true, adhocRemoteString: 'test command',
                    notifyOnsuccess: 'true', notifySuccessRecipients: 'spaghetti@nowhere.com',
                    notifyOnfailure: 'true', notifyFailureRecipients: 'milk@store.com',
            ]
            def results = sec._doupdate(params,'test','test',null)
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
            assertFalse execution.adhocExecution
            assertNotNull execution.workflow
            assertNotNull execution.workflow.commands
            assertEquals 1, execution.workflow.commands.size()
            def CommandExec cexec = execution.workflow.commands[0]
            assertTrue cexec.adhocExecution
            assertEquals 'test command', cexec.adhocRemoteString
            assertNull cexec.adhocFilepath
            assertNull execution.adhocLocalString
            assertNull execution.adhocRemoteString
            assertNull execution.argString

            assertNotNull execution.notifications
            assertEquals 2, execution.notifications.size()
            def nmap = [:]
            execution.notifications.each {not1 ->
                nmap[not1.eventTrigger] = not1
            }
            assertNotNull(nmap.onsuccess)
            assertNotNull(nmap.onfailure)
            assertTrue(nmap.onsuccess instanceof Notification)
            assertTrue(nmap.onfailure instanceof Notification)
            def Notification n = nmap.onsuccess
            assertEquals "onsuccess", n.eventTrigger
            assertEquals "email", n.type
            assertEquals "spaghetti@nowhere.com", n.content
            def Notification n2 = nmap.onfailure
            assertEquals "onfailure", n2.eventTrigger
            assertEquals "email", n2.type
            assertEquals "milk@store.com", n2.content
        }
        if (true) {//test update job  notifications, using form input parameters, invalid email addresses
            def se = new ScheduledExecution(jobName: 'monkey1', project: 'testProject', description: 'blah', adhocExecution: true, adhocRemoteString: 'test command',)
            def na1 = new Notification(eventTrigger: 'onsuccess', type: 'email', content: 'c@example.com,d@example.com')
            def na2 = new Notification(eventTrigger: 'onfailure', type: 'email', content: 'monkey@example.com')
            se.addToNotifications(na1)
            se.addToNotifications(na2)
            se.save(flush: true)

            assertNotNull se.id

            //try to do update of the ScheduledExecution
            def fwkControl = mockFor(FrameworkService, true)
            fwkControl.demand.getFrameworkFromUserSession {session, request -> return null }
            fwkControl.demand.existsFrameworkProject {project, framework ->
                assertEquals 'testProject', project
                return true
            }
            fwkControl.demand.getCommand {project, type, command, framework ->
                assertEquals 'testProject', project
                assertEquals 'aType', type
                assertEquals 'aCommand', command
                return null
            }
            fwkControl.demand.authorizeProjectJobAll {framework, resource, actions, project -> return true}
            fwkControl.demand.getFrameworkFromUserSession {session, request -> return null }
            fwkControl.demand.getFrameworkFromUserSession {session, request -> return null }
            sec.frameworkService = fwkControl.createMock()

//            def sesControl = mockFor(ScheduledExecutionService, true)
//            sesControl.demand.getByIDorUUID {id -> return se }
//            sec.scheduledExecutionService = sesControl.createMock()

            def params = [id: se.id.toString(), jobName: 'monkey1', project: 'testProject', description: 'blah', adhocExecution: true, adhocRemoteString: 'test command',
                    notifyOnsuccess: 'true', notifySuccessRecipients: 'spaghetti@ nowhere.com',
                    notifyOnfailure: 'true', notifyFailureRecipients: 'milkstore.com',
            ]
            def results = sec._doupdate(params,'test','test',null)
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
            assertTrue(executionErr.errors.hasFieldErrors('notifyFailureRecipients'))
            assertTrue(executionErr.errors.hasFieldErrors('notifySuccessRecipients'))

            final ScheduledExecution execution = ScheduledExecution.get(scheduledExecution.id)

            assertEquals 'monkey1', execution.jobName
            assertEquals 'testProject', execution.project
            assertEquals 'blah', execution.description
            assertEquals 'test command', execution.adhocRemoteString
            assertTrue execution.adhocExecution
            assertNotNull execution.notifications
            assertEquals 2, execution.notifications.size()
            def nmap = [:]
            execution.notifications.each {not1 ->
                nmap[not1.eventTrigger] = not1
            }
            assertNotNull(nmap.onsuccess)
            assertNotNull(nmap.onfailure)
            assertTrue(nmap.onsuccess instanceof Notification)
            assertTrue(nmap.onfailure instanceof Notification)
            def Notification n = nmap.onsuccess
            assertEquals "onsuccess", n.eventTrigger
            assertEquals "email", n.type
            assertEquals "c@example.com,d@example.com", n.content
            def Notification n2 = nmap.onfailure
            assertEquals "onfailure", n2.eventTrigger
            assertEquals "email", n2.type
            assertEquals "monkey@example.com", n2.content
        }
    }

    public void testDoUpdateJobShouldUpdateNotifications() {
        def sec = new ScheduledExecutionService()
        if (true) {//test update job  notifications
            def se = new ScheduledExecution(jobName: 'monkey1', project: 'testProject', description: 'blah',
                                            workflow: new Workflow(commands: [new CommandExec(adhocExecution: true, adhocRemoteString: 'test command',)])
            )
            def na1 = new Notification(eventTrigger: 'onsuccess', type: 'email', content: 'c@example.com,d@example.com')
            def na2 = new Notification(eventTrigger: 'onfailure', type: 'email', content: 'monkey@example.com')
            se.addToNotifications(na1)
            se.addToNotifications(na2)
            se.save()

            assertNotNull se.id

            //try to do update of the ScheduledExecution
            def fwkControl = mockFor(FrameworkService, true)
            fwkControl.demand.getFrameworkFromUserSession {session, request -> return null }
            fwkControl.demand.existsFrameworkProject {project, framework ->
                assertEquals 'testProject', project
                return true
            }
            fwkControl.demand.getCommand {project, type, command, framework ->
                assertEquals 'testProject', project
                assertEquals 'aType', type
                assertEquals 'aCommand', command
                return null
            }
            fwkControl.demand.authorizeProjectJobAll {framework, resource, actions, project -> return true}
            fwkControl.demand.getFrameworkFromUserSession {session, request -> return null }
            fwkControl.demand.getFrameworkFromUserSession {session, request -> return null }
            sec.frameworkService = fwkControl.createMock()

            def params = new ScheduledExecution(jobName: 'monkey1', project: 'testProject', description: 'blah', adhocExecution: true, adhocRemoteString: 'test command',
                                                notifications: [new Notification(eventTrigger: 'onsuccess', type: 'email', content: 'spaghetti@nowhere.com'),
                                                        new Notification(eventTrigger: 'onfailure', type: 'email', content: 'milk@store.com')
                                                ])
            def results = sec._doupdateJob(se.id, params,'test','test',null)
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

            assertFalse execution.adhocExecution
            assertNotNull execution.workflow
            assertNotNull execution.workflow.commands
            assertEquals 1, execution.workflow.commands.size()
            def CommandExec cexec = execution.workflow.commands[0]
            assertTrue cexec.adhocExecution
            assertEquals 'test command', cexec.adhocRemoteString
            assertNull cexec.adhocFilepath
            assertNull execution.adhocLocalString
            assertNull execution.adhocRemoteString
            assertNull execution.argString

            assertNotNull execution.notifications
            assertEquals 2, execution.notifications.size()
            def nmap = [:]
            execution.notifications.each {not1 ->
                nmap[not1.eventTrigger] = not1
            }
            assertNotNull(nmap.onsuccess)
            assertNotNull(nmap.onfailure)
            assertTrue(nmap.onsuccess instanceof Notification)
            assertTrue(nmap.onfailure instanceof Notification)
            def Notification n = nmap.onsuccess
            assertEquals "onsuccess", n.eventTrigger
            assertEquals "email", n.type
            assertEquals "spaghetti@nowhere.com", n.content
            def Notification n2 = nmap.onfailure
            assertEquals "onfailure", n2.eventTrigger
            assertEquals "email", n2.type
            assertEquals "milk@store.com", n2.content
        }
    }

    public void testDoUpdateJobShouldFailBadNotifications() {
        def sec = new ScheduledExecutionService()
        if (true) {//test update job  notifications, invalid email addresses
            def se = new ScheduledExecution(jobName: 'monkey1', project: 'testProject', description: 'blah', adhocExecution: true, adhocRemoteString: 'test command',)
            def na1 = new Notification(eventTrigger: 'onsuccess', type: 'email', content: 'c@example.com,d@example.com')
            def na2 = new Notification(eventTrigger: 'onfailure', type: 'email', content: 'monkey@example.com')
            se.addToNotifications(na1)
            se.addToNotifications(na2)
            se.save(flush: true)

            assertNotNull se.id

            //try to do update of the ScheduledExecution
            def fwkControl = mockFor(FrameworkService, true)
            fwkControl.demand.getFrameworkFromUserSession {session, request -> return null }
            fwkControl.demand.existsFrameworkProject {project, framework ->
                assertEquals 'testProject', project
                return true
            }
            fwkControl.demand.getCommand {project, type, command, framework ->
                assertEquals 'testProject', project
                assertEquals 'aType', type
                assertEquals 'aCommand', command
                return null
            }
            fwkControl.demand.authorizeProjectJobAll {framework, resource, actions, project -> return true}
            fwkControl.demand.getFrameworkFromUserSession {session, request -> return null }
            fwkControl.demand.getFrameworkFromUserSession {session, request -> return null }
            sec.frameworkService = fwkControl.createMock()

            def params = new ScheduledExecution(jobName: 'monkey1', project: 'testProject', description: 'blah', adhocExecution: true, adhocRemoteString: 'test command',
                                                notifications: [new Notification(eventTrigger: 'onsuccess', type: 'email', content: 'spaghetti@ nowhere.com'),
                                                        new Notification(eventTrigger: 'onfailure', type: 'email', content: 'milkstore.com')
                                                ])
            def results = sec._doupdateJob(se.id, params,'test','test',null)
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
            assertTrue(executionErr.errors.hasFieldErrors('notifyFailureRecipients'))
            assertTrue(executionErr.errors.hasFieldErrors('notifySuccessRecipients'))

            final ScheduledExecution execution = ScheduledExecution.get(scheduledExecution.id)

            assertEquals 'monkey1', execution.jobName
            assertEquals 'testProject', execution.project
            assertEquals 'blah', execution.description
            assertEquals 'test command', execution.adhocRemoteString
            assertTrue execution.adhocExecution
            assertNotNull execution.notifications
            assertEquals 2, execution.notifications.size()
            def nmap = [:]
            execution.notifications.each {not1 ->
                nmap[not1.eventTrigger] = not1
            }
            assertNotNull(nmap.onsuccess)
            assertNotNull(nmap.onfailure)
            assertTrue(nmap.onsuccess instanceof Notification)
            assertTrue(nmap.onfailure instanceof Notification)
            def Notification n = nmap.onsuccess
            assertEquals "onsuccess", n.eventTrigger
            assertEquals "email", n.type
            assertEquals "c@example.com,d@example.com", n.content
            def Notification n2 = nmap.onfailure
            assertEquals "onfailure", n2.eventTrigger
            assertEquals "email", n2.type
            assertEquals "monkey@example.com", n2.content
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
            fwkControl.demand.getFrameworkFromUserSession {session, request -> return null }
            fwkControl.demand.existsFrameworkProject {project, framework ->
                return true
            }
            fwkControl.demand.getCommand {project, type, command, framework ->
                return null
            }
            fwkControl.demand.authorizeProjectJobAll {framework, resource, actions, project -> return true}
            fwkControl.demand.getFrameworkFromUserSession {session, request -> return null }
            fwkControl.demand.getFrameworkFromUserSession {session, request -> return null }
            sec.frameworkService = fwkControl.createMock()

//            def sesControl = mockFor(ScheduledExecutionService, true)
//            sesControl.demand.getByIDorUUID {id -> return se }
//            sec.scheduledExecutionService = sesControl.createMock()

            def params = [id: se.id.toString(), description: 'changed description', workflow: ['commands[0]': [adhocExecution: true, adhocRemoteString: 'test command2',]], '_workflow_data': true]
            def results = sec._doupdate(params,'test','test',null)
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
                    scheduledExecution.workflow.commands.each {cexec ->
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

            def se = new ScheduledExecution(jobName: 'monkey1', project: 'testProject', description: 'blah3',nodeThreadcount: 1)
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
                    nodeThreadcount:'',
                    workflow: ['commands[0]': [adhocExecution: true, adhocRemoteString: 'test command2',]], '_workflow_data': true]
            def results = sec._doupdate(params, 'test', 'test', null)
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
            fwkControl.demand.getFrameworkFromUserSession {session, request -> return null }
            fwkControl.demand.existsFrameworkProject {project, framework ->
                return true
            }
            fwkControl.demand.getCommand {project, type, command, framework ->
                return null
            }
            fwkControl.demand.authorizeProjectJobAll {framework, resource, actions, project -> return true}
            fwkControl.demand.getFrameworkFromUserSession {session, request -> return null }
            fwkControl.demand.getFrameworkFromUserSession {session, request -> return null }
            sec.frameworkService = fwkControl.createMock()

//            def sesControl = mockFor(ScheduledExecutionService, true)
//            sesControl.demand.getByIDorUUID {id -> return se }
//            sec.scheduledExecutionService = sesControl.createMock()

            def params = [id: se.id.toString(), description: 'changed description',
                    options: ["options[0]": [name: 'test3', defaultValue: 'val3', enforced: false, valuesUrl: "http://test.com/test3"]]
            ]
            def results = sec._doupdate(params,'test','test',null)
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
            fwkControl.demand.getFrameworkFromUserSession {session, request -> return null }
            fwkControl.demand.existsFrameworkProject {project, framework ->
                return true
            }
            fwkControl.demand.getCommand {project, type, command, framework ->
                return null
            }
            fwkControl.demand.authorizeProjectJobAll {framework, resource, actions, project -> return true}
            fwkControl.demand.getFrameworkFromUserSession {session, request -> return null }
            fwkControl.demand.getFrameworkFromUserSession {session, request -> return null }
            sec.frameworkService = fwkControl.createMock()

//            def sesControl = mockFor(ScheduledExecutionService, true)
//            sesControl.demand.getByIDorUUID {id -> return se }
//            sec.scheduledExecutionService = sesControl.createMock()

            def params = [id: se.id.toString(), description: 'changed description', _nooptions: true]
            def results = sec._doupdate(params,'test','test',null)
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

    public void testDoUpdateOptions() {

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
            fwkControl.demand.getFrameworkFromUserSession {session, request -> return null }
            fwkControl.demand.existsFrameworkProject {project, framework ->
                return true
            }
            fwkControl.demand.getCommand {project, type, command, framework ->
                return null
            }
            fwkControl.demand.authorizeProjectJobAll {framework, resource, actions, project -> return true}
            fwkControl.demand.getFrameworkFromUserSession {session, request -> return null }
            fwkControl.demand.getFrameworkFromUserSession {session, request -> return null }
            sec.frameworkService = fwkControl.createMock()

//            def sesControl = mockFor(ScheduledExecutionService, true)
//            sesControl.demand.getByIDorUUID {id -> return se }
//            sec.scheduledExecutionService = sesControl.createMock()

            def params = [id: se.id.toString(), jobName: 'monkey1', project: 'testProject', description: 'blah2', adhocExecution: false, name: 'aResource', type: 'aType', command: 'aCommand']
            def results = sec._doupdate(params,'test','test',null)
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
            assertArrayEquals(['a', 'b', 'c'] as String[], next2.values as String[])

        }
        if (true) {//test update: set _nooptions to delete options

            def se = new ScheduledExecution(jobName: 'monkey1', project: 'testProject', description: 'blah2', adhocExecution: false, name: 'aResource', type: 'aType', command: 'aCommand')
            def opt1 = new Option(name: 'test1', defaultValue: 'val1', enforced: false, valuesUrl: "http://test.com/test")
            def opt2 = new Option(name: 'test2', defaultValue: 'val2', enforced: true, values: ['a', 'b', 'c'])
            se.addToOptions(opt1)
            se.addToOptions(opt2)
            se.save()

            assertNotNull se.id

            //try to do update of the ScheduledExecution
            def fwkControl = mockFor(FrameworkService, true)
            fwkControl.demand.getFrameworkFromUserSession {session, request -> return null }
            fwkControl.demand.existsFrameworkProject {project, framework ->
                return true
            }
            fwkControl.demand.getCommand {project, type, command, framework ->
                return null
            }
            fwkControl.demand.authorizeProjectJobAll {framework, resource, actions, project -> return true}
            fwkControl.demand.getFrameworkFromUserSession {session, request -> return null }
            fwkControl.demand.getFrameworkFromUserSession {session, request -> return null }
            sec.frameworkService = fwkControl.createMock()

//            def sesControl = mockFor(ScheduledExecutionService, true)
//            sesControl.demand.getByIDorUUID {id -> return se }
//            sec.scheduledExecutionService = sesControl.createMock()

            def params = [id: se.id.toString(), jobName: 'monkey1', project: 'testProject', description: 'blah2', adhocExecution: false, name: 'aResource', type: 'aType', command: 'aCommand', _nooptions: true]
            def results = sec._doupdate(params,'test','test',null)
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
            fwkControl.demand.getFrameworkFromUserSession {session, request -> return null }
            fwkControl.demand.existsFrameworkProject {project, framework ->
                return true
            }
            fwkControl.demand.getCommand {project, type, command, framework ->
                return null
            }
            fwkControl.demand.authorizeProjectJobAll {framework, resource, actions, project -> return true}
            fwkControl.demand.getFrameworkFromUserSession {session, request -> return null }
            fwkControl.demand.getFrameworkFromUserSession {session, request -> return null }
            sec.frameworkService = fwkControl.createMock()

//            def sesControl = mockFor(ScheduledExecutionService, true)
//            sesControl.demand.getByIDorUUID {id -> return se }
//            sec.scheduledExecutionService = sesControl.createMock()

            def params = [id: se.id.toString(), jobName: 'monkey1', project: 'testProject', description: 'blah2', adhocExecution: false, name: 'aResource', type: 'aType', command: 'aCommand',
                    options: ["options[0]": [name: 'test3', defaultValue: 'val3', enforced: false, valuesUrl: "http://test.com/test3"]]
            ]
            def results = sec._doupdate(params,'test','test',null)
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
        if (true) {//test update: set options to replace options, use old-style option.name to set argString of workflow item

            def se = new ScheduledExecution(jobName: 'monkey1', project: 'testProject', description: 'blah2', adhocExecution: true, adhocRemoteString: 'test command',)
            def opt1 = new Option(name: 'test1', defaultValue: 'val1', enforced: false, valuesUrl: "http://test.com/test")
            def opt2 = new Option(name: 'test2', defaultValue: 'val2', enforced: true, values: ['a', 'b', 'c'])
            se.addToOptions(opt1)
            se.addToOptions(opt2)
            se.save()

            assertNotNull se.id

            //try to do update of the ScheduledExecution
            def fwkControl = mockFor(FrameworkService, true)
            fwkControl.demand.getFrameworkFromUserSession {session, request -> return null }
            fwkControl.demand.existsFrameworkProject {project, framework ->
                return true
            }
            fwkControl.demand.getCommand {project, type, command, framework ->
                return null
            }
            fwkControl.demand.authorizeProjectJobAll {framework, resource, actions, project -> return true}
            fwkControl.demand.getFrameworkFromUserSession {session, request -> return null }
            fwkControl.demand.getFrameworkFromUserSession {session, request -> return null }
            sec.frameworkService = fwkControl.createMock()

//            def sesControl = mockFor(ScheduledExecutionService, true)
//            sesControl.demand.getByIDorUUID {id -> return se }
//            sec.scheduledExecutionService = sesControl.createMock()

            def params = [id: se.id.toString(), jobName: 'monkey1', project: 'testProject', description: 'blah2', adhocExecution: true, adhocRemoteString: 'test command',
                    'option.test3': 'val3', options: ["options[0]": [name: 'test3', defaultValue: 'val3', enforced: false, valuesUrl: "http://test.com/test3"]]
            ]
            def results = sec._doupdate(params,'test','test',null)
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

            assertNotNull execution.workflow
            assertEquals 1, execution.workflow.commands.size()
            final CommandExec exec = execution.workflow.commands[0]
            assertEquals 'test command', exec.adhocRemoteString
            assertEquals '-test3 ${option.test3}', exec.argString

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
            fwkControl.demand.getFrameworkFromUserSession {session, request -> return null }
            fwkControl.demand.existsFrameworkProject {project, framework ->
                return true
            }
            fwkControl.demand.getCommand {project, type, command, framework ->
                return null
            }
            fwkControl.demand.authorizeProjectJobAll {framework, resource, actions, project -> return true}
            fwkControl.demand.getFrameworkFromUserSession {session, request -> return null }
            fwkControl.demand.getFrameworkFromUserSession {session, request -> return null }
            sec.frameworkService = fwkControl.createMock()

//            def sesControl = mockFor(ScheduledExecutionService, true)
//            sesControl.demand.getByIDorUUID {id -> return se }
//            sec.scheduledExecutionService = sesControl.createMock()

            def params = [id: se.id.toString(), jobName: 'monkey1', project: 'testProject', description: 'blah2', adhocExecution: false, name: 'aResource', type: 'aType', command: 'aCommand',
                    options: ["options[0]": [name: 'test1', defaultValue: 'val3', enforced: false, valuesUrl: "http://test.com/test3"],
                            "options[1]": [name: 'test2', defaultValue: 'd', enforced: true, values: ['a', 'b', 'c', 'd']]]
            ]
            def results = sec._doupdate(params,'test','test',null)
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
            fwkControl.demand.getFrameworkFromUserSession {session, request -> return null }
            fwkControl.demand.existsFrameworkProject {project, framework ->
                return true
            }
            fwkControl.demand.getCommand {project, type, command, framework ->
                return null
            }
            fwkControl.demand.authorizeProjectJobAll {framework, resource, actions, project -> return true}
            fwkControl.demand.getFrameworkFromUserSession {session, request -> return null }
            fwkControl.demand.getFrameworkFromUserSession {session, request -> return null }
            sec.frameworkService = fwkControl.createMock()

//            def sesControl = mockFor(ScheduledExecutionService, true)
//            sesControl.demand.getByIDorUUID {id -> return se }
//            sec.scheduledExecutionService = sesControl.createMock()

            def ms = mockFor(MessageSource)
            ms.demand.getMessage {key, data, locale -> 'message'}
            ms.demand.getMessage {error, locale -> 'message'}
            sec.messageSource = ms.createMock()


            def params = [id: se.id.toString(), jobName: 'monkey1', project: 'testProject', description: 'blah2', adhocExecution: false, name: 'aResource', type: 'aType', command: 'aCommand',
                    options: ["options[0]": [name: 'test1', defaultValue: 'val3', enforced: false, multivalued: true],
                            "options[1]": [name: 'test2', defaultValue: 'val2', enforced: false, values: ['a', 'b', 'c', 'd'], multivalued: true, delimiter: "testdelim"]]
            ]
            def results = sec._doupdate(params,'test','test',null)
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
            fwkControl.demand.getFrameworkFromUserSession {session, request -> return null }
            fwkControl.demand.existsFrameworkProject {project, framework ->
                return true
            }
            fwkControl.demand.getCommand {project, type, command, framework ->
                return null
            }
            sec.frameworkService = fwkControl.createMock()

            def params = new ScheduledExecution(jobName: 'monkey1', project: 'testProject', description: 'blah2',
                                                workflow: new Workflow(commands: [new CommandExec(adhocRemoteString: 'test command', adhocExecution: true)]),
                                                )
            def results = sec._doupdateJob(se.id, params,'test','test',null)
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
            fwkControl.demand.getFrameworkFromUserSession {session, request -> return null }
            fwkControl.demand.existsFrameworkProject {project, framework ->
                return true
            }
            fwkControl.demand.getCommand {project, type, command, framework ->
                return null
            }
            fwkControl.demand.authorizeProjectJobAll {framework, resource, actions, project -> return true}
            fwkControl.demand.getFrameworkFromUserSession {session, request -> return null }
            fwkControl.demand.getFrameworkFromUserSession {session, request -> return null }
            sec.frameworkService = fwkControl.createMock()

            def params = [
                    id: se.id.toString(),
                    jobName: 'monkey2',
                    project: 'testProject',
                    description: 'blah',
                    workflow: [threadcount: 1, keepgoing: true, "commands[0]": [adhocExecution: true, adhocRemoteString: 'a remote string']],
                    _sessionopts: true,
                    _sessionEditOPTSObject:[:] //empty map to clear options
            ]

            def results = sec._doupdate(params, 'test', 'test', null)
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
            assertNull "should not have options",execution.options
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
            fwkControl.demand.getFrameworkFromUserSession {session, request -> return null }
            fwkControl.demand.existsFrameworkProject {project, framework ->
                return true
            }
            fwkControl.demand.getCommand {project, type, command, framework ->
                return null
            }
            sec.frameworkService = fwkControl.createMock()

            def params = new ScheduledExecution(jobName: 'monkey1', project: 'testProject', description: 'blah2',
                                                workflow: new Workflow(commands: [new CommandExec(adhocRemoteString: 'test command', adhocExecution: true)]),
                                                options: [
                                                        new Option(name: 'test3', defaultValue: 'val3', enforced: false, valuesUrl: "http://test.com/test3"),
                                                ]
            )
            def results = sec._doupdateJob(se.id, params,'test','test',null)
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
            fwkControl.demand.getFrameworkFromUserSession {session, request -> return null }
            fwkControl.demand.existsFrameworkProject {project, framework ->
                return true
            }
            fwkControl.demand.getCommand {project, type, command, framework ->
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
            def results = sec._doupdateJob(se.id, params,'test','test',null)
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
            fwkControl.demand.getFrameworkFromUserSession {session, request -> return null }
            fwkControl.demand.existsFrameworkProject {project, framework ->
                return true
            }
            fwkControl.demand.getCommand {project, type, command, framework ->
                return null
            }
            sec.frameworkService = fwkControl.createMock()

            def ms = mockFor(MessageSource)
            ms.demand.getMessage {key, data, locale -> 'message'}
            ms.demand.getMessage {error, locale -> 'message'}
            sec.messageSource = ms.createMock()

            def params = new ScheduledExecution(jobName: 'monkey1', project: 'testProject', description: 'blah2',
                                                workflow: new Workflow(commands: [new CommandExec(adhocRemoteString: 'test command', adhocExecution: true)]),
                                                options: [
                                                        new Option(name: 'test1', defaultValue: 'val3', enforced: false, valuesUrl: "http://test.com/test3", multivalued: true),
                                                        new Option(name: 'test2', defaultValue: 'd', enforced: true, values: ['a', 'b', 'c', 'd'], multivalued: true, delimiter: "testdelim")
                                                ]
            )
            def results = sec._doupdateJob(se.id, params,'test','test',null)
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

    public void testUploadShouldUpdateSameNameDupeOptionUpdate() {

        def sec = new ScheduledExecutionService()

        //create mock of FrameworkService
        def fwkControl = mockFor(FrameworkService, true)
        fwkControl.demand.getFrameworkFromUserSession {session, request -> return null }
        fwkControl.demand.getFrameworkFromUserSession {session, request -> return null }
        fwkControl.demand.existsFrameworkProject {project, framework -> return true }
        fwkControl.demand.authorizeProjectJobAll {framework, job, actions, project -> return true}
        fwkControl.demand.getFrameworkFromUserSession {session, request -> return null }
        fwkControl.demand.getFrameworkFromUserSession {session, request -> return null }
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
        def result=sec.loadJobs([upload], 'update', 'test', 'userrole,test', [:], null)
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
        fwkControl.demand.getFrameworkFromUserSession {session, request -> return null }
        fwkControl.demand.getFrameworkFromUserSession {session, request -> return null }
        fwkControl.demand.existsFrameworkProject {project, framework -> return true }
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
        def result = sec.loadJobs([upload],'skip','test','test,userrole', [:],null)
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
        fwkControl.demand.getFrameworkFromUserSession {session, request -> return null }
        fwkControl.demand.getFrameworkFromUserSession {session, request -> return null }
        fwkControl.demand.existsFrameworkProject {project, framework -> return true }
        fwkControl.demand.authorizeProjectResourceAll {framework, resource, actions, project -> return true}
        fwkControl.demand.authorizeProjectJobAll {framework, scheduledExecution, actions, project -> return true}
        fwkControl.demand.getFrameworkFromUserSession {session, request -> return null }
        fwkControl.demand.getFrameworkFromUserSession {session, request -> return null }
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
        def result = sec.loadJobs([upload],'create','test','test,userrole', [:],null)
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
        fwkControl.demand.getFrameworkFromUserSession {session, request -> return null }
        fwkControl.demand.getFrameworkFromUserSession {session, request -> return null }
        fwkControl.demand.existsFrameworkProject {project, framework -> return true }
        fwkControl.demand.authorizeProjectResourceAll {framework, resource, actions, project -> return true}
        fwkControl.demand.authorizeProjectJobAll {framework, scheduledExecution, actions, project -> return true}
        fwkControl.demand.getFrameworkFromUserSession {session, request -> return null }
        fwkControl.demand.getFrameworkFromUserSession {session, request -> return null }
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
        def result = sec.loadJobs([upload], 'skip', 'test', 'test,userrole', [:], null)
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
        fwkControl.demand.getFrameworkFromUserSession {session, request -> return null }
        fwkControl.demand.getFrameworkFromUserSession {session, request -> return null }
        fwkControl.demand.existsFrameworkProject {project, framework -> return true }
        fwkControl.demand.authorizeProjectResourceAll {framework, resource, actions, project -> return true}
        fwkControl.demand.authorizeProjectJobAll {framework, scheduledExecution, actions, project -> return true}
        fwkControl.demand.getFrameworkFromUserSession {session, request -> return null }
        fwkControl.demand.getFrameworkFromUserSession {session, request -> return null }
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

        def result = sec.loadJobs([upload], 'skip', 'test', 'test,userrole', [:], null)
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
        fwkControl.demand.getFrameworkFromUserSession {session, request -> return null }
        fwkControl.demand.getFrameworkFromUserSession {session, request -> return null }
        fwkControl.demand.existsFrameworkProject {project, framework -> return true }
        fwkControl.demand.authorizeProjectResourceAll {framework, resource, actions, project -> return true}
        fwkControl.demand.authorizeProjectJobAll {framework, scheduledExecution, actions, project -> return true}
        fwkControl.demand.getFrameworkFromUserSession {session, request -> return null }
        fwkControl.demand.getFrameworkFromUserSession {session, request -> return null }
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

        def result = sec.loadJobs([upload], 'skip', 'test', 'test,userrole', [:], null)
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

    public void testUploadShouldOverwriteFilters() {
        def sec = new ScheduledExecutionService()

        //create mock of FrameworkService
        def fwkControl = mockFor(FrameworkService, true)
        fwkControl.demand.getFrameworkFromUserSession {session, request -> return null }
        fwkControl.demand.getFrameworkFromUserSession {session, request -> return null }
        fwkControl.demand.existsFrameworkProject {project, framework -> return true }
        fwkControl.demand.authorizeProjectJobAll {framework, resource, actions, project -> return true}
        fwkControl.demand.getFrameworkFromUserSession {session, request -> return null }
        fwkControl.demand.getFrameworkFromUserSession {session, request -> return null }
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

        def result = sec.loadJobs([upload], 'update', 'test', 'test,userrole', [:], null)
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
        fwkControl.demand.getFrameworkFromUserSession {session, request -> return null }
        fwkControl.demand.getFrameworkFromUserSession {session, request -> return null }
        fwkControl.demand.existsFrameworkProject {project, framework -> return true }
        fwkControl.demand.authorizeProjectJobAll {framework, resource, actions, project -> return true}
        fwkControl.demand.getFrameworkFromUserSession {session, request -> return null }
        fwkControl.demand.getFrameworkFromUserSession {session, request -> return null }
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

        def result = sec.loadJobs([upload], 'update', 'test', 'test,userrole', [:], null)
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
        assertEquals(1,se.nodeThreadcount)

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

        def result = sec.loadJobs([upload], 'update', 'test', 'test,userrole', [:], null)
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

    public void testUploadErrorHandlers() {
        def sec = new ScheduledExecutionService()

        //create mock of FrameworkService
        def fwkControl = mockFor(FrameworkService, true)
        fwkControl.demand.getFrameworkFromUserSession {session, request -> return null }
        fwkControl.demand.getFrameworkFromUserSession {session, request -> return null }
        fwkControl.demand.existsFrameworkProject {project, framework -> return true }
        fwkControl.demand.authorizeProjectResourceAll {framework, resource, actions, project -> return true}
        fwkControl.demand.authorizeProjectJobAll {framework, resource, actions, project -> return true}
        fwkControl.demand.getFrameworkFromUserSession {session, request -> return null }
        fwkControl.demand.getFrameworkFromUserSession {session, request -> return null }
        sec.frameworkService = fwkControl.createMock()
        def ms = mockFor(MessageSource)
        ms.demand.getMessage {key, data, locale -> key}
        ms.demand.getMessage {error, locale -> error.toString()}
        sec.messageSource = ms.createMock()
        //test upload job with error-handlers

        def xml = '''
<joblist>
    <job>
        <name>testUploadErrorHandlers</name>
        <group>testgroup</group>
        <description>desc</description>
        <context>
            <project>project1</project>
        </context>

        <sequence keepgoing='false' strategy='node-first'>
          <command>
            <exec>echo hi</exec>

            <errorhandler>
              <exec>echo this is an errorhandler</exec>
            </errorhandler>
          </command>
          <command>
                <script>test2</script>
                <scriptargs>blah blah</scriptargs>
                <errorhandler>
                    <script>test2err</script>
                    <scriptargs>blah blah err</scriptargs>
                </errorhandler>
            </command>
            <command>
                <scriptfile>test3</scriptfile>
                <scriptargs>blah3 blah3</scriptargs>
                <errorhandler>
                    <scriptfile>test3err</scriptfile>
                    <scriptargs>blah3 blah3 err</scriptargs>
                </errorhandler>
            </command>
            <command>
                <jobref name="test" group="group"/>
                <errorhandler>
                    <jobref name="testerr" group="grouperr">
                        <arg line="line err"/>
                    </jobref>
                </errorhandler>
            </command>

        </sequence>

    </job>
</joblist>
'''
        def upload = new ScheduledExecution(
                jobName: 'testUploadErrorHandlers',
                groupPath: "testgroup",
                project: 'project1',
                description: 'desc',
                workflow: new Workflow(commands: [
                        new CommandExec(adhocExecution: true, adhocRemoteString: "echo test",errorHandler: new CommandExec(adhocExecution: true, adhocRemoteString: "echo this is an errorhandler")),
                        new CommandExec(argString: "blah blah", adhocLocalString: "test2",
                                        errorHandler: new CommandExec(argString: "blah blah err",
                                                                      adhocLocalString: "test2err")),
                        new CommandExec(argString: "blah3 blah3", adhocFilepath: "test3",
                                        errorHandler: new CommandExec(argString: "blah3 blah3 err",
                                                                      adhocFilepath: "test3err")),
                        new JobExec(jobGroup: "group",jobName: "test",
                                    errorHandler: new JobExec(jobName: "testerr",jobGroup: "grouperr",argString: "line err")),

                ])
        )
//        sec.request.addFile(new MockMultipartFile('xmlBatch', 'test.xml', 'text/xml', xml as byte[]))
        //set update

        def result = sec.loadJobs([upload], 'update', 'test', 'test,userrole', [:], null)
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
        (0..2).each {ndx ->
            assertTrue(test.workflow.commands[ndx] instanceof CommandExec)
            assertTrue(test.workflow.commands[ndx].errorHandler instanceof CommandExec)
            assertNotNull(test.workflow.commands[ndx].errorHandler.id)
        }
        [3].each {ndx ->
            assertTrue(test.workflow.commands[ndx] instanceof JobExec)
            assertTrue(test.workflow.commands[ndx].errorHandler instanceof JobExec)
            assertNotNull(test.workflow.commands[ndx].errorHandler.id)
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

    boolean unscheduleJob(String s, String s1) {
        return false
    }

    Date rescheduleJob(String s, String s1, Trigger trigger) {
        return null
    }

    void addJob(JobDetail detail, boolean b) {

    }

    boolean deleteJob(String s, String s1) {
        return false
    }

    void triggerJob(String s, String s1) {

    }

    void triggerJobWithVolatileTrigger(String s, String s1) {

    }

    void triggerJob(String s, String s1, JobDataMap map) {

    }

    void triggerJobWithVolatileTrigger(String s, String s1, JobDataMap map) {

    }

    void pauseJob(String s, String s1) {

    }

    void pauseJobGroup(String s) {

    }

    void pauseTrigger(String s, String s1) {

    }

    void pauseTriggerGroup(String s) {

    }

    void resumeJob(String s, String s1) {

    }

    void resumeJobGroup(String s) {

    }

    void resumeTrigger(String s, String s1) {

    }

    void resumeTriggerGroup(String s) {

    }

    void pauseAll() {

    }

    void resumeAll() {

    }

    String[] getJobGroupNames() {
        return new String[0]
    }

    String[] getJobNames(String s) {
        return new String[0]
    }

    Trigger[] getTriggersOfJob(String s, String s1) {
        return new Trigger[0]
    }

    String[] getTriggerGroupNames() {
        return new String[0]
    }

    String[] getTriggerNames(String s) {
        return new String[0]
    }

    Set getPausedTriggerGroups() {
        return null
    }

    JobDetail getJobDetail(String s, String s1) {
        return null
    }

    Trigger getTrigger(String s, String s1) {
        return null
    }

    int getTriggerState(String s, String s1) {
        return 0
    }

    void addCalendar(String s, Calendar calendar, boolean b, boolean b1) {

    }

    boolean deleteCalendar(String s) {
        return false
    }

    Calendar getCalendar(String s) {
        return null
    }

    String[] getCalendarNames() {
        return new String[0]
    }

    boolean interrupt(String s, String s1) {
        return false
    }

    void addGlobalJobListener(JobListener listener) {

    }

    void addJobListener(JobListener listener) {

    }

    boolean removeGlobalJobListener(String s) {
        return false
    }

    boolean removeJobListener(String s) {
        return false
    }

    List getGlobalJobListeners() {
        return null
    }

    Set getJobListenerNames() {
        return null
    }

    JobListener getGlobalJobListener(String s) {
        return null
    }

    JobListener getJobListener(String s) {
        return null
    }

    void addGlobalTriggerListener(TriggerListener listener) {

    }

    void addTriggerListener(TriggerListener listener) {

    }

    boolean removeGlobalTriggerListener(String s) {
        return false
    }

    boolean removeTriggerListener(String s) {
        return false
    }

    List getGlobalTriggerListeners() {
        return null
    }

    Set getTriggerListenerNames() {
        return null
    }

    TriggerListener getGlobalTriggerListener(String s) {
        return null
    }

    TriggerListener getTriggerListener(String s) {
        return null
    }

    void addSchedulerListener(SchedulerListener listener) {

    }

    boolean removeSchedulerListener(SchedulerListener listener) {
        return false
    }

    List getSchedulerListeners() {
        return null
    }
}