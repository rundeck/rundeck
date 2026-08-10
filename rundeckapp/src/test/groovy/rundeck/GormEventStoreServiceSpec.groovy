package rundeck

import com.fasterxml.jackson.databind.ObjectMapper
import grails.gorm.transactions.Rollback
import grails.testing.gorm.DataTest
import org.rundeck.app.data.model.v1.storedevent.StoredEventQueryType
import org.rundeck.app.data.providers.storedEvent.GormStoredEventProvider
import rundeck.services.Evt
import rundeck.services.EvtQuery
import rundeck.services.FrameworkService
import rundeck.services.GormEventStoreService
import spock.lang.Shared
import spock.lang.Specification

class GormEventStoreServiceSpec extends Specification implements DataTest {
    @Shared GormEventStoreService service
    @Shared FrameworkService framework

    private static class Foo {
        String event
    }

    def setupSpec() {
        mockDomain StoredEvent
        framework = Mock(FrameworkService) {
            it.serverUUID >> '16b02806-f4b3-4628-9d9c-2dd2cc67d53c'
        }
        service = new GormEventStoreService()
        service.storedEventProvider = new GormStoredEventProvider()
        service.frameworkService = framework
    }

    def setup() {}

    @Rollback
    def "test basic store and querys"() {
        def event = new Foo(event: 'test')

        when:
        service.storeEvent(new Evt(
                projectName: 'test',
                subsystem: 'test',
                topic: 'test',
                meta: event
        ))

        def res  = service.query(new EvtQuery(
                projectName: 'test'
        ))

        def storedEvent = new ObjectMapper().readValue(res.events[0].meta as String, Foo.class)

        then:
        storedEvent.event == 'test'
        res.events.size() == 1
        res.totalCount == 1
    }

    @Rollback
    def "test queries"() {
        when:
        service.storeEventBatch([
                [projectName: 'A', topic: 'test', subsystem: 'webhooks'] as Evt,
                [projectName: 'A', topic: 'test', subsystem: 'cluster'] as Evt,
                [projectName: 'B', topic: 'test', subsystem: 'cluster'] as Evt,
        ])

        def oneRes = service.query([projectName: 'A', subsystem: 'webhooks'] as EvtQuery)
        def twoRes = service.query([projectName: 'A'] as EvtQuery)
        then:
        oneRes.totalCount == 1
        twoRes.totalCount == 2
    }

    @Rollback
    def "test count query does not include events in results"() {
        when:
        service.storeEventBatch([
                [projectName: 'A', topic: 'test', subsystem: 'webhooks'] as Evt,
                [projectName: 'A', topic: 'test', subsystem: 'cluster'] as Evt,
                [projectName: 'B', topic: 'test', subsystem: 'cluster'] as Evt,
        ])

        def oneRes = service.query([projectName: 'A', subsystem: 'webhooks', queryType: StoredEventQueryType.COUNT] as EvtQuery)
        def twoRes = service.query([projectName: 'A', queryType: StoredEventQueryType.COUNT] as EvtQuery)
        then:
        oneRes.totalCount == 1
        oneRes.events.size() == 0
        twoRes.totalCount == 2
        twoRes.events.size() == 0
    }

    @Rollback
    def "test scoped service scopes store calls"() {
        when:
        def scoped = service.scoped([projectName: 'C'] as Evt, null)

        scoped.storeEventBatch([
                [projectName: 'A', topic: 'test', subsystem: 'test'] as Evt,
                [projectName: 'A', topic: 'test', subsystem: 'test'] as Evt,
                [projectName: 'B', topic: 'test', subsystem: 'test'] as Evt,
        ])

        def scopedRes = scoped.query([projectName: 'C'] as EvtQuery)
        def unscopedRes = scoped.query([projectName: 'A'] as EvtQuery)
        then:
        scopedRes.totalCount == 3
        unscopedRes.totalCount == 0
    }

    @Rollback
    def "test scoped service scopes queries"() {
        when:
        service.storeEventBatch([
                [projectName: 'A', topic: 'test', subsystem: 'test'] as Evt,
                [projectName: 'A', topic: 'test', subsystem: 'test'] as Evt,
                [projectName: 'B', topic: 'test', subsystem: 'test'] as Evt,
        ])

        // Scopes queries to project B
        def scoped = service.scoped(null, [projectName: 'B'] as EvtQuery)

        def scopedRes = scoped.query([projectName: 'A'] as EvtQuery)
        def unscopedRes = service.query([projectName: 'A'] as EvtQuery)
        then:
        scopedRes.totalCount == 1
        unscopedRes.totalCount == 2
    }

    @Rollback
    def "test wildcard topic query"() {
        when:
        service.storeEventBatch([
                [topic: 'foo/bar', projectName: 'test', subsystem: 'test'] as Evt,
                [topic: 'foo/bar/baz', projectName: 'test', subsystem: 'test'] as Evt,
                [topic: 'foo/baz',projectName: 'test', subsystem: 'test'] as Evt,
        ])


        def threeRes = service.query([topic: 'foo/*'] as EvtQuery)
        def twoRes = service.query([topic: 'foo/bar*'] as EvtQuery)
        then:
        threeRes.totalCount == 3
        twoRes.totalCount == 2
    }

