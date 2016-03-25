package rundeck.services


class ConfigurationService {
    static transactional = false
    def grailsApplication

    boolean isExecutionModeActive() {
        getAppConfig()?.executionMode=='active'
    }

    public ConfigObject getAppConfig() {
        grailsApplication.config?.rundeck
    }

    void setExecutionModeActive(boolean active){
        getAppConfig().executionMode=(active?'active':'passive')
    }
    /**
     * Lookup boolean config value, rundeck.some.property.name, evaluate true/false.
     * @param property property name
     * @param defval default value
     * @return
     */
    boolean getBoolean(String property, boolean defval) {
        def strings = property.split('\\.')
        def val = appConfig
        strings.each{
            val = val?."${it}"
        }
        booleanValue(defval,val)
    }
    /**
     * Lookup boolean config value, rundeck.service.component.property, evaluate true/false.
     * @param service service name
     * @param name component name
     * @param property property name
     * @param defval default value
     * @return
     */
    boolean getBoolean(String service, String name, String property, boolean defval) {
        def val = appConfig."${service}"?."${name}"?."${property}"
        return booleanValue(defval, val)
    }

    private boolean booleanValue(boolean defval, val) {
        if (defval) {
            //not found implies true
            return !(val in [false, 'false'])
        } else {
            //not found implies false
            return val in [true, 'true']
        }
    }

    String getCacheSpecFor(String service, String cache, String defval) {
        getAppConfig()?."${service}"?."${cache}"?.spec ?: defval
    }
    boolean getCacheEnabledFor(String service, String cache, boolean defval) {
        def val=getAppConfig()?."${service}"?."${cache}"?.enabled
        if(null!=val){
            return val in [true,'true']
        }else{
            return defval
        }
    }
}
