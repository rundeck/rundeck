package rundeck


class Messaging {
    String serverNodeUUID
    Date lastStatus
    Long scheduledExecutionId
    MessageType messageType

    static constraints = {
        scheduledExecutionId(nullable: true)
        lastStatus(nullable: true)
    }
    static mapping = {
        messageType(enumType: "string")
    }

    enum MessageType{
        NODE("NODE"),
        JOBOWNERSHIP("JOBOWNERSHIP")

        String name

        MessageType(String name){
            this.name = name
        }
    }

}
