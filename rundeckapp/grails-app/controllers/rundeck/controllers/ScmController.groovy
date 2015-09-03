package rundeck.controllers

import com.dtolabs.rundeck.core.authorization.AuthContext
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
        def pluginConfig = scmService.loadScmConfig(project, 'export')
        def plugins = scmService.listPlugins('export')
        def configuredPlugin = null
        if (pluginConfig?.type) {
            configuredPlugin = scmService.getExportPluginDescriptor(pluginConfig.type)
        }
        return [
                plugins         : plugins ?: [],
                configuredPlugin: configuredPlugin,
                pluginConfig    : pluginConfig,
                config          : pluginConfig?.config,
                type            : pluginConfig?.type
        ]
    }

    def setup(String project, String type) {

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
        if (scmService.projectHasConfiguredExportPlugin(project)) {
            return redirect(action: 'index', params: [project: project])
        }
        def describedPlugin = scmService.getExportPluginDescriptor(type)
        def pluginConfig = scmService.loadScmConfig(project, 'export')
        def config=[:]
        if(type==pluginConfig?.type){
            config=pluginConfig.config
        }
        [
                properties: scmService.getExportSetupProperties(project, type),
                type: type,
                plugin: describedPlugin,
                config: config
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
        if (!result.valid) {
            report = result.report
            request.error = message(code: "some.input.values.were.not.valid")
            log.error("configuration error: " + report)

            def describedPlugin = scmService.getExportPluginDescriptor(type)

            render view: 'setup',
                   model: [
                           properties: scmService.getExportSetupProperties(project, type),
                           type      : type,
                           plugin    : describedPlugin,
                           report    : report,
                           config    : config
                   ]

        } else {
            flash.message = "setup complete"
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

        flash.message = "Plugin disabled for SCM ${integration}: ${type}"
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
        if (result.valid) {
            flash.message = "Plugin enabled for SCM ${integration}: ${type}"

        } else {
            flash.warning = "Plugin was not enabled for SCM ${integration}: ${type}: Configuration was not valid.  Please reconfigure and try again."
        }

        redirect(action: 'index', params: [project: project])
    }

    def commit(String project) {
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
            deletedPaths = scmService.deletedExportFilesForProject(params.project)
        }
        //remove deleted paths that are known to be renamed jobs
        renamedJobPaths.values().each {
            deletedPaths.remove(it)
        }

        List<ScheduledExecution> jobs = jobIds.collect {
            ScheduledExecution.getByIdOrUUID(it)
        }
        def scmStatus = scmService.exportStatusForJobs(jobs).findAll {
            it.value.synchState != SynchState.CLEAN
        }
        jobs = jobs.findAll {
            it.extid in scmStatus.keySet()
        }
        def scmFiles = scmService.filePathsMapForJobRefs(scmService.jobRefsForJobs(jobs))
        [
                properties     : scmService.getExportCommitProperties(project, jobIds),
                jobs           : jobs,
                scmStatus      : scmStatus,
                selected       : params.jobIds ? jobIds : [],
                filesMap       : scmFiles,
                deletedPaths   : deletedPaths,
                selectedPaths  : selectedPaths,
                renamedJobPaths: renamedJobPaths,
        ]
    }

    def saveCommit(String project) {

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

        if (!params.jobIds && !params.deletePaths) {
            flash.message = "No Job Ids or Paths Selected"
            return redirect(action: 'index', params: [project: project])
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
        System.err.println("add delete paths: " + params.renamedPaths)

        def deletePathsToJobIds = deletePaths.collectEntries { [it, scmService.deletedJobForPath(project, it)?.id] }
        def result = scmService.exportCommit(project, params.commit, jobs, deletePaths)
        if (!result.valid) {
            def report = result.report
            request.error = message(code: "some.input.values.were.not.valid")
            log.debug("configuration error: " + report)

            def deletedPaths = scmService.deletedExportFilesForProject(project)
            def scmStatus = scmService.exportStatusForJobs(jobs)
            def scmFiles = scmService.filePathsMapForJobRefs(scmService.jobRefsForJobs(jobs))
            render view: 'commit',
                   model: [
                           properties   : scmService.getExportCommitProperties(project, jobIds),
                           jobs         : jobs,
                           scmStatus    : scmStatus,
                           selected     : params.jobIds ? jobIds : [],
                           filesMap     : scmFiles,
                           report       : report,
                           config       : params.commit,
                           deletedPaths : deletedPaths,
                           selectedPaths: deletePaths
                   ]
            return
        }

        def commitid = result.commitId
        def code = "scmController.action.commit.multi.succeed.message"
        def jobIdent = ''
        if (jobs.size() == 1 && deletePaths.size() == 0) {
            code = "scmController.action.commit.succeed.message"
            jobIdent = '{{Job ' + jobIds[0] + '}}'
        } else if (jobs.size() == 0 && deletePaths.size() == 1) {
            code = "scmController.action.commit.delete.succeed.message"
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
        def scmFilePaths = scmService.filePathsMapForJobs([job])
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
