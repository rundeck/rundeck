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
import grails.test.mixin.Mock
import grails.test.mixin.TestFor
import rundeck.Execution
import rundeck.JobExec
import rundeck.Notification
import rundeck.Option
import rundeck.ScheduledExecution
import rundeck.CommandExec
import rundeck.Workflow
import rundeck.WorkflowStep
import rundeck.services.FrameworkService
import rundeck.services.ScheduledExecutionService

/*
* ScheduledExecutionServiceTests.java
*
* User: Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
* Created: Jul 29, 2010 4:38:24 PM
* $Id$
*/

@TestFor(ScheduledExecutionService)
@Mock([ScheduledExecution,Workflow,WorkflowStep,CommandExec,JobExec,Execution, Option,Notification])
public class ScheduledExecutionServiceTests {


    private void assertParseParamNotifications(ArrayList<Map<String, Object>> expected, Map<String, Object> params) {
        def result = ScheduledExecutionService.parseParamNotifications(params)
        assertNotNull(result)
        assertEquals(expected, result)
    }
    public void testParseParamNotificationsSuccess() {
        assertParseParamNotifications(
                [[eventTrigger: 'onsuccess', type: 'email', content: 'c@example.com,d@example.com']],
                [notifyOnsuccess: 'true', notifySuccessRecipients: 'c@example.com,d@example.com']
        )
    }

    public void testParseParamNotificationsSuccessUrl() {
        assertParseParamNotifications(
                [[eventTrigger: 'onsuccess', type: 'url', content: 'http://blah.com']],
                [notifyOnsuccessUrl: 'true', notifySuccessUrl: 'http://blah.com']
        )
    }
    public void testParseParamNotificationsFailure() {
        assertParseParamNotifications(
                [[eventTrigger: 'onfailure', type: 'email', content: 'c@example.com,d@example.com']],
                [notifyOnfailure: 'true', notifyFailureRecipients: 'c@example.com,d@example.com']
        )
    }

    public void testParseParamNotificationsFailureUrl() {
        assertParseParamNotifications(
                [[eventTrigger: 'onfailure', type: 'url', content: 'http://blah.com']],
                [notifyOnfailureUrl: 'true', notifyFailureUrl: 'http://blah.com']
        )
    }
    public void testParseParamNotificationsStart() {
        assertParseParamNotifications(
                [[eventTrigger: 'onfailure', type: 'email', content: 'c@example.com,d@example.com']],
                [notifyOnfailure: 'true', notifyFailureRecipients: 'c@example.com,d@example.com']
        )
    }

