package rundeck.services.scm

import com.dtolabs.rundeck.plugins.scm.JobScmReference
import com.dtolabs.rundeck.plugins.scm.ScmExportPlugin
import com.dtolabs.rundeck.plugins.scm.ScmImportPlugin
import com.dtolabs.rundeck.plugins.scm.ScmOperationContext
import grails.test.hibernate.HibernateSpec
import grails.testing.services.ServiceUnitTest
import rundeck.ScheduledExecution
import rundeck.services.FrameworkService
import rundeck.services.ScheduledExecutionService
import rundeck.services.ScmService

class ScmLoaderServiceSpec extends HibernateSpec implements ServiceUnitTest<ScmLoaderService> {

    def "loaded export plugin not configured"(){

        given:
        service.frameworkService = Mock(FrameworkService)

        service.scheduledExecutionService = Mock(ScheduledExecutionService)
        service.scmService = Mock(ScmService)

        def project = "test"
        def scmPluginConfigData = Mock(ScmPluginConfigData)
        when:

        service.processScmExportLoader(project, scmPluginConfigData)

        then:
        0 * service.scmService.getLoadedExportPluginFor(project)

    }

    def "loaded import plugin not configured"(){

        given:
        service.frameworkService = Mock(FrameworkService)

        service.scheduledExecutionService = Mock(ScheduledExecutionService)
        service.scmService = Mock(ScmService)

        def project = "test"
        def scmPluginConfigData = Mock(ScmPluginConfigData)
        when:

        service.processScmImportLoader(project, scmPluginConfigData)

        then:
        0 * service.scmService.getLoadedImportPluginFor(project)

    }


    def "loaded export plugin load without cluster"(){

        given:
        def project = "test"

        service.frameworkService = Mock(FrameworkService){
            isClusterModeEnabled()>>false
        }

        def jobs = []

        def listWorkflow = [
                "schedlist": jobs
        ]
        service.scheduledExecutionService = Mock(ScheduledExecutionService){
            listWorkflows(_)>>listWorkflow
        }
        service.scmService = Mock(ScmService){
            projectHasConfiguredExportPlugin(project)>>true
        }
        def plugin  = Mock(ScmExportPlugin)
        def scmPluginConfigData = Mock(ScmPluginConfigData)

        when:

        service.processScmExportLoader(project, scmPluginConfigData)

        then:
        1 * service.scmService.getLoadedExportPluginFor(project)  >> plugin
        1 * service.scmService.exportjobRefsForJobs(jobs)
        1 * plugin.initJobsStatus(_)
        1 * plugin.refreshJobsStatus(_)
        service.scmProjectInitLoaded.containsKey(project+"-export")

    }

    def "loaded import plugin load without"(){

        given:
        def project = "test"

        service.frameworkService = Mock(FrameworkService){
            isClusterModeEnabled()>>false
        }

        def jobs = []

        def listWorkflow = [
                "schedlist": jobs
        ]
        service.scheduledExecutionService = Mock(ScheduledExecutionService){
            listWorkflows(_)>>listWorkflow
        }
        service.scmService = Mock(ScmService){
            projectHasConfiguredImportPlugin(project)>>true
        }
        def plugin  = Mock(ScmImportPlugin)
        def scmPluginConfigData = Mock(ScmPluginConfigData)

        when:

        service.processScmImportLoader(project, scmPluginConfigData)

        then:
        1 * service.scmService.getLoadedImportPluginFor(project)  >> plugin
        1 * service.scmService.scmJobRefsForJobs(jobs)
        1 * plugin.initJobsStatus(_)
        1 * plugin.refreshJobsStatus(_)
        service.scmProjectInitLoaded.containsKey(project+"-import")

    }


    def "loaded export plugin load cluster mode"(){

        given:
        def project = "test"

        service.frameworkService = Mock(FrameworkService){
            isClusterModeEnabled()>>true
        }

        def job = new ScheduledExecution()
        job.id = 123
        job.uuid = "123-123"
        job.jobName = "test"
        job.groupPath = "demo"
        job.project = project

        def jobs = [job]

        def listWorkflow = [
                "schedlist": jobs
        ]
        service.scheduledExecutionService = Mock(ScheduledExecutionService){
            listWorkflows(_)>>listWorkflow
        }
        def username = "admin"
        def roles = ["admin"]

        def plugin  = Mock(ScmExportPlugin)
        def scmPluginConfigData = Mock(ScmPluginConfigData){
            getSetting("username")>>username
            getSettingList("roles")>>roles
        }
        service.scmService = Mock(ScmService){
            projectHasConfiguredExportPlugin(project)>>true
        }
        def jobExportReference = Mock(JobScmReference){
            getId()>>job.id
            getProject()>>job.project
        }
        def jobList = [jobExportReference]
        when:

        service.processScmExportLoader(project, scmPluginConfigData)

        then:
        1 * service.scmService.getLoadedExportPluginFor(project)  >> plugin
        1 * service.scmService.exportjobRefsForJobs(jobs)>>jobList
        1 * plugin.initJobsStatus(_)
        1 * plugin.refreshJobsStatus(_)
        1 * service.scmService.scmOperationContext(username,roles,project)>>Mock(ScmOperationContext)
        jobs.size()*service.scmService.getRenamedPathForJobId(_,_)
        1 * plugin.clusterFixJobs(_,_,_)
        service.scmProjectInitLoaded.containsKey(project+"-export")

    }


    def "loaded import plugin load cluster mode"(){
        given:
        def project = "test"

        service.frameworkService = Mock(FrameworkService){
            isClusterModeEnabled()>>true
        }

        def job = new ScheduledExecution()
        job.id = 123
        job.uuid = "123-123"
        job.jobName = "test"
        job.groupPath = "demo"
        job.project = project

        def jobs = [job]

        def listWorkflow = [
                "schedlist": jobs
        ]
        service.scheduledExecutionService = Mock(ScheduledExecutionService){
            listWorkflows(_)>>listWorkflow
        }
        def username = "admin"
        def roles = ["admin"]

        def plugin  = Mock(ScmImportPlugin)
        def scmPluginConfigData = Mock(ScmPluginConfigData){
            getSetting("username")>>username
            getSettingList("roles")>>roles
        }
        service.scmService = Mock(ScmService){
            projectHasConfiguredImportPlugin(project)>>true
        }
        def jobExportReference = Mock(JobScmReference){
            getId()>>job.id
            getProject()>>job.project
        }
        def jobList = [jobExportReference]
        when:

        service.processScmImportLoader(project, scmPluginConfigData)

        then:
        1 * service.scmService.getLoadedImportPluginFor(project)  >> plugin
        1 * service.scmService.scmJobRefsForJobs(jobs) >> jobList
        1 * plugin.initJobsStatus(_)
        1 * plugin.refreshJobsStatus(_)
        1 * service.scmService.scmOperationContext(username,roles,project)>>Mock(ScmOperationContext)
        jobs.size()*service.scmService.getRenamedPathForJobId(project,_)
        1 * plugin.clusterFixJobs(_,_,_)
        service.scmProjectInitLoaded.containsKey(project+"-import")

    }
}