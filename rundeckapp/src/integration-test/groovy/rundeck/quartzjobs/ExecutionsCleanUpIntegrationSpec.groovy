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
import org.grails.config.NavigableMap
import org.junit.Assert
import org.junit.Test
import org.rundeck.app.data.providers.GormReferencedExecutionDataProvider
import org.rundeck.app.services.ExecutionFile
import rundeck.CommandExec
import rundeck.ExecReport
import rundeck.Execution
import rundeck.ScheduledExecution
import rundeck.Workflow
import rundeck.services.ConfigurationService
import rundeck.services.ExecutionService
import rundeck.services.FileUploadService
import rundeck.services.FrameworkService
import rundeck.services.JobSchedulerService
import rundeck.services.LogFileStorageService
import spock.lang.Specification

/**
 * $INTERFACE is ...
 * User: greg
 * Date: 12/9/13
 * Time: 1:10 PM
 */

@Integration
@Rollback
class ExecutionsCleanUpIntegrationSpec extends Specification{

    def testExecuteJobCleanerNoExecutionsToDelete(){
        given:
        String projName = 'projectTest1'
        int maxDaysToKeep = 10
        int minimumExecutionsToKeep = 0
        int maximumDeletionSize = 500
        Date startDate = new Date(2015 - 1900, 2, 8)
        Date endDate = ExecutionQuery.parseRelativeDate("${maxDaysToKeep}d", startDate)
        ExecutionQuery.metaClass.static.parseRelativeDate = { String recentFilter ->
            endDate
        }
        Date execDate = new Date(2015 - 1900, 02, 03)
        FrameworkService frameworkService = initNonClusterFrameworkService()

        ScheduledExecution se = setupJob(projName)
        ExecutionsCleanUp job = new ExecutionsCleanUp()
        when:
        Execution execution = setupExecution(se, projName, execDate, execDate, frameworkService.getServerUUID())
        then:
        1 == Execution.countByProject(projName)
        1 == ExecReport.countByCtxProject(projName)


        when:
        List execIdsToExclude = job.searchExecutions(
                frameworkService,
                new ExecutionService(),
                new JobSchedulerService(),
                projName, maxDaysToKeep, minimumExecutionsToKeep, maximumDeletionSize)
        then:
        execIdsToExclude.size() == 0
        1 == Execution.countByProject(projName)
        1 == ExecReport.countByCtxProject(projName)
    }

    def testExecuteJobCleanerWithExecutionsToDelete(){
        given:
        String projName = 'projectTest2'
        int maxDaysToKeep = 4
        int minimumExecutionsToKeep = 0
        int maximumDeletionSize = 500
        def logFileStorageService = Mock(LogFileStorageService)

        Date startDate = new Date(2015 - 1900, 2, 8)
        Date endDate = ExecutionQuery.parseRelativeDate("${maxDaysToKeep}d", startDate)
        ExecutionQuery.metaClass.static.parseRelativeDate = { String recentFilter ->
            endDate
        }
        Date execDate = new Date(2015 - 1900, 02, 03)
        ScheduledExecution se = setupJob(projName)
        ExecutionsCleanUp job = new ExecutionsCleanUp()
        job.referencedExecutionDataProvider = new GormReferencedExecutionDataProvider()

        def executionFile = Mock(ExecutionFile)

        FrameworkService frameworkService = initNonClusterFrameworkService()
        Execution execution = setupExecution(se, projName, execDate, execDate, frameworkService.getServerUUID())
        when:
        List execIds = job.searchExecutions(frameworkService,
                new ExecutionService(), new JobSchedulerService(), projName, maxDaysToKeep, minimumExecutionsToKeep, maximumDeletionSize, )



        then:
        execIds.size() > 0

        when:
        int sucessTotal = job.deleteByExecutionList(execIds, new FileUploadService(), logFileStorageService)

        then:
        execIds.size() == sucessTotal
        0 == Execution.countByProject(projName)
        0 == ExecReport.countByCtxProject(projName)
    }

    private FrameworkService initNonClusterFrameworkService() {
        NavigableMap cfg = new NavigableMap()
        cfg.setProperty("clusterMode.enabled",false)
        ConfigurationService cfgSvc = new ConfigurationService()
        cfgSvc.setAppConfig(cfg)
        return new FrameworkService(configurationService: cfgSvc)
    }


    def testExecuteJobCleanerOnClusterDeleteNullServerUUID(){
        given:
        String projName = 'projectTest3'
        int maxDaysToKeep = 4
        int minimumExecutionsToKeep = 0
        int maximumDeletionSize = 500

        Date startDate = new Date(2015 - 1900, 2, 8)
        Date endDate = ExecutionQuery.parseRelativeDate("${maxDaysToKeep}d", startDate)
        ExecutionQuery.metaClass.static.parseRelativeDate = { String recentFilter ->
            endDate
        }
        Date execDate = new Date(2015 - 1900, 02, 03)
        ScheduledExecution se = setupJob(projName)
        def mockfs=Mock(FrameworkService){
            getServerUUID()>> "aaaa"
            isClusterModeEnabled()>> true
        }
        def mockjs=Mock(JobSchedulerService)

        ExecutionsCleanUp job = new ExecutionsCleanUp()
        when:
        Execution execution = setupExecution(se, projName, execDate, execDate)

        then:
        1 == Execution.countByProject(projName)
        1 == ExecReport.countByCtxProject(projName)



        when:
        List execIdsToExclude = job.searchExecutions(mockfs,
                new ExecutionService(), mockjs, projName, maxDaysToKeep, minimumExecutionsToKeep, maximumDeletionSize, )
        then:
        execIdsToExclude.size() ==1
        execIdsToExclude.contains(execution.id)
        1 == Execution.countByProject(projName)
        1 == ExecReport.countByCtxProject(projName)

    }

    private Execution setupExecution(ScheduledExecution se, String projName, Date startDate, Date finishDate, String serverUUID = null) {
        Execution e
        Execution.withTransaction {
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
                    ),
            )
            if(serverUUID){
                e.serverNodeUUID = serverUUID
            }
            e.save()
            def er=ExecReport.fromExec(e)
            er.save()
        }

        return e
    }


    private ScheduledExecution setupJob(String projName, Closure extra=null) {
        ScheduledExecution.withTransaction {
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
