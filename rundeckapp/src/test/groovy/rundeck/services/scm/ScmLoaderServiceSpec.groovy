package rundeck.services.scm

import com.dtolabs.rundeck.core.jobs.JobRevReference
import com.dtolabs.rundeck.core.plugins.CloseableProvider
import com.dtolabs.rundeck.plugins.scm.JobChangeEvent
import com.dtolabs.rundeck.plugins.scm.JobScmReference
import com.dtolabs.rundeck.plugins.scm.ScmExportPlugin
import com.dtolabs.rundeck.plugins.scm.ScmImportPlugin
import com.dtolabs.rundeck.plugins.scm.ScmOperationContext
import grails.events.bus.EventBus
import grails.testing.gorm.DataTest
import grails.testing.services.ServiceUnitTest
import org.rundeck.app.config.ConfigService
import rundeck.ScheduledExecution
import rundeck.data.job.reference.JobRevReferenceImpl
import rundeck.services.FrameworkService

import rundeck.services.ScheduledExecutionService
import rundeck.services.ScmService
import spock.lang.Specification
import spock.lang.Unroll

class  ScmLoaderServiceSpec extends Specification implements ServiceUnitTest<ScmLoaderService>, DataTest {

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
        1 * service.scmService.scmJobRefsForJobs(jobs,_)
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
        1 * service.scmService.scmJobRefsForJobs(jobs,_) >> jobList
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
            1 * service.scmService.scmJobRefsForJobs(jobs, _) >> jobList
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
        1 * service.scmService.scmJobRefsForJobs(jobs,_) >> jobList
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

    def "SCM project loader succeeds after retrying" (){
        def project = "test"
        def integration = 'export'
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
            getEnabled()>>true
        }

        def jobExportReference = Mock(JobScmReference){
            getId()>>job.id
            getProject()>>job.project
        }
        def jobList = [jobExportReference]
        service.scmService = Mock(ScmService){
            _*projectHasConfiguredExportPlugin(project)>>true
            _*scmOperationContext(username,roles,project) >> Mock(ScmOperationContext)
            loadPluginWithConfig(_, _, _, _) >> { throw new Exception("fail plugin config")  }  >> { throw new Exception("fail plugin config")  }  >> _
            1 * getLoadedExportPluginFor(project)  >> plugin
            1 * exportjobRefsForJobs(jobs,_)>>jobList
            1 * loadScmConfig(project, integration) >> scmPluginConfigData
        }
        service.configurationService = Mock(ConfigService){
            _*getLong('scmLoader.init.retry', _) >> ((long)repeatMax)
            _*getLong('scmLoader.init.delay', _) >> 0L
        }
        def loader = service.createProjectLoader(project, 'export')

        when:
            loader.run()

