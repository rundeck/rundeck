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
import rundeck.ScheduledExecution
import rundeck.CommandExec
import rundeck.Workflow
import rundeck.services.FrameworkService
import rundeck.services.ScheduledExecutionService

/*
* ScheduledExecutionServiceTests.java
*
* User: Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
* Created: Jul 29, 2010 4:38:24 PM
* $Id$
*/

public class ScheduledExecutionServiceTests extends GrailsUnitTestCase {

    public void testGetGroups(){
        mockDomain(ScheduledExecution)
        def schedlist=[new ScheduledExecution(jobName:'test1',groupPath:'group1'),new ScheduledExecution(jobName:'test2',groupPath:null)]

        registerMetaClass(ScheduledExecution)
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

    void testClaimScheduledJobsUnassigned() {
        def (ScheduledExecution job1, String serverUUID2, ScheduledExecution job2, ScheduledExecution job3,
        ScheduledExecutionService testService, String serverUUID) = setupTestClaimScheduledJobs()

        assertEquals(null, job1.serverNodeUUID)
        assertEquals(serverUUID2, job2.serverNodeUUID)
        assertEquals(null, job3.serverNodeUUID)

        registerMetaClass(ScheduledExecution)
        ScheduledExecution.metaClass.static.withNewSession = { clos -> clos.call([:]) }
        def resultMap = testService.claimScheduledJobs(serverUUID)

        assertEquals(serverUUID, job1.serverNodeUUID)
        assertEquals(serverUUID2, job2.serverNodeUUID)
        assertEquals(null, job3.serverNodeUUID)

        assertTrue(resultMap[job1.extid])
        assertEquals(null, resultMap[job2.extid])
        assertEquals(null, resultMap[job3.extid])
    }

    void testClaimScheduledJobsFromServerUUID() {
        def (ScheduledExecution job1, String serverUUID2, ScheduledExecution job2, ScheduledExecution job3,
        ScheduledExecutionService testService, String serverUUID) = setupTestClaimScheduledJobs()

        assertEquals(job1, ScheduledExecution.lock(job1.id))
        assertEquals(job2, ScheduledExecution.lock(job2.id))
        assertEquals(job3, ScheduledExecution.lock(job3.id))
        assertEquals(null, job1.serverNodeUUID)
        assertEquals(serverUUID2, job2.serverNodeUUID)
        assertEquals(null, job3.serverNodeUUID)

        registerMetaClass(ScheduledExecution)
        ScheduledExecution.metaClass.static.withNewSession = { clos -> clos.call([:]) }

        def resultMap = testService.claimScheduledJobs(serverUUID, serverUUID2)

        assertEquals(null, job1.serverNodeUUID)
        assertEquals(serverUUID, job2.serverNodeUUID)
        assertEquals(null, job3.serverNodeUUID)

        assertEquals(null, resultMap[job1.extid])
        assertTrue(resultMap[job2.extid])
        assertEquals(null, resultMap[job3.extid])
    }

    private List setupTestClaimScheduledJobs() {
        mockDomain(ScheduledExecution)
        mockDomain(Workflow)
        mockDomain(CommandExec)
        mockLogging(ScheduledExecutionService)
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
        job1.save()
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
        job2.save()
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
        job3.save()
        def map = [(job1.id): job1, (job2.id): job2, (job3.id): job3]
        registerMetaClass(ScheduledExecution)
        ScheduledExecution.metaClass.static.lock = { id ->
            println("lock for id ${id}")
            return map[id]
        }

        [job1, serverUUID2, job2, job3, testService, serverUUID]
    }
}
