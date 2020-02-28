package webhooks

import grails.plugins.*
import grails.util.Environment
import webhooks.component.project.WebhooksProjectComponent
import webhooks.exporter.WebhooksProjectExporter
import webhooks.importer.WebhooksProjectImporter
import webhooks.menu.WebhooksMenuItem
import webhooks.plugins.JobRunWebhookEventPlugin
import webhooks.plugins.LogWebhookEventPlugin
import webhooks.plugins.PluginFactoryBean

class WebhooksGrailsPlugin extends Plugin {

    // the version or versions of Grails the plugin is designed for
    def grailsVersion = "3.3.8 > *"
    // resources that are excluded from plugin packaging
    def pluginExcludes = [
        "grails-app/views/error.gsp"
    ]

    // TODO Fill in these fields
    def title = "Webhooks" // Headline display name of the plugin
    def author = "Your name"
    def authorEmail = ""
    def description = '''\
Brief summary/description of the plugin.
'''
    def profiles = ['web']

    // URL to the plugin's documentation
    def documentation = "http://grails.org/plugin/webhooks"

    // Extra (optional) plugin metadata

    // License: one of 'APACHE', 'GPL2', 'GPL3'
//    def license = "APACHE"

    // Details of company behind the plugin (if there is one)
//    def organization = [ name: "My Company", url: "http://www.my-company.com/" ]

    // Any additional developers beyond the author specified above.
//    def developers = [ [ name: "Joe Bloggs", email: "joe@bloggs.net" ]]

    // Location of the plugin's issue tracker.
//    def issueManagement = [ system: "JIRA", url: "http://jira.grails.org/browse/GPMYPLUGIN" ]

    // Online location of the plugin's browseable source code.
//    def scm = [ url: "http://svn.codehaus.org/grails-plugins/" ]

    Closure doWithSpring() { {->

        if(application.config.rundeck.feature.webhooks.enabled in ["true",true]) {
            webhooksMenuItem(WebhooksMenuItem) {
                frameworkService =  ref("frameworkService")
            }

            [
                LogWebhookEventPlugin,
                JobRunWebhookEventPlugin
            ].each { type ->
                "rundeckAppPlugin_${type.simpleName}"(PluginFactoryBean, type)
            }
            webhooksProjectExporter(WebhooksProjectExporter) {
                webhookService = ref('webhookService')
            }
            webhooksProjectImporter(WebhooksProjectImporter) {
                webhookService = ref('webhookService')
            }
            webhooksProjectComponent(WebhooksProjectComponent)
        }
    }}

    void doWithDynamicMethods() {
        // TODO Implement registering dynamic methods to classes (optional)
    }

    void doWithApplicationContext() {
        // TODO Implement post initialization spring config (optional)
    }

    void onChange(Map<String, Object> event) {
        // TODO Implement code that is executed when any artefact that this plugin is
        // watching is modified and reloaded. The event contains: event.source,
        // event.application, event.manager, event.ctx, and event.plugin.
    }

    void onConfigChange(Map<String, Object> event) {
        // TODO Implement code that is executed when the project configuration changes.
        // The event is the same as for 'onChange'.
    }

    void onShutdown(Map<String, Object> event) {
        // TODO Implement code that is executed when the application shuts down (optional)
    }
}
