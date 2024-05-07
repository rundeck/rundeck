package com.rundeck.plugins.killhandler

import grails.plugins.*

class JobKillHandlerGrailsPlugin extends Plugin {

    // the version or versions of Grails the plugin is designed for
    def grailsVersion = "3.3.8 > *"
    // resources that are excluded from plugin packaging
    def pluginExcludes = [
            "grails-app/views/error.gsp"
    ]

    // TODO Fill in these fields
    def title = "Job Kill Handler" // Headline display name of the plugin
    def author = "Your name"
    def authorEmail = ""
    def description = '''\
Brief summary/description of the plugin.
'''
    def profiles = ['web']

    Closure doWithSpring() {
        { ->

            rundeckAddOn_KillHandlerProcessTrackingService(KillHandlerProcessTrackingService) {

            }

            rundeckAddOn_KillHandlerExecutionLifecyclePlugin(KillHandlerExecutionLifecyclePluginFactory) {
                pluginRegistry = ref('rundeckPluginRegistry')
            }
            rundeckAddOn_KillHandlerLogFilter(KillHandlerLogFilterPluginFactory) {
                pluginRegistry = ref('rundeckPluginRegistry')
            }
        }
    }

}
