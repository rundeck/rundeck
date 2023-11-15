package org.rundeck.app.data.project

import com.dtolabs.rundeck.core.authorization.AuthContextProcessor
import com.dtolabs.rundeck.core.authorization.UserAndRolesAuthContext
import com.dtolabs.rundeck.plugins.scm.ScmImportSynchState
import com.dtolabs.rundeck.plugins.scm.ScmPluginException
import groovy.transform.CompileStatic
import org.rundeck.app.components.jobs.ComponentMeta
import org.rundeck.app.components.project.ProjectMetadataComponent
import org.rundeck.core.auth.AuthConstants
import org.springframework.beans.factory.annotation.Autowired
import rundeck.services.ScmService

@CompileStatic
class ProjectScmMetadataComponent implements ProjectMetadataComponent {
    public static final String SCM_EXPORT = 'scmExport'
    public static final String SCM_IMPORT = 'scmImport'
    @Autowired
    ScmService scmService
    @Autowired
    AuthContextProcessor rundeckAuthContextProcessor
    final Set<String> availableMetadataNames = Collections.unmodifiableSet(
        new HashSet<String>(
            [SCM_EXPORT, SCM_IMPORT]
        )
    )

    @Override
    Optional<List<ComponentMeta>> getMetadataForProject(
        final String project,
        final Set<String> names,
        final UserAndRolesAuthContext authContext
    ) {

        List<ComponentMeta> metaItems = new ArrayList<>()
        if (
            (names.contains(SCM_EXPORT) || names.contains('*')) &&
            checkAuth(authContext, project, [AuthConstants.ACTION_EXPORT, AuthConstants.ACTION_SCM_EXPORT])
        ) {
            Map<String, Object> results = new HashMap<>()
            try {
                def ePluginConfig = scmService.loadScmConfig(project, ScmService.EXPORT)
                if(ePluginConfig){
                    results.configured = true
                }
                if (scmService.projectHasConfiguredExportPlugin(project)) {
                    results.enabled = scmService.loadScmConfig(project, ScmService.EXPORT)?.enabled
                    if (results.enabled) {
                        def validation = scmService
                            .userHasAccessToScmConfiguredKeyOrPassword(authContext, ScmService.EXPORT, project)
                        if (validation) {

                            results.status = scmService.exportPluginStatus(authContext, project)
                            results.actions = scmService.exportPluginActions(authContext, project)
                            results.renamed = scmService.getRenamedJobPathsForProject(project)
                        } else {
                            results.valid = false
                        }
                    }
                }
            } catch (ScmPluginException e) {
                results.warning = "Failed to update SCM Export status: ${e.message}".toString()
            }
            metaItems.add(ComponentMeta.with(SCM_EXPORT, results))
        }
        if (
            (names.contains(SCM_IMPORT) || names.contains('*')) &&
            checkAuth(authContext, project, [AuthConstants.ACTION_IMPORT, AuthConstants.ACTION_SCM_IMPORT])
        ) {
            Map<String, Object> results = new HashMap<>()
            try {

                def pluginConfig = scmService.loadScmConfig(project, ScmService.IMPORT)
                if(pluginConfig){
                    results.configured = true
                }
                if (scmService.projectHasConfiguredImportPlugin(project)) {
                    results.enabled = scmService.loadScmConfig(project, ScmService.IMPORT)?.enabled
                    if (results.enabled) {
                        def validation = scmService
                            .userHasAccessToScmConfiguredKeyOrPassword(authContext, ScmService.IMPORT, project)
                        if (validation) {
                            ScmImportSynchState status = scmService.importPluginStatus(authContext, project)
                            results.status = status
                            results.actions = scmService.importPluginActions(authContext, project, status)
                        } else {
                            results.valid = false
                        }
                    }
                }
            } catch (ScmPluginException e) {
                results.warning = "Failed to update SCM Import status: ${e.message}"
            }
            metaItems.add(ComponentMeta.with(SCM_IMPORT, results))
        }

        if (metaItems?.size()) {
            return Optional.of(metaItems)
        } else {
            return Optional.empty()
        }
    }

    private boolean checkAuth(UserAndRolesAuthContext authContext, String project, List<String> actions) {
        rundeckAuthContextProcessor.authorizeApplicationResourceAny(
            authContext,
            rundeckAuthContextProcessor.authResourceForProject(
                project
            ),
            [AuthConstants.ACTION_ADMIN, AuthConstants.ACTION_APP_ADMIN] + actions
        )
    }
}
