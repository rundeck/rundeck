package rundeck

import com.dtolabs.rundeck.app.support.DomainIndexHelper
import org.rundeck.app.data.model.v1.execution.RdReferencedExecution
import org.rundeck.app.data.model.v1.job.JobDataSummary

class ReferencedExecution implements RdReferencedExecution{
    String jobUuid
    String status
    Execution execution

    static belongsTo=[Execution]

    static constraints = {
        jobUuid(nullable:true)
        status(nullable:true)
        execution(nullable: false)

    }

    static mapping = {

        DomainIndexHelper.generate(delegate) {
            index 'REFEXEC_IDX_JOBUUID', ['jobUuid', 'status']
        }
    }

    static List<JobDataSummary> parentJobSummaries(String jobUuid, int max = 0){
        // Select DISTINCT se.id (Long scalar) to avoid ORA-22848: ScheduledExecution has TEXT columns
        // that Oracle rejects in SELECT DISTINCT on a full entity. Two queries total instead of N+1.
        def idQuery = "SELECT DISTINCT se.id FROM ReferencedExecution re JOIN re.execution e JOIN e.scheduledExecution se WHERE re.jobUuid = :jobUuid"
        def options = max > 0 ? [max: max] : [:]
        List<Long> ids = executeQuery(idQuery, [jobUuid: jobUuid], options)
        return ScheduledExecution.getAll(ids)*.toJobDataSummary()
    }

    static List<String> executionProjectList(String jobUuid, int max = 0){
        // Single JOIN query; DISTINCT on project (VARCHAR) is Oracle-safe (no CLOB involved).
        def query = "SELECT DISTINCT e.project FROM ReferencedExecution re JOIN re.execution e WHERE re.jobUuid = :jobUuid AND e.project IS NOT NULL"
        def options = max > 0 ? [max: max] : [:]
        return executeQuery(query, [jobUuid: jobUuid], options) as List<String>
    }

    Serializable getExecutionId(){
        execution.id
    }
}
