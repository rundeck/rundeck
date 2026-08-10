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

    /**
     * Maximum number of ids placed in a single {@code DELETE ... WHERE id IN (...)} statement, and
     * the batch size used when draining without a caller-supplied cap.
     */
    private static final int DELETE_CHUNK_SIZE = 1000

    /**
     * Deletes stored events matching the query.
     *
     * <p><b>Never issues an unbounded DELETE.</b> An unbounded delete on this table took over a
     * minute against a ~479,000 row backlog in production and exceeded the 28-second connection
     * timeout (RUN-4660).
     *
     * <p>Two modes:
     * <ul>
     *   <li>{@code maxResults} set — delete at most that many rows and return. Callers using this
     *       are periodic cleanups; a larger backlog drains across successive scheduled runs.</li>
     *   <li>{@code maxResults} absent — delete everything matching, but in bounded batches, so the
     *       "delete all events for this webhook" semantics are preserved without any single
     *       statement being unbounded.</li>
     * </ul>
     *
     * @param query the query selecting rows to delete
     * @return the number of rows actually deleted
     */
    @Override
    Number deleteStoredEvent(StoredEventQuery query) {
        Integer max = query.maxResults
        if (max != null && max > 0) {
            return deleteBounded(query, max)
        }

        // No cap requested: drain in bounded batches. Loops on rows *deleted*, not ids found, so a
        // batch that selects ids but deletes nothing terminates instead of spinning.
        long total = 0L
        long deleted
        while ((deleted = deleteBounded(query, DELETE_CHUNK_SIZE)) > 0L) {
            total += deleted
        }
        return total
    }

    /**
     * Deletes at most {@code selectMax} matching rows, oldest first.
     *
     * <p>Two deliberate details, both easy to "simplify" back into bugs:
     *
     * <p>1. {@code max} is passed to {@code list()} rather than set on the criteria.
     * {@code DetachedCriteria.max()} returns a <i>new</i> instance and does not mutate the
     * receiver, so the {@code max()} call inside {@link #genericCriteria}'s {@code build {}} block
     * has no effect — its return value is discarded there. {@code listStoredEvent} passes the
     * bounds explicitly for the same reason.
     *
     * <p>2. The bound is expressed as an explicit id list rather than by asking GORM for a limited
     * bulk delete. A bounded bulk {@code DELETE} has no portable SQL form ({@code DELETE ... LIMIT}
     * is MySQL/MariaDB only; PostgreSQL needs a subquery, Oracle {@code ROWNUM}, MSSQL
     * {@code DELETE TOP}), and whether GORM honours a limit on {@code deleteAll()} is undocumented.
     * An id list is standard SQL on every supported database.
     *
     * @param query     the query selecting rows to delete
     * @param selectMax maximum number of rows to delete in this call
     * @return the number of rows actually deleted
     */
    private static long deleteBounded(StoredEventQuery query, int selectMax) {
        List<Long> ids = (List<Long>) genericCriteria(query)
                .build {
                    order('lastUpdated', 'asc')
                    projections {
                        property('id')
                    }
                }
                .list(max: selectMax)

        // Nothing matched: skip the DELETE entirely. The scheduled cleanup runs on every node on
        // every interval regardless of backlog, so an idle run should cost one indexed SELECT and
        // no write transaction at all.
        if (!ids) {
            return 0L
        }

        long deleted = 0L
        ids.collate(DELETE_CHUNK_SIZE).each { List<Long> chunk ->
            deleted += (StoredEvent.where { id in chunk }.deleteAll() as Number).longValue()
        }
        return deleted
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

            if (query.maxResults != null && query.maxResults > 0) {
                max(query.maxResults)
            }
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
