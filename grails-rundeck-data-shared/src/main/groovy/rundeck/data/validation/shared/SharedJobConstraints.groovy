package rundeck.data.validation.shared

import com.dtolabs.rundeck.core.common.FrameworkResource
import grails.validation.Validateable
import org.rundeck.app.data.model.v1.job.JobData

class SharedJobConstraints implements Validateable {
    String groupPath
    String userRoleList
    String jobName
    String description
    String uuid;

    Boolean scheduled
    String project
    String argString
    String user
    String loglevel
    String serverNodeUUID
    String timeout
    String retry
    String retryDelay

    Boolean multipleExecutions
    String notifyAvgDurationThreshold
    String timeZone
    Boolean scheduleEnabled
    Boolean executionEnabled

    String defaultTab
    String maxMultipleExecutions

    static constraints = {
        jobName(blank: false, nullable: false, matches: "[^/]+", maxSize: 1024)
        groupPath(nullable:true, maxSize: 2048)
        user(nullable:true)
        loglevel(nullable:true)

        argString(nullable:true)
        description(nullable:true)
        multipleExecutions(nullable: true)
        serverNodeUUID(maxSize: 36, size: 36..36, blank: true, nullable: true, validator: { val, obj ->
            if (null == val) return true;
            try { return null != UUID.fromString(val) } catch (IllegalArgumentException e) {
                return false
            }
        })
        timeout(maxSize: 256, blank: true, nullable: true,)
        retry(maxSize: 256, blank: true, nullable: true,validator: { val, obj ->
            if (null == val) return true;
            if (val.indexOf('${')>=0) return true;
            try { return Integer.parseInt(val)>=0 } catch (NumberFormatException e) {
                return false
            }
        })

        scheduleEnabled(nullable: true)
        executionEnabled(nullable: true)

        timeZone(maxSize: 256, blank: true, nullable: true)
        retryDelay(nullable:true)

        notifyAvgDurationThreshold(nullable: true)
        defaultTab(maxSize: 256, blank: true, nullable: true)
        maxMultipleExecutions(maxSize: 256, blank: true, nullable: true)
    }
}
