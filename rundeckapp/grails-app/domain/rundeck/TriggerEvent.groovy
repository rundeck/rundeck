package rundeck

import com.dtolabs.rundeck.app.support.EmbeddedJsonData

class TriggerEvent implements EmbeddedJsonData {

    Date dateCreated
    Date lastUpdated
    String timeZone
    String eventData
    String eventType
    String associatedType
    String associatedId
    TriggerEvent associatedEvent

    static belongsTo = [triggerRep: TriggerRep]

    static constraints = {
        timeZone(maxSize: 256, blank: true, nullable: true)
        eventData(nullable: true, blank: true)
        eventType(maxSize: 256, nullable: true, blank: true)
        associatedType(maxSize: 256, nullable: true, blank: true)
        associatedId(maxSize: 256, nullable: true, blank: true)
        associatedEvent(nullable: true, blank: true)
    }

    static mapping = {
        eventData(type: 'text')
        eventType(type: 'string')
        associatedType(type: 'string')
        associatedId(type: 'string')
    }

    static transients = ['eventDataMap']

    Map getEventDataMap() {
        return asJsonMap(eventData)
    }

    void setEventDataMap(Map obj) {
        eventData = serializeJsonMap(obj)
    }
}
