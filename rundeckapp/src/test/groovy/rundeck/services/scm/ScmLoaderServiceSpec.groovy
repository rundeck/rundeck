package rundeck.services.scm

import com.dtolabs.rundeck.core.jobs.JobRevReference
import com.dtolabs.rundeck.core.plugins.CloseableProvider
import com.dtolabs.rundeck.core.plugins.configuration.ConfigurationException
import com.dtolabs.rundeck.plugins.scm.JobChangeEvent
import com.dtolabs.rundeck.plugins.scm.JobScmReference
import com.dtolabs.rundeck.plugins.scm.ScmExportPlugin
import com.dtolabs.rundeck.plugins.scm.ScmImportPlugin
import com.dtolabs.rundeck.plugins.scm.ScmOperationContext
import grails.events.bus.EventBus
import grails.test.hibernate.HibernateSpec
import grails.testing.services.ServiceUnitTest
import org.rundeck.storage.api.StorageException
import rundeck.ScheduledExecution
import rundeck.services.FrameworkService
import rundeck.services.JobRevReferenceImpl
import rundeck.services.ScheduledExecutionService
import rundeck.services.ScmService
import spock.lang.Unroll

class  ScmLoaderServiceSpec extends HibernateSpec implements ServiceUnitTest<ScmLoaderService> {

    def "loaded export plugin not configured"(){

        given:
        service.frameworkService = Mock(FrameworkService)

        service.scheduledExecutionService = Mock(ScheduledExecutionService)
        service.scmService = Mock(ScmService)

        def project = "test"
        def scmPluginConfigData = Mock(ScmPluginConfigData)
        when:

        service.processScmExportLoader(project, scmPluginConfigData, new ScmLoaderService.ScmLoaderStateImpl())

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

        service.processScmImportLoader(project, scmPluginConfigData, new ScmLoaderService.ScmLoaderStateImpl())

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
        def username = "admin"
        def roles = ["admin"]
        service.scheduledExecutionService = Mock(ScheduledExecutionService){
            listWorkflows(_)>>listWorkflow
        }
        service.scmService = Mock(ScmService){
            projectHasConfiguredExportPlugin(project)>>true
        }
        def plugin  = Mock(ScmExportPlugin)
        def scmPluginConfigData = Mock(ScmPluginConfigData){
            getSetting("username")>>username
            getSettingList("roles")>>roles
        }

        when:

        service.processScmExportLoader(project, scmPluginConfigData, new ScmLoaderService.ScmLoaderStateImpl())
        then:
        1 * service.scmService.scmOperationContext(username,roles,project) >> Mock(ScmOperationContext)
        1 * service.scmService.loadPluginWithConfig(_, _, _, _) >> Mock(CloseableProvider)
        1 * service.scmService.getLoadedExportPluginFor(project)  >> plugin
        1 * service.scmService.exportjobRefsForJobs(jobs,_)
        1 * plugin.initJobsStatus(_)
        1 * plugin.refreshJobsStatus(_)
        service.scmProjectInitLoaded.containsKey(project+"-export")

    }

    def "loaded import plugin load without cluster fix"(){

        given:
        def project = "test"

        service.frameworkService = Mock(FrameworkService){
            isClusterModeEnabled()>>false
        }

        def jobs = []

        def listWorkflow = [
                "schedlist": jobs
        ]
        def username = "admin"
        def roles = ["admin"]
        service.scheduledExecutionService = Mock(ScheduledExecutionService){
            listWorkflows(_)>>listWorkflow
        }
        service.scmService = Mock(ScmService){
            projectHasConfiguredImportPlugin(project)>>true
        }
        def plugin  = Mock(ScmImportPlugin)
        def scmPluginConfigData = Mock(ScmPluginConfigData){
            getSetting("username")>>username
            getSettingList("roles")>>roles
        }

        when:

        service.processScmImportLoader(project, scmPluginConfigData, new ScmLoaderService.ScmLoaderStateImpl())

        then:
        1 * service.scmService.scmOperationContext(username,roles,project) >> Mock(ScmOperationContext)
        1 * service.scmService.loadPluginWithConfig(_, _, _, _) >> Mock(CloseableProvider)
        1 * service.scmService.getLoadedImportPluginFor(project)  >> plugin
        1 * service.scmService.scmImportJobRefsForJobs(jobs,_)
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

        service.processScmExportLoader(project, scmPluginConfigData, new ScmLoaderService.ScmLoaderStateImpl())

        then:
        1 * service.scmService.scmOperationContext(username,roles,project) >> Mock(ScmOperationContext)
        1 * service.scmService.loadPluginWithConfig(_, _, _, _) >> Mock(CloseableProvider)
        1 * service.scmService.getLoadedExportPluginFor(project)  >> plugin
        1 * service.scmService.exportjobRefsForJobs(jobs,_)>>jobList
        1 * plugin.initJobsStatus(_)
        1 * plugin.refreshJobsStatus(_)
        jobs.size()*service.scmService.getRenamedPathForJobId(_,_)
        1 * plugin.clusterFixJobs(_,_,_)
        service.scmProjectInitLoaded.containsKey(project+"-export")

    }

