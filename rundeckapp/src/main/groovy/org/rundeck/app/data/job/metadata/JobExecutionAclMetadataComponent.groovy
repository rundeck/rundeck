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
        return evaluateExecutionAcl(se, new HashMap<List<String>, UserAndRolesAuthContext>())
    }

    /**
     * Bulk variant for job listings. Jobs in a list commonly share an owner, so the auth
     * context is built once per user/roles/project combination rather than once per job.
     *
     * @return job uuid to whether its saved owner is still authorized
     */
    Map<String, Boolean> validateExecutionAcl(Collection<ScheduledExecution> jobs) {
        Map<List<String>, UserAndRolesAuthContext> authContexts = new HashMap<>()
        Map<String, Boolean> results = new HashMap<>()
        for (ScheduledExecution se : jobs) {
            results.put(se.uuid, evaluateExecutionAcl(se, authContexts))
        }
        return results
    }

    /**
     * Builds a project-scoped ACL policy granting the job's saved user run access to this
     * job, for pre-filling the ACL editor. Scoped to the single job and to the username
     * rather than its roles, so accepting it as-is grants the least that resolves the
     * warning; the admin can widen it in the editor.
     *
     * Project-level policy files carry no `context:` block.
     */
    String buildRunGrantPolicy(ScheduledExecution se) {
        return """description: Allow ${se.user} to run job ${se.jobName}
for:
  job:
    - equals:
        uuid: ${se.uuid}
      allow: [run]
by:
  username: ${se.user}
"""
    }

    private boolean evaluateExecutionAcl(
        ScheduledExecution se,
        Map<List<String>, UserAndRolesAuthContext> authContexts
    ) {
        if (!se || !se.shouldScheduleExecution()) {
            return true
        }
        if (!se.user) {
            // the saved user is nullable: a trigger with no identity to authorize is
            // itself the misconfiguration, and building an auth context for it throws
            return false
        }
        List<String> key = [se.project, se.user] + se.userRoles
        UserAndRolesAuthContext authContext = authContexts.get(key)
        if (authContext == null) {
            authContext = rundeckAuthContextProcessor.getAuthContextForUserAndRolesAndProject(
                se.user,
                se.userRoles,
                se.project
            )
            authContexts.put(key, authContext)
        }
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
        // one cache for the whole batch: the browse page asks for many jobs at once and
        // they commonly share an owner
        Map<List<String>, UserAndRolesAuthContext> authContexts = new HashMap<>()
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
                        [valid: evaluateExecutionAcl(se, authContexts), user: se.user] as Map<String, Object>
                    )
                ]
            )
        }
        return metaItems
    }
}
