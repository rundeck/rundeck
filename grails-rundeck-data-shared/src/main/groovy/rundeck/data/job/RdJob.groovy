package rundeck.data.job

import com.dtolabs.rundeck.core.common.FrameworkResource
import com.dtolabs.rundeck.core.jobs.JobOption
import com.dtolabs.rundeck.plugins.ServiceNameConstants
import com.dtolabs.rundeck.plugins.jobs.JobOptionImpl
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import grails.util.Holders
import grails.validation.Validateable
import org.rundeck.app.components.jobs.JobDefinitionComponent
import org.rundeck.app.core.FrameworkServiceCapabilities
import org.rundeck.app.data.model.v1.job.JobData
import org.rundeck.app.data.model.v1.job.component.JobComponentData
import rundeck.data.constants.NotificationConstants
import rundeck.data.validation.validators.ValidatorUtils
import rundeck.data.validation.validators.execlifecycle.ExecutionLifecyclePluginValidator
import rundeck.data.validation.validators.jobargs.JobArgStringValidator
import rundeck.data.validation.validators.jobcomponent.JobComponentValidator
import rundeck.data.validation.validators.joboptions.JobOptionDataValidator
import rundeck.data.validation.validators.notification.EmailNotificationValidator
import rundeck.data.validation.validators.notification.PluginNotificationValidator
import rundeck.data.validation.validators.notification.UrlNotificationValidator
import rundeck.data.validation.validators.project.ProjectExistenceValidator
import rundeck.data.validation.validators.schedule.JobScheduleValidator
import rundeck.data.validation.validators.workflowstrategy.WorkflowStrategyValidator
import org.springframework.validation.Validator
import rundeck.data.validation.shared.SharedJobConstraints
import rundeck.data.validation.shared.SharedProjectNameConstraints

@JsonIgnoreProperties(["errors"])
class RdJob implements JobData, Validateable {
    Long id;
    String uuid;
    String jobName;
    String description;
    String project;
    String argString;
    String user;
    String timeout;
    String retry;
    String retryDelay;
    String groupPath;
    List<String> userRoles;
    Boolean scheduled;
    Boolean scheduleEnabled;
    Boolean executionEnabled;
    Boolean multipleExecutions;
    String notifyAvgDurationThreshold;
    String timeZone;
    String defaultTab;
    String maxMultipleExecutions;
    Date dateCreated
    Date lastUpdated

    String serverNodeUUID

    RdLogConfig logConfig = new RdLogConfig()
    RdNodeConfig nodeConfig = new RdNodeConfig()
    SortedSet<RdOption> optionSet;
    Set<RdNotification> notificationSet;
    RdWorkflow workflow;
    RdSchedule schedule;
    RdOrchestrator orchestrator;
    Map<String, Object> pluginConfigMap;
    Map<String, RdJobComponentData> components = [:]

    static constraints = {
        importFrom SharedProjectNameConstraints
        importFrom SharedJobConstraints

        id(nullable: true)
        dateCreated(nullable: true)
        lastUpdated(nullable: true)
        //For some reason this causes a problem in the SharedJobConstraints so it's included here
        uuid(unique: true, nullable:true, blank:false, matches: FrameworkResource.VALID_RESOURCE_NAME_REGEX)
        argString(validator: { val, obj, errors ->
            new JobArgStringValidator().validate(obj, errors)
        })
        project(validator: { val, obj, errors ->
            new ProjectExistenceValidator(Holders.grailsApplication.mainContext.getBean(FrameworkServiceCapabilities)).validate(val, errors)
        })
        logConfig(nullable: false, validator: ValidatorUtils.nestedValidator)
        schedule(nullable: true, validator: {val, obj, errors ->
            new JobScheduleValidator().validate(obj, errors)
            if(val && !val.validate()) {
                ValidatorUtils.processErrors("schedule", val, errors)
                return
            }
        })
        orchestrator(nullable: true, validator: ValidatorUtils.nestedValidator)
        workflow(nullable: false, validator: { val, obj, errors ->
            if(!val) return
            if(!val.validate()) {
                ValidatorUtils.processErrors("workflow", val, errors)
                return
            }
            new WorkflowStrategyValidator(Holders.grailsApplication.mainContext.getBean(FrameworkServiceCapabilities)).validate(obj, errors)
        })
        notificationSet(validator: { val, obj, errors ->
            val.eachWithIndex { notif, idx ->
                String propPath = "notificationSet[${idx}]"
                if(!notif.validate()) {
                    ValidatorUtils.processErrors(propPath, notif, errors)
                    return
                }
                Validator nvalidator
                if(notif.type == NotificationConstants.EMAIL_NOTIFICATION_TYPE) nvalidator = new EmailNotificationValidator()
                else if(notif.type == NotificationConstants.WEBHOOK_NOTIFICATION_TYPE) nvalidator = new UrlNotificationValidator()
                else nvalidator = new PluginNotificationValidator(Holders.grailsApplication.mainContext.getBean(FrameworkServiceCapabilities),obj.project)
                errors.pushNestedPath(propPath)
                nvalidator.validate(notif, errors)
                errors.popNestedPath()
            }
        })
        optionSet(validator: { val, obj, errors ->
            def validator = new JobOptionDataValidator(Holders.grailsApplication.mainContext.getBean(FrameworkServiceCapabilities),
                    obj)
            val.eachWithIndex { opt, idx ->
                String propPath = "optionSet[${idx}]"
                if(!opt.validate()){
                    ValidatorUtils.processErrors(propPath, opt, errors)
                    return
                }
                errors.pushNestedPath(propPath)
                validator.validate(opt, errors)
                errors.popNestedPath()
            }
        })
        pluginConfigMap(validator: { val, obj, errors ->
            if(val && val.containsKey(ServiceNameConstants.ExecutionLifecycle)) {
                new ExecutionLifecyclePluginValidator(Holders.grailsApplication.mainContext.getBean(FrameworkServiceCapabilities)).validate(obj, errors)
            }
        })
    }

    boolean hasSecureOptions() {
        return !optionSet ? false : optionSet?.any {
            it.secureInput || it.secureExposed
        }
    }

}
