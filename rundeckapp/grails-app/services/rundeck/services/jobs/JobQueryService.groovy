package rundeck.services.jobs

import com.dtolabs.rundeck.app.support.ScheduledExecutionQuery
import com.dtolabs.rundeck.core.plugins.configuration.Property
import org.rundeck.app.components.jobs.JobQuery
import org.rundeck.app.components.jobs.JobQueryInput

class JobQueryService implements JobQuery {

    def localJobQueryService

    @Override
    Map extendCriteria(JobQueryInput input, Map params, Object delegate) {
        localJobQueryService.extendCriteria(input, params, delegate)
    }

    @Override
    List<Property> getQueryProperties() {
        localJobQueryService.getQueryProperties()
    }
}

class LocalJobQueryService {

    Map extendCriteria(JobQueryInput input, Map params, Object delegate) {
        ScheduledExecutionQuery.IS_SCHEDULED_FILTER.each { key, val ->
            if(null!=input["${key}Filter"]){
                delegate.eq(val,input["${key}Filter"])
            }
        }
        //this returns an empty map since schedules is already considered as part of the attributes to show as part of the search
        //ergo, no need to add new values
        return [:]
    }

    List<Property> getQueryProperties() {
        return null
    }
}
