package rundeck

import com.dtolabs.rundeck.app.support.EmbeddedJsonData
import com.dtolabs.rundeck.core.common.FrameworkResource

class TriggerRep implements EmbeddedJsonData {
    long id
    String project
    String name
    String description
    String uuid
    String serverNodeUuid
    String conditionType
    String conditionData
    String actionType
    String actionData
    String triggerData
    boolean enabled
    Date dateCreated
    Date lastUpdated
    String userCreated
    String userModified
    String authUser
    String authRoleList

    static hasMany = [events: TriggerEvent]
    static constraints = {
        project(nullable: false, blank: false, matches: FrameworkResource.VALID_RESOURCE_NAME_REGEX)
        name(nullable: true)
        description(nullable: true)
        conditionType nullable: false, blank: false
        conditionData(nullable: true, blank: true)
        actionType(nullable: false, blank: false)
        actionData(nullable: true, blank: true)
        triggerData(nullable: true, blank: true)
        userCreated(nullable: false, blank: false)
        userModified(nullable: false, blank: false)
        authUser(nullable: false, blank: false)
        authRoleList(nullable: false, blank: false)
        uuid(size: 36..36, blank: false, nullable: false, validator: { val, obj ->
            try {
                return null != UUID.fromString(val)
            } catch (IllegalArgumentException e) {
                return false
            }
        })
        serverNodeUuid(size: 36..36, blank: true, nullable: true, validator: { val, obj ->
            try {
                return null != UUID.fromString(val)
            } catch (IllegalArgumentException e) {
                return false
            }
        })
    }
    static mapping = {
        actionData(type: 'text')
        conditionData(type: 'text')
        triggerData(type: 'text')
        userCreated(type: 'text')
        userModified(type: 'text')
        authUser(type: 'text')
        authRoleList(type: 'text')
    }
    //ignore fake property 'configuration' and do not store it
    static transients = ['actionConfig', 'conditionConfig', 'userData']

    public Map getActionConfig() {
        return asJsonMap(actionData)
    }

    public void setActionConfig(Map obj) {
        actionData = serializeJsonMap(obj)
    }

    public Map getConditionConfig() {
        return asJsonMap(conditionData)
    }

    public void setConditionConfig(Map obj) {
        conditionData = serializeJsonMap(obj)
    }

    public Map getUserData() {
        return asJsonMap(triggerData)
    }

    public void setUserData(Map obj) {
        triggerData = serializeJsonMap(obj)
    }


}
