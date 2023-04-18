package webhooks

class UrlMappings {

    static mappings = {

        if(getGrailsApplication().config.getProperty("rundeck.feature.webhooks.enabled", Boolean.class, false)) {
            "/api/$api_version/webhook/$authtoken"(controller: 'webhook',action:"post")

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
