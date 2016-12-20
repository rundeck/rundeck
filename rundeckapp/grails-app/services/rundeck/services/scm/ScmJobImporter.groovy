package rundeck.services.scm

import com.dtolabs.rundeck.plugins.scm.ImportResult
import com.dtolabs.rundeck.plugins.scm.ScmOperationContext
import org.springframework.beans.factory.annotation.Autowired
import rundeck.ScheduledExecution
import rundeck.codecs.JobsYAMLCodec
import rundeck.services.JobMetadataService
import rundeck.services.ScheduledExecutionService
import rundeck.services.ScmService

/**
 * Imports job definitions
 */

class ScmJobImporter implements ContextJobImporter {
    @Autowired ScheduledExecutionService scheduledExecutionService
    @Autowired JobMetadataService jobMetadataService

    @Override
    ImportResult importFromStream(
            final ScmOperationContext context,
            final String format,
            final InputStream input,
            final Map importMetadata
    )
    {

        def parseresult
        try {
            parseresult = scheduledExecutionService.parseUploadedFile(input, format)

        } catch (Throwable e) {
            return ImporterResult.fail("Failed to load job definition from input stream: " + e.message)
        }
        if (parseresult.error || parseresult.errorCode) {
            def message = parseresult.error ?:
                    scheduledExecutionService.messageSource.getMessage(
                            parseresult.errorCode,
                            parseresult.args,
                            null
                    )
            return ImporterResult.fail(message)
        }
        if (parseresult.jobset?.size() != 1) {
            return ImporterResult.fail(
                    "Expected a single job definition, but saw: ${parseresult.jobset ? parseresult.jobset.size() : 0}"
            )
        }

        importJob(context, parseresult.jobset[0], importMetadata)
    }

    private ImportResult importJob(
            final ScmOperationContext context,
            ScheduledExecution jobData,
            final Map importMetadata
    )
    {

        jobData.project = context.frameworkProject
        def loadresults = scheduledExecutionService.loadJobs(
                [jobData],
                'update',
                'preserve',
                [user: context.userInfo.userName, method: 'scm-import'],
                context.authContext
        )
        scheduledExecutionService.issueJobChangeEvents(loadresults.jobChangeEvents)

        loadresults.jobs.each { ScheduledExecution job ->
            jobMetadataService.setJobPluginMeta(job, 'scm-import', [version: job.version, pluginMeta: importMetadata])
        }
        def result = new ImporterResult()
        if (loadresults.errjobs) {
            result = ImporterResult.fail(loadresults.errjobs.collect { it.errmsg }.join(", "))
        } else {
            ScheduledExecution job = loadresults.jobs[0]
            result.job = ScmService.scmJobRef(ScmService.jobRevReference(job),[version: job.version, pluginMeta: importMetadata])
            result.created = result.job.version == 0L
            result.modified = result.job.version > 0
            result.successful = true
        }
        result
    }

    @Override
    ImportResult importFromMap(
            final ScmOperationContext context,
            final Map input,
            final Map importMetadata
    )
    {
        List<ScheduledExecution> jobset
        try {
            jobset = JobsYAMLCodec.createJobs([input])
        } catch (Throwable e) {
            return ImporterResult.fail("Failed to construct job definition map: " + e.message)
        }
        importJob(context, jobset[0], importMetadata)
    }
}
