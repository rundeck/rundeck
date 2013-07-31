/*
 * Copyright 2010 DTO Labs, Inc. (http://dtolabs.com)
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
import grails.test.GrailsUnitTestCase

import org.springframework.mock.web.MockMultipartHttpServletRequest
import org.springframework.mock.web.MockMultipartFile
import rundeck.services.NotificationService
import rundeck.services.PluginService

import javax.security.auth.Subject
import com.dtolabs.rundeck.core.authentication.Username
import com.dtolabs.rundeck.core.authentication.Group
import rundeck.CommandExec
import rundeck.Workflow
import rundeck.Option
import rundeck.ScheduledExecution
import rundeck.services.FrameworkService
import rundeck.services.ScheduledExecutionService
import rundeck.controllers.ScheduledExecutionController

/*
 * ScheduledExecValidationTests.java
 * 
 * User: Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
 * Created: May 17, 2010 4:47:49 PM
 * $Id$
 */

public class ScheduledExecValidationTests extends GrailsUnitTestCase{
    def sessionFactory
    protected void setUp() {
        super.setUp();
    }

    public void testUploadOptions() {
        def sec = new ScheduledExecutionController()

        sec.metaClass.request = new MockMultipartHttpServletRequest()

        ScheduledExecution expectedJob = new ScheduledExecution(
                uuid: 'testUUID',
                jobName: 'blue',
                project: 'AProject',
                adhocExecution: true,
                adhocFilepath: '/this/is/a/path',
                groupPath: 'some/where',
                description: 'a job',
                argString: '-a b -c d',
                workflow: new Workflow(keepgoing: true, commands: [new CommandExec([adhocRemoteString: 'test buddy', argString: '-delay 12 -monkey cheese -particle'])]),
                options: [new Option(name: 'testopt',defaultValue: '`ls -t1 /* | head -n1`',values: ['a','b','c'])]
                )

        //create mock of FrameworkService
        def fwkControl = mockFor(FrameworkService, true)
        fwkControl.demand.getFrameworkFromUserSession {session, request -> return null }
        fwkControl.demand.existsFrameworkProject {project, framework -> return true }
        fwkControl.demand.authorizeProjectResourceAll{framework, resource, actions, project-> return true}
        fwkControl.demand.authorizeProjectJobAll {framework, scheduledExecution, actions, project -> return true}
        fwkControl.demand.getFrameworkFromUserSession {session, request -> return null }
        fwkControl.demand.getFrameworkFromUserSession {session, request -> return null }
        sec.frameworkService = fwkControl.createMock()
        //mock the scheduledExecutionService
        def mock2 = mockFor(ScheduledExecutionService, true)
        mock2.demand.nextExecutionTimes {joblist -> return [] }
        mock2.demand.loadJobs {jobset, dupeOption, user, roleList, changeinfo, framework ->
            [
                    jobs:[expectedJob],
                    jobsi:[scheduledExecution:expectedJob,entrynum:0],
                    errjobs:[],
                    skipjobs:[]
            ]
        }
        sec.scheduledExecutionService = mock2.createMock()

        def xml = '''
<joblist>
    <job>
        <name>test1</name>
        <group>testgroup</group>
        <description>desc</description>
        <context>
            <project>project1</project>
             <options>
                <option name='testopt' value='`ls -t1 /* | head -n1`' values="a,b,c" />
              </options>
        </context>
        <sequence>
            <command><exec>echo test</exec></command>
        </sequence>
    </job>
</joblist>
'''
        final subject = new Subject()
        subject.principals << new Username('test')
        subject.principals.addAll(['userrole', 'test'].collect{new Group(it)})
        sec.request.setAttribute("subject", subject)
        sec.request.addFile(new MockMultipartFile('xmlBatch', 'test.xml', 'text/xml', xml as byte[]))
        def result = sec.upload()
        //[jobs: jobs, errjobs: errjobs, skipjobs: skipjobs, nextExecutions:scheduledExecutionService.nextExecutionTimes(jobs.grep{ it.scheduled }), messages: msgs, didupload: true]
        assertNotNull result
        assertTrue result.didupload
        assertNotNull result.jobs
        assertNotNull result.errjobs
        assertNotNull result.skipjobs
        assertEquals "shouldn't have error jobs: ${result.errjobs}", 0, result.errjobs.size()
        assertEquals "shouldn't have skipped jobs: ${result.skipjobs}", 0, result.skipjobs.size()
        assertEquals 1, result.jobs.size()
        assertTrue result.jobs[0] instanceof ScheduledExecution
        def ScheduledExecution job = result.jobs[0]
        assertNotNull job.options
        assertEquals 1,job.options.size()
        Option opt = job.options.iterator().next()
        assertEquals "testopt",opt.name
        assertEquals "`ls -t1 /* | head -n1`",opt.defaultValue
        assertNotNull opt.values
        assertEquals 3, opt.values.size()
        assertTrue opt.values.contains("a")
        assertTrue opt.values.contains("b")
        assertTrue opt.values.contains("c")
    }
    public void testUploadOptions2() {
        def sec = new ScheduledExecutionController()

        sec.metaClass.request = new MockMultipartHttpServletRequest()

        ScheduledExecution expectedJob = new ScheduledExecution(
                uuid: 'testUUID',
                jobName: 'blue',
                project: 'AProject',
                adhocExecution: true,
                adhocFilepath: '/this/is/a/path',
                groupPath: 'some/where',
                description: 'a job',
                argString: '-a b -c d',
                workflow: new Workflow(keepgoing: true, commands: [new CommandExec([adhocRemoteString: 'test buddy', argString: '-delay 12 -monkey cheese -particle'])]),
                options: [new Option(name: 'testopt', defaultValue: '`ls -t1 /* | head -n1`', values: ['a', 'b', 'c'])]
        )

        //create mock of FrameworkService
        def fwkControl = mockFor(FrameworkService, true)
        fwkControl.demand.getFrameworkFromUserSession {session, request -> return null }
        fwkControl.demand.existsFrameworkProject {project, framework -> return true }
        fwkControl.demand.authorizeProjectResourceAll {framework, resource, actions, project -> return true}
        fwkControl.demand.authorizeProjectJobAll {framework, scheduledExecution, actions, project -> return true}
        fwkControl.demand.getFrameworkFromUserSession {session, request -> return null }
        fwkControl.demand.getFrameworkFromUserSession {session, request -> return null }
        sec.frameworkService = fwkControl.createMock()
        //mock the scheduledExecutionService
        def mock2 = mockFor(ScheduledExecutionService, true)
        mock2.demand.nextExecutionTimes {joblist -> return [] }
        mock2.demand.loadJobs {jobset, dupeOption, user, roleList, changeinfo, framework ->
            [
                    jobs: [expectedJob],
                    jobsi: [scheduledExecution: expectedJob, entrynum: 0],
                    errjobs: [],
                    skipjobs: []
            ]
        }
        sec.scheduledExecutionService = mock2.createMock()
        def xml = '''
-
  project: project1
  loglevel: INFO
  sequence:
    keepgoing: false
    strategy: node-first
    commands:
    - exec: echo test
  description: desc
  name: test1
  group: testgroup
  options:
    testopt:
      value: '`ls -t1 /* | head -n1`'
      values: [a,b,c]

'''
        final subject = new Subject()
        subject.principals << new Username('test')
        subject.principals.addAll(['userrole', 'test'].collect {new Group(it)})
        sec.request.setAttribute("subject", subject)

        sec.request.addFile(new MockMultipartFile('xmlBatch', 'test.xml', 'text/yaml', xml as byte[]))
        sec.params.fileformat="yaml"
        def result = sec.upload()
        //[jobs: jobs, errjobs: errjobs, skipjobs: skipjobs, nextExecutions:scheduledExecutionService.nextExecutionTimes(jobs.grep{ it.scheduled }), messages: msgs, didupload: true]
        assertNotNull result
        assertTrue result.didupload
        assertNotNull result.jobs
        assertNotNull result.errjobs
        assertNotNull result.skipjobs
        assertEquals "shouldn't have error jobs: ${result.errjobs}", 0, result.errjobs.size()
        assertEquals "shouldn't have skipped jobs: ${result.skipjobs}", 0, result.skipjobs.size()
        assertEquals 1, result.jobs.size()
        assertTrue result.jobs[0] instanceof ScheduledExecution
        def ScheduledExecution job = result.jobs[0]
        assertNotNull job.options
        assertEquals 1,job.options.size()
        Option opt = job.options.iterator().next()
        assertEquals "testopt",opt.name
        assertEquals "`ls -t1 /* | head -n1`",opt.defaultValue
        assertNotNull opt.values
        assertEquals 3, opt.values.size()
        assertTrue opt.values.contains("a")
        assertTrue opt.values.contains("b")
        assertTrue opt.values.contains("c")
    }
    public void testUploadShouldCreate(){
        def sec = new ScheduledExecutionController()

		sec.metaClass.request = new MockMultipartHttpServletRequest()

        ScheduledExecution expectedJob = new ScheduledExecution(
                uuid: 'testUUID',
                jobName: 'test1',
                project: 'project1',
                adhocExecution: true,
                adhocFilepath: '/this/is/a/path',
                groupPath: 'testgroup',
                description: 'desc',
                argString: '-a b -c d',
                workflow: new Workflow(keepgoing: true, commands: [new CommandExec([adhocRemoteString: 'test buddy', argString: '-delay 12 -monkey cheese -particle'])]),
                options: [new Option(name: 'testopt', defaultValue: '`ls -t1 /* | head -n1`', values: ['a', 'b', 'c'])]
        )

        //create mock of FrameworkService
        def fwkControl = mockFor(FrameworkService, true)
        fwkControl.demand.getFrameworkFromUserSession{session,request-> return null }
        fwkControl.demand.getFrameworkFromUserSession{session,request-> return null }
        fwkControl.demand.existsFrameworkProject{project,framework-> return true }
        fwkControl.demand.authorizeProjectResourceAll {framework, resource, actions, project -> return true}
        fwkControl.demand.authorizeProjectJobAll {framework, scheduledExecution, actions, project -> return true}
        fwkControl.demand.getFrameworkFromUserSession {session, request -> return null }
        fwkControl.demand.getFrameworkFromUserSession {session, request -> return null }
        sec.frameworkService=fwkControl.createMock()
        //mock the scheduledExecutionService
        def mock2 = mockFor(ScheduledExecutionService, true)
        mock2.demand.nextExecutionTimes{joblist-> return [] }
        mock2.demand.loadJobs {jobset, dupeOption, user, roleList, changeinfo, framework ->
            [
                    jobs: [expectedJob],
                    jobsi: [scheduledExecution: expectedJob, entrynum: 0],
                    errjobs: [],
                    skipjobs: []
            ]
        }
        sec.scheduledExecutionService=mock2.createMock()

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

        final subject = new Subject()
        subject.principals << new Username('test')
        subject.principals.addAll(['userrole', 'test'].collect {new Group(it)})
        sec.request.setAttribute("subject", subject)

        sec.request.addFile(new MockMultipartFile('xmlBatch', 'test.xml', 'text/xml', xml as byte[]))
        def result = sec.upload()
        //[jobs: jobs, errjobs: errjobs, skipjobs: skipjobs, nextExecutions:scheduledExecutionService.nextExecutionTimes(jobs.grep{ it.scheduled }), messages: msgs, didupload: true]
        assertNotNull result
        assertTrue result.didupload
        assertNotNull result.jobs
        assertNotNull result.errjobs
        assertNotNull result.skipjobs
        assertEquals "shouldn't have error jobs: ${result.errjobs}",0,result.errjobs.size()
        assertEquals "shouldn't have skipped jobs: ${result.skipjobs}",0,result.skipjobs.size()
        assertEquals 1,result.jobs.size()
        assertTrue result.jobs[0] instanceof ScheduledExecution
        def ScheduledExecution job=result.jobs[0]
        assertEquals "test1",job.jobName
        assertEquals "testgroup",job.groupPath
        assertEquals "desc",job.description
        assertEquals "project1",job.project
    }

