/*
 Copyright 2013 SimplifyOps Inc, <http://simplifyops.com>

 Licensed under the Apache License, Version 2.0 (the "License");
 you may not use this file except in compliance with the License.
 You may obtain a copy of the License at

     http://www.apache.org/licenses/LICENSE-2.0

 Unless required by applicable law or agreed to in writing, software
 distributed under the License is distributed on an "AS IS" BASIS,
 WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 See the License for the specific language governing permissions and
 limitations under the License.
 */

package rundeck.quartzjobs

import com.dtolabs.rundeck.app.support.ExecutionQuery
import grails.gorm.transactions.Rollback
import grails.testing.mixin.integration.Integration
import groovy.mock.interceptor.MockFor
import org.junit.Assert
import org.junit.Test
import rundeck.CommandExec
import rundeck.Execution
import rundeck.ScheduledExecution
import rundeck.Workflow
import rundeck.services.ExecutionService
import rundeck.services.FileUploadService
import rundeck.services.FrameworkService
import rundeck.services.JobSchedulerService
import rundeck.services.LogFileStorageService

/**
 * $INTERFACE is ...
 * User: greg
 * Date: 12/9/13
 * Time: 1:10 PM
 */

@Integration
@Rollback
class ExecutionsCleanUpTest extends GroovyTestCase{

    @Test
    void testExecuteJobCleanerNoExecutionsToDelete(){
        String projName = 'projectTest'
        int maxDaysToKeep = 10
        int minimumExecutionsToKeep = 0
        int maximumDeletionSize = 500
        Date startDate = new Date(2015 - 1900, 2, 8)
        Date endDate = ExecutionQuery.parseRelativeDate("${maxDaysToKeep}d", startDate)
        ExecutionQuery.metaClass.static.parseRelativeDate = { String recentFilter ->
            endDate
        }
        Date execDate = new Date(2015 - 1900, 02, 03)
        ScheduledExecution se = setupJob(projName)
        Execution execution = setupExecution(se, projName, execDate, execDate)
        ExecutionsCleanUp job = new ExecutionsCleanUp()

        List execIdsToExclude = job.searchExecutions(
                new FrameworkService(),
                new ExecutionService(),
                new JobSchedulerService(),
                projName, maxDaysToKeep, minimumExecutionsToKeep, maximumDeletionSize)

        Assert.assertEquals(0, execIdsToExclude.size())
    }

    @Test
    void testExecuteJobCleanerWithExecutionsToDelete(){
        String projName = 'projectTest'
        int maxDaysToKeep = 4
        int minimumExecutionsToKeep = 0
        int maximumDeletionSize = 500
        def logFileStorageService = new MockFor(LogFileStorageService)
        logFileStorageService.demand.getFileForExecutionFiletype(1..999){Execution execution,
                                                                           String filetype,
                                                                            boolean useStoredPath,
                                                                            boolean partial ->
            null
        }
        Date startDate = new Date(2015 - 1900, 2, 8)
        Date endDate = ExecutionQuery.parseRelativeDate("${maxDaysToKeep}d", startDate)
        ExecutionQuery.metaClass.static.parseRelativeDate = { String recentFilter ->
            endDate
        }
        Date execDate = new Date(2015 - 1900, 02, 03)
        ScheduledExecution se = setupJob(projName)
        Execution execution = setupExecution(se, projName, execDate, execDate)
        ExecutionsCleanUp job = new ExecutionsCleanUp()

        List execIdsToExclude = job.searchExecutions(new FrameworkService(),
                new ExecutionService(), new JobSchedulerService(), projName, maxDaysToKeep, minimumExecutionsToKeep, maximumDeletionSize, )

        Assert.assertEquals(true, execIdsToExclude.size() > 0)

        int sucessTotal = job.deleteByExecutionList(
                execIdsToExclude, new FileUploadService(), logFileStorageService.proxyInstance())

        Assert.assertEquals(sucessTotal, execIdsToExclude.size())
    }

    @Test
    void testExecuteJobCleanerOnClusterDeleteNullServerUUID(){
        String projName = 'projectTest'
        int maxDaysToKeep = 4
        int minimumExecutionsToKeep = 0
        int maximumDeletionSize = 500
        def logFileStorageService = new MockFor(LogFileStorageService)
        logFileStorageService.demand.getFileForExecutionFiletype(1..999){Execution execution,
                                                                         String filetype,
                                                                         boolean useStoredPath,
                                                                         boolean partial ->
            null
        }
        Date startDate = new Date(2015 - 1900, 2, 8)
        Date endDate = ExecutionQuery.parseRelativeDate("${maxDaysToKeep}d", startDate)
        ExecutionQuery.metaClass.static.parseRelativeDate = { String recentFilter ->
            endDate
        }
        Date execDate = new Date(2015 - 1900, 02, 03)
        ScheduledExecution se = setupJob(projName)
        Execution execution = setupExecution(se, projName, execDate, execDate)
        ExecutionsCleanUp job = new ExecutionsCleanUp()

        def mockfs=new MockFor(FrameworkService)
        mockfs.demand.getServerUUID(1..1){
            "aaaa"
        }
        mockfs.demand.isClusterModeEnabled(1..5){
            true
        }

        FrameworkService frameworkService = mockfs.proxyInstance()

        def mockjs=new MockFor(JobSchedulerService)
        mockjs.demand.getDeadMembers(0..1){
            ["null","bbbb"]
        }

        JobSchedulerService jobSchedulerService = mockjs.proxyInstance()


        List execIdsToExclude = job.searchExecutions(frameworkService,
                new ExecutionService(), jobSchedulerService, projName, maxDaysToKeep, minimumExecutionsToKeep, maximumDeletionSize, )

        Assert.assertEquals(true, execIdsToExclude.size() > 0)

    }

    Execution setupExecution(ScheduledExecution se, String projName, Date startDate, Date finishDate) {
        Execution e
        Execution.withNewTransaction {
            e = new Execution(
                    project: projName,
                    user: 'bob',
                    status: 'success',
                    dateStarted: startDate,
                    dateCompleted: finishDate,
                    scheduledExecution: se,
                    workflow: new Workflow(
                            keepgoing: true,
                            commands: [new CommandExec(
                                    [adhocRemoteString: 'test buddy', argString: '-delay 12 -monkey cheese -particle']
                            )]
                    )
            )
            e.save()
        }
        return e
    }


    private ScheduledExecution setupJob(String projName, Closure extra=null) {
        ScheduledExecution.withNewTransaction {
            ScheduledExecution se = new ScheduledExecution(
                    jobName: 'blue',
                    project: projName,
                    groupPath: 'some/where',
                    description: 'a job',
                    argString: '-a b -c d',
                    workflow: new Workflow(
                            keepgoing: true,
                            commands: [new CommandExec(
                                    [adhocRemoteString: 'test buddy', argString: '-delay 12 -monkey cheese -particle']
                            )]
                    ),
                    )
            se.dateCreated = new Date()
            se.lastUpdated = new Date()
            if (extra != null) {
                extra.call(se)
            }
            se.workflow.save()
            se.save()
        }
    }
}
