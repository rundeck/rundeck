package rundeck.controllers

import com.dtolabs.rundeck.core.authorization.AuthContext
import com.dtolabs.rundeck.core.authorization.UserAndRolesAuthContext
import com.dtolabs.rundeck.plugins.scm.SynchState
import com.dtolabs.rundeck.server.authorization.AuthConstants
import rundeck.ScheduledExecution

class ScmController extends ControllerBase {
    def scmService
    def frameworkService

    def static allowedMethods = [
            disable: ['POST'],
    ]

    def index(String project) {
        def ePluginConfig = scmService.loadScmConfig(project, 'export')
        def iPluginConfig = scmService.loadScmConfig(project, 'import')
        def eplugins = scmService.listPlugins('export')
        def iplugins = scmService.listPlugins('import')
        def eConfiguredPlugin = null
        def iConfiguredPlugin = null
        if (ePluginConfig?.type) {
            eConfiguredPlugin = scmService.getPluginDescriptor('export', ePluginConfig.type)
        }
        if (iPluginConfig?.type) {
            iConfiguredPlugin = scmService.getPluginDescriptor('import', iPluginConfig.type)
        }
        def eEnabled = ePluginConfig?.enabled && scmService.projectHasConfiguredPlugin('export', project)
        def iEnabled = iPluginConfig?.enabled && scmService.projectHasConfiguredPlugin('import', project)

        return [
                plugins         : [import: iplugins, export: eplugins],
                configuredPlugin: [import: iConfiguredPlugin, export: eConfiguredPlugin],
                pluginConfig    : [import: iPluginConfig, export: ePluginConfig],
                enabled         : [import: iEnabled, export: eEnabled]
        ]
    }

    def setup(String integration, String project, String type) {

        AuthContext authContext = frameworkService.getAuthContextForSubjectAndProject(session.subject, project)
        if (unauthorizedResponse(
                frameworkService.authorizeApplicationResourceAll(
                        authContext,
                        frameworkService.authResourceForProject(project),
                        [AuthConstants.ACTION_CONFIGURE, AuthConstants.ACTION_ADMIN]
                ),
                AuthConstants.ACTION_CONFIGURE, 'Project', project
        )) {
            return
        }
        if (scmService.projectHasConfiguredPlugin(integration, project)) {
            return redirect(action: 'index', params: [project: project])
        }
        def describedPlugin = scmService.getPluginDescriptor(integration, type)
        def pluginConfig = scmService.loadScmConfig(project, integration)
        def config = [:]
        if (type == pluginConfig?.type) {
            config = pluginConfig.config
        }
        [
                properties : scmService.getSetupProperties(integration, project, type),
                type       : type,
                plugin     : describedPlugin,
                config     : config,
                integration: integration
        ]
    }

    def saveSetup(String integration, String project, String type) {

        AuthContext authContext = frameworkService.getAuthContextForSubjectAndProject(session.subject, project)
        if (unauthorizedResponse(
                frameworkService.authorizeApplicationResourceAll(
                        authContext,
                        frameworkService.authResourceForProject(project),
                        [AuthConstants.ACTION_CONFIGURE, AuthConstants.ACTION_ADMIN]
                ),
                AuthConstants.ACTION_CONFIGURE, 'Project', project
        )) {
            return
        }

        def config = params.config

        boolean valid = false
        //cancel modification
        if (params.cancel == 'Cancel') {
            return redirect(controller: 'scm', action: 'index', params: [project: project])
        }

        withForm {
            valid = true
        }.invalidToken {
            request.errorCode = 'request.error.invalidtoken.message'
            renderErrorView([:])
        }
        if (!valid) {
            return
        }

        //require type param
        def result = scmService.savePlugin(integration, project, type, config)
        def report
        if (result.error || !result.valid) {
            report = result.report
            request.error = result.error ? result.message : message(code: "some.input.values.were.not.valid")
            def describedPlugin = scmService.getPluginDescriptor(integration, type)

            render view: 'setup',
                   model: [
                           properties : scmService.getSetupProperties(integration, project, type),
                           type       : type,
                           plugin     : describedPlugin,
                           report     : report,
                           config     : config,
                           integration: integration,
                   ]

        } else {
            flash.message = message(code: 'scmController.action.setup.success.message')
            redirect(action: 'index', params: [project: project])
        }
    }

