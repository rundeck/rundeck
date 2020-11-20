package rundeck.services

import com.dtolabs.rundeck.core.event.Event
import com.dtolabs.rundeck.core.event.EventImpl
import com.dtolabs.rundeck.core.event.EventQuery
import com.dtolabs.rundeck.core.event.EventQueryImpl
import com.dtolabs.rundeck.core.event.EventQueryResult
import grails.gorm.transactions.Transactional
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

    @Transactional
    void storeEventBatch(List<Event> events) {
        events.each { event ->
            storeEvent(event)
        }
    }

    void storeEvent(Event event) {
        Event scopedEvent = scopeEvent(event)
        println(scopedEvent)
        service.storeEvent(scopedEvent)
    }

    EventQueryResult query(EventQuery query) {
        EventQuery scopedQuery = scopeQuery(query)
        println(scopedQuery.properties)
        service.query(scopedQuery)
    }

    private Event scopeEvent(Event event) {
        println(event)
        if (!this.eventTemplate)
            return event

        return mergeObject(Event, EventImpl, event, eventTemplate) as Event
    }

    private EventQuery scopeQuery(EventQuery query) {
        if (!this.queryTemplate)
            return query

        mergeObject(EventQuery, EventQueryImpl, query, queryTemplate) as EventQuery
    }

    /**
     * This is bananas. B. A. N. A. S.. Bananas.
     * @param inter An interface everything must match
     * @param type The concrete type to be constructed from the merged properties
     * @param a The object with the base set of properties
     * @param b The object with the properties to be merged
     * @return
     */
    private <I, C extends I, A extends I, B extends I> C mergeObject(Class<I> inter, Class<C> type, Object a, Object b) {
        return mergeObject(type, a, b)
    }
    private <C> C mergeObject(Class<C> type, Object a, Object b) {
        Map props =  type.newInstance().properties
        props.remove('class')

        Map eventMap = a.properties.findAll {k, v -> v}
        Map templateMap = b.properties.findAll {k, v -> v}
        Map merged = (eventMap + templateMap).findAll { k, v -> props.containsKey(k) }

        return  type.newInstance(merged) as C
    }
}


