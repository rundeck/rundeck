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

import grails.events.annotation.Subscriber
import grails.gorm.transactions.Transactional
import org.rundeck.app.data.providers.v1.PluginMetaDataProvider
import rundeck.PluginMeta
import rundeck.ScheduledExecution

@Transactional
class JobMetadataService {

    PluginMetaDataProvider pluginMetaDataProvider

    /**
     * Load scm metadata for the job
     * @param job job
     * @return map of metadata set by import plugin
     */
    Map getJobPluginMeta(final ScheduledExecution job, final String type) {
        return getJobPluginMeta(job.project, job.extid, type)
    }

    /**
     * Load scm metadata for the jobs
     * @param project project
     * @return map of metadata set by import plugin
     */
    List<PluginMeta> getJobsPluginMeta(final String project, final String type) {
        def found = pluginMetaDataProvider.findAllByProjectAndKeyLike(project, "%/${type}")
        return found
    }

    /**
     * Load scm metadata for the job
     * @param project project
     * @param id jobid
     * @return map of metadata set by import plugin
     */
    Map getJobPluginMeta(final String project, final String id, final String type) {
        def key = id + '/' + type
        try {
            def found = pluginMetaDataProvider.findByProjectAndKey(project, key)
            if (found) {
                return found.pluginData
            }
        } catch (Throwable e) {
            log.trace("Exception getting plugin meta for job", e)
        }
        return null
    }

    /**
     * Remove scm metadata for the job
     * @param job job
     */
    def removeJobPluginMeta(final ScheduledExecution job, final String type) {
        return removeJobPluginMeta(job.project, job.extid, type)
    }

    /**
     * Remove scm metadata for the job
     * @param project project
     * @param id jobid
     */
    def removeJobPluginMeta(final String project, final String id, final String type) {
        def key = id + '/' + type
        pluginMetaDataProvider.deleteByProjectAndKey(project, key)
    }

    /**
     * Remove scm metadata for the project
     * @param project project
     */
    def removeProjectPluginMeta(final String project, final String type) {
        pluginMetaDataProvider.deleteAllByProjectAndKeyLike(project, "%/${type}")
    }

    /**
     * Remove all scm metadata for the job
     * @param project project
     * @param id jobid
     */
    def removeJobPluginMetaAll(final String project, final String id) {
        pluginMetaDataProvider.deleteAllByProjectAndKeyLike(project, id + '/%')
    }

    /**
     * Remove all plugin metadata for the project
     * @param project project
     */
    @Subscriber('projectWasDeleted')
    def removeAllPluginMetaForProject(final String project) {
        pluginMetaDataProvider.deleteAllByProject(project);
    }

    /**
     * Set scm metadata for the job
     * @param job job
     */
    def setJobPluginMeta(final ScheduledExecution job, final String type, final Map metadata) {
        setJobPluginMeta(job.project, job.extid, type, metadata)
    }

    /**
     * Set scm metadata for the job
     * @param project project
     * @param id jobid
     */
    def setJobPluginMeta(final String project, final String id, final String type, final Map metadata) {
        def key = id + '/' + type
        pluginMetaDataProvider.setJobPluginMeta(project, id, key, metadata)
    }
}
