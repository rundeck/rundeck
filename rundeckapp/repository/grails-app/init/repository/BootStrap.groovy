package repository

class BootStrap {

    def grailsApplication

    def init = { servletContext ->
        log.debug("Repository enabled: " + grailsApplication.config.rundeck.features.repository.enabled)
    }
    def destroy = {
    }
}
