package rundeck.services.scm

import com.dtolabs.rundeck.plugins.scm.JobRenamed
import com.dtolabs.rundeck.plugins.scm.ScmOperationContext
import com.dtolabs.rundeck.plugins.scm.ScmUserInfo
import org.rundeck.app.components.RundeckJobDefinitionManager
import org.rundeck.app.components.jobs.ImportedJob
import rundeck.ScheduledExecution
import rundeck.services.JobMetadataService
import rundeck.services.ScheduledExecutionService
import spock.lang.Specification

import java.util.concurrent.atomic.AtomicInteger

class ScmJobImporterSpec extends Specification {
    def "import from stream"() {

        given:
            def sut = new ScmJobImporter()
            def ctx = Mock(ScmOperationContext) {
                getFrameworkProject() >> 'aProject'
                getUserInfo() >> Mock(ScmUserInfo) {
                    getUserName() >> 'bob'
                }
            }
            def format = 'xml'
            def input = new ByteArrayInputStream('test'.bytes)
            def meta = [:]
            def preserve = false
            def job = new ScheduledExecution()

            def imported = Mock(ImportedJob) {
                getJob() >> job
            }

            sut.scheduledExecutionService = Mock(ScheduledExecutionService)
            sut.jobMetadataService = Mock(JobMetadataService)
            AtomicInteger counter=new AtomicInteger(0)

        when:
            def result = sut.importFromStream(ctx, format, input, meta, preserve,null)
        then:
            result
            result.successful

            1 * sut.scheduledExecutionService.parseUploadedFile(input, format) >> [jobset: [imported]]
            1 * sut.scheduledExecutionService.loadImportedJobs(
                [imported],
                'update',
                'remove',
                [user: 'bob', method: 'scm-import'],
                _
            ) >> [jobs: [job], jobChangeEvents: []]
            1 * sut.jobMetadataService.setJobPluginMeta(job, 'scm-import', [version: null, pluginMeta: meta])>>{
                assert counter.getAndIncrement()==0
            }
            1 * sut.scheduledExecutionService.issueJobChangeEvents([])>>{
                assert counter.getAndIncrement()==1
            }
            job.project == 'aProject'
    }

    def "import from map"() {

        given:
            def sut = new ScmJobImporter()
            def ctx = Mock(ScmOperationContext) {
                getFrameworkProject() >> 'aProject'
                getUserInfo() >> Mock(ScmUserInfo) {
                    getUserName() >> 'bob'
                }
            }

            def input = [:]
            def meta = [:]
            def preserve = false
            def job = new ScheduledExecution()

            def imported = Mock(ImportedJob) {
                getJob() >> job
            }

            sut.scheduledExecutionService = Mock(ScheduledExecutionService)
            sut.jobMetadataService = Mock(JobMetadataService)
            sut.rundeckJobDefinitionManager = Mock(RundeckJobDefinitionManager)
            AtomicInteger counter=new AtomicInteger(0)

        when:
            def result = sut.importFromMap(ctx, input, meta, preserve)
        then:
            result
            result.successful

            1 * sut.rundeckJobDefinitionManager.createJobs([input]) >> [imported]
            1 * sut.scheduledExecutionService.loadImportedJobs(
                [imported],
                'update',
                'remove',
                [user: 'bob', method: 'scm-import'],
                _
            ) >> [jobs: [job], jobChangeEvents: []]
            1 * sut.jobMetadataService.setJobPluginMeta(job, 'scm-import', [version: null, pluginMeta: meta])>>{
                assert counter.getAndIncrement()==0
            }
            1 * sut.scheduledExecutionService.issueJobChangeEvents([])>>{
                assert counter.getAndIncrement()==1
            }
            job.project == 'aProject'
    }

    def "import from stream job rename"() {

        given:
        def sut = new ScmJobImporter()
        def ctx = Mock(ScmOperationContext) {
            getFrameworkProject() >> 'aProject'
            getUserInfo() >> Mock(ScmUserInfo) {
                getUserName() >> 'bob'
            }
        }
        def format = 'xml'
        def input = new ByteArrayInputStream('test'.bytes)
        def meta = [:]
        def preserve = false
        def job = new ScheduledExecution()
        job.jobName= "Test"
        job.groupPath = "Demo"

        def imported = Mock(ImportedJob) {
            getJob() >> job
        }

        sut.scheduledExecutionService = Mock(ScheduledExecutionService)
        sut.jobMetadataService = Mock(JobMetadataService)
        AtomicInteger counter=new AtomicInteger(0)
        def renamedJob = new JobRenamedImpTemp(uuid: "123", sourceId: "456")

        when:
        def result = sut.importFromStream(ctx, format, input, meta, preserve,renamedJob)
        then:
        result
        result.successful

        1 * sut.scheduledExecutionService.parseUploadedFile(input, format) >> [jobset: [imported]]
        1 * sut.scheduledExecutionService.loadImportedJobs(
                _,
                'update',
                'preserve',
                [user: 'bob', method: 'scm-import'],
                _
        ) >> [jobs: [job], jobChangeEvents: []]
        1 * sut.jobMetadataService.setJobPluginMeta(job, 'scm-import', [version: null, pluginMeta: meta, srcId: "456" ])>>{
            assert counter.getAndIncrement()==0
        }
        1 * sut.scheduledExecutionService.issueJobChangeEvents([])>>{
            assert counter.getAndIncrement()==1
        }
        job.project == 'aProject'
        job.uuid == "123"
    }

}


class JobRenamedImpTemp implements JobRenamed{
    String uuid
    String sourceId
}
