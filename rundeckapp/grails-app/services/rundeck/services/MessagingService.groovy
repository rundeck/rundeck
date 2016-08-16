package rundeck.services

import rundeck.Messaging
import groovy.time.TimeCategory
import rundeck.ScheduledExecution


class MessagingService {

    Messaging generateNodeMessage(String nodeuuid) {
        Date now = new Date()
        Messaging msg = Messaging.findOrCreateByServerNodeUUIDAndMessageType(nodeuuid, Messaging.MessageType.NODE);
        msg.lastStatus = now
        if (msg.save()) {
            log.debug("GENERATED MESSAGE for Node ${nodeuuid}")
            return msg
        } else {
            throw new Exception("Failed to save msg for Node ${nodeuuid}")
        }
    }

    Messaging generateJobMessage(ScheduledExecution se){
        println(se.id)
        Messaging msg = Messaging.findOrCreateByScheduledExecutionIdAndMessageType(se.id,Messaging.MessageType.JOBOWNERSHIP)
        msg.serverNodeUUID = se.serverNodeUUID
        msg.lastStatus = new Date()
        if (msg.save()) {
            log.debug("GENERATED MESSAGE for Job ${se.id}")
            return msg
        } else {
            throw new Exception("Failed to save msg for Job ${se.id}")
        }

    }

    List<String> getActiveNodes(String nodeuuid){
        Date now = new Date()

        use(TimeCategory){
            now = now - 30.seconds
        }

        List<String> retList = []
        retList.add(0,nodeuuid) //we add this node as first and default option
        Messaging.findAllByMessageType(Messaging.MessageType.NODE).each{ it ->
            if(it.lastStatus > now && it.serverNodeUUID != nodeuuid) {
                retList.add(it.serverNodeUUID)
            }
        }
        return retList
    }
    List<Messaging> getJobMessages(String nodeuuid){
        Messaging.findAllByMessageTypeAndServerNodeUUID(Messaging.MessageType.JOBOWNERSHIP, nodeuuid)
    }
}
