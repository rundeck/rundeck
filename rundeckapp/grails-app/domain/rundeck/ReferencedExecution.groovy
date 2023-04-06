package rundeck

import com.dtolabs.rundeck.app.support.DomainIndexHelper
import org.hibernate.sql.JoinType
import org.rundeck.app.data.model.v1.execution.RdReferencedExecution

class ReferencedExecution implements RdReferencedExecution{
    ScheduledExecution scheduledExecution
    String status
    Execution execution

    static belongsTo=[Execution]

    static constraints = {
        scheduledExecution(nullable:true)
        status(nullable:true)
        execution(nullable: false)

    }

    static mapping = {

        DomainIndexHelper.generate(delegate) {
            index 'REFEXEC_IDX_1', ['scheduledExecution', 'status']
        }
    }

    static List<ScheduledExecution> parentList(ScheduledExecution se, int max = 0){
        return createCriteria().list(max: (max!=0)?max:null) {
            createAlias('execution', 'e', JoinType.LEFT_OUTER_JOIN)
            isNotNull( 'e.scheduledExecution')
            eq("scheduledExecution", se)
            projections {
                distinct('e.scheduledExecution')
            }
        } as List<ScheduledExecution>
    }

    static List executionProjectList(ScheduledExecution se, int max = 0){
        return createCriteria().list(max: (max!=0)?max:null) {
            createAlias('execution', 'e', JoinType.LEFT_OUTER_JOIN)
            eq("scheduledExecution", se)
            projections {
                groupProperty('e.project', "project")
            }
        }
    }
}
