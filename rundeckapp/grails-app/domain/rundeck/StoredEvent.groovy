package rundeck

import com.dtolabs.rundeck.core.event.Event
import com.fasterxml.jackson.annotation.JsonFormat
import grails.compiler.GrailsCompileStatic
import groovy.transform.CompileStatic
import org.hibernate.validator.constraints.Length

import javax.persistence.*
import javax.validation.constraints.*

@Entity()
@Table(name = "stored_event", indexes = [
    @Index(columnList = "project_name,subsystem,last_updated", name = "events_project_subsystem_idx"),
    @Index(columnList = "project_name"),
    @Index(columnList = "last_updated"),
    @Index(columnList = "object_id")
])
@GrailsCompileStatic
class StoredEvent implements Event {

    @JsonFormat(shape = JsonFormat.Shape.STRING)
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

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id

    @Size(max=50)
    @Column(name = "server_uuid")
    String serverUUID

    @Enumerated(EnumType.ORDINAL)
    EventSeverity severity = EventSeverity.INFO

    @Column(name = "project_name")
    String projectName

    String subsystem

    @Column(length = 1024)
    String topic

    @Column(name = "object_id", length = 512)
    String objectId

    @Column(name = "last_updated")
    Date lastUpdated

    @Column(name = "schema_version")
    int schemaVersion = 0

    @Lob
    String meta

    StoredEvent() {}

    StoredEvent(
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
        this.meta = event
    }

//    static mapping = {
//        severity enumType: 'ordinal'
//    }

//    static constraints = {
//        serverUUID(maxSize: 36)
//        topic(maxSize: 1024)
//        objectId(maxSize: 256)
//    }
}
