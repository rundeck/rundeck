package rundeck.data.util

import spock.lang.Specification

class JobTakeoverQueryBuilderSpec extends Specification {
    def "BuildTakeoverQuery"() {
        given:
        String toServerUUID = "toServerUUID"
        String fromServerUUID = "fromServerUUID"
        boolean selectAll = false
        List<String> jobids = ["7a3e4b5d-7e03-4793-af2a-849408527cb6", "60e682d7-37e0-4fd5-b47b-4f470275dee3","';DELETE FROM rduser;"]

        when:
        String result = JobTakeoverQueryBuilder.buildTakeoverQuery(toServerUUID, fromServerUUID, selectAll, projectFilter, jobids, innerSchedFlag)

        then:
        result == expectedQry

        where:
        projectFilter | innerSchedFlag | expectedQry
        null          | true           | "SELECT DISTINCT se.id FROM scheduled_execution se LEFT JOIN execution e ON se.id = e.scheduled_execution_id WHERE ((e.status = 'scheduled' AND e.date_completed IS NULL AND e.date_started > current_timestamp AND e.server_nodeuuid = :fromServerUUID) OR (se.uuid in ('7a3e4b5d-7e03-4793-af2a-849408527cb6','60e682d7-37e0-4fd5-b47b-4f470275dee3') AND se.server_nodeuuid = :fromServerUUID))"
        null          | false          | "SELECT DISTINCT se.id FROM scheduled_execution se LEFT JOIN execution e ON se.id = e.scheduled_execution_id WHERE ((e.status = 'scheduled' AND e.date_completed IS NULL AND e.date_started > current_timestamp AND e.server_nodeuuid = :fromServerUUID) OR (se.scheduled = true AND se.uuid in ('7a3e4b5d-7e03-4793-af2a-849408527cb6','60e682d7-37e0-4fd5-b47b-4f470275dee3') AND se.server_nodeuuid = :fromServerUUID))"
        "one"         | true           | "SELECT DISTINCT se.id FROM scheduled_execution se LEFT JOIN execution e ON se.id = e.scheduled_execution_id WHERE ((e.status = 'scheduled' AND e.date_completed IS NULL AND e.date_started > current_timestamp AND e.project = :projectFilter AND e.server_nodeuuid = :fromServerUUID) OR (se.uuid in ('7a3e4b5d-7e03-4793-af2a-849408527cb6','60e682d7-37e0-4fd5-b47b-4f470275dee3') AND se.project = :projectFilter AND se.server_nodeuuid = :fromServerUUID))"
        "one"         | false          | "SELECT DISTINCT se.id FROM scheduled_execution se LEFT JOIN execution e ON se.id = e.scheduled_execution_id WHERE ((e.status = 'scheduled' AND e.date_completed IS NULL AND e.date_started > current_timestamp AND e.project = :projectFilter AND e.server_nodeuuid = :fromServerUUID) OR (se.scheduled = true AND se.uuid in ('7a3e4b5d-7e03-4793-af2a-849408527cb6','60e682d7-37e0-4fd5-b47b-4f470275dee3') AND se.project = :projectFilter AND se.server_nodeuuid = :fromServerUUID))"
    }

    def "CreateJobTakeoverExecutionQueryPart"() {

        when:
        String result = JobTakeoverQueryBuilder.createJobTakeoverExecutionQueryPart(selectAll, fromServerUUID, toServerUUID, projectFilter)

        then:
        result == expectedQry

        where:
        selectAll | fromServerUUID | toServerUUID    | projectFilter | expectedQry
        true      | null           | "dest-svr-uuid" | null          | "e.status = 'scheduled' AND e.date_completed IS NULL AND e.date_started > current_timestamp AND (e.server_nodeuuid IS NULL OR e.server_nodeuuid != :toServerUUID)"
        true      | null           | "dest-svr-uuid" | "prj-one"     | "e.status = 'scheduled' AND e.date_completed IS NULL AND e.date_started > current_timestamp AND e.project = :projectFilter AND (e.server_nodeuuid IS NULL OR e.server_nodeuuid != :toServerUUID)"
        false     | "src-svr-uuid" | "dest-svr-uuid" | null          | "e.status = 'scheduled' AND e.date_completed IS NULL AND e.date_started > current_timestamp AND e.server_nodeuuid = :fromServerUUID"
    }

    def "CreateServerNodeQueryPart"() {
        when:
        String result = JobTakeoverQueryBuilder.createServerNodeQueryPart(selectAll, fromServerUUID, toServerUUID)

        then:
        result == expectedQry

        where:
        selectAll | fromServerUUID | toServerUUID    | expectedQry
        true      | null           | "dest-svr-uuid" | "(se.server_nodeuuid IS NULL OR se.server_nodeuuid != :toServerUUID)"
        false     | null           | "dest-svr-uuid" | "se.server_nodeuuid IS NULL"
        false     | "src-svr-uuid" | "dest-svr-uuid" | "se.server_nodeuuid = :fromServerUUID"

    }
}
