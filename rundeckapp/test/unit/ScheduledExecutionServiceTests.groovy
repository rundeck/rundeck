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
import rundeck.Notification
import rundeck.Option
import rundeck.JobExec
import rundeck.PluginStep
import rundeck.ScheduledExecution
import rundeck.CommandExec
import rundeck.Workflow
import rundeck.WorkflowStep
import rundeck.controllers.ScheduledExecutionController
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
                [[eventTrigger: ScheduledExecutionController.ONSUCCESS_TRIGGER_NAME,
                        type: ScheduledExecutionController.EMAIL_NOTIFICATION_TYPE,
                        configuration:[recipients: 'c@example.com,d@example.com']]],
                [(ScheduledExecutionController.NOTIFY_ONSUCCESS_EMAIL): 'true', 
                        (ScheduledExecutionController.NOTIFY_SUCCESS_RECIPIENTS): 'c@example.com,d@example.com']
        )
    }
    public void testParseParamNotificationsSuccess_subject() {
        assertParseParamNotifications(
                [[eventTrigger: ScheduledExecutionController.ONSUCCESS_TRIGGER_NAME,
                        type: ScheduledExecutionController.EMAIL_NOTIFICATION_TYPE,
                        configuration:[recipients: 'c@example.com,d@example.com',
                                subject:'blah'
                        ]]],
                [(ScheduledExecutionController.NOTIFY_ONSUCCESS_EMAIL): 'true',
                        (ScheduledExecutionController.NOTIFY_SUCCESS_RECIPIENTS): 'c@example.com,d@example.com',
                        (ScheduledExecutionController.NOTIFY_SUCCESS_SUBJECT): 'blah']
        )
    }

    public void testParseParamNotificationsSuccessUrl() {
        assertParseParamNotifications(
                [[eventTrigger: ScheduledExecutionController.ONSUCCESS_TRIGGER_NAME,
                        type: ScheduledExecutionController.WEBHOOK_NOTIFICATION_TYPE,
                        content: 'http://blah.com']],
                [(ScheduledExecutionController.NOTIFY_ONSUCCESS_URL): 'true',
                        (ScheduledExecutionController.NOTIFY_SUCCESS_URL): 'http://blah.com']
        )
    }
    public void testParseParamNotificationsFailure() {
        assertParseParamNotifications(
                [[eventTrigger: ScheduledExecutionController.ONFAILURE_TRIGGER_NAME,
                        type: ScheduledExecutionController.EMAIL_NOTIFICATION_TYPE,
                        configuration: [recipients:'c@example.com,d@example.com']]],
                [(ScheduledExecutionController.NOTIFY_ONFAILURE_EMAIL): 'true',
                        (ScheduledExecutionController.NOTIFY_FAILURE_RECIPIENTS): 'c@example.com,d@example.com']
        )
    }
    public void testParseParamNotificationsFailure_subject() {
        assertParseParamNotifications(
                [[eventTrigger: ScheduledExecutionController.ONFAILURE_TRIGGER_NAME,
                        type: ScheduledExecutionController.EMAIL_NOTIFICATION_TYPE,
                        configuration: [recipients: 'c@example.com,d@example.com', subject:
                                'elf']]],
                [(ScheduledExecutionController.NOTIFY_ONFAILURE_EMAIL): 'true',
                        (ScheduledExecutionController.NOTIFY_FAILURE_RECIPIENTS): 'c@example.com,d@example.com',
                        (ScheduledExecutionController.NOTIFY_FAILURE_SUBJECT): 'elf']
        )
    }

    public void testParseParamNotificationsFailureUrl() {
        assertParseParamNotifications(
                [[eventTrigger: ScheduledExecutionController.ONFAILURE_TRIGGER_NAME,
                        type: ScheduledExecutionController.WEBHOOK_NOTIFICATION_TYPE,
                        content: 'http://blah.com']],
                [(ScheduledExecutionController.NOTIFY_ONFAILURE_URL): 'true',
                        (ScheduledExecutionController.NOTIFY_FAILURE_URL): 'http://blah.com']
        )
    }
    public void testParseParamNotificationsStart() {
        assertParseParamNotifications(
                [[eventTrigger: ScheduledExecutionController.ONFAILURE_TRIGGER_NAME,
                        type: ScheduledExecutionController.EMAIL_NOTIFICATION_TYPE,
                        configuration: [recipients: 'c@example.com,d@example.com']]],
                [(ScheduledExecutionController.NOTIFY_ONFAILURE_EMAIL): 'true',
                        (ScheduledExecutionController.NOTIFY_FAILURE_RECIPIENTS): 'c@example.com,d@example.com']
        )
    }
    public void testParseParamNotificationsStart_subject() {
        assertParseParamNotifications(
                [[eventTrigger: ScheduledExecutionController.ONFAILURE_TRIGGER_NAME,
                        type: ScheduledExecutionController.EMAIL_NOTIFICATION_TYPE,
                        configuration: [recipients: 'c@example.com,d@example.com',
                                subject: 'rango'
                        ]]],
                [(ScheduledExecutionController.NOTIFY_ONFAILURE_EMAIL): 'true',
                        (ScheduledExecutionController.NOTIFY_FAILURE_RECIPIENTS): 'c@example.com,d@example.com',
                        (ScheduledExecutionController.NOTIFY_FAILURE_SUBJECT): 'rango']
        )
    }

    public void testParseParamNotificationsStartUrl() {
        assertParseParamNotifications(
                [[eventTrigger: ScheduledExecutionController.ONSTART_TRIGGER_NAME,
                        type: ScheduledExecutionController.WEBHOOK_NOTIFICATION_TYPE,
                        content: 'http://blah.com']],
                [(ScheduledExecutionController.NOTIFY_ONSTART_URL): 'true',
                        (ScheduledExecutionController.NOTIFY_START_URL): 'http://blah.com']
        )
    }
    public void testParseParamNotificationsSuccessPluginEnabled() {
        assertParseParamNotifications(
                [[eventTrigger: ScheduledExecutionController.ONSUCCESS_TRIGGER_NAME, type: 'plugin1', configuration: [:]]],
                [
                        notifyPlugin: [
                                (ScheduledExecutionController.ONSUCCESS_TRIGGER_NAME): [
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
                [[eventTrigger: ScheduledExecutionController.ONFAILURE_TRIGGER_NAME, type: 'plugin1', configuration: [:]]],
                [
                        notifyPlugin: [
                                (ScheduledExecutionController.ONFAILURE_TRIGGER_NAME): [
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
                [[eventTrigger: ScheduledExecutionController.ONSTART_TRIGGER_NAME, type: 'plugin1', configuration: [:]]],
                [
                        notifyPlugin: [
                                (ScheduledExecutionController.ONSTART_TRIGGER_NAME): [
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
                                (ScheduledExecutionController.ONSUCCESS_TRIGGER_NAME): [
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
                [[eventTrigger: ScheduledExecutionController.ONSUCCESS_TRIGGER_NAME, type: 'plugin1', configuration: [a:'b',c:'def']]],
                [
                        notifyPlugin: [
                                (ScheduledExecutionController.ONSUCCESS_TRIGGER_NAME): [
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
                        [eventTrigger: ScheduledExecutionController.ONSUCCESS_TRIGGER_NAME, type: 'plugin1', configuration: [a:'b',c:'def']],
                        [eventTrigger: ScheduledExecutionController.ONSUCCESS_TRIGGER_NAME, type: 'plugin2', configuration: [g: 'h', i: 'jkl']]
                ],
                [
                        notifyPlugin: [
                                (ScheduledExecutionController.ONSUCCESS_TRIGGER_NAME): [
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
        String serverUUID) = setupTestClaimScheduledJobs()
        ScheduledExecutionService testService = new ScheduledExecutionService()

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
         String serverUUID) = setupTestClaimScheduledJobs()
        ScheduledExecutionService testService = new ScheduledExecutionService()

        assertEquals(null, job1.serverNodeUUID)
        assertEquals(serverUUID2, job2.serverNodeUUID)
        assertEquals(null, job3.serverNodeUUID)

        def resultMap = testService.claimScheduledJobs(serverUUID)

        assertTrue(resultMap[job1.extid].success)
        assertEquals(null, resultMap[job2.extid])
        assertEquals(null, resultMap[job3.extid])
        ScheduledExecution.withSession {session->
            session.flush()

            job1 = ScheduledExecution.get(job1.id)
            job1.refresh()
            job2 = ScheduledExecution.get(job2.id)
            job2.refresh()
            job3 = ScheduledExecution.get(job3.id)
            job3.refresh()
        }


        assertEquals(serverUUID, job1.serverNodeUUID)
        assertEquals(serverUUID2, job2.serverNodeUUID)
        assertEquals(null, job3.serverNodeUUID)

    }

    void testClaimScheduledJobsFromServerUUID() {
        def (ScheduledExecution job1, String serverUUID2, ScheduledExecution job2, ScheduledExecution job3,
         String serverUUID) = setupTestClaimScheduledJobs()
        ScheduledExecutionService testService = new ScheduledExecutionService()
        assertEquals(null, job1.serverNodeUUID)
        assertEquals(serverUUID2, job2.serverNodeUUID)
        assertEquals(null, job3.serverNodeUUID)

        def resultMap = testService.claimScheduledJobs(serverUUID, serverUUID2)

        ScheduledExecution.withSession { session ->
            session.flush()

            job1 = ScheduledExecution.get(job1.id)
            job1.refresh()
            job2 = ScheduledExecution.get(job2.id)
            job2.refresh()
            job3 = ScheduledExecution.get(job3.id)
            job3.refresh()
        }

        assertEquals(null, job1.serverNodeUUID)
        assertEquals(serverUUID, job2.serverNodeUUID)
        assertEquals(null, job3.serverNodeUUID)

        assertEquals(null, resultMap[job1.extid])
        assertTrue(resultMap[job2.extid].success)
        assertEquals(null, resultMap[job3.extid])
    }

    private List setupTestClaimScheduledJobs() {

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
        [job1, serverUUID2, job2, job3, serverUUID]
    }

    public void testValidateWorkflow() {
        ScheduledExecutionService testService = new ScheduledExecutionService()

        def cmdExecProps = [adhocRemoteString: 'test buddy2']
        def jobrefWorkflowStepProps = [jobName: "name", jobGroup: "group"]
        def jobrefNodeStepProps = [jobName: "name2", jobGroup: "group2", nodeStep: true]
        def pluginNodeStepProps = [type: 'plug1', nodeStep: true,]
        def pluginWorkflowStepProps = [type: 'plug1', nodeStep: false]

        //simple
        assertValidateWorkflow([new CommandExec(cmdExecProps)], testService, true)

        //exec step cannot have a workflow step jobref error handler
        assertValidateWorkflow(
                [new CommandExec(cmdExecProps + [errorHandler: new JobExec(jobrefWorkflowStepProps)])],
                testService, false)

        //exec step can have a job ref (node step) error handler
        assertValidateWorkflow(
                [new CommandExec(cmdExecProps + [errorHandler: new JobExec(jobrefNodeStepProps)])],
                testService, true)

        //exec step cannot have a workflow step plugin error handler
        assertValidateWorkflow(
                [new CommandExec(cmdExecProps + [errorHandler: new PluginStep(pluginWorkflowStepProps)])],
                testService, false)

        //exec step can have a node step plugin error handler
        assertValidateWorkflow(
                [new CommandExec(cmdExecProps + [errorHandler: new PluginStep(pluginNodeStepProps)])],
                testService, true)

        //node step plugin cannot have a workflow step error handler
        assertValidateWorkflow(
                [new PluginStep(pluginNodeStepProps + [errorHandler: new JobExec(jobrefWorkflowStepProps)])],
                testService, false)

        //workflow step plugin can have a workflow step error handler
        assertValidateWorkflow(
                [new PluginStep(pluginWorkflowStepProps + [ errorHandler: new JobExec(jobrefWorkflowStepProps)])],
                testService, true)

        //job ref(workflow step) can have another as error handler
        assertValidateWorkflow(
                [new JobExec(jobrefWorkflowStepProps + [ errorHandler: new JobExec(jobrefWorkflowStepProps)])],
                testService, true)

        //job ref(workflow step) can have a plugin workflow step handler
        assertValidateWorkflow(
                [new JobExec(jobrefWorkflowStepProps + [ errorHandler: new PluginStep(pluginWorkflowStepProps)])],
                testService, true)

        //job ref(workflow step) can have a node step plugin erro handler
        assertValidateWorkflow(
                [new JobExec(jobrefWorkflowStepProps + [ errorHandler: new PluginStep(pluginNodeStepProps)])],
                testService, true)


    }

    private void assertValidateWorkflow(List<WorkflowStep> commands, ScheduledExecutionService testService, boolean valid) {
        def workflow = new Workflow(keepgoing: true, commands: commands, strategy: 'node-first')
        ScheduledExecution scheduledExecution = new ScheduledExecution(
                jobName: 'blue2',
                project: 'AProject2',
                groupPath: 'some/where2',
                description: 'a job2',
                argString: '-a b -c d2',
                workflow: workflow,
                scheduled: false,
        )
        assert valid == testService.validateWorkflow(workflow, scheduledExecution)
        assert !valid == scheduledExecution.hasErrors()
        assert !valid == scheduledExecution.errors.hasFieldErrors('workflow')
        assert !valid == workflow.commands[0].hasErrors()
        assert !valid == workflow.commands[0].errors.hasFieldErrors('errorHandler')
    }
}
