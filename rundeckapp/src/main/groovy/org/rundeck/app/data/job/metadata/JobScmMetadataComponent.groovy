package org.rundeck.app.data.job.metadata

import com.dtolabs.rundeck.core.authorization.AuthContextProcessor
import com.dtolabs.rundeck.core.authorization.UserAndRolesAuthContext
import com.dtolabs.rundeck.core.plugins.views.Action
import com.dtolabs.rundeck.plugins.scm.JobImportState
import com.dtolabs.rundeck.plugins.scm.JobState
import com.dtolabs.rundeck.plugins.scm.ScmPluginException
import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import org.rundeck.app.components.jobs.ComponentMeta
import org.rundeck.app.components.jobs.JobMetadataComponent
import org.rundeck.app.data.model.v1.job.JobDataSummary
import org.rundeck.app.data.providers.v1.job.JobDataProvider
import org.rundeck.core.auth.AuthConstants
import org.springframework.beans.factory.annotation.Autowired
import rundeck.services.ScmService

@CompileStatic
@Slf4j
class JobScmMetadataComponent implements JobMetadataComponent {
    public static final String SCM_EXPORT = 'scmExport'
    public static final String SCM_IMPORT = 'scmImport'
    @Autowired
    ScmService scmService
    @Autowired
    AuthContextProcessor rundeckAuthContextProcessor
    @Autowired
    JobDataProvider jobDataProvider

    final Set<String> availableMetadataNames = Collections.unmodifiableSet(
        new HashSet<String>(
            [SCM_EXPORT, SCM_IMPORT]
        )
    )

    @Override
    Optional<List<ComponentMeta>> getMetadataForJob(
        final JobDataSummary job,
        final Set<String> names,
        final UserAndRolesAuthContext authContext
    ) {
        return getMetadataForJob(
            job.uuid,
            job.project,
            names,
            authContext
        )
    }

    @Override
    Optional<List<ComponentMeta>> getMetadataForJob(
        final String id,
        final String project,
        final Set<String> names,
        final UserAndRolesAuthContext authContext
    ) {
        def vals = getMetadataForJobIds(
            [id],
            project,
            names,
            authContext
        )
        return Optional.ofNullable(vals.get(id))
    }

    @Override
    Map<String, List<ComponentMeta>> getMetadataForJobs(
        final Collection<JobDataSummary> jobs,
        final Set<String> names,
        final UserAndRolesAuthContext authContext
    ) {
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
        if ((names.contains(SCM_EXPORT) || names.contains('*')) && authorizedForExport(authContext, project)) {
            try {
                if (scmService.projectHasConfiguredExportPlugin(project)) {
                    boolean enabled = scmService.loadScmConfig(project, 'export')?.enabled
                    if (enabled) {
                        def keyAccess = scmService
                            .userHasAccessToScmConfiguredKeyOrPassword(authContext, ScmService.EXPORT, project)
                        if (keyAccess) {
                            def jobsPluginMeta = scmService.getJobsPluginMeta(project, true)
                            def jobStates = scmService
                                .exportStatusForJobIds(project, authContext, ids, false, jobsPluginMeta)
                            jobStates.each {
                                metaItems.computeIfAbsent(it.key, { k -> [] }).add(
                                    ComponentMeta.with(
                                        SCM_EXPORT,
                                        [jobState: convertToMap(it.value)] as Map<String, Object>
                                    )

                                )
                            }
                        }
                    }
                }
            } catch (ScmPluginException e) {
                log.warn("Failed to get SCM Export status: ${e.message}")
            }
        }
        if ((names.contains(SCM_IMPORT) || names.contains('*')) && authorizedForImport(authContext, project)) {
            try {
                if (scmService.projectHasConfiguredImportPlugin(project)) {
                    boolean enabled = scmService.loadScmConfig(project, 'import')?.enabled
                    if (enabled) {
                        def keyAccess = scmService
                            .userHasAccessToScmConfiguredKeyOrPassword(authContext, ScmService.IMPORT, project)
                        if (keyAccess) {
                            def jobsPluginMeta = scmService.getJobsPluginMeta(project, true)
                            def jobStates = scmService
                                .importStatusForJobIds(project, authContext, ids, false, jobsPluginMeta)

                            jobStates.each {
                                metaItems.computeIfAbsent(it.key, { k -> [] }).add(
                                    ComponentMeta.with(
                                        SCM_IMPORT,
                                        [jobState: convertToMap(it.value)] as Map<String, Object>
                                    )

                                )
                            }
                        }
                    }
                }
            } catch (ScmPluginException e) {
                log.warn("Failed to get SCM Import status: ${e.message}")
            }
        }

        return metaItems
    }

    private boolean authorizedForExport(UserAndRolesAuthContext authContext, String project) {
        rundeckAuthContextProcessor.authorizeApplicationResourceAny(
            authContext,
            rundeckAuthContextProcessor.authResourceForProject(
                project
            ),
            [AuthConstants.ACTION_ADMIN, AuthConstants.ACTION_APP_ADMIN, AuthConstants.ACTION_EXPORT, AuthConstants
                .ACTION_SCM_EXPORT]
        )
    }

    private boolean authorizedForImport(UserAndRolesAuthContext authContext, String project) {
        rundeckAuthContextProcessor.authorizeApplicationResourceAny(
            authContext,
            rundeckAuthContextProcessor.authResourceForProject(
                project
            ),
            [AuthConstants.ACTION_ADMIN, AuthConstants.ACTION_APP_ADMIN, AuthConstants.ACTION_IMPORT, AuthConstants
                .ACTION_SCM_IMPORT]
        )
    }

    private static Map<String, Object> convertToMap(JobState jobState) {

        def result = [
            synchState: jobState.synchState.toString(),
            actions   : convertActions(jobState.actions),
        ]
        if (jobState.commit) {
            result.commit = jobState.commit.asMap()
        }
        return result as Map<String, Object>
    }

    private static Map<String, Object> convertToMap(JobImportState jobState) {
        def map = [
            synchState: jobState.synchState.toString(),
            actions   : convertActions(jobState.actions),
        ]
        if (jobState.jobRenamed) {
            map.jobRenamed =
                [
                    uuid       : jobState.jobRenamed.uuid,
                    sourceId   : jobState.jobRenamed.sourceId,
                    renamedPath: jobState.jobRenamed.renamedPath
                ]
        }
        if (jobState.commit) {
            map.commit = jobState.commit.asMap()
        }
        return map as Map<String, Object>
    }

    static List<Map<String, String>> convertActions(List<Action> actions) {
        if (!actions) {
            return [] as List<Map<String, String>>
        }
        actions.collect {
            [
                id         : it.id,
                title      : it.title,
                description: it.description,
                iconName   : it.iconName
            ]
        } as List<Map<String, String>>
    }
}
