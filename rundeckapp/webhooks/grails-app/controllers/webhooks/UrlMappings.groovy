package webhooks

class UrlMappings {

    static mappings = {

<<<<<<< HEAD
        if(getGrailsApplication().config.rundeck.feature.webhooks.enabled == "true") {
            "/api/$api_version/webhook/$authtoken"(controller: 'webhook') {
                action=[POST:"post"]
            }
=======
        if(getGrailsApplication().config.rundeck.feature.webhooks.enabled in ["true",true]) {
            "/api/$api_version/webhook/$authtoken"(controller: 'webhook',action:"post")
>>>>>>> 019a7e6293... Constrain webhook post url to POST http method.

            "/api/$api_version/project/${project}/webhooks"(controller: 'webhook', action: 'list')

            "/api/$api_version/project/${project}/webhook/$id?"(controller: 'webhook') {
                action=[GET:"get",POST:"save",DELETE:"remove"]
            }

            "/webhook/admin"(controller:"webhook",action:"admin")
            "/webhook/admin/editorData/$project"(controller: 'webhook', action: 'editorData')
            "/webhook/admin/save"(controller: 'webhook') {
                action=[POST:"save"]
            }
            "/webhook/admin/delete/$id"(controller: 'webhook') {
                action=[DELETE:"remove"]
            }

        }

    }
}
