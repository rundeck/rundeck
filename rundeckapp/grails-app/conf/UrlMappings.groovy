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
        "/api/$api_version/execution/$id"(controller: 'execution'){
            action=[GET:'apiExecution',DELETE: 'apiExecutionDelete']
        }
        "/api/$api_version/execution/$id/state"(controller: 'execution', action: 'apiExecutionState')
        "/api/$api_version/execution/$id/abort"(controller: 'execution', action: 'apiExecutionAbort')
        /** v5 */
        "/api/$api_version/execution/$id/output(.$format)?"(controller: 'execution', action: 'apiExecutionOutput')
        /** v10 */
        "/api/$api_version/execution/$id/output/state"(controller: 'execution', action: 'apiExecutionStateOutput')
        "/api/$api_version/execution/$id/output/node/$nodename"(controller: 'execution', action: 'apiExecutionOutput')
        "/api/$api_version/execution/$id/output/node/$nodename/step/$stepctx**?"(controller: 'execution', action: 'apiExecutionOutput')
        "/api/$api_version/execution/$id/output/step/$stepctx**?"(controller: 'execution', action: 'apiExecutionOutput')

        "/api/$api_version/executions/running"(controller: 'menu', action: 'apiExecutionsRunning')
        "/api/$api_version/executions/delete"(controller: 'execution', action: 'apiExecutionDeleteBulk')
        "/api/$api_version/executions"(controller: 'execution', action: 'apiExecutionsQuery')
        "/api/$api_version/history"(controller: 'reports', action: 'apiHistory')
        "/api/$api_version/job/$id"(controller: 'scheduledExecution') {
            action = [GET: 'apiJobExport', DELETE: 'apiJobDelete', PUT: 'apiJobUpdateSingle', POST: 'apiJobCreateSingle']
        }
        "/api/$api_version/job/$id/run"(controller: 'scheduledExecution', action: 'apiJobRun')
        "/api/$api_version/job/$id/executions"(controller: 'scheduledExecution') {
            action = [GET: 'apiJobExecutions', DELETE: 'apiJobExecutionsDelete', POST: 'apiJobRun']
        }
        "/api/$api_version/jobs"(controller: 'menu', action: 'apiJobsList')
        "/api/$api_version/jobs/export"(controller: 'menu', action: 'apiJobsExport')
        "/api/$api_version/jobs/import"(controller: 'scheduledExecution', action: 'apiJobsImport')
        "/api/$api_version/jobs/delete"(controller: 'scheduledExecution', action: 'apiJobDeleteBulk')
        "/api/$api_version/project/$project"(controller: 'project'){
            action = [GET: 'apiProjectGet', DELETE:'apiProjectDelete']
        }
        "/api/$api_version/project/$project/config"(controller: 'project'){
            action = [GET: 'apiProjectConfigGet', PUT: 'apiProjectConfigPut']
        }
        "/api/$api_version/project/$project/config/$keypath**"(controller: 'project'){
            action = [GET: 'apiProjectConfigKeyGet', PUT: 'apiProjectConfigKeyPut',
                    DELETE: 'apiProjectConfigKeyDelete']
        }
        "/api/$api_version/project/$project/export"(controller: 'project',action: 'apiProjectExport')
        "/api/$api_version/project/$project/import"(controller: 'project',action: 'apiProjectImport')
        /** v2 */
        "/api/$api_version/project/$project/resources/refresh"(controller: 'framework', action: 'apiProjectResourcesRefresh')
        /** v2  */
        "/api/$api_version/project/$project/resources"(controller: 'framework') {
            action = [GET: "apiResourcesv2",/* PUT: "update", DELETE: "delete",*/ POST: "apiProjectResourcesPost"]
        }
        /** v2 */
        "/api/$api_version/project/$project/jobs"(controller: 'menu', action: 'apiJobsListv2')
        "/api/$api_version/projects"(controller: 'project'){
            action = [GET: 'apiProjectList', POST:'apiProjectCreate']
        }
