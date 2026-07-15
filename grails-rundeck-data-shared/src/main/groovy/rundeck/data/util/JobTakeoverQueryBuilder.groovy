package rundeck.data.util


class JobTakeoverQueryBuilder {
    static String buildTakeoverQuery(String toServerUUID, String fromServerUUID, boolean selectAll, String projectFilter, List<String> jobids, ignoreInnerScheduled = false) {
        String useScheduledFlagQueryPart = !ignoreInnerScheduled ? "se.scheduled = true" : null
        String jobUuidQueryPart = jobids ? "se.uuid in (:jobids)" : null
        String projectPart = projectFilter ? "se.project = :projectFilter" : null
        String serverNodePart = createServerNodeQueryPart(selectAll, fromServerUUID, toServerUUID)
        String sePart = [useScheduledFlagQueryPart,jobUuidQueryPart,projectPart, serverNodePart].findAll{it}.join(" AND ")
        //return an id so that we can use ScheduledExecution.read(id) for efficiency
        return "SELECT DISTINCT se.id FROM scheduled_execution se WHERE ${sePart}"
    }


    static String createServerNodeQueryPart(boolean selectAll, String fromServerUUID, String toServerUUID) {
        String qry = null
        if(selectAll) {
            qry = "(se.server_nodeuuid IS NULL OR se.server_nodeuuid != :toServerUUID)"
        } else {
            qry = fromServerUUID ? "se.server_nodeuuid = :fromServerUUID" : "se.server_nodeuuid IS NULL"
        }
        return qry
    }

}
