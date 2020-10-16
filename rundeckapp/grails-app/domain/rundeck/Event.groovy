package rundeck

import groovy.transform.CompileStatic

class Event {

    @CompileStatic
    enum EventSeverity {
        ERROR(0),
        WARN(1),
        INFO(2),
        DEBUG(3),
        TRACE(4)

        final int id
        private EventSeverity(int id) { this.id = id }
    }

    Long id
    String serverUUID
    EventSeverity severity
    String projectName
    String subsystem
    String topic
    String objectId
    Date lastUpdated
    String event

    Event(
        String serverUUID,
        String projectName,
        String subsystem,
        String topic,
        String objectId,
        String event
    ) {
        this.serverUUID = serverUUID
        this.projectName = projectName
        this.subsystem = subsystem
        this.topic = topic
        this.objectId = objectId
        this.event = event
    }

    static mapping = {
        severity enumType: 'ordinal'
    }

    static constraints = {
        serverUUID(maxSize: 36)
        topic(maxSize: 1024)
        objectId(maxSize: 256)
    }
}
