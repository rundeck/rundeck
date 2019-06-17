package rundeck.controllers

import com.dtolabs.rundeck.core.live.LiveEventData
import com.google.common.eventbus.EventBus
import grails.converters.JSON

class LiveController {

    EventBus liveEventBus

    def broadcast() {
        String channel = params.id
        String targetUser = params.to

        liveEventBus.post(new LiveEventData(targetUser,channel,"A Message"))
        render "sent msg to channel: ${params.id}"
    }

    def retrieveConnectTokens() {
        def payload = [:]
        payload.hdr = request._csrf.headerName
        payload.token = request._csrf.token
        render payload as JSON
    }
}
