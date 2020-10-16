package rundeck.services

import com.fasterxml.jackson.databind.ObjectMapper
import grails.compiler.GrailsCompileStatic
import grails.gorm.PagedResultList
import grails.gorm.transactions.Transactional
import groovy.transform.CompileStatic
import org.grails.datastore.mapping.query.api.BuildableCriteria
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
    PagedResultList<Event> findEvents(EvtQuery event) {
        def c = Event.createCriteria()

        if (event.objectId)
            return queryObject(event)
        else
            return queryGeneric(event)
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
        event.attach()
        event.save()
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    private saveEvent(Event event) {
        event.attach()
        event.save()
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
class EvtQuery extends Evt {
    Date dateFrom
    Date dateTo
    Integer maxResults = 20
    Integer offset = 0
}