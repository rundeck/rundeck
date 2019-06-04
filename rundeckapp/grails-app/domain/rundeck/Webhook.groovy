package rundeck

class Webhook {

    static constraints = {
        name(matches: '^[a-zA-Z0-9\\.,@\\(\\)_\\\\/-]+$',unique: true)
    }

    String name
    String eventPlugin
    String pluginConfigurationJson
}