    /**
     * test application/x-www-form-urlencoded instead of multipart
     */
    public void testUploadFormContentShouldCreate() {
        def sec = new ScheduledExecutionController()

        ScheduledExecution expectedJob = new ScheduledExecution(
                uuid: 'testUUID',
                jobName: 'test1',
                project: 'project1',
                adhocExecution: true,
                adhocFilepath: '/this/is/a/path',
                groupPath: 'testgroup',
                description: 'desc',
                argString: '-a b -c d',
                workflow: new Workflow(keepgoing: true, commands: [new CommandExec([adhocRemoteString: 'test buddy', argString: '-delay 12 -monkey cheese -particle'])]),
                options: [new Option(name: 'testopt', defaultValue: '`ls -t1 /* | head -n1`', values: ['a', 'b', 'c'])]
        )

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
        def mock2 = mockFor(ScheduledExecutionService, true)
        mock2.demand.nextExecutionTimes {joblist -> return [] }
        mock2.demand.loadJobs {jobset, dupeOption, user, roleList, changeinfo, framework ->
            [
                    jobs: [expectedJob],
                    jobsi: [scheduledExecution: expectedJob, entrynum: 0],
                    errjobs: [],
                    skipjobs: []
            ]
        }
        sec.scheduledExecutionService = mock2.createMock()

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
        sec.params.xmlBatch=xml

        final subject = new Subject()
        subject.principals << new Username('test')
        subject.principals.addAll(['userrole', 'test'].collect {new Group(it)})
        sec.request.setAttribute("subject", subject)

        def result = sec.upload()
        assertNull sec.response.redirectedUrl
        assertNull "Result had an error: ${sec.flash.error}", sec.flash.error
        assertNull "Result had an error: ${sec.flash.message}", sec.flash.message
        assertNotNull result
        assertTrue result.didupload
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
    }
    /**
     * test missing content
     */
    public void testUploadMissingContent() {
        def sec = new ScheduledExecutionController()

        //create mock of FrameworkService
        def fwkControl = mockFor(FrameworkService, true)
        fwkControl.demand.getFrameworkFromUserSession {session, request -> return null }
        fwkControl.demand.getFrameworkFromUserSession {session, request -> return null }
        fwkControl.demand.existsFrameworkProject {project, framework -> return true }
        sec.frameworkService = fwkControl.createMock()
        //mock the scheduledExecutionService
        def mock2 = mockFor(ScheduledExecutionService, true)
        mock2.demand.nextExecutionTimes {joblist -> return [] }
        sec.scheduledExecutionService = mock2.createMock()

        def result = sec.upload()
        assertNull sec.flash.message
        assertNull result

    }
    /**
     * test missing File content
     */
    public void testUploadMissingFile() {
        def sec = new ScheduledExecutionController()

        sec.metaClass.request = new MockMultipartHttpServletRequest()

        //create mock of FrameworkService
        def fwkControl = mockFor(FrameworkService, true)
        fwkControl.demand.getFrameworkFromUserSession {session, request -> return null }
        fwkControl.demand.getFrameworkFromUserSession {session, request -> return null }
        fwkControl.demand.existsFrameworkProject {project, framework -> return true }
        sec.frameworkService = fwkControl.createMock()
        //mock the scheduledExecutionService
        def mock2 = mockFor(ScheduledExecutionService, true)
        mock2.demand.nextExecutionTimes {joblist -> return [] }
        sec.scheduledExecutionService = mock2.createMock()

        def result = sec.upload()
        assertEquals "No file was uploaded.", sec.flash.message
        assertNull result

    }







