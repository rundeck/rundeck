package verb

class BootStrap {

    def grailsApplication

    def init = { servletContext ->
        log.debug("Verb enabled: " + grailsApplication.config.rundeck.features.verb.enabled)
    }
    def destroy = {
    }
}
