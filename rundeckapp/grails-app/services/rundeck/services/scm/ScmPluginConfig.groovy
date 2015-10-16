package rundeck.services.scm

/**
 * Wraps stored plugin config data, writes and reads it
 */
class ScmPluginConfig implements ScmPluginConfigData {
    Properties properties
    String prefix

    ScmPluginConfig(final Properties properties, String prefix) {
        this.properties = properties ?: new Properties()
        this.prefix = prefix
    }

    @Override
    String getSetting(String name) {
        properties?.getProperty(prefix + '.' + name)
    }

    @Override
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

    @Override
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

    @Override
    void setSetting(String name, String value) {
        if (value != null) {
            properties?.setProperty(prefix + '.' + name, value)
        } else {
            properties?.remove(prefix + '.' + name)
        }
    }

    @Override
    String getType() {
        getSetting('type')
    }

    @Override
    void setType(String type) {
        setSetting('type', type)
    }

    @Override
    void setEnabled(boolean enabled) {
        setSetting('enabled', Boolean.toString(enabled))
    }

    @Override
    boolean getEnabled() {
        Boolean.parseBoolean(getSetting('enabled'))
    }

    @Override
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
    @Override
    void setConfig(Map config) {
        config.each { setSetting 'config.' + it.key, it.value }
    }


    @Override
    InputStream asInputStream() {
        def baos = new ByteArrayOutputStream()
        properties.store(baos, "stored config")
        new ByteArrayInputStream(baos.toByteArray())
    }

    void load(String prefix, InputStream os) {
        def props = new Properties()
        props.load(os)
        this.properties = props
        this.prefix =  prefix
    }

    static ScmPluginConfig loadFromStream(String prefix, InputStream os) {

        def props = new Properties()
        props.load(os)
        return new ScmPluginConfig(props, prefix)
    }

    @Override
    public String toString() {
        return "ScmPluginConfig{" +
                ", prefix='" + prefix + '\'' +
                ", type='" + getType() + '\'' +
                ", enabled='" + getEnabled() + '\'' +
                ", config='" + getConfig() + '\'' +
                '}';
    }
}
