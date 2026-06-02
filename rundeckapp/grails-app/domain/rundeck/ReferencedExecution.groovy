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
        final int PAGE_SIZE = 100
        def seen = new LinkedHashSet()
        int offset = 0
        while (true) {
            List page = findAllByJobUuid(jobUuid, [max: PAGE_SIZE, offset: offset, sort: 'id', order: 'asc'])
            if (!page) break
            for (ref in page) {
                def se = ref.execution?.scheduledExecution
                if (se != null) seen << se
                if (max > 0 && seen.size() >= max) return seen.toList()*.toJobDataSummary()
            }
            if (page.size() < PAGE_SIZE) break
            offset += PAGE_SIZE
        }
        return seen.toList()*.toJobDataSummary()
    }

    static List<String> executionProjectList(String jobUuid, int max = 0){
        final int PAGE_SIZE = 100
        def seen = new LinkedHashSet<String>()
        int offset = 0
        while (true) {
            List page = findAllByJobUuid(jobUuid, [max: PAGE_SIZE, offset: offset, sort: 'id', order: 'asc'])
            if (!page) break
            for (ref in page) {
                def project = ref.execution?.project
                if (project != null) seen << project
                if (max > 0 && seen.size() >= max) return seen.toList() as List<String>
            }
            if (page.size() < PAGE_SIZE) break
            offset += PAGE_SIZE
        }
        return seen.toList() as List<String>
    }

    Serializable getExecutionId(){
        execution.id
    }
}
