package rundeck

import com.dtolabs.rundeck.app.support.DomainIndexHelper
import org.hibernate.criterion.CriteriaSpecification
import org.hibernate.sql.JoinType

class ReferencedExecution {
    ScheduledExecution scheduledExecution
    String status
    Execution execution
    Long seId
    Long exId

    static belongsTo=[Execution]

    static constraints = {
        scheduledExecution(nullable:true)
        status(nullable:true)
        execution(nullable: false)
    }

    static mapping = {
        seId formula: 'scheduled_Execution_Id'
        exId formula: 'execution_Id'
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
}
