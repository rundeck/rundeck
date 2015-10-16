package rundeck.services.scm

/**
 * Created by greg on 10/15/15.
 */
interface ScmPluginConfigData {
    /**
     *
     * @return integration
     */
    String getPrefix()
    /**
     * get a string value
     * @param name name
     * @return value
     */
    String getSetting(String name)

    /**
     * Get a list value
     * @param name name
     * @return list value
     */
    List<String> getSettingList(String name)

    /**
     * Set a string list
     * @param name name
     * @param value vaue
     */
    void setSetting(String name, List<String> value)

    /**
     * Set a string
     * @param name name
     * @param value value
     */
    void setSetting(String name, String value)

    /**
     * @return the plugin type
     */
    String getType()

    /**
     * set the type
     * @param type
     */
    void setType(String type)

    /**
     * set enabled
     * @param enabled
     */
    void setEnabled(boolean enabled)

    /**
     * @return true if enabled
     */
    boolean getEnabled()

    /**
     *
     * @return config data
     */
    Map getConfig()
    /**
     * Add properties to the config
     * @param config map of config key/value
     */
    void setConfig(Map config)

    /**
     *
     * @return outputstream
     */
    InputStream asInputStream()

    /**
     * Load from input stream
     * @param integration integration
     * @param os stream
     */
    void load(String integration, InputStream os)
}
