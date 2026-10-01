package org.rundeck.app.data.job.metadata

import com.dtolabs.rundeck.core.authorization.UserAndRolesAuthContext
import groovy.transform.CompileStatic
import org.rundeck.app.authorization.AppAuthContextProcessor
import org.rundeck.app.components.jobs.ComponentMeta
import org.rundeck.app.components.jobs.JobMetadataComponent
import org.rundeck.app.data.model.v1.job.JobDataSummary
import org.rundeck.core.auth.AuthConstants
import org.springframework.beans.factory.annotation.Autowired
import rundeck.ScheduledExecution
import rundeck.services.data.IScheduledExecutionDataService

/**
 * Provides a non-blocking "executionAclValid" job metadata flag: whether a scheduled job's
 * stored owner (user/userRoles) is still authorized (ACTION_RUN) to execute it.
 *
 * This is a detection-only warning, not an enforcement mechanism: it does not affect
 * whether a scheduled job's Quartz trigger actually creates an execution.
 */
@CompileStatic
class JobExecutionAclMetadataComponent implements JobMetadataComponent {
    static final String NAME = 'executionAclValid'

    @Autowired
    AppAuthContextProcessor rundeckAuthContextProcessor

    @Autowired
    IScheduledExecutionDataService scheduledExecutionDataService

    @Override
    Set<String> getAvailableMetadataNames() {
        return [NAME].toSet()
    }

    /**
     * @return true if the job is not eligible to be triggered by Quartz, or its stored
     * owner is still authorized (ACTION_RUN) to run it; false if it is a scheduled,
     * enabled job whose stored owner is not authorized (the "at risk" case).
     */
    boolean validateExecutionAcl(ScheduledExecution se) {
        if (!se || !se.shouldScheduleExecution()) {
            return true
        }
        UserAndRolesAuthContext authContext = rundeckAuthContextProcessor.getAuthContextForUserAndRolesAndProject(
            se.user,
            se.userRoles,
            se.project
        )
        return rundeckAuthContextProcessor.authorizeProjectJobAll(
            authContext,
            se,
            [AuthConstants.ACTION_RUN],
            se.project
        )
    }

    @Override
    Optional<List<ComponentMeta>> getMetadataForJob(
        final JobDataSummary job,
        final Set<String> names,
        final UserAndRolesAuthContext authContext
    ) {
        return getMetadataForJob(job.uuid, job.project, names, authContext)
    }

    @Override
    Optional<List<ComponentMeta>> getMetadataForJob(
        final String id,
        final String project,
        final Set<String> names,
        final UserAndRolesAuthContext authContext
    ) {
        def vals = getMetadataForJobIds([id], project, names, authContext)
        return Optional.ofNullable(vals.get(id))
    }

    @Override
    Map<String, List<ComponentMeta>> getMetadataForJobs(
        final Collection<JobDataSummary> jobs,
        final Set<String> names,
        final UserAndRolesAuthContext authContext
    ) {
        if (!jobs) {
            return new HashMap<String, List<ComponentMeta>>()
        }
        return getMetadataForJobIds(
            jobs.collect { it.uuid },
            jobs.first().project,
            names,
            authContext
        )
    }

    @Override
    Map<String, List<ComponentMeta>> getMetadataForJobIds(
        final Collection<String> ids,
        final String project,
        final Set<String> names,
        final UserAndRolesAuthContext authContext
    ) {
        Map<String, List<ComponentMeta>> metaItems = new HashMap<>()
        if (!names.contains(NAME) && !names.contains('*')) {
            return metaItems
        }
        for (String id : ids) {
            ScheduledExecution se = scheduledExecutionDataService.findByUuid(id)
            if (!se) {
                continue
            }
            metaItems.put(
                id,
                [
                    ComponentMeta.with(
                        NAME,
                        [valid: validateExecutionAcl(se), user: se.user] as Map<String, Object>
                    )
                ]
            )
        }
        return metaItems
    }
}
