package repository

class BootStrap {
    RepositoryPluginService repositoryPluginService
    def grailsApplication

    def init = { servletContext ->
        boolean enabled = grailsApplication.config.rundeck.feature.repository.enabled in [true, 'true']
        boolean sync = grailsApplication.config.rundeck.feature.repository.syncOnBootstrap in [true,'true']
        log.debug("Repository enabled: " + enabled)
        if(enabled && sync) {
            log.info("Syncing installed plugins to this server")
            repositoryPluginService.syncInstalledArtifactsToPluginTarget()
        }
    }
    def destroy = {
    }
}
