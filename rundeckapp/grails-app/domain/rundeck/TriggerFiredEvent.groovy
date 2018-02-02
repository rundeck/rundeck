package rundeck

import com.dtolabs.rundeck.app.support.EmbeddedJsonData

class TriggerFiredEvent implements EmbeddedJsonData {

    Date dateCreated
    Date lastUpdated
    String timeZone
    String conditionData
    String state

    static belongsTo = [triggerRep: TriggerRep]

    static constraints = {
        timeZone(maxSize: 256, blank: true, nullable: true)
        conditionData(nullable: true, blank: true)
        state(maxSize: 256, nullable: true, blank: true)
    }

    static mapping = {
        conditionData(type: 'text')
        state(type: 'string')
    }

    static transients = ['conditionMap']

    public Map getConditionMap() {
        return asJsonMap(conditionData)
    }

    public void setConditionMap(Map obj) {
        conditionData = serializeJsonMap(obj)
    }
}
