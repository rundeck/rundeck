import rundeck.filters.ApiRequestFilters

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
        "/api/$api_version/execution/$id/output(.$format)?"(controller: 'execution', action: 'apiExecutionOutput')
        "/api/$api_version/execution/$id/output/state"(controller: 'execution', action: 'apiExecutionStateOutput')
        "/api/$api_version/execution/$id/output/node/$nodename"(controller: 'execution', action: 'apiExecutionOutput')
        "/api/$api_version/execution/$id/output/node/$nodename/step/$stepctx**?"(controller: 'execution', action: 'apiExecutionOutput')
        "/api/$api_version/execution/$id/output/step/$stepctx**?"(controller: 'execution', action: 'apiExecutionOutput')


        "/api/$api_version/executions/delete"(controller: 'execution', action: 'apiExecutionDeleteBulk')


        "/api/$api_version/job/$id"(controller: 'scheduledExecution') {
            action = [GET: 'apiJobExport', DELETE: 'apiJobDelete', PUT: 'apiJobUpdateSingle', POST: 'apiJobCreateSingle']
        }

        "/api/$api_version/job/$id/execution/enable"(controller: 'scheduledExecution') {
            action = [POST: 'apiFlipExecutionEnabled']
            status = true
        }
        "/api/$api_version/job/$id/execution/disable"(controller: 'scheduledExecution') {
            action = [POST: 'apiFlipExecutionEnabled']
            status = false
        }

        "/api/$api_version/job/$id/schedule/enable"(controller: 'scheduledExecution') {
            action = [POST: 'apiFlipScheduleEnabled']
            status = true
        }
        "/api/$api_version/job/$id/schedule/disable"(controller: 'scheduledExecution') {
            action = [POST: 'apiFlipScheduleEnabled']
            status = false
        }

        "/api/$api_version/job/$id/run"(controller: 'scheduledExecution', action: 'apiJobRun')
        "/api/$api_version/job/$id/executions"(controller: 'scheduledExecution') {
            action = [GET: 'apiJobExecutions', DELETE: 'apiJobExecutionsDelete', POST: 'apiJobRun']
        }

        "/api/$api_version/job/$id/scm/$integration/status"(controller: 'scm', action: 'apiJobStatus')
        "/api/$api_version/job/$id/scm/$integration/diff"(controller: 'scm', action: 'apiJobDiff')
        "/api/$api_version/job/$id/scm/$integration/action/$actionId/input"(controller: 'scm', action: 'apiJobActionInput')
        "/api/$api_version/job/$id/scm/$integration/action/$actionId"(controller: 'scm', action: 'apiJobActionPerform')

        "/api/$api_version/jobs/delete"(controller: 'scheduledExecution', action: 'apiJobDeleteBulk')
        "/api/$api_version/jobs/schedule/enable"(controller: 'scheduledExecution',action: 'apiFlipScheduleEnabledBulk') {
            status = true
        }
        "/api/$api_version/jobs/schedule/disable"(controller: 'scheduledExecution',action: 'apiFlipScheduleEnabledBulk'){
            status = false
        }
        "/api/$api_version/jobs/execution/enable"(controller: 'scheduledExecution',action: 'apiFlipExecutionEnabledBulk') {
            status = true
        }
        "/api/$api_version/jobs/execution/disable"(controller: 'scheduledExecution',action: 'apiFlipExecutionEnabledBulk'){
            status = false
        }


        "/api/$api_version/project/$project/executions/running"(controller: 'menu', action: 'apiExecutionsRunningv14')
        "/api/$api_version/project/$project/executions"(controller: 'execution', action: 'apiExecutionsQueryv14')
        "/api/$api_version/project/$project/jobs/export"(controller: 'menu', action: 'apiJobsExportv14')
        "/api/$api_version/project/$project/jobs/import"(controller: 'scheduledExecution', action: 'apiJobsImportv14')
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
        "/api/$api_version/project/$project/readme.md"(controller: 'project'){
            filename='readme.md'
            action = [GET: 'apiProjectFileGet', PUT: 'apiProjectFilePut', DELETE: 'apiProjectFileDelete']
        }
        "/api/$api_version/project/$project/motd.md"(controller: 'project'){
            filename='motd.md'
            action = [GET: 'apiProjectFileGet', PUT: 'apiProjectFilePut', DELETE: 'apiProjectFileDelete']
        }
        "/api/$api_version/project/$project/acl/$path**"(controller: 'project',action: 'apiProjectAcls')
        "/api/$api_version/project/$project/export"(controller: 'project',action: 'apiProjectExport')
        "/api/$api_version/project/$project/import"(controller: 'project',action: 'apiProjectImport')
        "/api/$api_version/project/$project/resources/refresh"(controller: 'framework', action: 'apiProjectResourcesRefresh')
        "/api/$api_version/project/$project/resources"(controller: 'framework') {
            action = [GET: "apiResourcesv2",/* PUT: "update", DELETE: "delete",*/ POST: "apiProjectResourcesPost"]
        }
        "/api/$api_version/project/$project/jobs"(controller: 'menu', action: 'apiJobsListv2')
        "/api/$api_version/project/$project/resource/$name"(controller: 'framework',action:"apiResourcev14")
        "/api/$api_version/project/$project/run/command"(controller: 'scheduledExecution', action: 'apiRunCommandv14')
        "/api/$api_version/project/$project/run/script"(controller: 'scheduledExecution', action: 'apiRunScriptv14')
        "/api/$api_version/project/$project/run/url"(controller: 'scheduledExecution', action: 'apiRunScriptUrl14')
        "/api/$api_version/project/$project/history"(controller: 'reports', action: 'apiHistoryv14')

        "/api/$api_version/project/$project/scm/$integration/plugins"(controller: 'scm', action: 'apiPlugins')
        "/api/$api_version/project/$project/scm/$integration/plugin/$type/input"(controller: 'scm', action: 'apiPluginInput')
        "/api/$api_version/project/$project/scm/$integration/plugin/$type/setup"(controller: 'scm', action: 'apiProjectSetup')
        "/api/$api_version/project/$project/scm/$integration/plugin/$type/enable"(controller: 'scm', action: 'apiProjectEnable')
        "/api/$api_version/project/$project/scm/$integration/plugin/$type/disable"(controller: 'scm', action: 'apiProjectDisable')
        "/api/$api_version/project/$project/scm/$integration/status"(controller: 'scm', action: 'apiProjectStatus')
        "/api/$api_version/project/$project/scm/$integration/config"(controller: 'scm', action: 'apiProjectConfig')
        "/api/$api_version/project/$project/scm/$integration/action/$actionId/input"(controller: 'scm', action: 'apiProjectActionInput')
        "/api/$api_version/project/$project/scm/$integration/action/$actionId"(controller: 'scm', action: 'apiProjectActionPerform')

        "/api/$api_version/projects"(controller: 'project'){
            action = [GET: 'apiProjectList', POST:'apiProjectCreate']
        }
        "/api/$api_version/scheduler/jobs"(controller: 'menu', action: 'apiSchedulerListJobs'){
            currentServer=true
        }
        "/api/$api_version/scheduler/server/$uuid/jobs"(controller: 'menu', action: 'apiSchedulerListJobs'){
            currentServer=false
        }
        "/api/$api_version/scheduler/takeover"(controller: 'scheduledExecution', action: 'apiJobClusterTakeoverSchedule')

        //////////
        //BEGIN deprecated as of v14
        "/api/$api_version/executions/running"(controller: 'menu', action: 'apiExecutionsRunning')
        "/api/$api_version/executions"(controller: 'execution', action: 'apiExecutionsQuery')
        "/api/$api_version/jobs"(controller: 'menu', action: 'apiJobsList')
        "/api/$api_version/jobs/export"(controller: 'menu', action: 'apiJobsExport')
        "/api/$api_version/jobs/import"(controller: 'scheduledExecution', action: 'apiJobsImport')
        "/api/$api_version/history"(controller: 'reports', action: 'apiHistory')
        "/api/$api_version/resources"(controller: 'framework', action: 'apiResources')
        "/api/$api_version/resource/$name"(controller: 'framework', action: 'apiResource')
        "/api/$api_version/run/command"(controller: 'scheduledExecution', action: 'apiRunCommand')
        "/api/$api_version/run/script"(controller: 'scheduledExecution', action: 'apiRunScript')
        "/api/$api_version/run/url"(controller: 'scheduledExecution', action: 'apiRunScriptUrl')
        //END deprecated
        ///////////////

        "/api/$api_version/system/info"(controller: 'api', action: 'apiSystemInfo')
        "/api/$api_version/system/logstorage"(controller: 'menu', action: 'apiLogstorageInfo')
        "/api/$api_version/system/logstorage/incomplete/resume"(controller: 'menu', action: 'apiResumeIncompleteLogstorage')
        "/api/$api_version/system/logstorage/incomplete"(controller: 'menu', action: 'apiLogstorageListIncompleteExecutions')
        "/api/$api_version/system/executions/enable"(controller: 'execution', action: 'apiExecutionModeActive')
        "/api/$api_version/system/executions/disable"(controller: 'execution', action: 'apiExecutionModePassive')
        "/api/$api_version/system/acl/$path**"(controller: 'framework',action: 'apiSystemAcls')
        "/api/$api_version/tokens/$user?"(controller: 'api', action: 'apiTokenList')
        "/api/$api_version/token/$token"(controller: 'api', action: 'apiTokenManage')

        "/api/$api_version/storage/keys/$resourcePath**"(controller: 'storage', action: 'apiKeys')
        "/api/$api_version/storage/keys"(controller: 'storage', action: 'apiKeys')

        //incubator endpoints
        "/api/$api_version/incubator/storage/$resourcePath**"(controller: 'storage') {
            action = [
                    GET: "apiGetResource",
                    PUT: "apiPutResource",
                    POST: "apiPostResource",
                    DELETE: "apiDeleteResource"
            ]
        }

        "/api/$api_version/incubator/feature/$featureName?"(controller: 'api',action: 'featureToggle')

        //promoted incubator endpoints
        "/api/$api_version/incubator/jobs/takeoverSchedule"(controller: 'api',action:'endpointMoved'){
            moved_to="/api/${ApiRequestFilters.API_CURRENT_VERSION}/scheduler/takeover"
        }

        //catchall
        "/api/$api_version/$action?"(controller: 'api', action: 'invalid')

        //simplified url mappings for link generation
        "/project/$project/nodes/"(controller: 'framework', action: 'nodes')
        "/project/$project/run/"(controller: 'framework', action: 'nodes')
        "/project/$project/scm/$integration?/$action?"(controller: 'scm')
        "/project/$project/command/run"(controller: 'framework',action: 'adhoc')
        "/project/$project/activity"(controller: 'reports', action: 'index')
        "/project/$project/history"(controller: 'reports', action: 'index')
        "/project/$project/jobs/$groupPath**?"(controller: 'menu', action: 'jobs')
        "/project/$project/job/show/$id/$fullName**?"(controller: 'scheduledExecution', action: 'show')
        "/project/$project/job/upload"(controller: 'scheduledExecution'){
            action = [GET: 'upload', POST: 'uploadPost']
        }
        "/project/$project/job/$id/scm/$integration/$action"(controller: 'scm')
        "/project/$project/job/$action?/$id?"(controller: 'scheduledExecution')
        "/resources/createProject"(controller: 'framework') {
            action = [GET: 'createProject', POST: 'createProjectPost']
        }
        "/resources/$action?/$id?"(controller: 'framework')
        "/project/$project/events/$action?/$id?(.$format)?"(controller: 'reports')
        "/project/$project/configure"(controller: 'menu', action: 'admin')
        "/project/$project/execution/show/$id"(controller: 'execution',action: 'show')
        "/project/$project/execution/$action/$id"(controller: 'execution')
        "/project/$project/exportPrepare"(controller: 'project',action: 'exportPrepare')
        "/project/$project/exportWait"(controller: 'project',action: 'exportWait')
        "/project/$project/export"(controller: 'project',action: 'export')
        "/project/$project/importArchive"(controller: 'project',action: 'importArchive')
        "/project/$project"(redirect:[controller: 'menu',action: 'index'])
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
