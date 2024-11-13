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

import com.dtolabs.rundeck.core.storage.AuthStorageTree
import com.dtolabs.rundeck.plugins.scm.JobChangeEvent
import grails.events.annotation.Subscriber
import org.rundeck.app.components.RundeckJobDefinitionManager
import rundeck.ScheduledExecution

class JobDescriptionGenerationService {
    RundeckJobDefinitionManager rundeckJobDefinitionManager
    GenAIService genAIService
    GithubJobDescriptionsService githubJobDescriptionsService
    ProjectManagerService projectManagerService
    AuthStorageTree authRundeckStorageTree


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

        // TODO: Use key storage
        final String genAiKey = projectProperties.getProperty('project.job-description-gen.gen-ai.key')
        final String storageKey = projectProperties.getProperty('project.job-description-gen.storage.key')


        String jobDefinition = generateJobExportDefinition(event.job, 'xml')
        String jobDescription = genAIService.getJobDescriptionFromJobDefinition(genAiKey, jobDefinition)
        githubJobDescriptionsService.createOrUpdateFile(storageKey, event.job.uuid, "Updated on ${new Date().format("yyyy-MM-dd HH:mm:ss")}", jobDescription)
    }

    private def generateJobExportDefinition(ScheduledExecution scheduledExecution, String format = 'yaml') {
        assert format in [ 'yaml',  'xml'] : "format must be yaml or xml"

        try (def writer = new StringWriter()) {
            rundeckJobDefinitionManager.exportAs(format, [scheduledExecution], writer)
            return writer.toString()
        }
    }

}
