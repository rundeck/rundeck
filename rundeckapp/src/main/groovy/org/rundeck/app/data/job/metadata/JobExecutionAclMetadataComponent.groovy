package org.rundeck.app.data.job.metadata

import com.dtolabs.rundeck.core.authorization.UserAndRolesAuthContext
import com.google.common.collect.Lists
import groovy.transform.CompileStatic
import org.rundeck.app.authorization.AppAuthContextProcessor
import org.rundeck.app.components.jobs.ComponentMeta
import org.rundeck.app.components.jobs.JobMetadataComponent
import org.rundeck.app.data.model.v1.job.JobDataSummary
import org.rundeck.core.auth.AuthConstants
import org.springframework.beans.factory.annotation.Autowired
import org.yaml.snakeyaml.DumperOptions
import org.yaml.snakeyaml.Yaml
import rundeck.ScheduledExecution
import rundeck.services.FrameworkService
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
    static final String CONF_PROJECT_DISABLE_EXECUTION = 'project.disable.executions'
    static final String CONF_PROJECT_DISABLE_SCHEDULE = 'project.disable.schedule'

    @Autowired
    AppAuthContextProcessor rundeckAuthContextProcessor

    @Autowired
    IScheduledExecutionDataService scheduledExecutionDataService

    @Autowired
    FrameworkService frameworkService

    /**
     * Per-call caches. Jobs in a listing commonly share both a saved owner and a
     * project, so neither the auth context nor the project config is rebuilt per job.
     */
    private static class EvaluationCache {
        Map<List<String>, UserAndRolesAuthContext> authContexts = new HashMap<>()
        Map<String, Boolean> projectSchedulingEnabled = new HashMap<>()
    }

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
        return evaluateExecutionAcl(se, new EvaluationCache())
    }

    /**
     * Bulk variant for job listings. Jobs in a list commonly share an owner, so the auth
     * context is built once per user/roles/project combination rather than once per job.
     *
     * @return job uuid to whether its saved owner is still authorized
     */
    Map<String, Boolean> validateExecutionAcl(Collection<ScheduledExecution> jobs) {
        EvaluationCache cache = new EvaluationCache()
        Map<String, Boolean> results = new HashMap<>()
        for (ScheduledExecution se : jobs) {
            results.put(se.uuid, evaluateExecutionAcl(se, cache))
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
        // serialized rather than interpolated: a job name may contain ':' or '#', and the
        // username has no format constraint, so either could otherwise produce a policy
        // that fails to parse or that parses into something else
        Map<String, Object> policy = [
            description: "Allow ${se.user} to run job ${se.jobName}".toString(),
            // project-level policy files carry no `context:` block
            for          : [job: [[equals: [uuid: se.uuid], allow: ['run']]]],
            by           : [username: se.user],
        ] as Map<String, Object>

        DumperOptions dumperOptions = new DumperOptions()
        dumperOptions.lineBreak = DumperOptions.LineBreak.UNIX
        dumperOptions.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK)
        return new Yaml(dumperOptions).dump(policy)
    }

    private boolean evaluateExecutionAcl(ScheduledExecution se, EvaluationCache cache) {
        if (!se || !se.shouldScheduleExecution()) {
            return true
        }
        if (!isProjectSchedulingEnabled(se.project, cache)) {
            // shouldScheduleExecution() covers only the job's own flags, but Quartz
            // deletes the trigger outright when the project disables executions or
            // scheduling: nothing will run as this owner, so there is nothing to warn
            // about. Mirrors the project-level gate in ExecutionJob.
            return true
        }
        if (!se.user) {
            // the saved user is nullable: a trigger with no identity to authorize is
            // itself the misconfiguration, and building an auth context for it throws
            return false
        }
        List<String> key = [se.project, se.user] + se.userRoles
        UserAndRolesAuthContext authContext = cache.authContexts.get(key)
        if (authContext == null) {
            authContext = rundeckAuthContextProcessor.getAuthContextForUserAndRolesAndProject(
                se.user,
                se.userRoles,
                se.project
            )
            cache.authContexts.put(key, authContext)
        }
        return rundeckAuthContextProcessor.authorizeProjectJobAll(
            authContext,
            se,
            [AuthConstants.ACTION_RUN],
            se.project
        )
    }

    /**
     * @return true if the project permits scheduled executions to fire at all, i.e. the
     * project is enabled and disables neither executions nor scheduling. Cached per
     * project for the duration of one call, since a listing is usually single-project.
     */
    private boolean isProjectSchedulingEnabled(String project, EvaluationCache cache) {
        Boolean cached = cache.projectSchedulingEnabled.get(project)
        if (cached != null) {
            return cached
        }
        boolean enabled = false
        if (!frameworkService.isFrameworkProjectDisabled(project)) {
            Map<String, String> props = frameworkService.getFrameworkProject(project).getProjectProperties()
            enabled = !'true'.equalsIgnoreCase(props.get(CONF_PROJECT_DISABLE_EXECUTION)) &&
                !'true'.equalsIgnoreCase(props.get(CONF_PROJECT_DISABLE_SCHEDULE))
        }
        cache.projectSchedulingEnabled.put(project, enabled)
        return enabled
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
        EvaluationCache cache = new EvaluationCache()
        // batched rather than one lookup per job: the browse endpoint can ask for every
        // job in a project, which is both an N+1 and, as a single IN, past the parameter
        // limit some databases impose. Same partition size as bulkNextExecutionTime.
        List<ScheduledExecution> jobs = Lists.partition(ids.toList(), 1000).collectMany {
            List<String> batch -> scheduledExecutionDataService.findAllByUuidInList(batch)
        }
        for (ScheduledExecution se : jobs) {
            metaItems.put(
                se.uuid,
                [
                    ComponentMeta.with(
                        NAME,
                        [valid: evaluateExecutionAcl(se, cache), user: se.user] as Map<String, Object>
                    )
                ]
            )
        }
        return metaItems
    }
}