    @Rollback
    def "test pagination"() {
        when:
        service.storeEventBatch([
                [projectName: 'test', topic: 'test', subsystem: 'test', meta: "ONE", sequence: 0] as Evt,
                [projectName: 'test', topic: 'test', subsystem: 'test', meta: "TWO", sequence: 1] as Evt,
                [projectName: 'test', topic: 'test', subsystem: 'test', meta: "THREE", sequence: 2] as Evt,
        ])

        def oneRes = service.query([maxResults: 1, offset: 0] as EvtQuery)
        def twoRes = service.query([maxResults: 2, offset: 1] as EvtQuery)
        then: 'events are returned newest first and paginated'
        oneRes.events.size() == 1
        oneRes.events[0].meta == '"THREE"'
        twoRes.events[1].meta == '"ONE"'
    }

    @Rollback
    def "test delete"() {
        when:
        service.storeEventBatch([
                [projectName: 'A', topic: 'test', subsystem: 'test'] as Evt,
                [projectName: 'A', topic: 'test', subsystem: 'test'] as Evt,
                [projectName: 'B', topic: 'test', subsystem: 'test'] as Evt,
        ])

        def threeRes = service.query([:] as EvtQuery)

        then:
        threeRes.totalCount == 3

        when:
        def delRes = service.query([projectName: 'A', queryType: StoredEventQueryType.DELETE] as EvtQuery)
        def oneRes = service.query([:] as EvtQuery)

        then:
        delRes.totalCount == 2
        oneRes.totalCount == 1
    }

    @Rollback
    def "test delete limit"() {
        when:
        // subsystem and topic are non-nullable; the original disabled version of this test omitted
        // them, which is part of why it no longer compiled or ran.
        service.storeEventBatch([
                [projectName: 'A', topic: 'test', subsystem: 'test'] as Evt,
                [projectName: 'A', topic: 'test', subsystem: 'test'] as Evt,
                [projectName: 'B', topic: 'test', subsystem: 'test'] as Evt,
        ])

        def delRes = service.query([projectName: 'A', maxResults: 1, queryType: StoredEventQueryType.DELETE] as EvtQuery)
        def twoRes = service.query([:] as EvtQuery)

        then: 'only maxResults rows are deleted, leaving one A and one B'
        delRes.totalCount == 1
        twoRes.totalCount == 2
    }

    @Rollback
    def "delete respects maxResults across a larger backlog"() {
        given:
        service.storeEventBatch((1..25).collect {
            [projectName: 'A', topic: 'test', subsystem: 'webhooks'] as Evt
        })

        when: 'a single capped invocation'
        def delRes = service.query(
                [subsystem: 'webhooks', maxResults: 10, queryType: StoredEventQueryType.DELETE] as EvtQuery
        )

        then: 'exactly maxResults deleted -- one batch per invocation, no internal draining'
        delRes.totalCount == 10
        service.query([:] as EvtQuery).totalCount == 15
    }

    @Rollback
    def "delete without maxResults still removes everything matching"() {
        given:
        service.storeEventBatch((1..12).collect {
            [projectName: 'A', topic: 'test', subsystem: 'webhooks'] as Evt
        } + [[projectName: 'B', topic: 'test', subsystem: 'other'] as Evt])

        when: 'no cap -- the WebhookService.deleteEvents semantics'
        def delRes = service.query(
                [subsystem: 'webhooks', queryType: StoredEventQueryType.DELETE] as EvtQuery
        )

        then: 'all matching rows are gone and the non-matching row survives'
        delRes.totalCount == 12
        service.query([:] as EvtQuery).totalCount == 1
    }

    @Rollback
    def "delete deletes oldest rows first"() {
        given:
        service.storeEventBatch([[projectName: 'A', topic: 'oldest', subsystem: 'webhooks'] as Evt])
        service.storeEventBatch([[projectName: 'A', topic: 'newest', subsystem: 'webhooks'] as Evt])

        when:
        service.query([subsystem: 'webhooks', maxResults: 1, queryType: StoredEventQueryType.DELETE] as EvtQuery)
        def remaining = service.query([:] as EvtQuery)

        then: 'the oldest row was the one removed'
        remaining.totalCount == 1
        remaining.events[0].topic == 'newest'
    }

    @Rollback
    def "delete matching nothing returns zero"() {
        given:
        service.storeEventBatch([[projectName: 'A', topic: 'test', subsystem: 'webhooks'] as Evt])

        when:
        def delRes = service.query(
                [subsystem: 'nosuchsubsystem', maxResults: 10, queryType: StoredEventQueryType.DELETE] as EvtQuery
        )

        then:
        delRes.totalCount == 0
        service.query([:] as EvtQuery).totalCount == 1
    }
}
