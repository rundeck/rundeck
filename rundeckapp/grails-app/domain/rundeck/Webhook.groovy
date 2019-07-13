package rundeck

class Webhook {

    String name
    String eventPlugin
    String pluginConfigurationJson

    AuthToken authToken

    static constraints = {
        name(matches: '^[a-zA-Z0-9\\.,@\\(\\)_\\\\/-]+$',unique: true)
    }

    static mapping = {
        authToken cascade: 'all-delete-orphan'
        pluginConfigurationJson type: 'text'
    }
}