    def "loaded export plugin load externally deleted jobs"(){

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
            getId()>>job.uuid
            getProject()>>job.project
        }
        def jobList = [jobExportReference]
        def oldReference = new JobRevReferenceImpl(
            jobName: 'ajob',
            id: 'asdf'
        )
        def oldstate = new ScmLoaderService.ScmLoaderStateImpl(
            inited: true,
            scannedJobs: ['asdf': oldReference]
        )
        service.targetEventBus=Mock(EventBus)
        when:

        service.processScmExportLoader(project, scmPluginConfigData, oldstate)

        then:
        1 * service.scmService.scmOperationContext(username,roles,project) >> Mock(ScmOperationContext)
        1 * service.scmService.loadPluginWithConfig(_, _, _, _) >> Mock(CloseableProvider)
        1 * service.scmService.getLoadedExportPluginFor(project)  >> plugin
        1 * service.scmService.exportjobRefsForJobs(jobs,_)>>jobList
        1 * service.scmService.deletedExportFilesForProject(project)>>[:]
        1 * plugin.initJobsStatus(_)
        1 * plugin.refreshJobsStatus(_)
        jobs.size()*service.scmService.getRenamedPathForJobId(_,_)
        0 * service.scmService.recordDeletedJob(project,'/old/job/path',_)
        1 * plugin.clusterFixJobs(_,_,_)
        service.scmProjectInitLoaded.containsKey(project+"-export")
        1 * service.eventBus.notify('multiJobChanged',{
            it[0].size()==1
            it[0][0].eventType == JobChangeEvent.JobChangeEventType.DELETE
            it[0][0].jobReference == oldReference
            it[0][0].originalJobReference == oldReference
        })
        oldstate.scannedJobs.keySet().contains '123-123'

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

        service.processScmImportLoader(project, scmPluginConfigData, new ScmLoaderService.ScmLoaderStateImpl())

