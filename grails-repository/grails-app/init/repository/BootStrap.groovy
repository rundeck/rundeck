package repository

class BootStrap {
    RepositoryPluginService repositoryPluginService
    def grailsApplication

    def init = { servletContext ->
        boolean enabled = grailsApplication.config.getProperty("rundeck.feature.repository.enabled",Boolean.class, false)
        boolean sync = grailsApplication.config.getProperty("rundeck.feature.repository.syncOnBootstrap",Boolean.class, false)
        log.debug("Repository enabled: " + enabled)
        if(enabled && sync) {
            log.info("Syncing installed plugins to this server")
            repositoryPluginService.syncInstalledArtifactsToPluginTarget()
        }
    }
    def destroy = {
    }
}
