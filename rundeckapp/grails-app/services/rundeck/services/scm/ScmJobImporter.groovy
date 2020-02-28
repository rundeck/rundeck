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

package rundeck.services.scm

import com.dtolabs.rundeck.plugins.scm.ImportResult
import com.dtolabs.rundeck.plugins.scm.ScmOperationContext
import org.rundeck.app.components.RundeckJobDefinitionManager
import org.rundeck.app.components.jobs.ImportedJob
import org.springframework.beans.factory.annotation.Autowired
import rundeck.ScheduledExecution
import rundeck.services.JobMetadataService
import rundeck.services.ScheduledExecutionService
import rundeck.services.ScmService

/**
 * Imports job definitions
 */

class ScmJobImporter implements ContextJobImporter {
    @Autowired ScheduledExecutionService scheduledExecutionService
    @Autowired JobMetadataService jobMetadataService
    @Autowired RundeckJobDefinitionManager rundeckJobDefinitionManager

    @Override
    ImportResult importFromStream(
            final ScmOperationContext context,
            final String format,
            final InputStream input,
            final Map importMetadata,
            final boolean preserveUuid
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

        importJob(context, parseresult.jobset[0], importMetadata, preserveUuid)
    }

    private ImportResult importJob(
            final ScmOperationContext context,
            ImportedJob<ScheduledExecution> jobData,
            final Map importMetadata,
            final boolean preserveUuid
    )
    {

        jobData.project = context.frameworkProject
        def loadresults = scheduledExecutionService.loadImportedJobs(
                [jobData],
                'update',
                preserveUuid ? 'preserve' : 'remove',
                [user: context.userInfo.userName, method: 'scm-import'],
                context.authContext
        )
        scheduledExecutionService.issueJobChangeEvents(loadresults.jobChangeEvents)

        if (loadresults.errjobs) {
            return ImporterResult.fail(loadresults.errjobs.collect { it.errmsg }.join(", "))
        }

        ScheduledExecution job = loadresults.jobs[0]

        def data = [version: job.version, pluginMeta: importMetadata]
        if (loadresults.idMap?.get(job.extid)) {
            data.srcId = loadresults.idMap[job.extid]
        }
        jobMetadataService.setJobPluginMeta(job, 'scm-import', data)

        def result = new ImporterResult()
        result.job = ScmService.scmJobRef(ScmService.jobRevReference(job), data)
        result.created = result.job.version == 0L
        result.modified = result.job.version > 0
        result.successful = true

        result
    }

    @Override
    ImportResult importFromMap(
            final ScmOperationContext context,
            final Map input,
            final Map importMetadata,
            final boolean preserveUuid
    )
    {
        List<ImportedJob<ScheduledExecution>> jobset
        try {
            jobset = rundeckJobDefinitionManager.createJobs([input])
        } catch (Throwable e) {
            return ImporterResult.fail("Failed to construct job definition map: " + e.message)
        }
        importJob(context, jobset[0], importMetadata, preserveUuid)
    }

    @Override
    ImportResult deleteJob(
        final ScmOperationContext context,
        final String project,
        final String jobid
    )
    {
        def res = scheduledExecutionService.deleteScheduledExecutionById(jobid, 'scm-import')
        def result = new ImporterResult()
        if(res?.success){
            result.successful = true
        }else{
            result.successful = false
            result.errorMessage = res.error?.message
        }
        result
    }

}
