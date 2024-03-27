package rundeck.services.component

import com.dtolabs.rundeck.core.authorization.AuthContext
import com.dtolabs.rundeck.core.authorization.UserAndRolesAuthContext
import org.rundeck.app.components.RundeckJobDefinitionManager
import org.rundeck.app.components.jobs.JobDefinitionComponent
import org.springframework.context.ApplicationContextAware
import rundeck.JobHistory
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
        //Here we need to add logic to allow a maximum ok X job history records
        def jobDefJSON = rundeckJobDefinitionManager.exportAs("json",[scheduledExecution])
        JobHistory jh = new JobHistory()
        jh.userName = user
        jh.jobDefinition = jobDefJSON
        jh.jobUuid = scheduledExecution.uuid
        jh.save()
    }

    /**
     * It removes the history when a job gets deleted
     * @param jobUuid
     */
    void deleteJobHistory(String jobUuid){
        JobHistory.executeUpdate("delete JobHistory jh where jh.jobUuid = :jobUuid", [jobUuid:jobUuid])
    }

    /**
     * It retrieves all job histories and then parse the json stored in the DB to an actual ScheduledExecution
     * It adds the history parameters to the scheduledExecution so it can be shown in the request
     * @param jobUuid
     * @return
     */
    def getJobHistory(String jobUuid){
        def histories = []
        JobHistory.findAllByJobUuid(jobUuid, [order: "dateCreated"]).each {
            def scheduleDefs = rundeckJobDefinitionManager.decodeFormat("json", it.jobDefinition)
            scheduleDefs[0].job.modifierUserName = it.userName
            scheduleDefs[0].job.modifiedDate = it.dateCreated
            scheduleDefs[0].job.historyId = it.id
            histories.add(scheduleDefs[0].job)
        }
        return histories
    }
}