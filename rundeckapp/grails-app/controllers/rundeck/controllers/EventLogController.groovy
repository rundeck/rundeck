package rundeck.controllers

import com.fasterxml.jackson.databind.ObjectMapper
import rundeck.services.EventLogService
import rundeck.services.Evt
import rundeck.services.EvtQuery
import rundeck.services.EvtQueryResult

class EventLogController extends ControllerBase {
    EventLogService eventLogService

    def queryEvent(EvtQuery q) {
        EvtQueryResult result = eventLogService.findEvents(q)

        render(result)
    }

    def createEvent(CreateEventDto req) {
        eventLogService.storeEvent(req.event)

        render([ok: '200'])
    }
}

class CreateEventDto {
    Evt event
}