        then:
            1 * plugin.initJobsStatus(_)
            1 * plugin.refreshJobsStatus(_)
            jobs.size()*service.scmService.getRenamedPathForJobId(_,_)
            1 * plugin.clusterFixJobs(_,_,_)
            service.scmProjectInitLoaded.containsKey(project+"-export")
            noExceptionThrown()
        where:
            repeatMax = 2
    }

    def "SCM project loader throws exception if retry does not succeed" (){
        def project = "test"
        def integration = 'export'
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
            getEnabled()>>true
        }

        def jobExportReference = Mock(JobScmReference){
            getId()>>job.id
            getProject()>>job.project
        }
        def jobList = [jobExportReference]
        service.scmService = Mock(ScmService){
            _*projectHasConfiguredExportPlugin(project)>>true
            _*scmOperationContext(username,roles,project) >> Mock(ScmOperationContext)
            loadPluginWithConfig(_, _, _, _) >> { throw new Exception("fail plugin config")  } >> { throw new Exception("fail plugin config")  } >> _
            0 * getLoadedExportPluginFor(project)  >> plugin
            0 * exportjobRefsForJobs(jobs,_)>>jobList
            1 * loadScmConfig(project, integration) >> scmPluginConfigData
        }
        service.configurationService = Mock(ConfigService){
            1*getLong('scmLoader.init.retry', _) >> ((long)repeatMax)
            1*getLong('scmLoader.init.delay', _) >> 0L
        }

        when:
            service.createProjectLoader(project, 'export').run()

        then:
            noExceptionThrown()
            1 * service.scmService.markRetryFailure(project, integration, _)

            0 * plugin.initJobsStatus(_)
            0 * plugin.refreshJobsStatus(_)
            0 * service.scmService.getRenamedPathForJobId(_,_)
            0 * plugin.clusterFixJobs(_,_,_)
            !service.scmProjectInitLoaded.containsKey(project+"-export")
        where: "max 2 attempts"
            repeatMax << [0L, 1L]
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

    @Unroll
    def "SCM project loader does not persist enabled=false to disk when initialization fails after retries exhausted - integration: #integration"() {
        given:
        def project = "test"
        def username = "admin"
        def roles = ["admin"]

        service.frameworkService = Mock(FrameworkService) {
            isClusterModeEnabled() >> false
        }
        service.scheduledExecutionService = Mock(ScheduledExecutionService) {
            listWorkflows(_) >> ["schedlist": []]
        }
        def scmPluginConfigData = Mock(ScmPluginConfigData) {
            getSetting("username") >> username
            getSettingList("roles") >> roles
            getEnabled() >> true
            getProperties() >> [:]
        }
        service.scmService = Mock(ScmService) {
            _* projectHasConfiguredExportPlugin(project) >> true
            _* projectHasConfiguredImportPlugin(project) >> true
            _* scmOperationContext(username, roles, project) >> Mock(ScmOperationContext)
            // Every attempt to load the plugin throws — simulates git server unreachable
            _* loadPluginWithConfig(_, _, _, _) >> { throw new RuntimeException("Connection refused: git server unreachable") }
            1 * loadScmConfig(project, integration) >> scmPluginConfigData
        }
        service.configurationService = Mock(ConfigService) {
            _* getLong('scmLoader.init.retry', _) >> 0L
            _* getLong('scmLoader.init.delay', _) >> 0L
            _* getLong('scmLoader.slowPoll.interval', _) >> ScmLoaderService.DEFAULT_SLOWPOLL_INTERVAL
        }

        when:
        service.createProjectLoader(project, integration).run()

        then:
        noExceptionThrown()
        // Must NOT write enabled=false to disk — connectivity errors are transient.
        // Writing enabled=false would require manual re-activation after recovery (RUN-4525).
        0 * service.scmService.storeConfig(*_)
        // Instead, the loader enters slow-poll mode: it records a cooldown and keeps the plugin enabled.
        1 * service.scmService.markRetryFailure(project, integration, _)

        where:
        integration       | _
        ScmService.EXPORT | _
        ScmService.IMPORT | _
    }

    @Unroll
    def "getScmLoaderSlowPollInterval enforces a 60 second floor - configured: #configured"() {
        given:
        // getLong(String, Long) returns a primitive long — a real ConfigService already applies
        // ScmLoaderService.DEFAULT_SLOWPOLL_INTERVAL internally when the key is unset, so the
        // "unset" case is simulated the same way here rather than stubbing a null return.
        service.configurationService = Mock(ConfigService) {
            getLong('scmLoader.slowPoll.interval', _) >> configured
        }

        expect:
        service.getScmLoaderSlowPollInterval() == expected

        where:
        configured                                  | expected
        ScmLoaderService.DEFAULT_SLOWPOLL_INTERVAL   | ScmLoaderService.DEFAULT_SLOWPOLL_INTERVAL
        10_000L                                      | ScmLoaderService.DEFAULT_SLOWPOLL_INTERVAL
        60_000L                                       | 60_000L
        120_000L                                     | 120_000L
    }

    def "ProjectScmLoader run() no-ops during retry cooldown without attempting to load the plugin"() {
        given:
        def project = "test"
        def integration = 'export'

        service.scmService = Mock(ScmService) {
            loadScmConfig(project, integration) >> Mock(ScmPluginConfigData) { getEnabled() >> true }
            isInRetryCooldown(project, integration) >> true
        }
        service.configurationService = Mock(ConfigService) {
            _* getLong(*_) >> 0L
        }

        when:
        service.createProjectLoader(project, integration).run()

        then:
        noExceptionThrown()
        0 * service.scmService.loadPluginWithConfig(*_)
        0 * service.scmService.markRetryFailure(*_)
    }

    def "ProjectScmLoader run() makes exactly one attempt (no fast-retry burst) once already in slow-poll mode"() {
        // Once a project/integration has already failed and entered slow-poll mode, a re-invocation
        // after the cooldown has elapsed must try exactly once — not repeat the full fast-retry burst
        // (5 attempts) every time the cooldown expires (RUN-4525).
        given:
        def project = "test"
        def integration = 'export'
        def username = "admin"
        def roles = ["admin"]

        service.frameworkService = Mock(FrameworkService) {
            isClusterModeEnabled() >> false
        }
        service.scheduledExecutionService = Mock(ScheduledExecutionService) {
            listWorkflows(_) >> ["schedlist": []]
        }
        def scmPluginConfigData = Mock(ScmPluginConfigData) {
            getSetting("username") >> username
            getSettingList("roles") >> roles
            getEnabled() >> true
            getProperties() >> [:]
        }
        service.scmService = Mock(ScmService) {
            _* projectHasConfiguredExportPlugin(project) >> true
            _* projectHasConfiguredImportPlugin(project) >> true
            _* scmOperationContext(username, roles, project) >> Mock(ScmOperationContext)
            // Every attempt to load the plugin throws — simulates git server unreachable.
            // Exact cardinality (not "_*"): proves attemptOnce() tries exactly once, not a full
            // fast-retry burst — this is verified automatically at the end of the test, so it
            // must NOT also be re-declared (bare, without a response) in the "then:" block below,
            // which would register a competing, response-less interaction for the same method and
            // shadow this one's throw behavior.
            1 * loadPluginWithConfig(_, _, _, _) >> { throw new RuntimeException("Connection refused: git server unreachable") }
            1 * loadScmConfig(project, integration) >> scmPluginConfigData
            isInRetryCooldown(project, integration) >> false
            hasRetryState(project, integration) >> true
        }
        service.configurationService = Mock(ConfigService) {
            _* getLong('scmLoader.init.retry', _) >> 5L
            _* getLong('scmLoader.init.delay', _) >> 0L
            _* getLong('scmLoader.slowPoll.interval', _) >> ScmLoaderService.DEFAULT_SLOWPOLL_INTERVAL
        }

        when:
        service.createProjectLoader(project, integration).run()

        then:
        noExceptionThrown()
        1 * service.scmService.markRetryFailure(project, integration, _)
    }

    def "reloadPlugin clears retry state when a config change is detected"() {
        given:
        def project = "test"
        def integration = ScmService.EXPORT
        def key = service.getProjectIntegration(project, integration)
        // reloadPlugin() compares pluginConfig.getProperties() under @CompileStatic, which resolves
        // to Groovy's reflective bean-properties map (built from the real getPrefix/getConfig/
        // getEnabled/getType getters), not a stubbable "getProperties()" interaction — so the two
        // configs must differ via one of those real getters (getConfig() here) to be detected as changed.
        def cachedConfig = Mock(ScmPluginConfigData) { getConfig() >> [original: '1'] }
        def newConfig = Mock(ScmPluginConfigData) { getConfig() >> [changed: '2'] }
        service.scmPluginMeta.put(key, cachedConfig)
        service.scmService = Mock(ScmService)

        when:
        def result = service.reloadPlugin(project, integration, newConfig)

        then:
        result
        1 * service.scmService.clearRetryState(project, integration)
    }
}
