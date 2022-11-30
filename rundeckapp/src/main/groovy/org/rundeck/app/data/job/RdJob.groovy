package org.rundeck.app.data.job

import com.dtolabs.rundeck.plugins.ServiceNameConstants
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import grails.util.Holders
import grails.validation.Validateable
import org.rundeck.app.NotificationConstants
import org.rundeck.app.data.model.v1.job.JobData
import org.rundeck.app.data.validation.validators.ValidatorUtils
import org.rundeck.app.data.validation.validators.execlifecycle.ExecutionLifecyclePluginValidator
import org.rundeck.app.data.validation.validators.jobargs.JobArgStringValidator
import org.rundeck.app.data.validation.validators.joboptions.JobOptionDataValidator
import org.rundeck.app.data.validation.validators.notification.EmailNotificationValidator
import org.rundeck.app.data.validation.validators.notification.PluginNotificationValidator
import org.rundeck.app.data.validation.validators.notification.UrlNotificationValidator
import org.rundeck.app.data.validation.validators.project.ProjectExistenceValidator
import org.rundeck.app.data.validation.validators.schedule.JobScheduleValidator
import org.rundeck.app.data.validation.validators.workflowstrategy.WorkflowStrategyValidator
import org.springframework.validation.Validator
import rundeck.ScheduledExecution
import rundeck.services.FrameworkService
import rundeck.services.NotificationService
import rundeck.services.UserService

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
    Boolean nodesSelectedByDefault = true;
    Boolean nodeKeepgoing=false;
    Boolean doNodedispatch=false;
    String  nodeRankAttribute;
    Boolean nodeRankOrderAscending=true;
    Boolean nodeFilterEditable = false;
    Integer nodeThreadcount=1;
    String  nodeThreadcountDynamic;
    String serverNodeUUID

    RdLogConfig logConfig = new RdLogConfig()
    RdNodeConfig nodeConfig = new RdNodeConfig()
    SortedSet<RdOption> optionSet;
    Set<RdNotification> notificationSet;
    RdWorkflow workflow;
    RdSchedule schedule;
    RdOrchestrator orchestrator;
    Map<String, Object> pluginConfigMap;

    static constraints = {
        importFrom ScheduledExecution
        id(nullable: true)
        dateCreated(nullable: true)
        lastUpdated(nullable: true)
        argString(validator: { val, obj, errors ->
            new JobArgStringValidator().validate(obj, errors)
        })
        project(validator: { val, obj, errors ->
            new ProjectExistenceValidator(Holders.grailsApplication.mainContext.getBean(FrameworkService)).validate(val, errors)
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
            new WorkflowStrategyValidator(Holders.grailsApplication.mainContext.getBean(FrameworkService)).validate(obj, errors)
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
                else nvalidator = new PluginNotificationValidator(Holders.grailsApplication.mainContext.getBean(FrameworkService),obj.project)
                errors.pushNestedPath(propPath)
                nvalidator.validate(notif, errors)
                errors.popNestedPath()
            }
        })
        optionSet(validator: { val, obj, errors ->
            def validator = new JobOptionDataValidator(Holders.grailsApplication.mainContext.getBean(FrameworkService),
                    Holders.grailsApplication.mainContext.getBean(UserService),
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
                new ExecutionLifecyclePluginValidator(Holders.grailsApplication.mainContext.getBean(FrameworkService)).validate(obj, errors)
            }
        })
    }

}