    def disable(String integration, String project, String type) {

        AuthContext authContext = frameworkService.getAuthContextForSubjectAndProject(session.subject, project)
        if (unauthorizedResponse(
                frameworkService.authorizeApplicationResourceAll(
                        authContext,
                        frameworkService.authResourceForProject(project),
                        [AuthConstants.ACTION_CONFIGURE, AuthConstants.ACTION_ADMIN]
                ),
                AuthConstants.ACTION_CONFIGURE, 'Project', project
        )) {
            return
        }

        boolean valid = false
        //cancel modification
        if (params.cancel == 'Cancel') {
            return redirect(controller: 'scm', action: 'index', params: [project: project])
        }

        withForm {
            valid = true
        }.invalidToken {
            request.errorCode = 'request.error.invalidtoken.message'
            renderErrorView([:])
        }
        if (!valid) {
            return
        }

        //require type param
        scmService.disablePlugin(integration, project, type)

        flash.message = message(code: "scmController.action.disable.success.message", args: [integration, type])
        redirect(action: 'index', params: [project: project])
    }

    def enable(String integration, String project, String type) {

        AuthContext authContext = frameworkService.getAuthContextForSubjectAndProject(session.subject, project)
        if (unauthorizedResponse(
                frameworkService.authorizeApplicationResourceAll(
                        authContext,
                        frameworkService.authResourceForProject(project),
                        [AuthConstants.ACTION_CONFIGURE, AuthConstants.ACTION_ADMIN]
                ),
                AuthConstants.ACTION_CONFIGURE, 'Project', project
        )) {
            return
        }

        boolean valid = false
        //cancel modification
        if (params.cancel == 'Cancel') {
            return redirect(controller: 'scm', action: 'index', params: [project: project])
        }

        withForm {
            valid = true
        }.invalidToken {
            request.errorCode = 'request.error.invalidtoken.message'
            renderErrorView([:])
        }
        if (!valid) {
            return
        }

        //require type param
        def result = scmService.enablePlugin(integration, project, type)
        if (result.error) {
            flash.warn = message(code: "scmController.action.enable.error.message", args: [integration, type])
            if (result.message) {
                flash.error = result.message
            }
        } else if (result.valid) {
            flash.message = message(code: "scmController.action.enable.success.message", args: [integration, type])

        } else {
            flash.warn = message(code: "scmController.action.enable.invalid.message", args: [integration, type])
            if (result.message) {
                flash.error = result.message
            }
        }

        redirect(action: 'index', params: [project: project])
    }

    def exportAction(String integration, String project, String actionId) {
        AuthContext authContext = frameworkService.getAuthContextForSubjectAndProject(session.subject, project)


        def requiredAction = integration == 'export' ? AuthConstants.ACTION_EXPORT : AuthConstants.ACTION_IMPORT
        if (unauthorizedResponse(
                frameworkService.authorizeApplicationResourceAny(authContext,
                                                                 frameworkService.authResourceForProject(project),
                                                                 [
                                                                         AuthConstants.ACTION_ADMIN,
                                                                         requiredAction
                                                                 ]
                ),
                requiredAction, 'Project', project
        )) {
            return
        }
        if (!scmService.projectHasConfiguredPlugin(integration, project)) {
            return redirect(action: 'index', params: [project: project])
        }
        List<String> jobIds = []
        Map deletedPaths = [:]
        List<String> selectedPaths = []
        Map<String, String> renamedJobPaths = scmService.getRenamedJobPathsForProject(params.project)
        if (params.jobIds) {
            jobIds = [params.jobIds].flatten()
        } else if (params.allJobs) {
            jobIds = ScheduledExecution.findAllByProject(params.project).collect {
                it.extid
            }
            if (integration == 'export') {
                deletedPaths = scmService.deletedExportFilesForProject(params.project)
            }
        }
        //remove deleted paths that are known to be renamed jobs
        renamedJobPaths.values().each {
            deletedPaths.remove(it)
        }

        List<ScheduledExecution> jobs = jobIds.collect {
            ScheduledExecution.getByIdOrUUID(it)
        }
        def scmStatus = []
        scmStatus = scmService.exportStatusForJobs(jobs).findAll {
            it.value.synchState != SynchState.CLEAN
        }
        jobs = jobs.findAll {
            it.extid in scmStatus.keySet()
        }
        def scmProjectStatus = scmService.getPluginStatus(integration, project)
        def scmFiles = integration == 'export' ? scmService.exportFilePathsMapForJobRefs(
                scmService.jobRefsForJobs(jobs)
        ) : null

        def trackingItems = integration == 'import' ? scmService.getTrackingItemsForAction(project, actionId) : null


        [
                actionView      : scmService.getInputView(integration, project, actionId),
                jobs            : jobs,
                scmStatus       : scmStatus,
                selected        : params.jobIds ? jobIds : [],
                filesMap        : scmFiles,
                trackingItems   : trackingItems,
                deletedPaths    : deletedPaths,
                selectedPaths   : selectedPaths,
                renamedJobPaths : renamedJobPaths,
                scmProjectStatus: scmProjectStatus,
                actionId        : actionId,
                integration     : integration
        ]
    }

