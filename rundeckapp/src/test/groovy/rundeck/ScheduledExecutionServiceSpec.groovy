package rundeck

import grails.test.hibernate.HibernateSpec
import groovy.mock.interceptor.MockFor

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

//import grails.test.GrailsUnitTestCase

import org.junit.Test
import org.rundeck.app.authorization.AppAuthContextEvaluator
import rundeck.controllers.ScheduledExecutionController
import rundeck.services.FrameworkService
import rundeck.services.ScheduledExecutionService

import static org.junit.Assert.*

/*
* rundeck.ScheduledExecutionServiceTests.java
*
* User: Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
* Created: Jul 29, 2010 4:38:24 PM
* $Id$
*/

public class ScheduledExecutionServiceSpec extends HibernateSpec {

    List<Class> getDomainClasses() { [ScheduledExecution, Workflow,CommandExec]}

    public void testGetGroups(){
        when:
            ScheduledExecution job1 = new ScheduledExecution(
                jobName: 'test1',
                project: "proj1",
                groupPath: 'group1',
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
                jobName: 'test2',
                project: "proj1",
                description: 'a job',
                argString: '-a b -c d',
                workflow: new Workflow(keepgoing: true, commands:
                    [new CommandExec([adhocRemoteString: 'test buddy'])]),
                serverNodeUUID: null,
                scheduled: true
            )
            assertTrue(job2.validate())
            assertNotNull(job2.save())

        ScheduledExecutionService test = new ScheduledExecutionService()
        test.rundeckAuthContextEvaluator=Mock(AppAuthContextEvaluator){
            _ * authResourceForJob(_) >> {
                [type: 'job', name: it[0].jobName, group: it[0].groupPath ?: '']
            }
            _*authorizeProjectResources(*_)>>{fwk,Set resset,actionset,proj->
                assertEquals 2,resset.size()
                def list = resset.sort{a,b->a.name<=>b.name}
                assertEquals([type:'job',name:'test1',group:'group1'],list[0])
                assertEquals([type:'job',name:'test2',group:''],list[1])

                assertEquals 1,actionset.size()
                assertEquals 'read',actionset.iterator().next()

                assertEquals 'proj1',proj

                return [[authorized:true,resource:list[0]],[authorized:false,resource:list[1]]]
            }
        }
        def result=test.getGroups("proj1",null)
        then:
        assertEquals 1,result.size()
        assertEquals 1,result['group1']

    }

    void testBlah() {
        when:
        def (ScheduledExecution job1, String serverUUID2, ScheduledExecution job2, ScheduledExecution job3,
        String serverUUID) = setupTestClaimScheduledJobs()
        ScheduledExecutionService testService = new ScheduledExecutionService()

        then:
        assertEquals(null, job1.serverNodeUUID)
        assertEquals(serverUUID2, job2.serverNodeUUID)
        assertEquals(null, job3.serverNodeUUID)

        when:
        job1.serverNodeUUID=serverUUID
        then:
        assertNotNull(job1.save(flush: true))

        when:
        job1 = ScheduledExecution.get(job1.id)

        then:
        assertEquals(serverUUID, job1.serverNodeUUID)
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
        when:
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

        then:
        // the asserts above validate the test
        1 == 1

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
