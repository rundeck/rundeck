package rundeck.services.scm

/**
 * Wraps stored plugin config data, writes and reads it
 */
class ScmPluginConfig {
    Properties properties
    String integration//export or import
    private String prefix

    ScmPluginConfig(final Properties properties, String integration) {
        this.properties = properties ?: new Properties()
        this.integration = integration
        prefix = 'scm.' + integration
    }

    String getSetting(String name) {
        properties?.getProperty(prefix + '.' + name)
    }

    List<String> getSettingList(String name) {
        def val = getSetting(name + '.count')
        if (val) {
            int size = 0
            try {
                size = Integer.parseInt(val)
            } catch (NumberFormatException e) {
                return null
            }
            def items = []
            def count = 0
            while (count < size) {
                items << getSetting(name + '.' + count)
                count++
            }
            return items
        }
        return null
    }

    void setSetting(String name, List<String> value) {
        if (value != null) {
            setSetting(name + '.count', Integer.toString(value.size()))
            value.eachWithIndex { String entry, int i ->
                setSetting(name + '.' + i, entry)
            }
        } else {
            setSetting(name + '.count', (String) null)
        }
    }

    void setSetting(String name, String value) {
        if (value != null) {
            properties?.setProperty(prefix + '.' + name, value)
        } else {
            properties?.remove(prefix + '.' + name)
        }
    }

    String getType() {
        getSetting('type')
    }

    void setType(String type) {
        setSetting('type', type)
    }

    void setEnabled(boolean enabled) {
        setSetting('enabled', Boolean.toString(enabled))
    }

    boolean getEnabled() {
        Boolean.parseBoolean(getSetting('enabled'))
    }

    Map getConfig() {
        return properties.findAll {
            it.key.startsWith(prefix + '.config.')
        }.collectEntries {
            [it.key.substring((prefix + '.config.').length()), it.value]
        }
    }

    /**
     * Add properties to the config
     * @param config map of config key/value
     */
    void setConfig(Map config) {
        config.each { setSetting 'config.' + it.key, it.value }
    }


    def asInputStream() {
        def baos = new ByteArrayOutputStream()
        properties.store(baos, "scm config")
        new ByteArrayInputStream(baos.toByteArray())
    }

    static ScmPluginConfig loadFromStream(String integration, InputStream os) {

        def props = new Properties()
        props.load(os)
        return new ScmPluginConfig(props, integration)
    }

    @Override
    public String toString() {
        return "ScmPluginConfig{" +
                "integration='" + integration + '\'' +
                ", prefix='" + prefix + '\'' +
                ", type='" + getType() + '\'' +
                ", enabled='" + getEnabled() + '\'' +
                ", config='" + getConfig() + '\'' +
                '}';
    }
}
