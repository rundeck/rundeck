package rundeck.services


class ConfigurationService {
    static transactional = false
    def grailsApplication

    boolean isPassiveModeEnabled() {
        grailsApplication.config?.rundeck?.passiveMode?.enabled
    }
    void setPassiveModeEnabled(boolean enabled){
        grailsApplication.config.rundeck.passiveMode.enabled=enabled
    }
}
