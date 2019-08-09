package webhooks

class UrlMappings {

    static mappings = {
        "/$controller/$action?/$id?(.$format)?"{
            constraints {
                // apply constraints here
            }
        }

        if(getGrailsApplication().config.rundeck.feature.webhooks.enabled == "true") {
            "/api/$api_version/webhook/$authtoken"(controller: 'webhook') {
                action=[POST:"post"]
            }

            "/api/$api_version/webhook-admin/list/$project"(controller: 'webhook', action: 'list')
            "/api/$api_version/webhook-admin/editorData/$project"(controller: 'webhook', action: 'editorData')

            "/api/$api_version/webhook-admin/save"(controller: 'webhook') {
                action=[POST:"save"]
            }
            "/api/$api_version/webhook-admin/delete/$id"(controller: 'webhook') {
                action=[DELETE:"remove"]
            }
            "/webhook-admin/save"(controller: 'webhook') {
                action=[POST:"save"]
            }
            "/webhook-admin/delete/$id"(controller: 'webhook') {
                action=[DELETE:"remove"]
            }
        }

        "/"(view:"/index")
        "500"(view:'/error')
        "404"(view:'/notFound')
    }
}