//        "/api/renderError"(controller: 'api', action: 'renderError')
//        "/api/error"(controller: 'api', action: 'error')
        "/api/$api_version/resources"(controller: 'framework', action: 'apiResources')
        "/api/$api_version/resource/$name"(controller: 'framework', action: 'apiResource')
        "/api/$api_version/run/command"(controller: 'scheduledExecution', action: 'apiRunCommand')
        "/api/$api_version/run/script"(controller: 'scheduledExecution', action: 'apiRunScript')
        "/api/$api_version/run/url"(controller: 'scheduledExecution', action: 'apiRunScriptUrl')
        "/api/$api_version/system/info"(controller: 'api', action: 'apiSystemInfo')
        "/api/$api_version/tokens/$user?"(controller: 'api', action: 'apiTokenList')
        "/api/$api_version/token/$token"(controller: 'api', action: 'apiTokenManage')

        "/api/$api_version/storage/keys/$resourcePath**"(controller: 'storage', action: 'apiKeys')
        "/api/$api_version/storage/keys"(controller: 'storage', action: 'apiKeys')

        //incubator endpoints
        "/api/$api_version/incubator/jobs/takeoverSchedule"(controller: 'scheduledExecution', action: 'apiJobClusterTakeoverSchedule')
        "/api/$api_version/incubator/storage/$resourcePath**"(controller: 'storage') {
            action = [
                    GET: "apiGetResource",
                    PUT: "apiPutResource",
                    POST: "apiPostResource",
                    DELETE: "apiDeleteResource"
            ]
        }

        "/api/$api_version/incubator/feature/$featureName?"(controller: 'api',action: 'featureToggle')


        //catchall
        "/api/$api_version/$action?"(controller: 'api', action: 'invalid')

        //simplified url mappings for link generation
        "/project/$project/nodes/"(controller: 'framework', action: 'nodes')
        "/project/$project/run/"(controller: 'framework', action: 'nodes')
        "/project/$project/command/run"(controller: 'framework',action: 'adhoc')
        "/project/$project/activity"(controller: 'reports', action: 'index')
        "/project/$project/history"(controller: 'reports', action: 'index')
        "/project/$project/jobs/$groupPath**?"(controller: 'menu', action: 'jobs')
        "/project/$project/job/show/$id/$fullName**?"(controller: 'scheduledExecution', action: 'show')
        "/project/$project/job/upload"(controller: 'scheduledExecution'){
            action = [GET: 'upload', POST: 'uploadPost']
        }
        "/project/$project/job/$action?/$id?"(controller: 'scheduledExecution')
        "/resources/createProject"(controller: 'framework') {
            action = [GET: 'createProject', POST: 'createProjectPost']
        }
        "/resources/$action?/$id?"(controller: 'framework')
        "/project/$project/events/$action?/$id?"(controller: 'reports')
        "/project/$project/configure"(controller: 'menu', action: 'admin')
        "/project/$project/execution/show/$id"(controller: 'execution',action: 'show')
        "/project/$project/execution/$action/$id"(controller: 'execution')
        "/project/$project/export"(controller: 'project',action: 'export')
        "/project/$project/importArchive"(controller: 'project',action: 'importArchive')
        "/project/$project"(controller: 'menu',action: 'index')
        "/project/$project/$action"(controller: 'project')
        "/storage/access/keys/$resourcePath**"(controller: 'storage', action: 'keyStorageAccess')
        "/storage/access/keys"(controller: 'storage', action: 'keyStorageAccess')
        "/storage/upload/keys"(controller: 'storage', action: 'keyStorageUpload')
        "/storage/delete/keys"(controller: 'storage', action: 'keyStorageDelete')
        "/storage/download/keys"(controller: 'storage', action: 'keyStorageDownload')
        "/storage/download/keys/$resourcePath**"(controller: 'storage', action: 'keyStorageDownload')
        "/job/show/$id"(controller: 'scheduledExecution',action: 'show')
        "/execution/show/$id"(controller: 'execution',action: 'show')
        "404"(view: '/404')
        "500"(view: '/error')
    }
}
