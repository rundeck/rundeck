package rundeck.services


class ConfigurationService {
    static transactional = false
    def grailsApplication

    boolean isExecutionModeActive() {
        getAppConfig()?.executionMode == 'active'
    }

    public ConfigObject getAppConfig() {
        grailsApplication.config?.rundeck
    }

    void setExecutionModeActive(boolean active) {
        getAppConfig().executionMode = (active ? 'active' : 'passive')
    }
    /**
     * Lookup boolean config value, rundeck.some.property.name, evaluate true/false.
     * @param property property name
     * @param defval default value
     * @return
     */
    String getString(String property, String defval) {
        def strings = property.split('\\.')
        def val = appConfig
        strings.each {
            val = val?."${it}"
        }
        stringValue(defval, val)
    }
    /**
     * Lookup integer config value, rundeck.some.property.name
     * @param property property name
     * @param defval default value
     * @return
     */
    int getInteger(String property, int defval) {
        def strings = property.split('\\.')
        def val = appConfig
        strings.each {
            val = val?."${it}"
        }
        intValue(defval, val)
    }
    /**
     * Lookup Long config value, rundeck.some.property.name
     * @param property property name
     * @param defval default value
     * @return
     */
    long getLong(String property, long defval) {
        def strings = property.split('\\.')
        def val = appConfig
        strings.each {
            val = val?."${it}"
        }
        longValue(defval, val)
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
        strings.each {
            val = val?."${it}"
        }
        booleanValue(defval, val)
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

    private int intValue(int defval, val) {
        if (val instanceof Integer) {
            return val
        } else if (val instanceof Number) {
            return (int) val
        } else if (val instanceof String) {
            return val.toInteger()
        }
        return defval
    }

    private long longValue(long defval, val) {
        if (val instanceof Long) {
            return val
        } else if (val instanceof Number) {
            return (long) val
        } else if (val instanceof String) {
            return val.toInteger()
        }
        return defval
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

    private String stringValue(String defval, val) {
        if (val instanceof ConfigObject) {
            return val.isEmpty() ? defval : val.toString()
        } else if (val instanceof String) {
            return val
        } else {
            return val?.toString() ?: defval
        }
    }

    String getCacheSpecFor(String service, String cache, String defval) {
        getAppConfig()?."${service}"?."${cache}"?.spec ?: defval
    }

    boolean getCacheEnabledFor(String service, String cache, boolean defval) {
        def val = getAppConfig()?."${service}"?."${cache}"?.enabled
        if (null != val) {
            return val in [true, 'true']
        } else {
            return defval
        }
    }
}
