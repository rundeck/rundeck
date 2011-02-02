class UrlMappings {
	static mappings = {
	  "/$controller/$action?/$id?"{
	      constraints {
			 // apply constraints here
		  }
	  }
      //1.2 api url paths
        "/api/jobs" (controller: 'menu', action: 'apiJobsList')
        "/api/jobs/export" (controller: 'menu', action: 'apiJobsExport')
        "/api/jobs/import" (controller: 'scheduledExecution', action: 'upload')

        "/api/job/$id/run" (controller: 'scheduledExecution', action: 'runJobByName')
        "/api/job/$id" (controller: 'scheduledExecution', action: 'show')

        "/api/executions/running" (controller: 'menu', action: 'nowRunning')

        "/api/execution/$id" (controller: 'execution', action: 'show')
        "/api/execution/$id/abort" (controller: 'execution', action: 'kill')

        "/api/run/command" (controller: 'scheduledExecution', action: 'runAndForget')
        "/api/run/script" (controller: 'scheduledExecution', action: 'runAndForget')

        "/api/projects" (controller: 'framework', action: 'listProjects')
        "/api/project/$project?" (controller: 'framework', action: 'getProject')
        "/api/renderError" (controller: 'api', action: 'renderError')
        "/api/$action?" (controller: 'api', action: 'invalid')

	  "/run/$id?"( controller:'framework',action:'nodes')
	  "/history/$id?"( controller:'reports',action:'index')
	  "/jobs/$id?"( controller:'menu',action:'jobs')
	  "/job/$action?/$id?"( controller:'scheduledExecution')
	  "/resources/$action?/$id?"( controller:'framework')
	  "/events/$action?/$id?"( controller:'reports')
	  "500"(view:'/error')
	}	
}
