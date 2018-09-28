package verb

class BootStrap {

    def grailsApplication

    def init = { servletContext ->
        println "Basic bootstrap. I'm a verb."
        println "Verb enabled: " + grailsApplication.config.rundeck.features.verb.enabled

    }
    def destroy = {
    }
}
