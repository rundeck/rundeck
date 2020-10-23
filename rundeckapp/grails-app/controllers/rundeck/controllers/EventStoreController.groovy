package rundeck.controllers

import com.dtolabs.rundeck.core.event.EventQueryResult
import grails.compiler.GrailsCompileStatic
import grails.converters.JSON
import groovy.transform.CompileStatic
import rundeck.services.EventStoreService
import rundeck.services.Evt
import rundeck.services.EvtQuery

@GrailsCompileStatic
class EventStoreController extends ControllerBase {
    EventStoreService eventStoreService

    def queryEvents(QueryEventsDTO q) {
        println(q?.query)
        EventQueryResult result = eventStoreService.query(q.query)

        render(result as JSON)
    }

    def createEvent(CreateEventDto req) {
        eventStoreService.storeEvent(req.event)

        render([ok: '200'])
    }
}

@CompileStatic
class QueryEventsDTO {
    EvtQuery query
}

@CompileStatic
class CreateEventDto {
    Evt event
}