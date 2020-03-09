package rundeck.services.scm

import com.dtolabs.rundeck.plugins.scm.ScmOperationContext
import com.dtolabs.rundeck.plugins.scm.ScmUserInfo
import org.rundeck.app.components.RundeckJobDefinitionManager
import org.rundeck.app.components.jobs.ImportedJob
import rundeck.ScheduledExecution
import rundeck.services.JobMetadataService
import rundeck.services.ScheduledExecutionService
import spock.lang.Specification

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

        when:
            def result = sut.importFromStream(ctx, format, input, meta, preserve)
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
            1 * sut.jobMetadataService.setJobPluginMeta(job, 'scm-import', [version: null, pluginMeta: meta])
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
            1 * sut.jobMetadataService.setJobPluginMeta(job, 'scm-import', [version: null, pluginMeta: meta])
            job.project == 'aProject'
    }
}
