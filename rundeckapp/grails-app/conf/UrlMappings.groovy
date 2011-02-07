class UrlMappings {
    static mappings = {
        "/$controller/$action?/$id?" {
            constraints {
                // apply constraints here
            }
        }
        //1.2 api url paths

        "/api/$action?"(controller: 'api', action: 'invalid')
        "/api/error"(controller: 'api', action: 'error')
        "/api/execution/$id"(controller: 'execution', action: 'apiExecution')
        "/api/execution/$id/abort"(controller: 'execution', action: 'apiExecutionAbort')
        "/api/executions/running"(controller: 'menu', action: 'apiExecutionsRunning')
        "/api/history"(controller: 'reports', action: 'apiHistory')
        "/api/job/$id"(controller: 'api') {
            //passthrough from ApiController to ScheduledExecutionController
            action = [GET: "apiJobExport", DELETE: "apiJobDelete"]
        }
        "/api/job/$id/run"(controller: 'scheduledExecution', action: 'apiJobRun')
        "/api/jobs"(controller: 'menu', action: 'apiJobsList')
        "/api/jobs/export"(controller: 'menu', action: 'apiJobsExport')
        "/api/jobs/import"(controller: 'scheduledExecution', action: 'apiJobsImport')
        "/api/project/$project?"(controller: 'framework', action: 'apiProject')
        "/api/projects"(controller: 'framework', action: 'apiProjects')
        "/api/renderError"(controller: 'api', action: 'renderError')
        "/api/run/command"(controller: 'scheduledExecution', action: 'apiRunCommand')
        "/api/run/script"(controller: 'scheduledExecution', action: 'apiRunScript')

        //simplified url mappings for link generation
        "/run/$id?"(controller: 'framework', action: 'nodes')
        "/history/$id?"(controller: 'reports', action: 'index')
        "/jobs/$id?"(controller: 'menu', action: 'jobs')
        "/job/$action?/$id?"(controller: 'scheduledExecution')
        "/resources/$action?/$id?"(controller: 'framework')
        "/events/$action?/$id?"(controller: 'reports')
        "500"(view: '/error')
    }
}
