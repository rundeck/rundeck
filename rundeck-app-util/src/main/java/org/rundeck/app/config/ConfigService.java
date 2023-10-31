package org.rundeck.app.config;

import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Configuration provider service
 */
public interface ConfigService {
    /**
     * @return app config map
     */
    Map<String, Object> getAppConfig();

    /**
     * get string config value
     *
     * @param property property name
     * @return value or null
     */
    String getString(String property);

    /**
     * get config map for a path
     * @param path path
     * @return 
     */
    public Map<String, Object> getConfig(String path);

    /**
     * get string config value
     *
     * @param prop   property name
     * @param defval default value
     * @return value or default value
     */
    String getString(SysConfigProp prop, String defval);


    /**
     * get string config value
     *
     * @param property property name
     * @param defval   default value
     * @return value or default value
     */
    String getString(String property, String defval);

    /**
     * Lookup integer config value, rundeck.some.property.name
     *
     * @param prop   prop
     * @param defval default value
     */
    int getInteger(SysConfigProp prop, Integer defval);

    /**
     * Lookup integer config value, rundeck.some.property.name
     *
     * @param property property name
     * @param defval   default value
     */
    int getInteger(String property, Integer defval);

    /**
     * Lookup Long config value, rundeck.some.property.name
     *
     * @param prop   property
     * @param defval default value
     */
    long getLong(SysConfigProp prop, Long defval);

    /**
     * Lookup Long config value, rundeck.some.property.name
     *
     * @param property property name
     * @param defval   default value
     */
    long getLong(String property, Long defval);

    /**
     * Lookup boolean config value, rundeck.some.property.name, evaluate true/false.
     *
     * @param prop   prop
     * @param defval default value
     */
    boolean getBoolean(SysConfigProp prop, boolean defval);

    /**
     * Lookup boolean config value, rundeck.some.property.name, evaluate true/false.
     *
     * @param property property name
     * @param defval   default value
     */
    boolean getBoolean(String property, boolean defval);

    /**
     * Lookup boolean config value, rundeck.service.component.property, evaluate true/false.
     *
     * @param service  service name
     * @param name     component name
     * @param property property name
     * @param defval   default value
     * @return config value
     */
    boolean getBoolean(String service, String name, String property, boolean defval);

    /**
     * Lookup config value, rundeck.some.property.name, provide default value if null
     *
     * @param prop   property name
     * @param defval default value
     */
    Object getValue(SysConfigProp prop, Object defval);

    Object getValue(SysConfigProp prop);


    /**
     * Lookup config value, rundeck.some.property.name, provide default value if null
     *
     * @param property property name
     * @param defval   default value
     */
    Object getValue(String property, Object defval);

    Object getValue(String property);


    /**
     * Lookup a string property and interpret it as a time duration in the form "1d2h3m15s"
     *
     * @param prop   property name
     * @param defval default time duration string to use if unset
     * @param unit   units to return
     * @return duration in the given unit
     */
    long getTimeDuration(SysConfigProp prop, String defval, TimeUnit unit);

    /**
     * Lookup a string property and interpret it as a time duration in the form "1d2h3m15s"
     *
     * @param prop   property name
     * @param defval default time duration string to use if unset
     * @return duration in seconds
     */
    long getTimeDuration(SysConfigProp prop, String defval);

    /**
     * Lookup a string property and interpret it as a time duration in the form "1d2h3m15s"
     *
     * @param prop   property name
     * @param defval default time duration string to use if unset
     * @param unit   units to return
     * @return duration in the given unit
     */
    long getTimeDuration(String prop, String defval, TimeUnit unit);

    /**
     * Lookup a string property and interpret it as a time duration in the form "1d2h3m15s"
     *
     * @param prop   property name
     * @param defval default time duration string to use if unset
     * @return duration in seconds
     */
    long getTimeDuration(String prop, String defval);


    /**
     * Lookup a string property and interpret it as a file size in the form  "###[tgkm][b]" (case insensitive)
     *
     * @param prop   property name
     * @param defval default value to return
     * @return bytes
     */
    long getFileSize(SysConfigProp prop, long defval);

    /**
     * Lookup a string property and interpret it as a file size in the form  "###[tgkm][b]" (case insensitive)
     *
     * @param property property name
     * @param defval   default value to return
     * @return bytes
     */
    long getFileSize(String property, long defval);
}
