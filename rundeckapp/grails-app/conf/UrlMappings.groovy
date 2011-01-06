class UrlMappings {
	static mappings = {
	  "/$controller/$action?/$id?"{
	      constraints {
			 // apply constraints here
		  }
	  }
	  "/run/$id?"( controller:'framework',action:'nodes')
	  "/history/$id?"( controller:'reports',action:'index')
	  "/jobs/$id?"( controller:'menu',action:'jobs')
	  "/job/$action?/$id?"( controller:'scheduledExecution')
	  "/resources/$action?/$id?"( controller:'framework')
	  "/events/$action?/$id?"( controller:'reports')
	  "500"(view:'/error')
	}	
}