    public void testCopy(){
        def sec = new ScheduledExecutionController()
        if (true) {//test basic copy action

            def se = new ScheduledExecution(jobName: 'monkey1', project: 'testProject', description: 'blah2', adhocExecution: false, name: 'aResource', type: 'aType', command: 'aCommand')
            se.save()

            assertNotNull se.id

            //try to do update of the ScheduledExecution
            def fwkControl = mockFor(FrameworkService, true)
            fwkControl.demand.getFrameworkFromUserSession {session, request -> return null }
            fwkControl.demand.projects {return []}
            fwkControl.demand.authorizeProjectResourceAll {framework, resource, actions, project -> return true}
            fwkControl.demand.authorizeProjectJobAll {framework, resource, actions, project -> return true}
            fwkControl.demand.getFrameworkFromUserSession {session, request -> return null }
            fwkControl.demand.getFrameworkFromUserSession {session, request -> return null }
            fwkControl.demand.getNodeStepPluginDescriptions { [] }
            fwkControl.demand.getStepPluginDescriptions { [] }
            sec.frameworkService = fwkControl.createMock()
            def seServiceControl = mockFor(ScheduledExecutionService,true)

            seServiceControl.demand.getByIDorUUID {id -> return se }
            seServiceControl.demand.userAuthorizedForJob {user,schedexec, framework -> return true }
            sec.scheduledExecutionService = seServiceControl.createMock()

            def pControl = mockFor(NotificationService)
            pControl.demand.listNotificationPlugins(){->
                []
            }
            sec.notificationService=pControl.createMock()

            def params = [id: se.id.toString()]
            sec.params.putAll(params)
            sec.copy()
            assertNull sec.response.redirectedUrl
            def copied = sec.modelAndView.model.scheduledExecution
            assertNotNull(copied)
            assertEquals(se.jobName,copied.jobName)
        }
    }
}
