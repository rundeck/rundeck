% Notification Plugin
% Greg Schueler
% April 18, 2013

## About 

Notifications are actions that are performed when a Job starts or finishes.

Currently there are three conditions that can trigger notifications:

* `onstart` - the Job started
* `onsuccess` - the Job completed without error
* `onfailure` - the Job failed or was aborted

Rundeck has two built-in notification types that can be configured for Jobs:

1. Send an email to a list of addresses
2. POST XML to a list of URLs

This chapter discusses how to create your own notification plugins.

## Plugin execution

When a notification is defined for a Job, and the associated trigger occurs, your plugin will be executed
and passed in two sets of Map data:

1. Configuration data - the user-supplied configuration for the plugin
2. Execution data - information about the Job and Execution for the notification

### Configuration data

The Configuration data is fully custom depending on your plugin, and is described in the [Plugin configuration properties](#plugin-configuration-properties) section.

#### Property References

The specific data values of the Configuration section are allowed to have 
embedded Property References as described in the 
[Jobs - Context Variables](../manual/jobs.html#context-variables) section.

For example, when a user configures your plugin, they could embed an option value using: `${option.myoption}`.  This value will be replaced with the runtime option value before being passed to your plugin.

When defining Configuration properties that use custom Validation, keep in mind
that the value set by a user may have such an embedded property reference and
therefore may not pass the validation rules you have defined.  If you want to
allow these property references for a Configuration property, it must be a String type property, and any custom validation code should allow the embedded
property references, for example by looking for a '${' sequence and allowing
the value.

### Execution data

The execution data is included as a Map called `execution` 
containing the following keys and values:

`execution.id`: ID of the execution

`execution.href`: URL to the execution output view

`execution.status`: Execution state ('running','failed','aborted','succeeded')

`execution.user`: User who started the job

`execution.dateStarted`: Start time (java.util.Date)

`execution.dateStartedUnixtime`: Start time as milliseconds since epoch (long)

`execution.dateStartedW3c`: Start time as a W3C formatted String

`execution.description`: Summary string for the execution

`execution.argstring`: Argument string for any job options

`execution.project`: Project name

`execution.loglevel`: Loglevel string ('ERROR','WARN','INFO','VERBOSE','DEBUG')

The following values may be available after the job is finished (not available for `onstart` trigger):

`execution.failedNodeListString`: Comma-separated list of any nodes that failed, if present

`execution.failedNodeList`: Java List of any node names that failed, if present

`execution.succeededNodeListString`: Comma-separated list of any nodes that succeeded, if present

`execution.succeededNodeList`: Java List of any node names that succeeded, if present

`execution.nodestatus`: Java Map containing summary counts of node success/failure/total, in the form: `[succeeded: int, failed: int, total: int]`

`execution.dateEnded`: End time (java.util.Date)

`execution.dateEndedUnixtime`: End time as milliseconds since epoch (long)

`execution.dateEndedW3c`: End time as W3C formatted string

`execution.abortedby`: User who aborted the execution

`job` information is in a `job` entry and contains another Map:

`job.id`: Job ID
 
`job.href`: URL to Job view page
 
`job.name`: Job name
 
`job.group`: Job group
 
`job.project`: Project name
 
`job.description`: Job Description
 
`job.averageDuration`: Average job duration in Milliseconds, if available

`execution.context` - this is a map containing all of the context variables available to the execution when it ran or will run, such as [Jobs - Context Variables](../manual/jobs.html#context-variables). The contents of this Map are the specific context namespaces and variables.
    
`execution.context.option`: a Map containing the Job Option keys/values.

`job`: a Map containing the Job context data, as provided to executions.  This map will contain some duplicate information as the `execution.job` map previously described.

In Groovy, you can simply reference any values in the Execution data maps using 
[Groovy Gpath](http://groovy.codehaus.org/GPath), e.g.:

~~~~ {.java}
println execution.context.option.myoption
~~~~~~~

## Plugin configuration properties

Each plugin can define a set of "configuration" properties which allow users to specify input that the plugin can
use when it operates.

Notification plugins support scoped properties, allowing some of the configuration to be defined, or defaulted, on a per-project or per-Rundeck instance basis.

## Plugin types

Rundeck supports two types of Notification plugins:

1. Java-based development deployed as a Jar file.
2. Groovy-based deployed as a single `.groovy` script.

Currently "script-based" plugins (shell scripts, that is) are not supported.

## Example code

See the source directory `examples/example-groovy-notification-plugins` for
examples of Notification plugins written in Groovy.

* On github: [example-groovy-notification-plugins](https://github.com/rundeck/rundeck/tree/development/examples/example-groovy-notification-plugins) 

See the source directory `examples/example-java-notification-plugin` for
Java examples.

* On github: [example-java-notification-plugin](https://github.com/rundeck/rundeck/tree/development/examples/example-java-notification-plugin) 

## Java Plugin Type

Java-based plugins can be developed just as any other Rundeck plugin, as described in the chapter [Plugin Development - Java Plugin Development](plugin-development.html#java-plugin-development).

These plugin classes should implement the interface
[NotificationPlugin](../javadoc/com/dtolabs/rundeck/plugins/notification/NotificationPlugin.html):

~~~~~~ {.java}
public interface NotificationPlugin {
    /**
     * Post a notification for the given trigger, dataset, and configuration
     * @param trigger event type causing notification
     * @param executionData execution data
     * @param config notification configuration
     */
    public boolean postNotification(String trigger,Map executionData,Map config);
}
~~~~~~~~~~~
    
To define configuration properties for your plugin, you use the same mechanisms as for Workflow Steps, described under the chapter [Plugin Development - Plugin Descriptions](plugin-development.html#plugin-descriptions).

The simplest way to do this is to use [Plugin Annotations](plugin-annotations.html). Here is an example class annotated to describe it to the Rundeck GUI:

~~~~~~ {.java}
@Plugin(service="Notification", name="example")
@PluginDescription(title="Example Plugin", description="An example Plugin for Rundeck Notifications.")
public class ExampleNotificationPlugin implements NotificationPlugin{
 
    @PluginProperty(name = "test" ,title = "Test String", description = "a description")
    private String test;
 
    public boolean postNotification(String trigger, Map executionData, Map config) {
        System.err.printf("Trigger %s fired for %s, configuration: %s\n",trigger,executionData,config);
        System.err.printf("Local field test is: %s\n",test);
        return true;
    }
}
~~~~~~~~~

## Groovy Plugin Type

For Notifications, we introduce a new way to develop plugins, using a simple Groovy-based DSL.  This gives you a simpler way to develop plugins, but still provides the power of Java.

To create a Groovy based plugin, create a file named `MyNotificationPlugin.groovy` in the plugins directory for Rundeck.

You must restart rundeck to make the plugin available the first time, but you can subsequently update the .groovy script without restarting Rundeck.

### Groovy DSL

Within the Groovy script, you define your plugin by calling the `rundeckPlugin` method, and pass it both the Class of the type of plugin, and a Closure used to build the plugin object.

~~~~~~~ {.java}
import  com.dtolabs.rundeck.plugins.notification.NotificationPlugin
rundeckPlugin(NotificationPlugin){
    //plugin definition goes here...
}
~~~~~~~~~~

In this case we use the same `NotificationPlugin` interface used for Java plugins.

#### Definition

Within the definition section you can define your plugin's Description to be shown in the Rundeck GUI, as well as
configuration properties to present to the user.

*Properties*

Set these properties to change the GUI display of your plugin:

~~~~~ {.java}
title='My Plugin'
description='Does some action'
~~~~~~~~~

*Configuration*

Use a `configuration` closure to define configuration properties:

~~~~~ {.java}
configuration{
    //property definitions go here...
}
~~~~~~~~

*Property Definitions*

User configuration properties can be defined in a few ways. To define a property within the `configuration` section, you can use either of these forms:

1. method call form, specifying the attributes of the property:

~~~~~ {.java}
myproperty (title: "My Property", description: "Something", type: 'Integer')
~~~~~~~~~

2. assignment form. This form guesses the data type and sets the defaultValue, but does not add any other attributes.

~~~~~ {.java}
myproperty2="default value"
//the above is equivalent to:
myproperty2(defaultValue:"default value", type: 'String')
 
myproperty3=["value","another","text"]
//the above is equivalent to:
myproperty3(type:'FreeSelect',values:["value","another","text"])
~~~~~~~~

Each property has several attributes you can define, but only `name` and `type` are required:

* `name` - the unique identifier for this property
* `type` - the data type to use for the property, defaults to String.  Available types:
    * `String` - user can enter text
    * `Integer`, `Long` - user can enter a number
    * `Boolean` - user is shown a checkbox
    * `Select` or `FreeSelect` - user can choose from a list. With `FreeSelect`, the user can also type in any value
* `title` - a user-readable string to describe the property
* `description` - a string describing the property
* `required` - whether the property is required to have a value
* `defaultValue` - any default value for the property
* `scope` - defines the scope for the property.  Allowed values are described under the chapter [Plugin Annotations - Property Scopes](plugin-annotations.html#property-scopes). You may also simply use a String matching the name of the scope, e.g. "Instance".  The default scope if unspecified is "Instance".

In addition to these properties, for `Select` or `FreeSelect` type, you can define:

* `values` - list of string values the user can select from

To define a validation check for a property, use the first form and supply a closure. The implicit `it` variable will be the value of the property to check, and your closure should return `true` if the value is valid.

~~~~~~ {.java}
phone_number(title: "Phone number"){
   it.replaceAll(/[^\d]/,'')==~/^\d{10}$/
}
~~~~~~~

**A Note about Scopes and Validation**:

The user is presented with any `Instance` scoped properties in the Rundeck GUI when defining a Job, and any invalid configuration values will present an error when saving the Job.  This includes failing to set a value for a "required" property.  However, if you have properties that are scoped for `Project` or lower, those properties will not be shown in the GUI. In that case, the validation will not be checked for the properties when saving the Job definition, and will only be performed when the Notification is triggered.

### Notification handlers

For a `NotificationPlugin`, you can define custom handlers for each of the notification triggers (`onsuccess`, `onfailure`, and `onstart`).

Simply define a closure with the given trigger name, and return a true value if your action was successful:

~~~~~~ {.java}
onstart{ Map execution, Map configuration ->
    //perform an action using the execution and configuration
    println "Job ${execution.job.name} has been started by ${execution.user}..."
    return true
}
onsuccess{ Map execution, Map configuration ->
    //perform an action using the execution and configuration
    println "Success! Job ${execution.job.name} worked fine."
    return true
}
onfailure{ Map execution, Map configuration ->
    //perform an action using the execution and configuration
    println "Oh No! Job ${execution.job.name} didn't work out."
    return true
}
~~~~~~~~

If your closure returns a `false` value, then Rundeck will log an error in the server log.

### Example

Here is a minimal example:

**MinimalNotificationPlugin.groovy**:

~~~~~ {.java}
import com.dtolabs.rundeck.plugins.notification.NotificationPlugin;
 
rundeckPlugin(NotificationPlugin){
    onstart { 
        println("job start: data ${execution}")
        true
    }
 
    onfailure {
        println("failure: data ${execution}")
        true
    }
 
    onsuccess {
        println("success: data ${execution}")
        true
    }
}
~~~~~~~~~~

Here is a full example showing plugin GUI metadata, configuration properties, and
alternate closure parameter lists:

**MyNotificationPlugin.groovy**:

~~~~~~ {.java}
import com.dtolabs.rundeck.plugins.notification.NotificationPlugin;
 
rundeckPlugin(NotificationPlugin) {
    title="Example Plugin"
    description="An example"
 
    configuration{
 
        test1 title:"Test1", description:"Simple string"
        
        //Validation can be added with a closure
        test2(title:'Test2',description:"Matches a regex"){
            it=~/^\d+$/
        }
        
        //required select value, becomes a Select type
        test3 values: ["a","b","c"], required:true
        
        //if not required, becomes a FreeSelect
        test4 values: ["a","b","c"]
        
        //If type is not specified, the defaultValue will be used to guess
        test5 defaultValue: 3 //becomes Integer type
        test6 defaultValue:true //becomes Boolean type
 
        //these properties are assigned default values and automatically typed
        test7=123
        test8="abc"
        test9=true
        test10=false
        test11=["x","y","z"] //becomes a FreeSelect
 
        //redefining the same property will modify it
        test11 title:"My Select Field", description:"Free Select field", defaultValue:"y", required:true
 
        //the scope indicates the property will not show up in the GUI when configuring the Notification, but must be defined in the project.properties or framework.properties at runtime
        test11 required:true, scope: 'Project'
        
    }
 
    onstart { Map executionData,Map config ->
        println("script, start: data ${executionData}, config: ${config}")
        true
    }
 
    onfailure { Map executionData ->
        //Single argument, the configuration properties are available automatically
        println("script, failure: data ${executionData}, test1: ${test1}, test2: ${test2} test3: ${test3}")
        true
    }
 
    onsuccess {
        //with no args, there is a "configuration" and an "execution" variable in the context
        println("script, success: data ${execution}, test1: ${configuration.test1}, test2: ${configuration.test2} test3: ${configuration.test3}")
        true
    }
}

~~~~~~~~~
