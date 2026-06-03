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
        def seen = new LinkedHashSet()
        for (ref in findAllByJobUuid(jobUuid)) {
            def se = ref.execution?.scheduledExecution
            if (se != null) seen << se
            if (max > 0 && seen.size() >= max) break
        }
        return seen.toList()*.toJobDataSummary()
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