    def exportActionSubmit(String integration, String project, String actionId) {

        UserAndRolesAuthContext authContext = frameworkService.getAuthContextForSubjectAndProject(session.subject, project)
        def requiredAction = integration == 'export' ? AuthConstants.ACTION_EXPORT :
                AuthConstants.ACTION_IMPORT
        if (unauthorizedResponse(
                frameworkService.authorizeApplicationResourceAny(authContext,
                                                                 frameworkService.authResourceForProject(project),
                                                                 [AuthConstants.ACTION_ADMIN, requiredAction]
                ),
                requiredAction, 'Project', project
        )) {
            return
        }

        if (!scmService.projectHasConfiguredPlugin(integration, project)) {
            return redirect(action: 'index', params: [project: project])
        }
        boolean valid = false
        //cancel modification
        if (params.cancel == 'Cancel') {
            return redirect(controller: 'scm', action: 'index', params: [project: project])
        }

        withForm {
            valid = true
        }.invalidToken {
            request.errorCode = 'request.error.invalidtoken.message'
            renderErrorView([:])
        }
        if (!valid) {
            return
        }
        List<String> jobIds = [params.jobIds].flatten().findAll { it }

        List<ScheduledExecution> jobs = jobIds.collect {
            ScheduledExecution.getByIdOrUUID(it)
        }
        List<String> deletePaths = [params.deletePaths].flatten().findAll { it }
        jobIds.each {
            if (params."renamedPaths.${it}") {
                deletePaths << params."renamedPaths.${it}"
            }
        }

        List<String> chosenTrackedItems = [params.chosenTrackedItem].flatten().findAll { it }

        def deletePathsToJobIds = deletePaths.collectEntries { [it, scmService.deletedJobForPath(project, it)?.id] }
        def result
        if (integration == 'export') {
            result = scmService.performExportAction(
                    actionId,
                    session.user,
                    project,
                    params.pluginProperties,
                    jobs,
                    deletePaths
            )
        } else {
            result = scmService.performImportAction(
                    actionId,
                    authContext,
                    project,
                    params.pluginProperties,
                    chosenTrackedItems
            )
        }
        if (!result.valid || result.error) {
            def report = result.report
            if (result.missingUserInfoField) {
                request.errors = [result.message]
                request.error = message(code: "scmController.action.saveCommit.userInfoMissing.message")
                request.errorHelp = message(code: "scmController.action.saveCommit.userInfoMissing.errorHelp")
            } else {
                request.error = result.error ? result.message : message(code: "some.input.values.were.not.valid")
            }
            def deletedPaths = scmService.deletedExportFilesForProject(project)
            def scmStatus = scmService.exportStatusForJobs(jobs)
            def scmFiles = integration == 'export' ? scmService.exportFilePathsMapForJobRefs(
                    scmService.jobRefsForJobs(jobs)
            ) : null

            def scmProjectStatus = scmService.getPluginStatus(integration, params.project)
            def trackingItems = integration == 'import' ? scmService.getTrackingItemsForAction(project, actionId) : null

            render view: 'exportAction',
                   model: [
                           actionView      : scmService.getInputView(integration,project, actionId),
                           jobs            : jobs,
                           scmStatus       : scmStatus,
                           selected        : params.jobIds ? jobIds : [],
                           filesMap        : scmFiles,
                           trackingItems   : trackingItems,
                           selectedItems: chosenTrackedItems,
                           report          : report,
                           config          : params.pluginProperties,
                           deletedPaths    : deletedPaths,
                           selectedPaths   : deletePaths,
                           scmProjectStatus: scmProjectStatus,
                           actionId        : actionId,
                           integration     : integration
                   ]
            return
        }
        if(integration=='export') {
            def commitid = result.commitId
            if (result.message) {
                flash.message = result.message
            } else {
                def code = "scmController.action.export.multi.succeed.message"
                def jobIdent = ''
                if (jobs.size() == 1 && deletePaths.size() == 0) {
                    code = "scmController.action.export.succeed.message"
                    jobIdent = '{{Job ' + jobIds[0] + '}}'
                } else if (jobs.size() == 0 && deletePaths.size() == 1) {
                    code = "scmController.action.export.delete.succeed.message"
                    jobIdent = deletePathsToJobIds[deletePaths[0]] ?: ''
                }

                flash.message = message(
                        code: code,
                        args: [
                                commitid,
                                jobs.size() + deletePaths.size(),
                                jobIdent
                        ]
                )
            }
        }else{
            if (result.message) {
                flash.message = result.message
            }else{
                flash.message = message(
                        code: 'scmController.action.import.success',
                        args: [],
                        default: "SCM Import Successful"
                )
            }
        }
        redirect(action: 'jobs', controller: 'menu', params: [project: params.project])
    }

