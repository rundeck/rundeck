% Content Converter Plugins
% Greg Schueler
% April 26, 2018

## About

Content Converter Plugins can convert log data into HTML or other data formats, to enable richer logs to be presented in the Rundeck GUI when viewing the Execution Output Logs.

In addition, Content Converters can be chained together in a limited way, allowing one plugin to do the work
of (say) converting a List of Java Strings into an HTML `<ol>`, while another plugin does the work of converting CSV formatted data
into a List of Strings. You could add another plugin which can convert log data into a List of Strings, and it would
also be rendered into a `<ol>` in the final output due to the first plugin.

Each Content Converter Plugin can be asked for "Data Types" that it can accept, and also describes the Output "Data Types" that it can produce.

A "Data Type" consist of a Java type (class), and a String such as `text/html`. 

## Behavior

Content Converter plugins are applied automatically to Log Output when viewing it in the Rundeck GUI.

However, the Log output must have certain metadata entries set for the Log Events.  Plain log output will not be
rendered in any special way (aside from ANSI Color rendering.)  

For this reason, usually a [Log Filter Plugin](log-filter-plugins.html) is used to annotate the log output with the correct data type when
used with Rundeck's Command or Script steps,
however custom Step plugins can add this metadata in the logs they emit.

## Log Metadata

Log events have Metadata (key/value strings) associated with them.

If a Log event has a `content-data-type` metadata value, Rundeck will attempt to chain together up to two Content Converter Plugins
to convert the specified data type into `text/html` for rendering in the GUI.  

Using a Log Filter plugin such as the "Render Formatted Data" built-in Log Filter Plugin allows adding adding the `content-data-type` to the output
of Commands or Script steps.

Additional metadata can be passed to the Content Converter plugins.  All log metadata entries with keys starting with `content-meta:` will be extracted from the
Log Event metadata, and the `content-meta:` prefix removed.

## Java Plugin Type

Plugins must implement the [ContentConverterPlugin] interface, and declare as a provider of server `ContentConverter`.

Methods:

* `boolean isSupportsDataType(Class<?> clazz, String dataType)`: called to detect if the plugin supports the input Data Type.
* `Class<?> getOutputClassForDataType(Class<?> clazz, String dataType)`: gets the Java Class for the input data type
* `String getOutputDataTypeForContentDataType(Class<?> clazz, String dataType)`: gets the data type string for the input data type.
* `Object convert(Object data, String dataType, Map<String,String> metadata)`: Convert the input data type to the output object, includes metadata about the log event as described in [Log Metadata](#log-metadata).

[ContentConverterPlugin]: ../javadoc/com/dtolabs/rundeck/plugins/logs/ContentConverterPlugin.html


### Groovy ContentConverter

Create a groovy script that calls the `rundeckPlugin` method and passes the `ContentConverterPlugin` as the type of plugin:

~~~~~ {.java}
import com.dtolabs.rundeck.plugins.logs.ContentConverterPlugin
rundeckPlugin(ContentConverterPlugin){
    //plugin definition
}
~~~~~~

To define metadata about your plugin, see the [Plugin Development - Groovy Plugin Development](plugin-plugin.html#groovy-plugin-development) chapter.

The `ContentConverterPlugin` Groovy DSL supports defining conversions between data types.

Data types are represented with the `DataType` class (internal to the groovy plugin builder),
and can be created by calling the `dataType(Class,String)` method with a Java Class, and a data type string.

The built-in `convert(DataType input, Datatype output, Closure closure)` method allows you to define conversions from one
Data Type to another.  Your closure will be called with the input data, and is expected to return the output data.
Returning `null` will simply skip the conversion.

#### convert declaration

Call `convert` using explicit data types and a closure to define the conversion:

~~~~~ {.java}
/**
 * Converts two data types
 */
convert(dataType(String,'application/x-my-data'), dataType(String,'text/html')) { 
    //properties available via delegation:
    // data: the input data
    // metadata: input metadata map
    // dataType: input DataType
    
    //return type must match the output Java class in the DataType:
    return "hello ${data}, it seems you are ${metadata.mood?:'happy'}."
}
~~~~~~

When the DataType uses a Java String as its class, you can omit calling `dataType`,
and simply pass the dataType string:

~~~~~ {.java}
/**
 * Called to convert two data types
 */
convert('application/x-my-data', 'text/html') { 
	//data is a String, and we should return a String
	return '<b>'+data+' more data</b>'
}
~~~~~~

And if you are going to return `text/html` the output declaration can be skipped:

~~~~~ {.java}
/**
 * Called to convert two data types
 */
convert('application/x-my-data') { 
	//return type defaults to String and datatype 'text/html'
	return '<b>'+data+' more data</b>'
}
~~~~~~

Since Rundeck will chain together up to two ContentConverters to render `text/html` for a given
input data type, you can define multiple conversion, if you want to use
an intermediate type.

~~~~~ {.java}
/**
 * Convert a string into an intermediate java type
 */
convert('application/x-my-data-type', dataType(SomeClass,'application/x-another-type')) { 
	//use an intermediate object
	return new SomeClass(data)
}
/**
 * Expect the intermediate type as input, and default to HTML output
 */
convert(dataType(SomeClass,'application/x-another-type')) { 
	//now `data` will be a SomeClass object
	return data.generateHtml()
}
~~~~~~


## Localization

For the basics of plugin localization see: [Plugin Development - Plugin Localization][].

## Example

Several built-in plugins are listed here:

* [`rundeckapp/src/groovy/com/dtolabs/rundeck/server/plugins/logs`](https://github.com/rundeck/rundeck/tree/master/rundeckapp/src/groovy/com/dtolabs/rundeck/server/plugins/logs)

Several of the built-in rundeck plugins convert their input into a "Data Type" of: A Java List or Map object and type name of `application/x-java-map-or-list`.  This "Data Type" can be rendered to HTML via the [HTMLTableViewConverterPlugin].

Your plugins can make use of this built-in plugin and therefore do not have to convert directly to HTML.
See the [JsonConverterPlugin] for an example.


[JsonConverterPlugin]: https://github.com/rundeck/rundeck/blob/master/rundeckapp/src/groovy/com/dtolabs/rundeck/server/plugins/logs/JsonConverterPlugin.groovy
[HTMLTableViewConverterPlugin]: https://github.com/rundeck/rundeck/tree/master/rundeckapp/src/groovy/com/dtolabs/rundeck/server/plugins/logs/HTMLTableViewConverterPlugin.groovy

## Example Groovy plugins

See <https://github.com/rundeck/rundeck/tree/master/examples/example-groovy-content-converter-plugins>.

[Plugin Development - Plugin Localization]: plugin-development.html#plugin-localization