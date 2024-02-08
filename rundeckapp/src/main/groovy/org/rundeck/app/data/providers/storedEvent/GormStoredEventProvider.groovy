package org.rundeck.app.data.providers.storedEvent

import grails.gorm.DetachedCriteria
import org.rundeck.app.data.model.v1.page.Page
import org.rundeck.app.data.model.v1.page.Pageable

import org.rundeck.app.data.model.v1.storedevent.StoredEventData
import org.rundeck.app.data.model.v1.storedevent.StoredEventQuery
import org.rundeck.app.data.providers.v1.storedevent.StoredEventProvider
import rundeck.StoredEvent
import rundeck.data.paging.RdPageable

class GormStoredEventProvider implements StoredEventProvider {
    @Override
    StoredEventData createStoredEvent(String serverUUID, String projectName, String subsystem, String topic, String objectId, Long sequence, String meta) {
        StoredEvent domainEvent = new StoredEvent(
                serverUUID,
                projectName,
                subsystem,
                topic,
                objectId,
                sequence,
                meta)

        domainEvent.save(failOnError: true)
        return domainEvent
    }

    @Override
    Page<StoredEventData> listStoredEvent(StoredEventQuery query) {
        DetachedCriteria<StoredEvent> c = genericCriteria(query)
        def list = c.build {
            order('lastUpdated', 'desc')
            order('sequence', 'desc')
        }.list(max: query.maxResults, offset: query.offset)
        def result = new GormPage<StoredEventData>()
        result.results = list
        result.total = (Long) c.count()
        result.pageable = new RdPageable(max: query.maxResults, offset: query.offset).withOrder('lastUpdated', 'desc')
        return result
    }

    @Override
    Number countStoredEvent(StoredEventQuery query) {
        DetachedCriteria<StoredEvent> c = genericCriteria(query)
        return c.count().longValue()
    }

    @Override
    Number deleteStoredEvent(StoredEventQuery query) {
        return genericCriteria(query).deleteAll().longValue()
    }

    private static DetachedCriteria<StoredEvent> genericCriteria(StoredEventQuery query) {
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

        } as DetachedCriteria<StoredEvent>
    }

    static class GormPage<T> implements Page<T> {
        List<T> results = []
        Long total
        Pageable pageable
    }
}
