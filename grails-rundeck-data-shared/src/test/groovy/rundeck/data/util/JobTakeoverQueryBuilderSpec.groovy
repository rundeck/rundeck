package rundeck.data.util

import spock.lang.Specification

class JobTakeoverQueryBuilderSpec extends Specification {
    def "BuildTakeoverQuery"() {
        given:
        String toServerUUID = "toServerUUID"
        String fromServerUUID = "fromServerUUID"
        boolean selectAll = false
        List<String> jobids = ["7a3e4b5d-7e03-4793-af2a-849408527cb6", "60e682d7-37e0-4fd5-b47b-4f470275dee3"]

        when:
        String result = JobTakeoverQueryBuilder.buildTakeoverQuery(toServerUUID, fromServerUUID, selectAll, projectFilter, jobids, innerSchedFlag)

        then:
        result == expectedQry

        where:
        projectFilter | innerSchedFlag | expectedQry
        null          | true           | "SELECT DISTINCT se.id FROM scheduled_execution se WHERE se.uuid in (:jobids) AND se.server_nodeuuid = :fromServerUUID"
        null          | false          | "SELECT DISTINCT se.id FROM scheduled_execution se WHERE se.scheduled = true AND se.uuid in (:jobids) AND se.server_nodeuuid = :fromServerUUID"
        "one"         | true           | "SELECT DISTINCT se.id FROM scheduled_execution se WHERE se.uuid in (:jobids) AND se.project = :projectFilter AND se.server_nodeuuid = :fromServerUUID"
        "one"         | false          | "SELECT DISTINCT se.id FROM scheduled_execution se WHERE se.scheduled = true AND se.uuid in (:jobids) AND se.project = :projectFilter AND se.server_nodeuuid = :fromServerUUID"
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
