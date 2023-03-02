package rundeck

import com.dtolabs.rundeck.app.support.DomainIndexHelper
import org.hibernate.sql.JoinType
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
        return createCriteria().list(max: (max!=0)?max:null) {
            createAlias('execution', 'e', JoinType.LEFT_OUTER_JOIN)
            isNotNull( 'e.jobUuid')
            eq("jobUuid", jobUuid)
            projections {
                distinct('e.jobUuid')
            }
        }*.toJobDataSummary()
    }

    static List<String> executionProjectList(String jobUuid, int max = 0){
        if(!jobUuid) return []
        return createCriteria().list(max: (max!=0)?max:null) {
            createAlias('execution', 'e', JoinType.LEFT_OUTER_JOIN)
            eq("jobUuid", jobUuid)
            projections {
                groupProperty('e.project', "project")
            }
        } as List<String>
    }

    Serializable getExecutionId(){
        execution.id
    }
}
