package rundeck.services

import com.dtolabs.rundeck.core.event.Event
import com.dtolabs.rundeck.core.event.EventQuery
import com.dtolabs.rundeck.core.event.EventQueryResult
import com.fasterxml.jackson.databind.ObjectMapper
import grails.compiler.GrailsCompileStatic
import grails.gorm.DetachedCriteria
import grails.gorm.PagedResultList
import grails.gorm.transactions.Transactional
import grails.validation.Validateable
import groovy.transform.CompileStatic
import org.grails.datastore.mapping.query.api.BuildableCriteria
import org.hibernate.Session
import org.springframework.transaction.annotation.Propagation
import rundeck.*

@GrailsCompileStatic
class EventStoreService implements com.dtolabs.rundeck.core.event.EventStoreService {
    FrameworkService frameworkService

    void storeEventBatch(List<Event> events, boolean transactional = true) {
        if (transactional)
            saveEventBatchTransactional(events)
        else
            saveEventBatch(events)
    }

    void storeEvent(Event event, boolean transactional = true) {

        ObjectMapper mapper = new ObjectMapper()

        String eventMetaString = mapper.writeValueAsString(event.meta)

        String serverUUID = frameworkService.getServerUUID()

        StoredEvent domainEvent = new StoredEvent(
                serverUUID,
                event.projectName,
                event.subsystem,
                event.topic,
                event.objectId,
                eventMetaString)

        if (transactional)
            saveEventTransactional(domainEvent)
        else
            saveEvent(domainEvent)
    }

    EventQueryResult findEvents(EventQuery event) {
        PagedResultList<StoredEvent> results

        if (event.objectId)
            results = queryObject(event)
        else
            results = queryGeneric(event)

        new EvtQueryResult(
            totalCount: results.totalCount,
            events: new ArrayList<Event>(results)
        )
    }

    @Transactional
    long removeBefore(Date date) {
        new DetachedCriteria(StoredEvent).build {
            lt('lastUpdated', date)
        }.deleteAll()longValue()
    }

    @Transactional
    long removeBetween(Date fromDate, Date toDate) {
        new DetachedCriteria(StoredEvent).build {
            between('lastUpdated', fromDate, toDate)
        }.deleteAll().longValue()
    }

    @Transactional
    private PagedResultList<StoredEvent> queryGeneric(EventQuery event) {
        BuildableCriteria c = StoredEvent.createCriteria()

        c.list (max: event.maxResults, offset: event.offset) {
            if (event.projectName)
                eq('projectName', event.projectName)
            if (event.subsystem)
                eq('subsystem', event.subsystem)

            if (event.topic)
                like('topic', event.topic.replace('*', '%'))

            if (event.dateFrom && event.dateTo)
                between('lastUpdated', event.dateFrom, event.dateTo)
        } as PagedResultList<StoredEvent>
    }

    @Transactional
    private PagedResultList<StoredEvent> queryObject(EventQuery event) {
        BuildableCriteria c = StoredEvent.createCriteria()

        c.list (max: event.maxResults, offset: event.offset) {
            eq('objectId', event.objectId)

            if (event.dateFrom && event.dateTo)
                between('lastUpdated', event.dateFrom, event.dateTo)

        } as PagedResultList<StoredEvent>
    }

    @Transactional
    private saveEventTransactional(StoredEvent event) {
        StoredEvent.withSession { Session session ->
            session.save(event)
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    private saveEvent(StoredEvent event) {
        StoredEvent.withSession { Session session ->
            session.save(event)
        }
    }

    @Transactional
    private saveEventBatchTransactional(List<Event> events) {
        events.each { Event event ->
            storeEvent(event)
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    private saveEventBatch(List<Event> events) {
        events.each { Event event ->
            storeEvent(event)
        }
    }
}

@CompileStatic
class Evt implements Event {
    String projectName
    String subsystem
    String topic
    String objectId
    Object meta
}

@CompileStatic
class EvtQuery extends Evt implements Validateable, EventQuery {
    Date dateFrom
    Date dateTo
    Integer maxResults = 20
    Integer offset = 0
}

@CompileStatic
class EvtQueryResult implements EventQueryResult {
    Integer totalCount
    List<Event> events
}