    public void testParseParamNotificationsStartUrl() {
        assertParseParamNotifications(
                [[eventTrigger: 'onstart', type: 'url', content: 'http://blah.com']],
                [notifyOnstartUrl: 'true', notifyStartUrl: 'http://blah.com']
        )
    }
    public void testParseParamNotificationsSuccessPluginEnabled() {
        assertParseParamNotifications(
                [[eventTrigger: 'onsuccess', type: 'plugin1', configuration: [:]]],
                [
                        notifyPlugin: [
                                'success': [
                                        type: 'plugin1',
                                        enabled: [
                                                'plugin1': 'true'
                                        ],
                                        'plugin1': [
                                                config: [:]
                                        ]
                                ]
                        ],
                ]
        )
    }
    public void testParseParamNotificationsFailurePluginEnabled() {
        assertParseParamNotifications(
                [[eventTrigger: 'onfailure', type: 'plugin1', configuration: [:]]],
                [
                        notifyPlugin: [
                                'failure': [
                                        type: 'plugin1',
                                        enabled: [
                                                'plugin1': 'true'
                                        ],
                                        'plugin1': [
                                                config: [:]
                                        ]
                                ]
                        ],
                ]
        )
    }
    public void testParseParamNotificationsStartPluginEnabled() {
        assertParseParamNotifications(
                [[eventTrigger: 'onstart', type: 'plugin1', configuration: [:]]],
                [
                        notifyPlugin: [
                                'start': [
                                        type: 'plugin1',
                                        enabled: [
                                                'plugin1': 'true'
                                        ],
                                        'plugin1': [
                                                config: [:]
                                        ]
                                ]
                        ],
                ]
        )
    }
    public void testParseParamNotificationsSuccessPluginDisabled() {
        assertParseParamNotifications(
                [],
                [
                        notifyPlugin: [
                                'success': [
                                        type: 'plugin1',
                                        enabled: [
                                                'plugin1': 'false'
                                        ],
                                        'plugin1': [
                                                config: [:]
                                        ]
                                ]
                        ],
                ]
        )
    }
    public void testParseParamNotificationsSuccessPluginConfiguration() {
        assertParseParamNotifications(
                [[eventTrigger: 'onsuccess', type: 'plugin1', configuration: [a:'b',c:'def']]],
                [
                        notifyPlugin: [
                                'success': [
                                        type: 'plugin1',
                                        enabled: [
                                                'plugin1': 'true'
                                        ],
                                        'plugin1': [
                                                config: [a:'b',c:'def']
                                        ]
                                ]
                        ],
                ]
        )
    }
    public void testParseParamNotificationsSuccessPluginMultiple() {
        assertParseParamNotifications(
                [
                        [eventTrigger: 'onsuccess', type: 'plugin1', configuration: [a:'b',c:'def']],
                        [eventTrigger: 'onsuccess', type: 'plugin2', configuration: [g: 'h', i: 'jkl']]
                ],
                [
                        notifyPlugin: [
                                'success': [
                                        type: ['plugin1','plugin2'],
                                        enabled: [
                                                'plugin1': 'true',
                                                'plugin2': 'true'
                                        ],
                                        'plugin1': [
                                                config: [a:'b',c:'def']
                                        ],
                                        'plugin2': [
                                                config: [g:'h',i:'jkl']
                                        ]
                                ]
                        ],
                ]
        )
    }
    public void testParseNotificationsFromParamsSuccess() {
        def params = [
                notifyOnsuccess: 'true', notifySuccessRecipients: 'c@example.com,d@example.com',
        ]
        ScheduledExecutionService.parseNotificationsFromParams(params)
        assertNotNull(params.notifications)
        assertEquals([
            [eventTrigger: 'onsuccess', type: 'email', content: 'c@example.com,d@example.com'],
        ],params.notifications)
    }
    public void testParseNotificationsFromParamsFailure() {
        def params = [
                notifyOnfailure: 'true', notifyFailureRecipients: 'monkey@example.com',
        ]
        ScheduledExecutionService.parseNotificationsFromParams(params)
        assertNotNull(params.notifications)
        assertEquals([
                [eventTrigger: 'onfailure', type: 'email', content: 'monkey@example.com'],
        ],params.notifications)
    }
    public void testParseNotificationsFromParamsStart() {
        def params = [
                notifyOnstart: 'true', notifyStartRecipients: 'monkey@example.com',
        ]
        ScheduledExecutionService.parseNotificationsFromParams(params)
        assertNotNull(params.notifications)
        assertEquals([
                [eventTrigger: 'onstart', type: 'email', content: 'monkey@example.com'],
        ],params.notifications)
    }
    public void testGetGroups(){
        def schedlist=[new ScheduledExecution(jobName:'test1',groupPath:'group1'),new ScheduledExecution(jobName:'test2',groupPath:null)]

        ScheduledExecution.metaClass.static.findAllByProject={proj-> return schedlist}

        ScheduledExecutionService test = new ScheduledExecutionService()
        def fwkControl = mockFor(FrameworkService, true)

        fwkControl.demand.authResourceForJob{job->
            [type:'job',name:job.jobName,group:job.groupPath?:'']
        }
        fwkControl.demand.authResourceForJob{job->
            [type:'job',name:job.jobName,group:job.groupPath?:'']
        }
        fwkControl.demand.authorizeProjectResources{fwk,Set resset,actionset,proj->
            assertEquals 2,resset.size()
            def list = resset.sort{a,b->a.name<=>b.name}
            assertEquals([type:'job',name:'test1',group:'group1'],list[0])
            assertEquals([type:'job',name:'test2',group:''],list[1])
            
            assertEquals 1,actionset.size()
            assertEquals 'read',actionset.iterator().next()

            assertEquals 'proj1',proj

            return [[authorized:true,resource:list[0]],[authorized:false,resource:list[1]]]
        }
        test.frameworkService = fwkControl.createMock()
        def result=test.getGroups("proj1",null)
        assertEquals 1,result.size()
        assertEquals 1,result['group1']

    }

