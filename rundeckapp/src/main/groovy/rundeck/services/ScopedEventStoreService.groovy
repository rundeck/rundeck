package rundeck.services

import com.dtolabs.rundeck.core.event.Event
import com.dtolabs.rundeck.core.event.EventQuery
import com.dtolabs.rundeck.core.event.EventQueryResult
import groovy.transform.CompileStatic

@CompileStatic
class ScopedEventStoreService implements com.dtolabs.rundeck.core.event.EventStoreService {
    @Delegate(interfaces = false)
    EventStoreService service

    Event eventTemplate
    EventQuery queryTemplate

    ScopedEventStoreService(EventStoreService service, Event eventTemplate, EventQuery queryTemplate) {
        this.service = service
        this.eventTemplate = eventTemplate
        this.queryTemplate = queryTemplate
    }

    void storeEvent(Event event) {
        Event scopedEvent = scopeEvent(event)
        println(scopedEvent)
        service.storeEvent(scopedEvent)
    }

    EventQueryResult query(EventQuery query) {
        EventQuery scopedQuery = scopeQuery(query)
        service.query(scopedQuery)
    }

    private Event scopeEvent(Event event) {
        println(event)
        if (!this.eventTemplate)
            return event

        (event.properties.findAll {k, v -> v} +
            eventTemplate.properties.findAll {k, v -> v}).findAll {k, v -> k != 'class'} as Evt

    }

    private EventQuery scopeQuery(EventQuery query) {
        if (!this.queryTemplate)
            return query

        (query.properties.findAll {k, v -> v} +
            queryTemplate.properties.findAll {k, v -> v}).findAll {k, v -> k != 'class'} as EvtQuery

    }
}


