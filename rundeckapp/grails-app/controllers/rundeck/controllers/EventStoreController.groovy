package rundeck.controllers

import com.dtolabs.rundeck.core.event.EventQueryResult
import grails.compiler.GrailsCompileStatic
import grails.converters.JSON
import rundeck.services.EventStoreService
import rundeck.services.Evt
import rundeck.services.EvtQuery
import rundeck.services.EvtQueryResult

@GrailsCompileStatic
class EventStoreController extends ControllerBase {
    EventStoreService eventStoreService

    def queryEvents(QueryEventsDTO q) {
        println(q?.query)
        EventQueryResult result = eventStoreService.findEvents(q.query)

        render(result as JSON)
    }

    def createEvent(CreateEventDto req) {
        eventStoreService.storeEvent(req.event)

        render([ok: '200'])
    }
}

class QueryEventsDTO {
    EvtQuery query
}

class CreateEventDto {
    Evt event
}