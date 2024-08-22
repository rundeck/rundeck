package rundeck.data.util

class JobTakeoverQueryBuilder {
    static String buildTakeoverQuery(String toServerUUID, String fromServerUUID, boolean selectAll, String projectFilter, List<String> jobids, ignoreInnerScheduled = false) {
        String executionQueryPart = createJobTakeoverExecutionQueryPart(selectAll,fromServerUUID, toServerUUID, projectFilter)
        String useScheduledFlagQueryPart = !ignoreInnerScheduled ? "se.scheduled = true" : null
        String jobUuidQueryPart = jobids ? filteredJobIdsPart(jobids) : null
        String projectPart = projectFilter ? "se.project = :projectFilter" : null
        String serverNodePart = createServerNodeQueryPart(selectAll, fromServerUUID, toServerUUID)
        String sePart = [useScheduledFlagQueryPart,jobUuidQueryPart,projectPart, serverNodePart].findAll{it}.join(" AND ")
        //return an id so that we can use ScheduledExecution.read(id) for efficiency
        return "SELECT DISTINCT se.id FROM scheduled_execution se LEFT JOIN execution e ON se.id = e.scheduled_execution_id WHERE ((${executionQueryPart}) OR (${sePart}))"
    }

    static String filteredJobIdsPart(List<String> jobids) {
        var joinedValidJobIds = jobids.findAll{jobid -> isValidUUID(jobid)}.collect{"'${it}'"}.join(",")
        if (joinedValidJobIds.isEmpty()) {
            // This will force this part of the condition to be false
            return "1=0"
        } else {
            return "se.uuid in (${joinedValidJobIds})"
        }
    }

    static String createJobTakeoverExecutionQueryPart(boolean selectAll,String fromServerUUID, String toServerUUID, String projectFilter) {
        String qry = "e.status = 'scheduled' AND e.date_completed IS NULL AND e.date_started > current_timestamp"
        if(projectFilter) {
            qry += " AND e.project = :projectFilter"
        }
        if(selectAll) {
            qry += " AND (e.server_nodeuuid IS NULL OR e.server_nodeuuid != :toServerUUID)"
        } else {
            qry += fromServerUUID ? " AND e.server_nodeuuid = :fromServerUUID" : " AND e.server_nodeuuid IS NULL"
        }
        return qry
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

    static boolean isValidUUID(String val) {
        if (null == val) return false;
        try { return null!= UUID.fromString(val) } catch (IllegalArgumentException e) {
            return false
        }
    }
}
