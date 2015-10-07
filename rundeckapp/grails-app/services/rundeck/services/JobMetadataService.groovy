package rundeck.services

import grails.transaction.Transactional
import rundeck.PluginMeta
import rundeck.ScheduledExecution

@Transactional
class JobMetadataService {

    /**
     * Load scm metadata for the job
     * @param job job
     * @return map of metadata set by import plugin
     */
    Map getJobPluginMeta(final ScheduledExecution job, final String type) {
        return getJobPluginMeta(job.project, job.extid, type)
    }

    /**
     * Load scm metadata for the job
     * @param project project
     * @param id jobid
     * @return map of metadata set by import plugin
     */
    Map getJobPluginMeta(final String project, final String id, final String type) {
        def key = id + '/' + type
        def found = PluginMeta.findByProjectAndKey(project, key)
        if (found) {
            log.debug("found job metadata for ${id}: ${found.pluginData}")
            return found.pluginData
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
        def found = PluginMeta.findByProjectAndKey(project, key)
        if (found) {
            found.delete(flush: true)
        }
    }
    /**
     * Remove all scm metadata for the job
     * @param project project
     * @param id jobid
     */
    def removeJobPluginMetaAll(final String project, final String id) {
        def found = PluginMeta.findAllByProjectAndKeyLike(project, id + '/%')
        if (found) {
            found*.delete(flush: true)
        }
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
        def found = PluginMeta.findByProjectAndKey(project, key)
        if (!found) {
            found = new PluginMeta()
            found.project = project
            found.key = key
        }
        log.debug("setJobPluginMeta(${project},${id},${type}) to ${metadata}")
        found.setPluginData(metadata)
        found.save(flush: true)
    }
}
