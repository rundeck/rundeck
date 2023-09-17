package rundeck.data.validation.validators.jobcomponent

import org.rundeck.app.components.jobs.JobDefinitionComponent
import org.rundeck.app.data.model.v1.job.JobData
import org.rundeck.app.data.model.v1.job.component.JobComponentData
import org.springframework.validation.Errors
import org.springframework.validation.Validator

class JobComponentValidator implements Validator {

    Map<String, JobDefinitionComponent> jobDefinitionComponents
    def rundeckJobDefinitionManager

    JobComponentValidator(Map<String, JobDefinitionComponent> jobDefinitionComponents,def rundeckJobDefinitionManager) {
        this.jobDefinitionComponents = jobDefinitionComponents
        this.rundeckJobDefinitionManager = rundeckJobDefinitionManager
    }

    @Override
    boolean supports(Class<?> clazz) {
        return JobData.class.isAssignableFrom(clazz);
    }

    @Override
    void validate(Object target, Errors errors) {
        JobData rdJob = (JobData)target
        rdJob.components.each{ String componentName, JobComponentData value ->
            if(!jobDefinitionComponents.containsKey(componentName)) {
                errors.rejectValue("components",
                        "jobData.components.notfound",
                        [componentName] as Object[],
                        'Job Component of type: {0} could not be found')
            } else {
                def componentDef = jobDefinitionComponents[componentName]
                def cobj = componentDef.importCanonicalMap(rdJob, value)
                def rpt = componentDef.validateImported(rdJob, cobj)
                if(!rpt.valid) {
                    rpt.errors.each { k, v  ->
                        errors.rejectValue("components",
                                "jobData.components.invalidconfiguration",
                                [componentName, k, v] as Object[],
                                'Job Component: {0} invalid config: {1} : {2}')
                    }
                }
            }
        }

    }
}
