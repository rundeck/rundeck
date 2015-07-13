package rundeck.services


class ConfigurationService {
    static transactional = false
    def grailsApplication

    boolean isExecutionModeActive() {
        grailsApplication.config?.rundeck?.executionMode=='active'
    }
    void setExecutionModeActive(boolean active){
        grailsApplication.config.rundeck.executionMode=(active?'active':'passive')
    }
}
