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
        List refs = findAllByJobUuid(jobUuid)
        List seList = refs*.execution*.scheduledExecution.findAll { it != null }.unique()
        if (max > 0 && seList.size() > max) {
            seList = seList[0..<max]
        }
        return seList*.toJobDataSummary()
    }

    static List<String> executionProjectList(String jobUuid, int max = 0){
        List refs = findAllByJobUuid(jobUuid)
        List projects = refs*.execution*.project.findAll { it != null }.unique()
        if (max > 0 && projects.size() > max) {
            projects = projects[0..<max]
        }
        return projects as List<String>
    }

    Serializable getExecutionId(){
        execution.id
    }
}
