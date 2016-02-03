package rundeck.services


class ConfigurationService {
    static transactional = false
    def grailsApplication

    boolean isExecutionModeActive() {
        getAppConfig()?.executionMode=='active'
    }

    private ConfigObject getAppConfig() {
        grailsApplication.config?.rundeck
    }

    void setExecutionModeActive(boolean active){
        getAppConfig().executionMode=(active?'active':'passive')
    }

    String getCacheSpecFor(String service, String cache, String defval) {
        getAppConfig()?."${service}"?."${cache}"?.spec ?: defval
    }
    boolean getCacheEnabledFor(String service, String cache, boolean defval) {
        getAppConfig()?."${service}"?."${cache}"?.enabled ?: defval
    }
    boolean getIncubatorEnabled(String feature){
        getAppConfig()?.incubator?."${feature}"
    }
}
