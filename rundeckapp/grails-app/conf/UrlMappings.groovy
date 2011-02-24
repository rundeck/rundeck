class UrlMappings {
    static mappings = {
        "/$controller/$action?/$id?" {
            constraints {
                // apply constraints here
            }
        }
        /*******
        * API url paths, v1
        */
        "/api/$api_version/error"(controller: 'api', action: 'error')
        "/api/$api_version/execution/$id"(controller: 'execution', action: 'apiExecution')
        "/api/$api_version/execution/$id/abort"(controller: 'execution', action: 'apiExecutionAbort')
        "/api/$api_version/executions/running"(controller: 'menu', action: 'apiExecutionsRunning')
        "/api/$api_version/history"(controller: 'reports', action: 'apiHistory')
        "/api/$api_version/job/$id"(controller: 'api') {
            //passthrough from ApiController to ScheduledExecutionController
            action = [GET: "apiJobExport", DELETE: "apiJobDelete"]
        }
        "/api/$api_version/job/$id/run"(controller: 'scheduledExecution', action: 'apiJobRun')
        "/api/$api_version/jobs"(controller: 'menu', action: 'apiJobsList')
        "/api/$api_version/jobs/export"(controller: 'menu', action: 'apiJobsExport')
        "/api/$api_version/jobs/import"(controller: 'scheduledExecution', action: 'apiJobsImport')
        "/api/$api_version/project/$project?"(controller: 'framework', action: 'apiProject')
        "/api/$api_version/projects"(controller: 'framework', action: 'apiProjects')
        "/api/renderError"(controller: 'api', action: 'renderError')
        "/api/error"(controller: 'api', action: 'error')
        "/api/$api_version/report/create"(controller: 'reports', action: 'apiReportCreate')
        "/api/$api_version/resources"(controller: 'framework', action: 'apiResources')
        "/api/$api_version/resource/$name"(controller: 'framework', action: 'apiResource')
        "/api/$api_version/run/command"(controller: 'scheduledExecution', action: 'apiRunCommand')
        "/api/$api_version/run/script"(controller: 'scheduledExecution', action: 'apiRunScript')
        "/api/$api_version/$action?"(controller: 'api', action: 'invalid')

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
