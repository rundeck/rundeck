package rundeck

import com.dtolabs.rundeck.core.event.Event
import grails.compiler.GrailsCompileStatic
import org.rundeck.app.data.model.v1.storedevent.EventSeverity

@GrailsCompileStatic
class StoredEvent implements Event {
    String serverUUID
    EventSeverity severity = EventSeverity.INFO
    String projectName
    String subsystem
    String topic
    String objectId
    Long sequence = 0
    Date lastUpdated
    int schemaVersion = 0
    String meta

    static constraints = {
        projectName(maxSize: 255)
        serverUUID(maxSize: 36)
        subsystem(maxSize: 128)
        objectId(nullable: true, maxSize: 64)
        topic(maxSize: 255)
        meta(nullable: true)
    }

    static mapping = {
        projectName index: 'STORED_EVENT_IDX_PROJECT_NAME'
        subsystem index: 'STORED_EVENT_IDX_SUBSYSTEM'
        lastUpdated index: 'STORED_EVENT_IDX_LAST_UPDATED'
        sequence index: 'STORED_EVENT_IDX_SEQUENCE'
        topic index: 'STORED_EVENT_IDX_TOPIC'
        objectId index: 'STORED_EVENT_IDX_OBJECT_ID'

        serverUUID column: 'server_uuid'
        meta type: 'text'
        severity enumType: 'ordinal'
        version false
    }

    StoredEvent() {}

    StoredEvent(
        String serverUUID,
        String projectName,
        String subsystem,
        String topic,
        String objectId,
        Long sequence,
        String event
    ) {
        this.serverUUID = serverUUID
        this.projectName = projectName
        this.subsystem = subsystem
        this.topic = topic
        this.objectId = objectId
        this.sequence = sequence?:0
        this.meta = event
    }
}
