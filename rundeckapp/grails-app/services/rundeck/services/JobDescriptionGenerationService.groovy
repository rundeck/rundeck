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

package rundeck.services

import com.dtolabs.rundeck.core.common.IRundeckProject
import com.dtolabs.rundeck.plugins.scm.JobChangeEvent
import grails.events.annotation.Subscriber
import com.dtolabs.rundeck.core.jobs.JobReference
import com.dtolabs.rundeck.core.jobs.JobRevReference
import rundeck.ScheduledExecution

class JobDescriptionGenerationService {
    GenAIService genAIService
    GithubJobDescriptionsService githubJobDescriptionsService
    ProjectManagerService projectManagerService
    ScheduledExecutionService scheduledExecutionService
    JobSimilarityDetectionService jobSimilarityDetectionService


    @Subscriber('jobChanged')
    void onJobChange(StoredJobChangeEvent event) {

        // Process selected events types only
        if ((event.eventType in [JobChangeEvent.JobChangeEventType.CREATE, JobChangeEvent.JobChangeEventType.MODIFY, JobChangeEvent.JobChangeEventType.MODIFY_RENAME]) == false) {
            return
        }

        def projectProperties=projectManagerService.getFrameworkProject(event.job.project)
        final boolean jobDescriptionGenEnabled = Boolean.valueOf(projectProperties.getProperty('project.job-description-gen.enable') ?: 'false')
        if (!jobDescriptionGenEnabled) {
            return
        }

        String updateText = generateUpdateText(event)

        saveToStorage(event, updateText)
    }

    private  generateUpdateText(StoredJobChangeEvent event) {

        final JobReference previousJobRef = event.originalJobReference
        final String previousJobDefinition = event.originalJobDefinitionXml

        final JobRevReference updatedJobRef = event.jobReference
        final updatedJobDefinition = scheduledExecutionService.generateJobExportDefinition(event.job, 'xml')

        def projectProperties=projectManagerService.getFrameworkProject(event.job.project)
        String updatedJobDescriptionText = genAIService.getJobDescriptionFromJobDefinition(projectProperties, updatedJobDefinition)
        String jobDiffText = genAIService.getJobDiffDescription(projectProperties, previousJobDefinition, updatedJobDefinition)

        String updateText = """
# Revision ${updatedJobRef.version})\n
## Job Description
${updatedJobDescriptionText}
"""

        if (jobDiffText) {
            updateText = updateText + """
## Changes from previous revision ${previousJobRef.hasProperty('version') ? "(revision ${previousJobRef.version})" : ""}
${jobDiffText}
"""
        }

        def jobSimilaritiesText = jobSimilaritiesText(projectProperties, event.job)
        if (jobSimilaritiesText) {
            updateText = updateText + jobSimilaritiesText
        }

        return updateText
    }

    private String jobSimilaritiesText(IRundeckProject projectProperties, ScheduledExecution changedSe) {
        Set<ScheduledExecution> similarJobs = jobSimilarityDetectionService.findSimilarJobs(projectProperties, changedSe)

        if (similarJobs.isEmpty()) {
            return ""
        }

        def sb = new StringBuilder()
        sb.append("## Potentally similar jobs (${similarJobs.size()})\n")

        similarJobs.each { similarJob ->
            sb.append("### ${similarJob.uuid}: ${similarJob.jobName}\n\n")
            def similarityStr = genAIService.getJobDiffDescription(
                    projectProperties, scheduledExecutionService.generateJobExportDefinition(changedSe, 'xml'),
                    scheduledExecutionService.generateJobExportDefinition(similarJob, 'xml'),
                    "Determine if two jobs are doing something similar. If no, explain in one sentence. If yes, provide at most two sentences that summarize the similarity")
            sb.append("**Similarity analysis**: ${similarityStr}\n\n")
        }

        sb.toString()
    }

    private saveToStorage(StoredJobChangeEvent event, String updateText) {
        def projectProperties=projectManagerService.getFrameworkProject(event.job.project)

        String filePath = "${event.job.uuid}.md"
        String existingFileContent = githubJobDescriptionsService.getFileContent(projectProperties, filePath)

        String newFileContent = updateText + (existingFileContent ?: "")

        githubJobDescriptionsService.createOrUpdateFile(projectProperties, filePath, "Updated on ${new Date().format("yyyy-MM-dd HH:mm:ss")}", newFileContent)
    }

}
