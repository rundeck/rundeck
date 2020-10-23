package rundeck.services

import com.dtolabs.rundeck.core.event.Event
import com.dtolabs.rundeck.core.event.EventImpl
import com.dtolabs.rundeck.core.event.EventQuery
import com.dtolabs.rundeck.core.event.EventQueryImpl
import com.dtolabs.rundeck.core.event.EventQueryResult
import com.dtolabs.rundeck.core.event.EventQueryResultImpl
import com.dtolabs.rundeck.core.event.EventQueryType
import com.fasterxml.jackson.databind.ObjectMapper
import grails.compiler.GrailsCompileStatic
import grails.gorm.DetachedCriteria
import grails.gorm.PagedResultList
import grails.gorm.transactions.Transactional
import grails.validation.Validateable
import groovy.transform.CompileStatic
import rundeck.StoredEvent


@GrailsCompileStatic
class EventStoreService implements com.dtolabs.rundeck.core.event.EventStoreService {
    FrameworkService frameworkService

    @Transactional
    void storeEventBatch(List<Event> events) {
        events.each { event ->
            storeEvent(event)
        }
    }

    @Transactional
    void storeEvent(Event event) {

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

        domainEvent.save()
    }

    @Transactional
    EventQueryResult query(EventQuery query) {
        DetachedCriteria<StoredEvent> c = genericCriteria(query)

        switch (query.queryType) {
            case EventQueryType.COUNT:
                long count = c.count().longValue()
                return new EvtQueryResult(
                    totalCount: count,
                    events: Collections.emptyList() as List<Event>
                )
            case EventQueryType.SELECT:
                long count = c.count().longValue()
                List<StoredEvent> events = c.list(max: query.maxResults, offset: query.offset)
                return new EvtQueryResult(
                    totalCount: count,
                    events: events
                )
            case EventQueryType.DELETE:
                long count = c.deleteAll().longValue()
                return new EvtQueryResult(
                    totalCount: count,
                    events: Collections.emptyList() as List<Event>
                )
        }
    }

    @Transactional
    private PagedResultList<StoredEvent> queryGeneric(EventQuery event) {
        DetachedCriteria<StoredEvent> c = genericCriteria(event)

        c.list (max: event.maxResults, offset: event.offset) as PagedResultList<StoredEvent>
    }

    private DetachedCriteria<StoredEvent> genericCriteria(EventQuery query) {
        new DetachedCriteria(StoredEvent).build {
            if (query.projectName)
                eq('projectName', query.projectName)

            if (query.subsystem)
                eq('subsystem', query.subsystem)

            if (query.topic)
                like('topic', query.topic.replace('*', '%'))

            if (query.objectId)
                eq('objectId', query.objectId)

            if (query.dateFrom && query.dateTo)
                between('lastUpdated', query.dateFrom, query.dateTo)

            if (query.dateTo && !query.dateFrom)
                le('lastUpdated', query.dateTo)

            if (query.dateFrom && !query.dateTo)
                ge('lastUpdated', query.dateFrom)

            max(query.maxResults)
            if (query.offset)
                offset(query.offset)

            order('lastUpdated', 'desc')
        }
    }
}

@CompileStatic
class Evt extends EventImpl {}

@CompileStatic
class EvtQuery extends EventQueryImpl implements Validateable {
    Integer maxResults = 20
    Integer offset = 0
}

@CompileStatic
class EvtQueryResult extends EventQueryResultImpl {}