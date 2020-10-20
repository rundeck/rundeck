package rundeck.controllers


import rundeck.services.EventStoreService
import rundeck.services.Evt
import rundeck.services.EvtQuery
import rundeck.services.EvtQueryResult

class EventLogController extends ControllerBase {
    EventStoreService eventStoreService

    def queryEvent(EvtQuery q) {
        EvtQueryResult result = eventStoreService.findEvents(q)

        render(result)
    }

    def createEvent(CreateEventDto req) {
        eventStoreService.storeEvent(req.event)

        render([ok: '200'])
    }
}

class CreateEventDto {
    Evt event
}