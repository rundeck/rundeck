package rundeck.services

import com.dtolabs.rundeck.core.event.Event
import com.dtolabs.rundeck.core.event.EventImpl
import com.dtolabs.rundeck.core.event.EventQueryImpl
import com.dtolabs.rundeck.core.event.EventQueryResult
import com.dtolabs.rundeck.core.event.EventQueryResultImpl
import com.dtolabs.rundeck.core.event.EventStoreService
import com.fasterxml.jackson.databind.ObjectMapper
import grails.compiler.GrailsCompileStatic
import grails.gorm.transactions.Transactional
import groovy.transform.CompileStatic
import org.rundeck.app.data.model.v1.page.Page
import org.rundeck.app.data.model.v1.storedevent.StoredEventData
import org.rundeck.app.data.model.v1.storedevent.StoredEventQuery
import org.rundeck.app.data.model.v1.storedevent.StoredEventQueryType
import org.rundeck.app.data.providers.v1.storedevent.StoredEventProvider

@GrailsCompileStatic
class GormEventStoreService implements EventStoreService {
    FrameworkService frameworkService
    StoredEventProvider storedEventProvider

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

        storedEventProvider.createStoredEvent(
                serverUUID,
                event.projectName,
                event.subsystem,
                event.topic,
                event.objectId,
                event.sequence,
                eventMetaString)
    }

    @Transactional
    EventQueryResult query(StoredEventQuery query) {
        switch (query.queryType) {
            case StoredEventQueryType.COUNT:
                long count = storedEventProvider.countStoredEvent(query).longValue()
                return new EvtQueryResult(
                    totalCount: count,
                    events: Collections.emptyList() as List<Event>
                )
            case StoredEventQueryType.SELECT:
                Page<StoredEventData> events = storedEventProvider.listStoredEvent(query)

                return new EvtQueryResult(
                    totalCount: events.total,
                    events: events.results as List<Event>
                )
            case StoredEventQueryType.DELETE:
                long count = storedEventProvider.deleteStoredEvent(query).longValue()
                return new EvtQueryResult(
                    totalCount: count,
                    events: Collections.emptyList() as List<Event>
                )
            default:
                return new EvtQueryResult(totalCount: 0, events: Collections.emptyList() as List<Event>)
        }
    }

    @Transactional
    private Page<StoredEventData> queryGeneric(StoredEventQuery event) {
        storedEventProvider.listStoredEvent(event)
    }

    EventStoreService scoped(Event eventTemplate, StoredEventQuery queryTemplate) {
        return new ScopedEventStoreService(this, eventTemplate, queryTemplate)
    }
}

@CompileStatic
class Evt extends EventImpl {}

@CompileStatic
class EvtQuery extends EventQueryImpl {
    Integer maxResults = 20
    Integer offset = 0
}

@CompileStatic
class EvtQueryResult extends EventQueryResultImpl {}