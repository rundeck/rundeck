/*
 * Copyright 2016 SimplifyOps, Inc. (http://simplifyops.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package rundeck.services

import com.dtolabs.rundeck.core.config.RundeckConfigBase
import org.grails.config.NavigableMap
import org.rundeck.util.Sizes
import org.springframework.beans.factory.InitializingBean

import java.util.concurrent.TimeUnit

class ConfigurationService implements InitializingBean {
    static transactional = false
    def grailsApplication
    private org.grails.config.NavigableMap appCfg = new org.grails.config.NavigableMap()

    boolean isExecutionModeActive() {
        getAppConfig()?.executionMode == 'active'
    }

    public org.grails.config.NavigableMap getAppConfig() {
        return appCfg
    }

    public void setAppConfig(org.grails.config.NavigableMap configMap) {
        this.appCfg = configMap
    }

    public org.grails.config.NavigableMap getConfig(String path){
        return getValue(path);
    }

    void setExecutionModeActive(boolean active) {
        getAppConfig().executionMode = (active ? 'active' : 'passive')
    }
    String getString(String property) {
        getString(property,null)
    }
    /**
     * Lookup boolean config value, rundeck.some.property.name, evaluate true/false.
     * @param property property name
     * @param defval default value
     * @return
     */
    String getString(String property, String defval) {
        def val = getValue(property)
        stringValue(defval, val)
    }
    /**
     * Lookup integer config value, rundeck.some.property.name
     * @param property property name
     * @param defval default value
     * @return
     */
    int getInteger(String property, int defval) {
        def val = getValue(property)
        intValue(defval, val)
    }
    /**
     * Lookup Long config value, rundeck.some.property.name
     * @param property property name
     * @param defval default value
     * @return
     */
    long getLong(String property, long defval) {
        def val = getValue(property)
        longValue(defval, val)
    }
    /**
     * Lookup boolean config value, rundeck.some.property.name, evaluate true/false.
     * @param property property name
     * @param defval default value
     * @return
     */
    boolean getBoolean(String property, boolean defval) {
        def val = getValue(property)
        booleanValue(defval, val)
    }
    /**
     * Set boolean config value, rundeck.some.property.name, to true/false.
     * @param property property name
     * @param val value to set
     */
    def setBoolean(String property, boolean val) {
        def strings = property.split('\\.')
        def cval = appConfig
        if(strings.length>1) {
            strings[0..-2].each {
                cval = cval.getAt(it)
            }
        }
        cval.putAt(strings[-1],val)
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

    /**
     * Lookup config value, rundeck.some.property.name, provide default value if null
     * @param property property name
     * @param defval default value
     * @return
     */
    Object getValue(String property, Object defval = null) {
        def val = getValueFromRoot(property,appCfg)
        if((val == null || isEmptyNavigableMap(val)) && RundeckConfigBase.DEPRECATED_PROPS.containsKey(property)) {
            //try to get the value from the deprecated property
            val = getDeprecatedPropertyValue(property)
            if(val) {
                //if the value exists warn the user to update their config to use the new property name
                log.warn("Property '${RundeckConfigBase.DEPRECATED_PROPS[property]}' has been deprecated. Please update your config to use: '${property}'")
            }
        }

        return val == null ? defval : val
    }

    protected boolean isEmptyNavigableMap(def val) {
        return val instanceof NavigableMap.NullSafeNavigator && val.isEmpty()
    }

    protected def getValueFromRoot(String property, def root) {
        try {
            def strings = property.split('\\.')
            def val = root
            strings.each {
                val = val?."${it}"
            }
            return val
        } catch(Exception ex) {
            log.warn("Could not get value for property: ${property}")
        }
        return null
    }

    protected def getDeprecatedPropertyValue(property) {
        return getValueFromRoot(RundeckConfigBase.DEPRECATED_PROPS[property], grailsApplication.config.rundeck)
    }
    /**
     * Lookup a string property and interpret it as a time duration in the form "1d2h3m15s"
     * @param property property name
     * @param defval default time duration string to use if unset
     * @param unit units to return
     * @return duration in the given unit
     */
    long getTimeDuration(String property, String defval, TimeUnit unit = TimeUnit.SECONDS) {
        def confStr = getString(property, defval)
        if (!Sizes.validTimeDuration(confStr)) {
            log.warn(
                    "Invalid configuration for ${property}: " + confStr + ", using ${defval}"
            )
            confStr = defval
        }
        Sizes.parseTimeDuration(confStr, unit)
    }

    /**
     * Lookup a string property and interpret it as a file size in the form  "###[tgkm][b]" (case insensitive)
     * @param property property name
     * @param defval default value to return
     * @return bytes
     */
    long getFileSize(String property, long defval) {
        def confStr = getString(property, null)
        confStr ? (Sizes.parseFileSize(confStr) ?: defval) : defval
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

    @Override
    void afterPropertiesSet() throws Exception {
        appCfg = grailsApplication?.config?.rundeck
    }

}
