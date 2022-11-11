package org.rundeck.app.data.job

import com.dtolabs.rundeck.core.common.FrameworkResource
import grails.util.Holders
import grails.validation.Validateable
import org.rundeck.app.NotificationConstants
import org.rundeck.app.data.model.v1.job.JobData
import org.rundeck.app.data.model.v1.job.config.LogConfig
import org.rundeck.app.data.model.v1.job.config.NodeConfig
import org.rundeck.app.data.model.v1.job.notification.NotificationData
import org.rundeck.app.data.model.v1.job.option.OptionData
import org.rundeck.app.data.model.v1.job.option.OptionValueData
import org.rundeck.app.data.model.v1.job.orchestrator.OrchestratorData
import org.rundeck.app.data.model.v1.job.schedule.ScheduleData
import org.rundeck.app.data.model.v1.job.workflow.WorkflowData
import org.rundeck.app.data.model.v1.job.workflow.WorkflowStepData
import org.rundeck.app.data.validators.notification.EmailNotificationValidator
import org.rundeck.app.data.validators.notification.PluginNotificationValidator
import org.rundeck.app.data.validators.notification.UrlNotificationValidator
import org.springframework.validation.Validator
import rundeck.ScheduledExecution
import rundeck.services.FrameworkService
import rundeck.services.NotificationService

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
    String userRoleList;
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
    SortedSet<RdOptionData> optionSet;
    Set<RdNotificationData> notificationSet;
    RdWorkflowData workflow;
    RdScheduleData schedule;
    RdOrchestratorData orchestrator;
    Map<String, Object> pluginConfigMap;

    static nestedvalidator = { val, obj, errors ->
        if(!val) return
        if (!val.validate()) {
            val.errors.allErrors.each { err ->
                def fieldName = err.arguments ? err.arguments[0] : err.properties['field']
                if (fieldName) {
                    String errorCode = "${propertyName}.${err.code}"
                    if (val.hasProperty(fieldName)) {
                        errorCode = "${propertyName}.${err.arguments[0]}.${err.code}"
                    }
                    errors.rejectValue("${propertyName}.${err.properties['field']}", errorCode, err.arguments, "Invalid value for {0}")
                }
            }
        }
    }

    static constraints = {
        importFrom(ScheduledExecution)
        logConfig(nullable: false, validator: nestedvalidator)
        schedule(nullable: true, validator: nestedvalidator)
        notificationSet(validator: { val, obj, errors ->
            val.each { notif ->
                Validator nvalidator
                if(notif.type == NotificationConstants.EMAIL_NOTIFICATION_TYPE) nvalidator = new EmailNotificationValidator()
                else if(notif.type == NotificationConstants.WEBHOOK_NOTIFICATION_TYPE) nvalidator = new UrlNotificationValidator()
                else nvalidator = new PluginNotificationValidator(Holders.grailsApplication.mainContext.getBean(NotificationService),Holders.grailsApplication.mainContext.getBean(FrameworkService),obj.project, [:], [:])
                nvalidator.validate(notif, errors)
            }
        })
    }

    static class RdLogConfig implements LogConfig, Validateable {
        String loglevel="WARN";
        String logOutputThreshold;
        String logOutputThresholdAction;
        String logOutputThresholdStatus;

        static constraints = {
            loglevel(nullable:true)
            logOutputThreshold(maxSize: 256, blank:true, nullable: true)
            logOutputThresholdAction(maxSize: 256, blank:true, nullable: true,inList: ['halt','truncate'])
            logOutputThresholdStatus(maxSize: 256, blank:true, nullable: true)
        }
    }

    static class RdNodeConfig implements NodeConfig, Validateable {
        String nodeInclude
        String nodeExclude
        String nodeIncludeName
        String nodeExcludeName
        String nodeIncludeTags
        String nodeExcludeTags
        String nodeIncludeOsName
        String nodeExcludeOsName
        String nodeIncludeOsFamily
        String nodeExcludeOsFamily
        String nodeIncludeOsArch
        String nodeExcludeOsArch
        String nodeIncludeOsVersion
        String nodeExcludeOsVersion
        Boolean nodeExcludePrecedence=true
        Boolean successOnEmptyNodeFilter=false
        String filter
        String filterExclude
        Boolean excludeFilterUncheck = false

        static constraints = {
            nodeInclude(nullable:true)
            nodeExclude(nullable:true)
            nodeIncludeName(nullable:true)
            nodeExcludeName(nullable:true)
            nodeIncludeTags(nullable:true)
            nodeExcludeTags(nullable:true)
            nodeIncludeOsName(nullable:true)
            nodeExcludeOsName(nullable:true)
            nodeIncludeOsFamily(nullable:true)
            nodeExcludeOsFamily(nullable:true)
            nodeIncludeOsArch(nullable:true)
            nodeExcludeOsArch(nullable:true)
            nodeIncludeOsVersion(nullable:true)
            nodeExcludeOsVersion(nullable:true)
            nodeExcludePrecedence(nullable:true)
            successOnEmptyNodeFilter(nullable: true)
            filter(nullable:true)
        }
    }

    static class RdNotificationData implements NotificationData {
        Long id
        String eventTrigger;
        String type;
        String format;
        String content;
    }
    static class RdWorkflowData implements WorkflowData {
        Long id
        Integer threadcount=1;
        Boolean keepgoing=false;
        List<RdWorkflowStep> steps;
        String strategy="node-first";
        String pluginConfig;
    }
    static class RdWorkflowStep implements WorkflowStepData {
        Long id
        WorkflowStepData errorHandler;
        Boolean keepgoingOnSuccess;
        String description;
        Map<String, Object> configuration
        Boolean nodeStep;
        String pluginType;
        Map<String,Object> pluginConfig;
    }
    static class RdScheduleData implements ScheduleData, Validateable {
        String minute;
        String hour;
        String dayOfMonth;
        String month;
        String dayOfWeek;
        String seconds;
        String year;
        String crontabString;

        static constraints = {
            seconds(nullable: true, matches: /^[0-9*\/,-]*$/)
            minute(nullable:true, matches: /^[0-9*\/,-]*$/ )
            hour(nullable:true, matches: /^[0-9*\/,-]*$/ )
            dayOfMonth(nullable:true, matches: /^[0-9*\/,?LW-]*$/ )
            month(nullable:true, matches: /^[0-9a-zA-z*\/,-]*$/ )
            dayOfWeek(nullable:true, matches: /^[0-9a-zA-z*\/?,L#-]*$/ )
            year(nullable:true, matches: /^[0-9*\/,-]*$/)
            crontabString(bindable: true,nullable: true)
        }
    }
    static class RdOrchestratorData implements OrchestratorData {
        Long id
        String type
        Map<String,Object> configuration
    }

    static class RdOptionData implements OptionData, Comparable<OptionData>, Validateable {
        Long id;
        String name;
        Integer sortIndex;
        String description;
        String defaultValue;
        String defaultStoragePath;
        Boolean enforced;
        Boolean required;
        Boolean isDate;
        String dateFormat;
        String label;
        URL valuesUrl;
        URL valuesUrlLong;
        String regex;
        String valuesList;
        String valuesListDelimiter;
        Boolean multivalued;
        String delimiter;
        Boolean secureInput;
        Boolean secureExposed;
        String optionType;
        String configData;
        Boolean multivalueAllSelected;
        String optionValuesPluginType;
        List<OptionValueData> valuesFromPlugin;
        Boolean hidden;
        Boolean sortValues;
        List<String> optionValues;

        static constraints={
            name(nullable:false,blank:false,matches: '[a-zA-Z_0-9.-]+')
            description(nullable:true)
            defaultValue(nullable:true)
            defaultStoragePath(nullable:true,matches: '^(/?)keys/.+')
            enforced(nullable:false)
            required(nullable:true)
            isDate(nullable:true)
            dateFormat(nullable: true, maxSize: 30)
            valuesUrl(nullable:true)
            valuesUrlLong(nullable:true, maxSize: 3000)
            regex(nullable:true)
            delimiter(nullable:true)
            multivalued(nullable:true)
            secureInput(nullable:true)
            secureExposed(nullable:true)
            sortIndex(nullable:true)
            optionType(nullable: true, maxSize: 255)
            configData(nullable: true)
            multivalueAllSelected(nullable: true)
            label(nullable: true)
            optionValuesPluginType(nullable: true)
            hidden(nullable: true)
            valuesList(nullable: true)
            valuesListDelimiter(nullable: true)
            sortValues(nullable: true)
        }

        @Override
        public int compareTo(OptionData obj) {
            if (null != sortIndex && null != obj.sortIndex) {
                return sortIndex.compareTo(obj.sortIndex);
            } else if (null == sortIndex && null == obj.sortIndex) {
                return name.compareTo(obj.name);
            } else {
                return sortIndex != null ? -1 : 1;
            }
        }
    }

    static class RdOptionValueData implements OptionValueData {
        String name;
        String value;
    }
}
