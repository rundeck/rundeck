package com.dtolabs.rundeck.plugins;

/**
 * Constants for plugin grouping metadata.
 * Use with @PluginMetadata to categorize plugins for UI grouping.
 *
 * Example:
 * <pre>
 * @Plugin(name = "my-plugin", service = ServiceNameConstants.WorkflowStep)
 * @PluginDescription(title = "My Plugin", description = "Does stuff")
 * @PluginMetadata(key = PluginGroupConstants.PLUGIN_GROUP_KEY,
 *                 value = PluginGroupConstants.GROUP_AWS_S3)
 * public class MyPlugin implements StepPlugin { ... }
 * </pre>
 */
public class PluginGroupConstants {

    /**
     * The metadata key for plugin grouping.
     * Use this with @PluginMetadata annotation.
     */
    public static final String PLUGIN_GROUP_KEY = "groupBy";

    // Predefined Plugin Groups (alphabetical order)

    /** Ansible automation plugins */
    public static final String GROUP_ANSIBLE = "Ansible";

    /** General AWS infrastructure plugins */
    public static final String GROUP_AWS = "AWS";

    /** AWS S3 storage plugins */
    public static final String GROUP_AWS_S3 = "AWS S3";

    /** AWS CloudWatch monitoring and logging plugins */
    public static final String GROUP_AWS_CLOUDWATCH = "AWS CloudWatch";

    /** AWS Lambda serverless function plugins */
    public static final String GROUP_AWS_LAMBDA = "AWS Lambda";

    /** AWS RDS database plugins */
    public static final String GROUP_AWS_RDS = "AWS RDS";

    /** AWS EC2 virtual machine plugins */
    public static final String GROUP_AWS_VM = "AWS VM";

    /** Microsoft Azure cloud plugins */
    public static final String GROUP_AZURE = "Azure";

    /** Datadog monitoring and observability plugins */
    public static final String GROUP_DATADOG = "Datadog";

    /** Google Cloud Platform plugins */
    public static final String GROUP_GCP = "GCP";

    /** Atlassian Jira integration plugins */
    public static final String GROUP_JIRA = "Jira";

    /** Kubernetes container orchestration plugins */
    public static final String GROUP_KUBERNETES = "Kubernetes";

    /** Oracle Cloud Infrastructure plugins */
    public static final String GROUP_ORACLE = "Oracle";

    /** PagerDuty incident management plugins */
    public static final String GROUP_PAGERDUTY = "PagerDuty";

    /** PowerShell execution plugins */
    public static final String GROUP_PS1 = "PS1";

    /** ServiceNow Change Management plugins */
    public static final String GROUP_SERVICENOW_CHANGE = "ServiceNow Change";

    /** ServiceNow Incident Management plugins */
    public static final String GROUP_SERVICENOW_INCIDENT = "ServiceNow Incident";

    /** Sensu monitoring plugins */
    public static final String GROUP_SENSU = "Sensu";

    /** Sumo Logic log analytics plugins */
    public static final String GROUP_SUMO_LOGIC = "Sumo Logic";

    /** VMWare virtualization plugins */
    public static final String GROUP_VMWARE = "VMWare";

    /** Default group for plugins without explicit groupBy metadata */
    public static final String GROUP_OTHER = "Other";
}