    void testBlah() {

        def (ScheduledExecution job1, String serverUUID2, ScheduledExecution job2, ScheduledExecution job3,
        ScheduledExecutionService testService, String serverUUID) = setupTestClaimScheduledJobs()

        assertEquals(null, job1.serverNodeUUID)
        assertEquals(serverUUID2, job2.serverNodeUUID)
        assertEquals(null, job3.serverNodeUUID)

        job1.serverNodeUUID=serverUUID
        assertNotNull(job1.save(flush: true))

        job1 = ScheduledExecution.get(job1.id)

        assertEquals(serverUUID, job1.serverNodeUUID)
    }
    void testClaimScheduledJobsUnassigned() {
        def (ScheduledExecution job1, String serverUUID2, ScheduledExecution job2, ScheduledExecution job3,
        ScheduledExecutionService testService, String serverUUID) = setupTestClaimScheduledJobs()

        assertEquals(null, job1.serverNodeUUID)
        assertEquals(serverUUID2, job2.serverNodeUUID)
        assertEquals(null, job3.serverNodeUUID)

        def resultMap = testService.claimScheduledJobs(serverUUID)

        assertTrue(resultMap[job1.extid])
        assertEquals(null, resultMap[job2.extid])
        assertEquals(null, resultMap[job3.extid])
        job1=ScheduledExecution.get(job1.id)
        job2=ScheduledExecution.get(job2.id)
        job3=ScheduledExecution.get(job3.id)

        assertEquals(serverUUID, job1.serverNodeUUID)
        assertEquals(serverUUID2, job2.serverNodeUUID)
        assertEquals(null, job3.serverNodeUUID)

    }

    void testClaimScheduledJobsFromServerUUID() {
        def (ScheduledExecution job1, String serverUUID2, ScheduledExecution job2, ScheduledExecution job3,
        ScheduledExecutionService testService, String serverUUID) = setupTestClaimScheduledJobs()

        assertEquals(null, job1.serverNodeUUID)
        assertEquals(serverUUID2, job2.serverNodeUUID)
        assertEquals(null, job3.serverNodeUUID)

        def resultMap = testService.claimScheduledJobs(serverUUID, serverUUID2)

        assertEquals(null, job1.serverNodeUUID)
        assertEquals(serverUUID, job2.serverNodeUUID)
        assertEquals(null, job3.serverNodeUUID)

        assertEquals(null, resultMap[job1.extid])
        assertTrue(resultMap[job2.extid])
        assertEquals(null, resultMap[job3.extid])
    }

    private List setupTestClaimScheduledJobs() {
        ScheduledExecutionService testService = new ScheduledExecutionService()
        def serverUUID = UUID.randomUUID().toString()
        def serverUUID2 = UUID.randomUUID().toString()
        ScheduledExecution job1 = new ScheduledExecution(
                jobName: 'blue',
                project: 'AProject',
                groupPath: 'some/where',
                description: 'a job',
                argString: '-a b -c d',
                workflow: new Workflow(keepgoing: true, commands:
                        [new CommandExec([adhocRemoteString: 'test buddy'])]),
                serverNodeUUID: null,
                scheduled: true
        )
        assertTrue(job1.validate())
        assertNotNull(job1.save())
        ScheduledExecution job2 = new ScheduledExecution(
                jobName: 'blue2',
                project: 'AProject2',
                groupPath: 'some/where2',
                description: 'a job2',
                argString: '-a b -c d2',
                workflow: new Workflow(keepgoing: true, commands:
                        [new CommandExec([adhocRemoteString: 'test buddy2'])]),
                serverNodeUUID: serverUUID2,
                scheduled: true
        )
        assertTrue(job2.validate())
        assertNotNull(job2.save())
        ScheduledExecution job3 = new ScheduledExecution(
                jobName: 'blue2',
                project: 'AProject2',
                groupPath: 'some/where2',
                description: 'a job2',
                argString: '-a b -c d2',
                workflow: new Workflow(keepgoing: true, commands:
                        [new CommandExec([adhocRemoteString: 'test buddy2'])]),
                scheduled: false,
        )
        assertTrue(job3.validate())
        assertNotNull(job3.save())
        [job1, serverUUID2, job2, job3, testService, serverUUID]
    }
}
