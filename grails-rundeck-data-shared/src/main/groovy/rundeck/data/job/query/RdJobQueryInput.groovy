package rundeck.data.job.query

import grails.validation.Validateable
import org.rundeck.app.components.jobs.JobQueryInput
import org.rundeck.app.data.model.v1.query.JobQueryInputData
import rundeck.data.paging.RdPageable

class RdJobQueryInput extends RdPageable implements JobQueryInput, JobQueryInputData, Validateable {
    Map<String,Object> inputParamMap
    String jobFilter
    String jobExactFilter
    String projFilter
    String groupPath
    String groupPathExact

    String descFilter
    String loglevelFilter
    String idlist
    Boolean scheduledFilter
    Boolean scheduleEnabledFilter
    Boolean executionEnabledFilter
    String serverNodeUUIDFilter

    Integer daysAhead
    Boolean runJobLaterFilter
    Boolean paginatedRequired

    static constraints = {
        jobFilter(nullable: true)
        jobExactFilter(nullable: true)
        projFilter(nullable: true)
        groupPath(nullable: true)
        groupPathExact(nullable: true)
        descFilter(nullable: true)
        loglevelFilter(nullable: true)
        idlist(nullable: true)
        scheduledFilter(nullable: true)
        scheduleEnabledFilter(nullable: true)
        executionEnabledFilter(nullable: true)
        serverNodeUUIDFilter(size: 36..36, blank: true, nullable: true, validator: { val, obj ->
            if (null == val) return true;
            try { return null != UUID.fromString(val) } catch (IllegalArgumentException e) {
                return false
            }
        })
        daysAhead(nullable: true)
        runJobLaterFilter(nullable: true)
        paginatedRequired(nullable: true)
    }

    @Override
    String getSortBy() {
        return getSortOrders()?.size() > 0 ? getSortOrders()[0].getColumn() : null
    }

    @Override
    String getSortOrder() {
        return getSortOrders()?.size() > 0 ? getSortOrders()[0].getDirection() : null
    }
}
