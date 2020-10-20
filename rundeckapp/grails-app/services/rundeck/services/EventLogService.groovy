package rundeck.services

import com.fasterxml.jackson.databind.ObjectMapper
import grails.compiler.GrailsCompileStatic
import grails.gorm.PagedResultList
import grails.gorm.transactions.Transactional
import grails.validation.Validateable
import groovy.transform.CompileStatic
import org.grails.datastore.mapping.query.api.BuildableCriteria
import org.hibernate.Session
import org.springframework.transaction.annotation.Propagation
import rundeck.*

@GrailsCompileStatic
class EventLogService {
    FrameworkService frameworkService

    void storeEvent(Evt event, boolean transactional = true) {

        ObjectMapper mapper = new ObjectMapper()

        String eventMetaString = mapper.writeValueAsString(event.meta)

        String serverUUID = frameworkService.getServerUUID()

        Event domainEvent = new Event(
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

    @Transactional
    EvtQueryResult findEvents(EvtQuery event) {
        PagedResultList<Event> results

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
    private PagedResultList<Event> queryGeneric(EvtQuery event) {
        BuildableCriteria c = Event.createCriteria()

        c.list (max: event.maxResults, offset: event.offset) {
            if (event.projectName)
                eq('projectName', event.projectName)
            if (event.subsystem)
                eq('subsystem', event.subsystem)

            like('topic', event.topic.replace('*', '%'))

            if (event.dateFrom && event.dateTo)
                between('lastUpdated', event.dateFrom, event.dateTo)
        } as PagedResultList<Event>
    }

    @Transactional
    private PagedResultList<Event> queryObject(EvtQuery event) {
        BuildableCriteria c = Event.createCriteria()

        c.list (max: event.maxResults, offset: event.offset) {
            eq('objectId', event.objectId)

            if (event.dateFrom && event.dateTo)
                between('lastUpdated', event.dateFrom, event.dateTo)

        } as PagedResultList<Event>
    }

    @Transactional
    private saveEventTransactional(Event event) {
        Event.withSession { Session session ->
            session.save(event)
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    private saveEvent(Event event) {
        Event.withSession { Session session ->
            session.save(event)
        }
    }
}

@CompileStatic
class Evt {
    String projectName
    String subsystem
    String topic
    String objectId
    Object meta
}

@CompileStatic
class EvtQuery extends Evt implements Validateable {
    Date dateFrom
    Date dateTo
    Integer maxResults = 20
    Integer offset = 0
}

@CompileStatic
class EvtQueryResult {
    Integer totalCount
    List<Event> events
}