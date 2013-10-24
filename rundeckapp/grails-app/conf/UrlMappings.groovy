class UrlMappings {
    static mappings = {
        "/$controller/$action?/$id?" {
            constraints {
                // apply constraints here
            }
        }
        "/"(controller: 'menu', action: 'index')
        /*******
        * API url paths, v1
        */
        "/api/$api_version/execution/$id"(controller: 'execution', action: 'apiExecution')
        "/api/$api_version/execution/$id/state/$path**?"(controller: 'execution', action: 'apiExecutionState')
        "/api/$api_version/execution/$id/abort"(controller: 'execution', action: 'apiExecutionAbort')
        /** v5 */
        "/api/$api_version/execution/$id/output"(controller: 'execution', action: 'apiExecutionOutput')
        "/api/$api_version/executions/running"(controller: 'menu', action: 'apiExecutionsRunning')
        "/api/$api_version/executions"(controller: 'execution', action: 'apiExecutionsQuery')
        "/api/$api_version/history"(controller: 'reports', action: 'apiHistory')
        "/api/$api_version/job/$id"(controller: 'scheduledExecution') {
            action = [GET: 'apiJobExport', DELETE: 'apiJobDelete', PUT: 'apiJobUpdateSingle', POST: 'apiJobCreateSingle']
        }
        "/api/$api_version/job/$id/run"(controller: 'scheduledExecution', action: 'apiJobRun')
        "/api/$api_version/job/$id/executions"(controller: 'scheduledExecution', action: 'apiJobExecutions')
        "/api/$api_version/jobs"(controller: 'menu', action: 'apiJobsList')
        "/api/$api_version/jobs/export"(controller: 'menu', action: 'apiJobsExport')
        "/api/$api_version/jobs/import"(controller: 'scheduledExecution', action: 'apiJobsImport')
        "/api/$api_version/jobs/delete"(controller: 'scheduledExecution', action: 'apiJobDeleteBulk')
        "/api/$api_version/project/$project?"(controller: 'framework', action: 'apiProject')
        /** v2 */
        "/api/$api_version/project/$project/resources/refresh"(controller: 'framework', action: 'apiProjectResourcesRefresh')
        /** v2  */
        "/api/$api_version/project/$project/resources"(controller: 'framework') {
            action = [GET: "apiResourcesv2",/* PUT: "update", DELETE: "delete",*/ POST: "apiProjectResourcesPost"]
        }
        /** v2 */
        "/api/$api_version/project/$project/jobs"(controller: 'menu', action: 'apiJobsListv2')
        "/api/$api_version/projects"(controller: 'framework', action: 'apiProjects')
//        "/api/renderError"(controller: 'api', action: 'renderError')
//        "/api/error"(controller: 'api', action: 'error')
        "/api/$api_version/report/create"(controller: 'reports', action: 'apiReportCreate')
        "/api/$api_version/resources"(controller: 'framework', action: 'apiResources')
        "/api/$api_version/resource/$name"(controller: 'framework', action: 'apiResource')
        "/api/$api_version/run/command"(controller: 'scheduledExecution', action: 'apiRunCommand')
        "/api/$api_version/run/script"(controller: 'scheduledExecution', action: 'apiRunScript')
        "/api/$api_version/run/url"(controller: 'scheduledExecution', action: 'apiRunScriptUrl')
        "/api/$api_version/system/info"(controller: 'api', action: 'apiSystemInfo')

        //incubator endpoints
        "/api/$api_version/incubator/jobs/takeoverSchedule"(controller: 'scheduledExecution', action: 'apiJobClusterTakeoverSchedule')

        //catchall
        "/api/$api_version/$action?"(controller: 'api', action: 'invalid')

        //simplified url mappings for link generation
        "/nodes/"(controller: 'framework', action: 'nodes')
        "/run/"(controller: 'framework', action: 'nodes')
        "/activity"(controller: 'reports', action: 'index')
        "/history"(controller: 'reports', action: 'index')
        "/jobs/$groupPath**?"(controller: 'menu', action: 'jobs')
        "/job/show/$id/$fullName**?"(controller: 'scheduledExecution',action: 'show')
        "/job/$action?/$id?"(controller: 'scheduledExecution')
        "/resources/createProject"(controller: 'framework') {
            action = [GET: 'createProject', POST: 'createProjectPost']
        }
        "/resources/$action?/$id?"(controller: 'framework')
        "/events/$action?/$id?"(controller: 'reports')
        "/configure"(controller: 'menu', action: 'admin')
        "500"(view: '/error')
    }
}