    /**
     * Ajax endpoint for job diff
     */
    def diffRemote(String project, String jobId) {
        if (!scmService.projectHasConfiguredExportPlugin(project)) {
            return redirect(action: 'index', params: [project: project])
        }
        if (!jobId) {
            flash.message = "No jobId Selected"
            return redirect(action: 'index', params: [project: project])
        }
        def job = ScheduledExecution.getByIdOrUUID(jobId)
        def diff = scmService.exportDiff(project, job)
        render(contentType: 'application/json') {
            modified = diff?.modified ?: false
            newNotFound = diff?.newNotFound ?: false
            oldNotFound = diff?.oldNotFound ?: false
            content = diff?.content ?: ''
        }
    }

    def diff(String project, String jobId) {
        AuthContext authContext = frameworkService.getAuthContextForSubjectAndProject(session.subject, project)

        if (unauthorizedResponse(
                frameworkService.authorizeApplicationResourceAny(authContext,
                                                                 frameworkService.authResourceForProject(project),
                                                                 [AuthConstants.ACTION_ADMIN, AuthConstants.ACTION_EXPORT]
                ),
                AuthConstants.ACTION_EXPORT, 'Project', project
        )) {
            return
        }
        if (!scmService.projectHasConfiguredExportPlugin(project)) {
            return redirect(action: 'index', params: [project: project])
        }
        if (!jobId) {
            flash.message = "No jobId Selected"
            return redirect(action: 'index', params: [project: project])
        }
        def job = ScheduledExecution.getByIdOrUUID(jobId)
        def scmStatus = scmService.exportStatusForJobs([job])
        def scmFilePaths = scmService.exportFilePathsMapForJobs([job])
        def diffResult = scmService.exportDiff(project, job)
        def scmExportRenamedPath = scmService.getRenamedJobPathsForProject(params.project)?.get(job.extid)
        if (params.download == 'true') {
            if (params.download) {
                response.addHeader("Content-Disposition", "attachment; filename=\"${job.extid}.diff\"")
            }
            render(contentType: 'text/plain', text: diffResult?.content ?: '')
            return
        }
        [
                diffResult          : diffResult,
                scmStatus           : scmStatus,
                job                 : job,
                scmFilePaths        : scmFilePaths,
                scmExportRenamedPath: scmExportRenamedPath
        ]
    }
}
