package rundeck

import com.dtolabs.rundeck.app.support.DomainIndexHelper
import org.hibernate.sql.JoinType
import org.rundeck.app.data.model.v1.execution.RdReferencedExecution

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

    static List<ScheduledExecution> parentList(ScheduledExecution se, int max = 0){
        return createCriteria().list(max: (max!=0)?max:null) {
            createAlias('execution', 'e', JoinType.LEFT_OUTER_JOIN)
            isNotNull( 'e.scheduledExecution')
            eq("jobUuid", se.uuid)
            projections {
                distinct('e.scheduledExecution')
            }
        } as List<ScheduledExecution>
    }

    static List executionProjectList(ScheduledExecution se, int max = 0){
        return createCriteria().list(max: (max!=0)?max:null) {
            createAlias('execution', 'e', JoinType.LEFT_OUTER_JOIN)
            eq("jobUuid", se.uuid)
            projections {
                groupProperty('e.project', "project")
            }
        }
    }

    static List<Long> parentListScheduledExecutionId(ScheduledExecution se, int max = 0){
        return createCriteria().list(max: (max!=0)?max:null) {
            createAlias('execution', 'e', JoinType.LEFT_OUTER_JOIN)
            isNotNull( 'e.scheduledExecution')
            eq("jobUuid", se.uuid)
            projections {
                distinct('e.scheduledExecution.id')
            }
        } as List<Long>
    }

    Serializable getExecutionId(){
        execution.id
    }
}