        then:
        1 * service.scmService.scmOperationContext(username,roles,project) >> Mock(ScmOperationContext)
        1 * service.scmService.loadPluginWithConfig(_, _, _, _) >> Mock(CloseableProvider)
        1 * service.scmService.getLoadedImportPluginFor(project)  >> plugin
        1 * service.scmService.scmImportJobRefsForJobs(jobs,_) >> jobList
        1 * plugin.initJobsStatus(_)
        1 * plugin.refreshJobsStatus(_)
        jobs.size()*service.scmService.getRenamedPathForJobId(project,_)
        1 * plugin.clusterFixJobs(_,_,_)
        service.scmProjectInitLoaded.containsKey(project+"-import")

    }

    def "processScmImportLoader externally deleted jobs"() {
        given:
            def project = "test"

            service.frameworkService = Mock(FrameworkService) {
                isClusterModeEnabled() >> true
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
            service.scheduledExecutionService = Mock(ScheduledExecutionService) {
                listWorkflows(_) >> listWorkflow
            }
            def username = "admin"
            def roles = ["admin"]

            def plugin = Mock(ScmImportPlugin)
            def scmPluginConfigData = Mock(ScmPluginConfigData) {
                getSetting("username") >> username
                getSettingList("roles") >> roles
            }
            service.scmService = Mock(ScmService) {
                projectHasConfiguredImportPlugin(project) >> true
            }
            def jobExportReference = Mock(JobScmReference) {
                getId() >> job.id
                getProject() >> job.project
            }
            def oldReference = new JobRevReferenceImpl(
                jobName: 'ajob',
                id: 'asdf'
            )

            def oldstate = new ScmLoaderService.ScmLoaderStateImpl(
                inited: true,
                scannedJobs: ['asdf': oldReference]
            )
            def jobList = [jobExportReference]
            service.targetEventBus = Mock(EventBus)
        when:

            service.processScmImportLoader(project, scmPluginConfigData, oldstate)

        then:
            1 * service.scmService.scmOperationContext(username,roles,project) >> Mock(ScmOperationContext)
            1 * service.scmService.loadPluginWithConfig(_, _, _, _) >> Mock(CloseableProvider)
            1 * service.scmService.getLoadedImportPluginFor(project) >> plugin
            1 * service.scmService.scmImportJobRefsForJobs(jobs, _) >> jobList
            1 * plugin.initJobsStatus(_)
            1 * plugin.refreshJobsStatus(_)
            jobs.size() * service.scmService.getRenamedPathForJobId(project, _)
            1 * plugin.clusterFixJobs(_, _, _)
            service.scmProjectInitLoaded.containsKey(project + "-import")
            1 * service.eventBus.notify('multiJobChanged',{
                it[0].size()==1
                it[0][0].eventType == JobChangeEvent.JobChangeEventType.DELETE
                it[0][0].jobReference == oldReference
                it[0][0].originalJobReference == oldReference
            })
            oldstate.scannedJobs.keySet().contains '123-123'

    }

    @Unroll
    def "scanner state detects deleted jobs"(){
        given:
            def state = new ScmLoaderService.ScmLoaderStateImpl()

            def job1 = new ScheduledExecution(uuid:'uuid1',jobName:'ajob',project:'aproj',groupPath: 'test1')
            def job2 = new ScheduledExecution(uuid:'uuid2',jobName:'bjob',project:'aproj',groupPath: 'test1')
            def job3 = new ScheduledExecution(uuid:'uuid3',jobName:'cjob',project:'bproj',groupPath: 'test2')
            def job4 = new ScheduledExecution(uuid:'uuid4',jobName:'cjob',project:'bproj',groupPath: 'test2')
            def job5 = new ScheduledExecution(uuid:'uuid5',jobName:'cjob',project:'bproj',groupPath: 'test2')

            state.addJobs([job1, job2, job3, job4, job5])
            List<JobRevReference> found=[]
        when:
            state.detectDeletedJobs([job2, job3], known, found.&addAll)
        then:
            found.size() == expected.size()
            found*.id.containsAll expected
        where:
            known                       | expected
            []                          | ['uuid1', 'uuid4', 'uuid5']
            ['uuid1']                   | ['uuid4', 'uuid5']
            ['uuid1', 'uuid4']          | ['uuid5']
            ['uuid1', 'uuid4', 'uuid5'] | []
    }

    def "SCM export project config changed" (){
        given:
        def project = "test"

        service.frameworkService = Mock(FrameworkService){
            isClusterModeEnabled()>>true
        }
        def jobs = []

        def listWorkflow = [
                "schedlist": jobs
        ]
        service.scheduledExecutionService = Mock(ScheduledExecutionService){
            listWorkflows(_)>>listWorkflow
        }
        def username = "admin"
        def roles = ["admin"]
        def config = ["url":"http://localhost","branch": "dev"]
        def configCached = ["url":"http://localhost","branch": "main"]

        def plugin  = Mock(ScmExportPlugin)
        def scmPluginConfigData = Mock(ScmPluginConfig){
            getSetting("username")>>username
            getSettingList("roles")>>roles
            getProperties() >> config
        }

        def scmPluginConfigDataCached = Mock(ScmPluginConfig){
            getSetting("username")>>username
            getSettingList("roles")>>roles
            getProperties() >> configCached
        }

        service.scmService = Mock(ScmService){
            projectHasConfiguredExportPlugin(project)>>true
        }
        def jobList = []

        service.scmPluginMeta.put(project + "-export", scmPluginConfigDataCached)
        when:

        service.processScmExportLoader(project, (ScmPluginConfigData)scmPluginConfigData, new ScmLoaderService
            .ScmLoaderStateImpl())

        then:
        1 * service.scmService.scmOperationContext(username,roles,project) >> Mock(ScmOperationContext)
        1 * service.scmService.loadPluginWithConfig(_, _, _, _) >> Mock(CloseableProvider)
        1 * service.scmService.getLoadedExportPluginFor(project)  >> plugin
        1 * service.scmService.exportjobRefsForJobs(_,_)>>jobList
        1 * service.scmService.unregisterPlugin("export", project)
        1 * service.scmService.initProject(project, "export")
        service.scmProjectInitLoaded.containsKey(project+"-export")
    }

    def "SCM import plugin config changed"(){
        given:
        def project = "test"

        service.frameworkService = Mock(FrameworkService){
            isClusterModeEnabled()>>true
        }

        def jobs = []

        def listWorkflow = [
                "schedlist": jobs
        ]
        service.scheduledExecutionService = Mock(ScheduledExecutionService){
            listWorkflows(_)>>listWorkflow
        }
        def username = "admin"
        def roles = ["admin"]
        def config = ["url":"http://localhost","branch": "dev"]
        def configCached = ["url":"http://localhost","branch": "main"]

        def plugin  = Mock(ScmImportPlugin)
        def scmPluginConfigData = Mock(ScmPluginConfig){
            getSetting("username")>>username
            getSettingList("roles")>>roles
            getProperties() >> config
        }

        def scmPluginConfigDataCached = Mock(ScmPluginConfig){
            getSetting("username")>>username
            getSettingList("roles")>>roles
            getProperties() >> configCached
        }
        service.scmService = Mock(ScmService){
            projectHasConfiguredImportPlugin(project)>>true
        }
        def jobList = []
        service.scmPluginMeta.put(project + "-import", scmPluginConfigDataCached)

        when:

        service.processScmImportLoader(project, (ScmPluginConfigData)scmPluginConfigData, new ScmLoaderService.ScmLoaderStateImpl())

        then:
        1 * service.scmService.scmOperationContext(username,roles,project) >> Mock(ScmOperationContext)
        1 * service.scmService.loadPluginWithConfig(_, _, _, _) >> Mock(CloseableProvider)
        1 * service.scmService.getLoadedImportPluginFor(project)  >> plugin
        1 * service.scmService.scmImportJobRefsForJobs(jobs,_) >> jobList
        1 * service.scmService.unregisterPlugin("import", project)
        1 * service.scmService.initProject(project, "import")
        service.scmProjectInitLoaded.containsKey(project+"-import")

    }

    def "SCM export project bad plugin config" (){
        given:
        def project = "test"
        def username = "admin"
        def roles = ["admin"]
        def config = ["url":"http://localhost","branch": "dev"]
        def configCached = ["url":"http://localhost","branch": "main"]

        def plugin  = Mock(ScmExportPlugin)
        def scmPluginConfigData = Mock(ScmPluginConfig){
            getSetting("username")>>username
            getSettingList("roles")>>roles
            getProperties() >> config
        }

        service.scmService = Mock(ScmService){
            projectHasConfiguredExportPlugin(project)>>true
        }

        service.scmService.scmOperationContext(username,roles,project) >> Mock(ScmOperationContext)
        service.scmService.loadPluginWithConfig(_, _, _, _) >> { throw new Exception("fail plugin config")  }

        when:

        service.processScmExportLoader(project, (ScmPluginConfigData)scmPluginConfigData, new ScmLoaderService
                .ScmLoaderStateImpl())

        then:
        Exception e = thrown()
        0 * service.scmService.getLoadedExportPluginFor(project)  >> plugin
        0 * service.scmService.exportjobRefsForJobs(_,_)
        0 * service.scmService.unregisterPlugin("export", project)
        0 * service.scmService.initProject(project, "export")
    }

    def "SCM import bad plugin config"(){
        given:
        def project = "test"
        def username = "admin"
        def roles = ["admin"]
        def config = ["url":"http://localhost","branch": "dev"]
        def configCached = ["url":"http://localhost","branch": "main"]

        def plugin  = Mock(ScmImportPlugin)
        def scmPluginConfigData = Mock(ScmPluginConfig){
            getSetting("username")>>username
            getSettingList("roles")>>roles
            getProperties() >> config
        }

        service.scmService = Mock(ScmService){
            projectHasConfiguredImportPlugin(project)>>true
        }
        service.scmService.scmOperationContext(username,roles,project) >> Mock(ScmOperationContext)
        service.scmService.loadPluginWithConfig(_, _, _, _) >> { throw new Exception("fail plugin config")  }

        when:

        service.processScmImportLoader(project, (ScmPluginConfigData)scmPluginConfigData, new ScmLoaderService.ScmLoaderStateImpl())

        then:
        Exception e = thrown()
        0 * service.scmService.getLoadedExportPluginFor(project)  >> plugin
        0 * service.scmService.exportjobRefsForJobs(_,_)
        0 * service.scmService.unregisterPlugin("import", project)
        0 * service.scmService.initProject(project, "import")

    }
}
