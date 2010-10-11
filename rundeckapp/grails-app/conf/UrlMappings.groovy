class UrlMappings {
	static mappings = {
	  "/$controller/$action?/$id?"{
	      constraints {
			 // apply constraints here
		  }
	  }
	  "/job/$action?/$id?"( controller:'scheduledExecution')
	  "/resources/$action?/$id?"( controller:'framework')
	  "/events/$action?/$id?"( controller:'reports')
	  "500"(view:'/error')
	}	
}
