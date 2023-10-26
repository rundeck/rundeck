package rundeck.data.validation.shared

import grails.validation.Validateable

class SharedServerNodeUuidConstraints implements Validateable {
    String serverNodeUUID

    static constraints = {
        serverNodeUUID(maxSize: 36, size:36..36, blank: true, nullable: true, validator: { val, obj ->
            if (null == val) return true;
            try { return null!= UUID.fromString(val) } catch (IllegalArgumentException e) {
                return false
            }
        })
    }
}
