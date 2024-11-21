package com.rundeck.plugins.migwiz


import grails.plugins.Plugin

class MigrationWizardGrailsPlugin extends Plugin {

    // the version or versions of Grails the plugin is designed for
    def grailsVersion = "3.3.11 > *"
    // resources that are excluded from plugin packaging
    def pluginExcludes = [
            "grails-app/views/error.gsp"
    ]

    def title = "Migration Wizard Plugin" // Headline display name of the plugin
    def author = "Alberto Hormazabal"
    def authorEmail = "alberto@rundeck.com"
    def description = '''Take your existing projects to the cloud!'''

    def profiles = ['web']
    def loadAfter = ['databaseMigration']
    // URL to the plugin's documentation
    def documentation = "http://docs.rundeck.com"

    Closure doWithSpring() {
        { ->
            // Create beans
            migWizUIPlugin(MigWizUIPluginFactory) {
                pluginRegistry = ref('rundeckPluginRegistry')
            }

            migrationWizardService(MigrationWizardService)
        }
    }

    void doWithDynamicMethods() {
        // TODO Implement registering dynamic methods to classes (optional)
    }

//    @Override
//    void doWithApplicationContext() {
//        def updateOnStart = config.getProperty("grails.plugin.databasemigration.updateOnStart", Boolean, false)
//        if(updateOnStart) {
//            new DatabaseMigrationTransactionManager(applicationContext, 'dataSource').withTransaction {
//                GrailsLiquibase gl = new GrailsLiquibase(applicationContext)
//                gl.dataSource = applicationContext.getBean('dataSource', DataSource)
//                gl.changeLog = config.getProperty("grails.plugin.migration.migwiz.changelog", String)
//                gl.dataSourceName = 'dataSource'
//                gl.afterPropertiesSet()
//            }
//        }
//    }

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
