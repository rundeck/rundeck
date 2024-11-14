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

import com.dtolabs.rundeck.plugins.scm.JobChangeEvent
import grails.events.annotation.Subscriber
import com.dtolabs.rundeck.core.jobs.JobReference
import com.dtolabs.rundeck.core.jobs.JobRevReference

class JobDescriptionGenerationService {
    GenAIService genAIService
    GithubJobDescriptionsService githubJobDescriptionsService
    ProjectManagerService projectManagerService
    ScheduledExecutionService scheduledExecutionService


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
# Job Description (revision ${updatedJobRef.version})
${updatedJobDescriptionText}
"""

        if (jobDiffText) {
            updateText = updateText + """
# Changes from previous revision ${previousJobRef.hasProperty('version') ? "(revision ${previousJobRef.version})" : ""}
${jobDiffText}
"""
        }

        return updateText
    }

    private saveToStorage(StoredJobChangeEvent event, String updateText) {
        def projectProperties=projectManagerService.getFrameworkProject(event.job.project)
        githubJobDescriptionsService.createOrUpdateFile(projectProperties, "${event.job.uuid}.md", "Updated on ${new Date().format("yyyy-MM-dd HH:mm:ss")}", updateText)
    }

}
