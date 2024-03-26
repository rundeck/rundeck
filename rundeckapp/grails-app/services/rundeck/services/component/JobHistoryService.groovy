package rundeck.services.component

import com.dtolabs.rundeck.core.authorization.AuthContext
import com.dtolabs.rundeck.core.authorization.UserAndRolesAuthContext
import org.rundeck.app.components.RundeckJobDefinitionManager
import org.rundeck.app.components.jobs.JobDefinitionComponent
import org.springframework.context.ApplicationContextAware
import rundeck.ScheduledExecution

class JobHistoryService implements JobDefinitionComponent, ApplicationContextAware{

    RundeckJobDefinitionManager rundeckJobDefinitionManager
    static final String componentName = "JobHistory"

    @Override
    String getName() {
        return componentName
    }

    @Override
    Map exportCanonicalMap(Map jobDataMap) {
        return null
    }

    @Override
    Map exportXMap(Map jobXMap) {
        return null
    }

    @Override
    Map importXMap(Map jobXMap, Map partialMap) {
        return null
    }

    @Override
    Object importCanonicalMap(Object job, Map jobDataMap) {
        return null
    }

    @Override
    Object updateJob(Object job, Object imported, Object associate, Map params) {
        return null
    }

    @Override
    void persist(Object job, Object associate, UserAndRolesAuthContext authContext) {

    }

    @Override
    void wasPersisted(Object job, Object associate, UserAndRolesAuthContext authContext) {
        saveJobHistory(job, authContext.getUsername())
    }

    @Override
    void willDeleteJob(Object job, AuthContext authContext) {

    }

    @Override
    void didDeleteJob(Object job, AuthContext authContext) {
        deleteJobHistory(job.uuid)
    }

    /**
     * It saves the job definition as yaml
     * @param scheduledExecution
     * @param user
     */
    void saveJobHistory(ScheduledExecution scheduledExecution, String user) {
        def jobDefYaml = rundeckJobDefinitionManager.exportAsYaml([scheduledExecution])
    }

    void deleteJobHistory(String jobUuid){

    }
}