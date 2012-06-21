% Plugin Development
% Greg Schueler
% November 20, 2010

There are currently two ways to develop plugins:

1. Develop Java code that is distributed within a Jar 
file.  See [Java plugin development](plugin-development.html#java-plugin-development).
2. Write shell/system scripts that implement your desired behavior and put them
in a zip file with some metadata.   See [Script Plugin Development](plugin-development.html#script-plugin-development).

Either way, the resultant plugin archive file, either a .jar java archive, 
or a .zip file archive, will be placed in the `$RDECK_BASE/libext` dir.

## Java Plugin Development

Java plugins are distributed as .jar files containing the necessary classes for 
one or more service provider, as well as any other java jar dependency files.

The `.jar` file you distribute must have this metadata within the main Manifest
for the jar file to be correctly loaded by the system:

* `Rundeck-Plugin-Version: 1.0`
* `Rundeck-Plugin-Archive: true`
* `Rundeck-Plugin-Classnames: classname,..`
* `Rundeck-Plugin-Libs: lib/something.jar ...` *(optional)*

Each classname listed must be a valid "Provider Class" as defined below.

Additionally, you should include a manifest entry to indicate the plugin file's version:

* `Rundeck-Plugin-File-Version: 1.x`

This version number will be used to load only the newest plugin file, if more than one provider of
the same name and type is defined.

### Provider Classes

A "Provider Class" is a java class that implements a particular interface and declares
itself as a provider for a particular Rundeck "Service".  

Each plugin also defines a "Name" that identifies it for use in Rundeck.  The Name
of a plugin is also referred to as a "Provider Name", as the plugin class is a
provider of a particular service.

You should choose a unique but simple name for your provider.

Each plugin class must have the "com.dtolabs.rundeck.core.plugins.Plugin"
annotation applied to it.

    @Plugin(name="myprovider", service="NodeExecutor")
    public class MyProvider implements NodeExecutor{
    ...

Your provider class must have at least a zero-argument constructor, and optionally 
can have a single-argument constructor with a 
`com.dtolabs.rundeck.core.common.Framework` parameter, in which case your
class will be constructed with this constructor and passed the Framework
instance.

You may log messages to the ExecutionListener available via 
`ExecutionContext#getExecutionListener()` method.

You can also send output to `System.err` and `System.out` and it will be 
captured as output of the execution.

### Jar Dependencies

If your Java classes require external libraries that are not included with
the Rundeck runtime, you can include them in your .jar archive. (Look in
`$RDECK_BASE/tools/lib` to see the set of
third-party jars that are available for your classes by default at runtime).

Specify the `Rundeck-Plugin-Libs` attribute in the Main attributes of the
Manifest for the jar, set the value to a space-separated list of jar file names
as you have included them in the jar.

E.g.:

    Rundeck-Plugin-Libs: lib/somejar-1.2.jar lib/anotherjar-1.3.jar

Then include the jar files in the Plugin's jar contents:

    META-INF/
    META-INF/MANIFEST.MF
    com/
    com/mycompany/
    com/mycompany/rundeck/
    com/mycompany/rundeck/plugin/
    com/mycompany/rundeck/plugin/test/
    com/mycompany/rundeck/plugin/test/TestNodeExecutor.class
    lib/
    lib/somejar-1.2.jar
    lib/anotherjar-1.3.jar

### Available Services:

* `NodeExecutor` - executes a command on a node
* `FileCopier` - copies a file to a node
* `ResourceModelSource` - produces a set of Node definitions for a project
* `ResourceFormatParser` - parses a document into a set of Node resources
* `ResourceFormatGenerator` - generates a document from a set of Node resources

## Provider Lifecycle

Provider classes are instantiated when needed by the Framework object, and the
instance is retained within the Service for future reuse. The Framework
object may exist across multiple executions, and the provider instance may be
reused.

Provider instances may also be used by multiple threads.

Your provider class should not use any instance fields and should be
careful not to use un-threadsafe operations.

### Node Executor Providers

A Node Executor provider executes a certain command on a remote or 
local node.

Your provider class must implement the `com.dtolabs.rundeck.core.execution.service.NodeExecutor` interface:

    public interface NodeExecutor {
        public NodeExecutorResult executeCommand(ExecutionContext context, 
            String[] command, INodeEntry node) throws ExecutionException;
    }


A Node Executor can be me made Configurable on a per-project basis via the Web GUI by
implementing the `com.dtolabs.rundeck.core.plugins.configuration.Describable`
interface. It is up to your plugin implementation to use configuration properties
from the `FrameworkProject` instance to configure itself. You must also be sure
to return an appropriate mapping in the `getPropertiesMapping` method of the `Description` interface to declare the property names to be used in the 
`project.properties` file.

More information is available in the Javadoc.

### File Copier Providers

A File Copier provider copies a file or script to a remote
or local node.

Your provider class must implement the `com.dtolabs.rundeck.core.execution.service.FileCopier` interface:

    public interface FileCopier {
        public String copyFileStream(final ExecutionContext context, InputStream input, INodeEntry node) throws
            FileCopierException;

        public String copyFile(final ExecutionContext context, File file, INodeEntry node) throws FileCopierException;

        public String copyScriptContent(final ExecutionContext context, String script, INodeEntry node) throws
            FileCopierException;
    }


A File Copier can be me made Configurable on a per-project basis via the Web GUI by
implementing the `com.dtolabs.rundeck.core.plugins.configuration.Describable`
interface. It is up to your plugin implementation to use configuration properties
from the `FrameworkProject` instance to configure itself. You must also be sure
to return an appropriate mapping in the `getPropertiesMapping` method of the `Description` interface to declare the property names to be used in the 
`project.properties` file.

More information is available in the Javadoc.

### Resource Model Source Providers

A Resource Model Source provider is actually a Factory class.  An instance of your Resource Model Source provider will be
re-used, so each time a new Resource Model Source with a new configuration is required, your Factory class
will be invoked to produce it.

Your provider class must implement the `com.dtolabs.rundeck.core.resources.ResourceModelSourceFactory` interface:

    public interface ResourceModelSourceFactory {
        /**
         * Return a resource model source for the given configuration
         */
        public ResourceModelSource createResourceModelSource(Properties configuration) throws ConfigurationException;
    }

A Resource Model Source provider can also be Configurable via the Web GUI by
implementing the `com.dtolabs.rundeck.core.plugins.configuration.Describable`
interface. This allows you to return a descriptor of the configuration parameters for your plugin, which is used by the GUI to render a web form. The
properties the user configures are stored in the `project.properties` configuration file, and are passed to your factory method as the `configuration`
properties.

More information is available in the Javadoc.

### Resource Format Parser and Generator Providers

Resource format Parser and Generator providers are used to serialize a set of
Node resources into a textual format for transport or storage.

Each Parser and Generator must declare the set of filename extensions (such as "xml" or "json") that it supports, as well as the set of MIME types that it supports (such as "text/xml" or "application/json".)  This lets other services retrieve the appropriate
parser or generator when all that is known about the source or destination of serialized data is a filename or a MIME type.

For Parsers, your provider class must implement the `com.dtolabs.rundeck.core.resources.format.ResourceFormatParser` interface:

    public interface ResourceFormatParser {
        /**
         * Return the list of file extensions that this format parser can parse.
         */
        public Set<String> getFileExtensions();
    
        /**
         * Return the list of MIME types that this format parser can parse. This may include wildcards such as
         * "*&#47;xml".
         */
        public Set<String> getMIMETypes();
    
        /**
         * Parse a file
         */
        public INodeSet parseDocument(File file) throws ResourceFormatParserException;
    
        /**
         * Parse an input stream
         */
        public INodeSet parseDocument(InputStream input) throws ResourceFormatParserException;
    }


For Generators, your provider class must implement the `com.dtolabs.rundeck.core.resources.format.ResourceFormatGenerator` interface:

    public interface ResourceFormatGenerator {
    
        /**
         * Return the list of file extensions that this format generator can generate
         */
        public Set<String> getFileExtensions();
    
        /**
         * Return the list of MIME types that this format generator can generate. If more than one
         * are returned, then the first value will be used by default if necessary.
         */
        public List<String> getMIMETypes();
    
        /**
         * generate formatted output
         */
        public void generateDocument(INodeSet nodeset, OutputStream stream) throws ResourceFormatGeneratorException,
            IOException;
    }

More information is available in the Javadoc.

## Script Plugin Development

Script plugins can provide the same services as Java plugins, but they do so
with a script that is invoked in an external system processes by the JVM.

These Services support Script Plugins:

* `NodeExecutor`
* `FileCopier`
* `ResourceModelSource`

**Note:** Currently, the Resource Format Parser and Generator services *do not* support Script Plugins.

You must create a zip file with the following structure:

    [name]-plugin.zip
    \- [name]-plugin/ -- root directory of zip contents, same name as zip file
       |- plugin.yaml -- plugin metadata file
       \- contents/
          |- ...      -- script or resource files
          \- ...

Here is an example:

    $ unzip -l example-1.0-plugin.zip 
    Archive:  example-1.0-plugin.zip
      Length     Date   Time    Name
     --------    ----   ----    ----
            0  04-12-11 11:31   example-1.0-plugin/
            0  04-11-11 15:31   example-1.0-plugin/contents/
         2142  04-11-11 15:31   example-1.0-plugin/contents/script1.sh
         1591  04-11-11 13:10   example-1.0-plugin/contents/script2.sh
          576  04-12-11 10:58   example-1.0-plugin/plugin.yaml
     --------                   -------
         4309                   5 files

The filename of the plugin zip must end with "-plugin.zip" to be recognized as a
plugin archive. The zip must contain a top-level directory with the same base name 
as the zip file (sans ".zip").

The file `plugin.yaml` must have this structure:

    # yaml plugin metadata
    
    name: plugin name
    version: plugin version
    rundeckPluginVersion: 1.0
    author: author name
    date: release date
    providers:
        - name: provider
          service: service name
          plugin-type: script
          script-interpreter: [interpreter]
          script-file: [script file name]
          script-args: [script file args]

The main metadata that is required:

* `name` - name for the plugin
* `version` - version number of the plugin
* `rundeckPluginVersion` - Rundeck Plugin type version, currently "1.0"
* `providers` - list of provider metadata maps

These are optional:

* `author` - optional author info
* `date` - optional release date info

This provides the necessary metadata about the plugin, including one or more 
entries in the `providers` list to declare those providers defined in the plugin.

### Provider metadata

Required provider entries:

* `name` - provider name
* `service` - service name, one of these valid services:
    * `NodeExecutor`
    * `FileCopier`
    * `ResourceModelSource`
* `plugin-type` - must be "script" currently.
* `script-file` - must be the name of a file relative to the `contents` directory

For `ResourceModelSource` service, this additional entry is required:

* `resource-format` - Must be the name of one of the supported [Resource Model Document Formats](../manual/rundeck-basics.html#resource-model-document-formats).

Optional entries:

* `script-interpreter` - A system command that should be used to execute the 
    script.  This can be a single binary path, e.g. `/bin/bash`, or include
    any args to the command, such as `/bin/bash -c`.
* `script-args` - the arguments to use when executing the script file.
* `interpreter-args-quoted` - true/false - (default false). If true, the execution will 
    be done by passing the file and args as a single argument to the interpreter:
     `${interpreter} "${file} ${arg1} ${arg2}..."`. If false,
    the execution will be done by passing the file and args as separate arguments:
     `${interpreter} ${file} ${arg1} ${arg2}...`

### Configurable Resource Model Source Script Plugin
 
The `ResourceModelSource` service allows the plugins to be configured via the Rundeck Web GUI. You are thus able to declare configuration properties for
your plugin, which will be displayed as a web form when the Project is configured, or can be manually configured in the `project.properties` file.

You can use these metadata entries to declare configuration properties for your
plugin:

Create a `config` entry in each provider definition, containing a sequence of
map entries for each configuration property you want to define. In the map entry include:

* `type` - The type of property.  Must be one of:
    * `String`
    * `Boolean` must be "true" or "false"
    * `Integer`
    * `Long`
    * `Select` must be on of a set of values
    * `FreeSelect` may be one of a set of values
* `name` - Name to identify the property
* `title` - Title to display in the GUI (optional)
* `description` - Description to display in the GUI (optional)
* `required` - (true/false) if true, require a non-empty value (optional)
* `default` - A default value to use (optional)
* `values` - A comma-separated list of values to use for Select or FreeSelect. Required for Select/FreeSelect.

When your script is invoked, each configuration property defined for the plugin will be set as `config.NAME` in the data context passed to your script (see below).

Here is an example `providers` section for a Resource Model Source plugin that asks for two input properties from the user and produces "resourceyaml" formatted output:

    providers:
        - name: mysource
          service: ResourceModelSource
          plugin-type: script
          script-interpreter: bash -c
          script-file: generate.sh
          resource-format: resourceyaml
          config:
            - type: Integer
              name: count
              title: Count
              description: Enter the number of nodes to generate
            - type: FreeSelect
              name: flavor
              title: Flavor
              description: Select a flavor
              required: true
              default: vanilla
              values: vanilla,blueberry,strawberry,chocolate

### How script plugin providers are invoked

When the provider is used for node execution or file copying, the script file,
interpreter, and args are combined into a commandline executed by the system in
this pattern:

    [interpreter] [filename] [args...]

If the interpreter is not specified, then the script file is executed directly,
and that means it must be acceptable by the system to be executed directly (include
any necessary `#!` line, etc).

`script-args`  can
contain data-context properties such as `${node.name}`.  Additionally, the
specific Service will provide some additional context properties that can be used:

* NodeExecutor will define `${exec.command}` containing the command to be executed
* FileCopier will define `${file-copy.file}` containing the local path to the file
that needs to be copied.
* ResourceModelSource will define `${config.KEY}` for each configuration property KEY that is defined.

All script-plugins will also be provided with these context entries:

* `rundeck.base` - base directory of the Rundeck installation
* `rundeck.project` - project name
* `plugin.base` - base directory of the expanded 'contents' dir of the plugin
* `plugin.file` - the plugin file itself
* `plugin.scriptfile` - the path to the script file being executed for the plugin
* `plugin.vardir` - var dir the plugin can use for caching data, local to the project
* `plugin.tmpdir` - temp dir the plugin can use

In addition, all of the data-context properties that are available in the
`script-args` are provided as environment variables to the 
script or interpreter when it is executed.

Environment variables are generated in all-caps with this format:

    RD_[KEY]_[NAME]

The `KEY` and `NAME` are the same as `${key.name}`. Any characters in the key or name
that are not valid Bash shell variable characters are replaced with underscore '_'.

Examples:

* `${node.name}` becomes `$RD_NODE_NAME`
* `${node.some-attribute}` becomes `$RD_NODE_SOME_ATTRIBUTE`
* `${exec.command}` becomes `$RD_EXEC_COMMAND`
* `${file-copy.file}` becomes `$RD_FILE_COPY_FILE`

### Script provider requirements

The specific service has expectations about
the way your provider script behaves:

* Exit code of 0 indicates success
* Any other exit code indicates failure

For `NodeExecutor`

:   All output to `STDOUT`/`STDERR` will be captured for the job's output

For `FileCopier`

:   The first line of output of `STDOUT` MUST be the filepath of the file copied 
    to the target node.  Other output is ignored. All output to `STDERR` will be
    captured for the job's output.

For `ResourceModelSource`

:   All output on `STDOUT` will be captured and passed to a `ResourceFormatParser` for the specified `resource-format` to create the Node definitions.

