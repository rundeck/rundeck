package rundeck

import com.dtolabs.rundeck.app.support.DomainIndexHelper

class ReferencedExecution {
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
        def refExecs = findAllByScheduledExecution(se)?.collect{ re ->
            re.execution?.scheduledExecution
        }?.unique().findAll(){it!=null}
        refExecs.subList(0, (max!=0 && refExecs.size()>max)?max:refExecs.size())

    }
}
