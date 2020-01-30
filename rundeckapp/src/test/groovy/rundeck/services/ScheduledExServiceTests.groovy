

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

package rundeck.services


import groovy.mock.interceptor.MockFor
import groovy.mock.interceptor.StubFor
import org.springframework.context.ApplicationContext

import static org.junit.Assert.*

import com.dtolabs.rundeck.core.authorization.UserAndRolesAuthContext
import com.dtolabs.rundeck.core.common.IRundeckProject
import grails.test.mixin.TestMixin
import grails.test.runtime.DirtiesRuntime
import grails.test.mixin.Mock
import grails.test.mixin.TestFor
import grails.test.mixin.web.ControllerUnitTestMixin;

import org.junit.Assert
import org.quartz.*
import org.quartz.impl.matchers.GroupMatcher
import org.quartz.spi.JobFactory
import org.springframework.context.MessageSource

import rundeck.*
import rundeck.controllers.ScheduledExecutionController

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




    /**
     * utility method to mock a class
     */
    private <T> T mockWith(Class<T> clazz, loose=false,Closure clos) {
        def mock = new MockFor(clazz,loose)
        mock.demand.with(clos)
        return mock.proxyInstance()
    }

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

    def setupDoUpdate(sec){
        def projectMock = mockWith(IRundeckProject){
            delegate.'getProperties'{->
                return [:]
            }
        }

        sec.frameworkService = mockWith(FrameworkService){
            authorizeProjectJobAll { framework, resource, actions, project -> return true }
            authorizeProjectJobAll { framework, resource, actions, project -> return true }
            isClusterModeEnabled{->false}
//            getFrameworkFromUserSession { session, request -> return null }
            existsFrameworkProject { project ->
                assertEquals 'testProject', project
                return true
            }
//            getFrameworkFromUserSession { session, request -> return null }
//            getFrameworkFromUserSession { session, request -> return null }
            getFrameworkProject { project ->
                assertEquals 'testProject', project
                return projectMock
            }
            projectNames{authContext -> []}
            getFrameworkNodeName(0..1){ -> "testProject"}
            filterNodeSet(0..1){nodeselector, project -> null}
            filterAuthorizedNodes(0..1){project, actions, unfiltered, authContext -> null}
        }
    }
    def setupDoUpdateJob(sec){
        def projectMock = mockWith(IRundeckProject){
            delegate.'getProperties'{->
                return [:]
            }
        }

        sec.frameworkService = mockWith(FrameworkService){
            isClusterModeEnabled{->false}
//            getFrameworkFromUserSession { session, request -> return null }
            existsFrameworkProject { project, framework ->
                assertEquals 'testProject', project
                return true
            }
//            authorizeProjectJobAll { framework, resource, actions, project -> return true }
//            getFrameworkFromUserSession { session, request -> return null }
//            getFrameworkFromUserSession { session, request -> return null }
            getFrameworkProject { project ->
                assertEquals 'testProject', project
                return projectMock
            }
        }
    }
    public void testDoUpdate() {
        def sec = new ScheduledExecutionService()
        def se = new ScheduledExecution(jobName: 'monkey1', project: 'testProject', description: 'blah')
        se.save()

        assertNotNull se.id

        //try to do update of the ScheduledExecution
        setupDoUpdate(sec)
        def jobSchedulesServiceMock = new MockFor(JobSchedulesService)
        jobSchedulesServiceMock.demand.shouldScheduleExecution(1..1){se.scheduled}
        sec.jobSchedulesService = jobSchedulesServiceMock.proxyInstance()

        def params = [id: se.id.toString(), jobName: 'monkey2', project: 'testProject', description: 'blah',
                _workflow_data: true,
                workflow: [threadcount: 1, keepgoing: true, strategy:'node-first', "commands[0]": [adhocExecution: true, adhocRemoteString: 'test command']],
        ]
        def results = sec._doupdate(params, testUserAndRolesContext())
        def succeeded = results.success
        def scheduledExecution = results.scheduledExecution
        if (scheduledExecution && scheduledExecution.errors.hasErrors()) {
            scheduledExecution.errors.allErrors.each {
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

        assertNull execution.notifications
        assertNull execution.options
    }















    public void ScheduledInvalidDayOfMonth() {
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
        def fwkControl = new MockFor(FrameworkService, true)
        fwkControl.demand.getFrameworkFromUserSession { session, request -> return null }
        fwkControl.demand.existsFrameworkProject { project, framework ->
            assertEquals 'testProject', project
            return true
        }
        fwkControl.demand.authorizeProjectJobAll { framework, resource, actions, project -> return true }
        fwkControl.demand.getFrameworkFromUserSession { session, request -> return null }
        fwkControl.demand.getFrameworkFromUserSession { session, request -> return null }
        fwkControl.demand.getRundeckBase {-> 'test-base' }
        sec.frameworkService = fwkControl.proxyInstance()
        sec.frameworkService.metaClass.isClusterModeEnabled = {
            return false
        }
        sec.executionServiceBean=mockWith(ExecutionService){
            getExecutionsAreActive{-> true}
        }

        def qtzControl = new MockFor(FakeScheduler, true)
        qtzControl.demand.checkExists { key -> false }
        qtzControl.demand.getListenerManager { -> [addJobListener:{a,b->}] }
        qtzControl.demand.scheduleJob { jobDetail, trigger -> new Date() }
        sec.quartzScheduler = qtzControl.proxyInstance()
        def params = [id: se.id.toString(), jobName: 'monkey1', project: 'testProject', description: 'blah',
                workflow: [threadcount: 1, keepgoing: true, strategy:'node-first', "commands[0]": [adhocExecution: true, adhocRemoteString: 'a remote string']],
                _workflow_data: true,
                scheduled: true,
                crontabString: crontabString, useCrontabString: 'true']
        def results = sec._doupdate(params, testUserAndRolesContext('test', 'userrole,test'))
        def succeeded = results.success
        def scheduledExecution = results.scheduledExecution
        if (scheduledExecution && scheduledExecution.errors.hasErrors()) {
            scheduledExecution.errors.allErrors.each {
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

        def ms = new StubFor(MessageSource)
//        ms.demand.getMessage { key, data, locale -> key + ":" + data.toString() + ":" + locale.toString() }
        ms.demand.asBoolean(0..99) { -> true  }
        ms.demand.asBoolean(0..99) { obj -> true  }
        ms.demand.getMessage(0..99) { error, locale -> error.toString()  }
        sec.messageSource = ms.proxyInstance()

        def jobMap = [jobName: 'monkey1', project: 'testProject', description: 'blah',]
        def se = new ScheduledExecution(jobMap)
        def extraParams = [:]
        if (jobConfigure) {
            extraParams = jobConfigure.call(se)
        }
        assertNotNull se.save()

        assertNotNull se.id
        assertFalse se.scheduled
        def projectMock = mockWith(IRundeckProject){
            delegate.'getProperties'{->
                return [:]
            }
        }

        def fs = new StubFor(FrameworkService)
        fs.demand.isClusterModeEnabled(0..99){
            return false
        }
        fs.demand.authorizeProjectJobAll { framework, resource, actions, project -> return true }
//            authorizeProjectJobAll { framework, resource, actions, project -> return true }
//            getFrameworkFromUserSession { session, request -> return null }
        fs.demand.existsFrameworkProject { project ->
            assertEquals 'testProject', project
            return true
        }
//            getFrameworkFromUserSession { session, request -> return null }
//            getFrameworkFromUserSession { session, request -> return null }
        fs.demand.getFrameworkProject { project ->
            assertEquals 'testProject', project
            return projectMock
        }
        fs.demand.projectNames{authContext -> []}


        sec.frameworkService = fs.proxyInstance()
//        sec.frameworkService.metaClass.isClusterModeEnabled = {
//            return false
//        }

        def params = [id: se.id.toString(), scheduled: true, crontabString: crontabString, useCrontabString: 'true', jobName: 'monkey1', project: 'testProject', description: 'blah', adhocExecution: false,]
        def results = sec._doupdate(params + (extraParams ?: [:]), testUserAndRolesContext('test', 'test'))

        def succeeded = results.success
        def scheduledExecution = results.scheduledExecution
        if (scheduledExecution && scheduledExecution.errors.hasErrors()) {
            scheduledExecution.errors.allErrors.each {
            }
        }
        assertFalse succeeded
        assertTrue scheduledExecution.errors.hasErrors()
        assertTrue scheduledExecution.errors.hasFieldErrors('crontabString')

        results
    }




    private UserAndRolesAuthContext testUserAndRolesContext(String user='test',String roleset='test') {
        [getUsername: { user }, getRoles: { roleset.split(',') as Set }] as UserAndRolesAuthContext
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

        def projectMock = mockWith(IRundeckProject){
            delegate.'getProperties'{->
                return [:]
            }
        }
        def jobSchedulesServiceMock = new MockFor(JobSchedulesService)
        jobSchedulesServiceMock.demand.shouldScheduleExecution(1..1){se.scheduled}
        sec.jobSchedulesService = jobSchedulesServiceMock.proxyInstance()
        //try to do update of the ScheduledExecution
        sec.frameworkService = mockWith(FrameworkService,true){
            authorizeProjectJobAll { framework, resource, actions, project -> return true }
            isClusterModeEnabled{->false}
            existsFrameworkProject { project ->
                assertEquals 'testProject', project
                return true
            }
            getFrameworkProject { project ->
                assertEquals 'testProject', project
                return projectMock
            }
            projectNames{authContext -> []}
            getFrameworkFromUserSession(0..1) { session, request -> return null }
            getCommand(0..1) { project, type, command, framework ->
                assertEquals 'testProject', project
                assertEquals 'aType', type
                assertEquals 'aCommand', command
                return null
            }
            getFrameworkNodeName(0..1){ -> "testProject"}
            filterNodeSet(0..1){nodeselector, project -> null}
            filterAuthorizedNodes(0..1){project, actions, unfiltered, authContext -> null}
        }


        def params = [id: se.id.toString(), jobName: 'monkey1', project: 'testProject', description: 'blah2',
                workflow: [threadcount: 1, keepgoing: true, strategy: 'node-first',
                           "commands[0]": [adhocExecution: true, adhocRemoteString: 'test command']],
                _workflow_data: true,
        ] + inputParams
        def results = sec._doupdate(params, testUserAndRolesContext())
        def succeeded = results.success
        def scheduledExecution = results.scheduledExecution
        if (scheduledExecution && scheduledExecution.errors.hasErrors()) {
            scheduledExecution.errors.allErrors.each {
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

    public void testUploadShouldSkipSameNameDupeOptionSkip() {
        def sec = new ScheduledExecutionService()

        //create mock of FrameworkService

        sec.frameworkService = mockWith(FrameworkService){

            existsFrameworkProject{project->true}
            getAuthContextWithProject{ctx,project->null}
            authorizeProjectResourceAll { framework, resource, actions, project -> return true }
            existsFrameworkProject { project, framework -> return true }
            authorizeProjectJobAll { framework, scheduledExecution, actions, project -> return true }
        }
        //mock the scheduledExecutionService
//        def mock2 = new MockFor(ScheduledExecutionService, true)
//        mock2.demand.nextExecutionTimes {joblist -> return [] }
//        sec.scheduledExecutionService = mock2.proxyInstance()

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
        def result = sec.loadJobs([upload], 'skip', null,[:],  testUserAndRolesContext('test', 'userrole,test'))
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






    public void testLoadJobs_JobShouldRequireProject() {
        def sec = new ScheduledExecutionService()

        //create mock of FrameworkService
        def fwkControl = new MockFor(FrameworkService, true)
        fwkControl.demand.getFrameworkFromUserSession { session, request -> return null }
        fwkControl.demand.getFrameworkFromUserSession { session, request -> return null }
        fwkControl.demand.existsFrameworkProject { project, framework -> return true }
        fwkControl.demand.authorizeProjectResourceAll { framework, resource, actions, project -> return true }
        fwkControl.demand.authorizeProjectJobAll { framework, scheduledExecution, actions, project -> return true }
        fwkControl.demand.getFrameworkFromUserSession { session, request -> return null }
        fwkControl.demand.getFrameworkFromUserSession { session, request -> return null }
        sec.frameworkService = fwkControl.proxyInstance()

        //null project

        def upload = new ScheduledExecution(
                jobName: 'test1',
                groupPath: "testgroup",
                project: null,
                description: 'desc',
                workflow: new Workflow(commands: [new CommandExec(adhocExecution: true, adhocRemoteString: "echo test")])
        )

        def result = sec.loadJobs([upload], 'create', null,[:],  testUserAndRolesContext('test', 'userrole,test'))
        assertNotNull result
        assertNotNull result.jobs
        assertNotNull result.errjobs
        assertNotNull result.skipjobs
        assertEquals "shouldn't have skipped jobs: ${result.skipjobs}", 0, result.skipjobs.size()
        assertEquals "shouldn't have error jobs: ${result.errjobs}", 1, result.errjobs.size()
        assertEquals "should have success jobs: ${result.jobs}", 0, result.jobs.size()
        assertEquals "Project was not specified", result.errjobs[0].errmsg
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
    void resetTriggerFromErrorState(TriggerKey triggerKey) throws SchedulerException {

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
