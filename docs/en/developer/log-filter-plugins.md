% Log Filter Plugins
% Greg Schueler
% April 26, 2018

## About

Log Filter plugins provide a way to process logging output from commands, scripts and other workflow step types. They can filter the output (remove, add, modify), or they can be used to capture or convert the output.  They can also add metadata to the logs, or emit new log data.

Log Filter plugins implement the [LogFilterPlugin] interface, and provider the `LogFilter` service.

## Behavior

Users creating Workflows can configure Log Filter plugins within their workflow in two ways:

1. At the top level, applying to all steps in the workflow
2. At a step level, applying only to specific step(s).


## Java Plugin Type

* *Note*: Refer to [Java Development](plugin-development.html#java-plugin-development) for information about developing a Java plugin for Rundeck.

The plugin interface is [LogFilterPlugin][]. You can use the [PluginLoggingContext] to get the data context and add data to it via the Output context.

When the top level, or step level is ready, all configured Log Filter plugins will be initialized using the optional `init` method.

For each log event emitted within the configured level, each configured Log Filter plugin will have its `handleEvent` method called, in order,
with a [LogEventControl] instance. The LogEventControl can be used to get or modify the content of the [LogEvent] (message, type, level, metadata).
It can also control how the log event is handled:

* `emit`: the log should be emitted
* `quell`: the log should be futher processed, but not emitted in the final log
* `quiet`: the final loglevel should be set to VERBOSE
* `remove`: the log event should not be processed further

[LogFilterPlugin]: ../javadoc/com/dtolabs/rundeck/plugins/logging/LogFilterPlugin.html
[PluginLoggingContext]: ../javadoc/com/dtolabs/rundeck/core/logging/PluginLoggingContext.html
[LogEventControl]: ../javadoc/com/dtolabs/rundeck/core/logging/LogEventControl.html
[LogEvent]: ../javadoc/com/dtolabs/rundeck/core/logging/LogEvent.html

### Groovy LogFilter

Create a groovy script that calls the `rundeckPlugin` method and passes the `LogFilterPlugin` as the type of plugin:

~~~~~ {.java}
import com.dtolabs.rundeck.plugins.logging.LogFilterPlugin
rundeckPlugin(LogFilterPlugin){
    //plugin definition
}
~~~~~~

To define metadata about your plugin, and configuration properties, see the [Plugin Development - Groovy Plugin Development](plugin-plugin.html#groovy-plugin-development) chapter.

The `LogFilterPlugin` Groovy DLS supports these closure definitions:

* `init` initialization for the plugin (optional)
* `handleEvent` handle a log event (required)
* `complete` complete the log filter processing after all events have been handled (optional)

Closure descriptions:

`init`

~~~~~ {.java}
/**
 * Called to initialization the plugin with the context
 */
init { PluginLoggingContext context, Map configuration ->
    //perform initialization
}
~~~~~~

`handleEvent`

~~~~~ {.java}
/**
 * Called to initialization the plugin with the context
 */
handleEvent { PluginLoggingContext context, LogEventControl event, Map configuration ->
    //handle the event
}
~~~~~~

`complete`

~~~~~ {.java}
/**
 * Called to complete the plugin processing
 */
complete { PluginLoggingContext context, Map configuration ->
    //finish
}
~~~~~~

## Localization

For the basics of plugin localization see: [Plugin Development - Plugin Localization][].

## Example

Several built-in plugins are listed here:

* [`rundeckapp/src/groovy/com/dtolabs/rundeck/server/plugins/logging`](https://github.com/rundeck/rundeck/tree/master/rundeckapp/src/groovy/com/dtolabs/rundeck/server/plugins/logging)

## Example Groovy plugins

See <https://github.com/rundeck/rundeck/tree/master/examples/example-groovy-log-filter-plugins>.

[Plugin Development - Plugin Localization]: plugin-development.html#plugin-localization