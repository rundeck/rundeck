package rundeck.services

import com.dtolabs.rundeck.core.event.Event
import com.dtolabs.rundeck.core.event.EventImpl
import com.dtolabs.rundeck.core.event.EventQueryImpl
import com.dtolabs.rundeck.core.event.EventQueryResult
import com.dtolabs.rundeck.core.event.EventStoreService
import grails.gorm.transactions.Transactional
import groovy.transform.CompileStatic
import org.rundeck.app.data.model.v1.storedevent.StoredEventQuery

@CompileStatic
class ScopedEventStoreService implements EventStoreService {
    EventStoreService service

    Event eventTemplate
    StoredEventQuery queryTemplate

    ScopedEventStoreService(EventStoreService service, Event eventTemplate, StoredEventQuery queryTemplate) {
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
        service.storeEvent(scopedEvent)
    }

    EventQueryResult query(StoredEventQuery query) {
        StoredEventQuery scopedQuery = scopeQuery(query)
        service.query(scopedQuery)
    }

    private Event scopeEvent(Event event) {
        if (!this.eventTemplate)
            return event

        return mergeObject(Event, EventImpl, event, eventTemplate) as Event
    }

    private StoredEventQuery scopeQuery(StoredEventQuery query) {
        if (!this.queryTemplate)
            return query

        mergeObject(StoredEventQuery, EventQueryImpl, query, queryTemplate) as StoredEventQuery
    }

    /**
     * More restrictive mergeObject where objects must all implement the given interface
     * This is bananas. B. A. N. A. S.. Bananas.
     * @param inter An interface everything must match
     * @param type The concrete type to be constructed from the merged properties
     * @param a The object with the base set of properties
     * @param b The object with the properties to be merged
     * @return
     */
    private <I, C extends I, A extends I, B extends I> C mergeObject(Class<I> inter, Class<C> type,A a,B b) {
        return mergeObject(type, a, b)
    }

    /**
     * Constructs a new instance of the supplied class with the merged properties of the supplied objects.
     * The supplied class must have a map constructor.
     * @param type The class to be constructed from merged properties
     * @param a The object with the base set of properties
     * @param b The object with the properties to be merged
     * @return
     */
    private <C> C mergeObject(Class<C> type, Object a, Object b) {
        Map props =  type.newInstance().properties
        props.remove('class')

        Map eventMap = a.properties.findAll {k, v -> v}
        Map templateMap = b.properties.findAll {k, v -> v}
        Map merged = (eventMap + templateMap).findAll { k, v -> props.containsKey(k) }

        return  type.newInstance(merged) as C
    }

    EventStoreService scoped(Event eventTemplate, StoredEventQuery queryTemplate) {
        return new ScopedEventStoreService(this, eventTemplate, queryTemplate)
    }
}


