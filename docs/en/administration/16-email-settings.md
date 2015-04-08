% Email Settings

This document describes how to configure Rundeck for email
support. 
Email settings are located in the rundeck-config.properties file. Depending on the installer used, the configuration files will be under a base directory:

* RPM/DEB: /etc/rundeck/rundeck-config.properties
* Launcher: $RDECK_BASE/server/config/rundeck-config.properties

Refer to the appropriate configuration file paths from [Configuration -> Configuration Layout](configuration-file-reference.html#configuration-layout) to locate the Rundeck configuration paths depending on your install.

## SMTP server settings

By default the plugin assumes an unsecured mail server configured at localhost on port 25.
You can specify your own with these settings:

	grails.mail.host=localhost
	grails.mail.port=25

To add authentication:

	grails.mail.username=user
	grails.mail.password=pass

### Advanced SMTP settings
If you need more advanced configuration (e.g., authenticated and secured over SSL), 
see the grails Mail plugin configuration: 
[Grails Mail Configuration](http://www.grails.org/plugin/mail#Configuration)

The caveat for using this is that in our experience it requires using a .groovy formatted configuration file to support the extended configuration properties.

See [Groovy config format](configuration-file-reference.html#groovy-config-format).

## Notification email settings

The URL and From: address used in [Job email notifications](../manual/jobs.html#job-notifications)  are managed via the settings located in the rundeck-config.properties file.

The two properties are:

* grails.serverURL
* grails.mail.default.from

Here's an example:

    grails.serverURL=https://node.fully.qualified.domain.name:4443
    grails.mail.default.from=deployer@domain.com

### Custom Email Templates

You can define these properties to customize the email notifications. Each property can be defined for a specific Trigger, or for the general case.  Available triggers are: `success`,`failure`, and `start`. In addition, you can have custom settings for a project and job name combination as well:

    # trigger-specific templating
    rundeck.mail.[trigger].template.subject=[custom subject line]
    rundeck.mail.[trigger].template.file=[path to template file]
    rundeck.mail.[trigger].template.log.formatted=true/false (if true, prefix log lines with context information)

    # project and job specific
    rundeck.mail.[project].[jobname].template.subject=[custom subject line]
    rundeck.mail.[project].[jobname].template.file=[path to template file]
    rundeck.mail.[project].[jobname].template.log.formatted=true/false (if true, prefix log lines with context information)

    # apply to any triggers not specified
    rundeck.mail.template.subject=[Default subject line]
    rundeck.mail.template.file=[path to template file]
    rundeck.mail.template.log.formatted=true/false (if true, prefix log lines with context information)

If a template filepath ends with `.md` or `.markdown`, then it will be interpreted as a Markdown formatted template.  Otherwise it is expected that the template file contains HTML.

The Subject line, filepath, and file contents can all contain embedded property references of the form `${group.key}`.  The available properties are mostly the same as those available for Notification Plugins, including the `execution.*` and `job.*` values.  See [Plugin Development - Notification Plugin - Execution Data][].

[Plugin Development - Notification Plugin - Execution Data]: ../developer/notification-plugin.html#execution-data

The "Context Variables" values used within the execution are available just as they are in the execution, so options would be available as `${option.name}`.

In addition these properties are defined:

* `rundeck.href`: URL to the Rundeck server
* `notification.trigger`: Trigger name
* `notification.eventStatus`: A string indicating the combination of execution status, and notification trigger, suitable for an email subject line, such as "KILLED", "FAILURE", "STARTING", "SUCCESS".
* `execution.projectHref`: URL to the Project within Rundeck.

## Troubleshooting

See the [service.log](logging.html#service.log) for mail error messages.

