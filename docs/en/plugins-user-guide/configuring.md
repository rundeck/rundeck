% Configuring

## Configuring Plugins for Use

### Workflow Steps

Workflow Step and Workflow Node Step providers are used to define custom steps that
can be performed in Workflows.

You can select a step plugin in the GUI when adding a new step to a Workflow.  You
will be prompted to enter any configuration properties for the step, and can save those
configuration values into your workflow.

If you are defining a workflow in an XML/Yaml-formatted file, you can
specify the configuration properties there.

Each configuration property has a "scope" defined by the provider. Scoped properties 
allow default values to be specified at Framework (application) or Project level 
configuration properties.  Properties can also be defined to only
exist at Framework or Project levels. 

1. Instance (Job) scope: the property values defined in the Job definition
2. Project scope: property values defined in the Project's *project.properties* file
3. Framework (Application) scope: property values defined in Rundeck's *framework.properties* file.

When determining the property value to use, Rundeck will evaluate the most-specific scope first (Instance level), and
then widen the scope to Project, then Framework definitions.

When you create a Job in the Rundeck GUI, you will be shown the Instance-scope properties
as part of the GUI Workflow Builder for any plugin step that you add to your workflow.

When a property can be configured at the framework/project level, you will be able to define it like this:

*Framework scope property definition in `framework.properties`*

    framework.plugin.[ServiceName].[providerName].[property]=value

*Project scope property definition in `project.properties`*

    project.plugin.[ServiceName].[providerName].[property]=value

### Node Execution 

The two *Node services*, Node Executor and File Copier, are both configured similarly.
They are configured for particular nodes on a node-specific basis,
or set as a default provider for a project or for the system.

If multiple providers are defined the most specific definition takes precedence
in this order:

1. Node specific
2. Project scope
3. Framework scope

#### Node Specific

To enable a provider for a node, add an attribute to the node definition.

*Node Executor provider attributes*:

`node-executor`

:    specifies the provider name for a non-local node.

`local-node-executor`

:    specifies the provider name for the local (server) node.


*FileCopier provider attributes*:

`file-copier`

:    specifies the provider by name for a non-local node.

`local-file-copier`

:    specifies the provider by name for the local (server) node.

Example Node in YAML specifying `stub` NodeExecutor and FileCopier:

~~~~~~~ {.yaml}
remotehost:
    hostname: remotehost
    node-executor: stub
    file-copier: stub
~~~~~~~~~

#### Project or Framework Scope

*Node Executor*

You can define the default connection providers to use for nodes at either the Project or
Framework scope (or both).  To do so, configure any of the following properties
in the `project.properties` or the `framework.properties` files.  

`service.NodeExecutor.default.provider`

:   Specifies the default NodeExecutor provider for remote nodes

`service.NodeExecutor.default.local.provider`

:   Specifies the default Node Executor provider for the  local node.

*File Copier*

`service.FileCopier.default.provider`

:   Specifies the default File Copier provider for remote nodes.

`service.FileCopier.default.local.provider`

:   Specifies the default File Copier provider for the local node.

Example `project.properties` to set default local providers to `stub`:

    service.NodeExecutor.default.local.provider=stub
    service.FileCopier.default.local.provider=stub

### Resource Model Sources

The *Resource Model Sources* providers can be configured for a single project 
in the `project.properties` file.

You can define multiple Resource Model Sources for the project, and can mix and match
the specific providers depending on your needs.

When you define multiple Source providers in a project, then the resulting set of Nodes will 
effectively be a merge of all the sources, in the order in which they are declared. This
means that if two or more Sources provide a definition of a node with the same name, then
the definition from lowest Source in the list will be used.

The order that the providers are loaded (and thus the nodes are merged) is:

1. `project.resources.file`: A File Model Source with default configuration.
2. `project.resources.url`: A URL Model Source with default configuration. (optional)
3. All `resources.source.N` configurations in order starting at 1

#### Resource Model Source configuration

The `project.properties` file for each project allows you to configure the Resource Model Sources in these ways:

* Define `project.resources.file` - this file path is used as a File Source path, with *autogeneration* and *includeServerNode* both true.
* Define `project.resources.url` - this URL is used as a URL Source url, with caching enabled

You may also define a list of more sources in this way:

Starting at index `1`, define these properties for your Source numbered `N`:

    resources.source.N.type=<provider-name>
    resources.source.N.config.<property>=<value>
    resources.source.N.config.<property2>=<value2>
    ...

Using one of the available Resource Model Source provider names for the `<provider-name>` value. For each Resource Model Source provider, 
you can specify the configuration properties for the source.

Example project.properties configuration of a default File provider, and two other providers:

    project.resources.file=/home/rundeck/projects/example/etc/resources.xml
    
    resources.source.1.type=url
    resources.source.1.url=http://server/nodes.yaml
    
    resources.source.2.type=directory
    resources.source.2.directory=/home/rundeck/projects/example/resources

### Resource Format Generators and Parsers

Resource Format Generators and Parsers define support for file formats that can
be generated from or parsed into a set of Resource Node definitions.

These are used by other parts of the system, such as the Resource Model Sources.

There is no configuration necessary to use these providers, however the specific
Provider Name that each generator and parser defines has to be known in order
to make use of the provider.  The specific Provider Name is used as the 
"format name" when you want to use the parser or generator.

For example, to enable a particular Resource Format parser to be used by a File
Resource Model Source (see [File Resource Model Source Configuration](resource-model-source-plugins.html#file-resource-model-source-configuration)), you should specify
the Provider Name for the parser as the format for the source:

    resources.source.1.format=myformat

This would specify the use of "myformat" provider.

In other cases, the exact name of the provider may not be known (for example 
when loading content from a remote URL).  Each Generator and Parser must define
a list of MIME Type strings and file extensions that they support. These 
are used to determine which parser/generator is to be used.

### Notifications

Notifications can be configured for Jobs, and can be triggered when certain
conditions occur.  These conditions are called "triggers", these are the 
currently available triggers:

* `onstart` - the Job started
* `onsuccess` - the Job succeeded
* `onfailure` - the Job failed

When you define the Job in the GUI or via [XML](../man5/job-v20.html#notification) or
[Yaml](../man5/job-yaml-v12.html#notification), you can add any of the available Notification plugin types to happen for
any of the possible triggers.  Each Notification plugin type may have unique
configuration properties that you can specify. Each combination of trigger and
 Notification type has a unique configuration.

When defining configuration values for a plugin, you can usually substitute
any "Job context variables" that are listed under [User Guide - Jobs - Context Variables](../manual/jobs.html#context-variables). (Note: Some configuration properties of a plugin may not support this feature.)

In addition, you can also use these variables:

* `${job.user.name}` - the user who executed the job
* `${job.user.email}` - the email of the executing user if set in their user profile
* `${job.user.firstName}` - the first name of the executing user if set in their user profile
* `${job.user.lastName}` - the first name of the executing user if set in their user profile

### Logging

Logging plugins consist of Readers and Writers, and Log File Storage.
Rundeck has a built-in Reader/Writer called the **Local File Log** that is used by default.

Logging plugins are enabled in the `rundeck-config` file.  You should add an entry identifying the plugin by its "provider name".  For Java plugins, this provider name is defined in the Java code.  For Groovy plugins, the provider name is usually just the name of the Groovy script file, such as "MyStreamingLogWriterPlugin".

To add StreamingLogWriter plugins, add a comma separated list to this entry, note that this will enable these plugins in addition to the **Local File Log Writer**:

* `rundeck.execution.logs.streamingWriterPlugins`
    * example value: `MyStreamingLogWriterPlugin,otherPlugin`

To change the StreamingLogReader plugin. Note that this will replace the **Local File Log Reader**, but will not disable the **Local File Log Writer**:

* `rundeck.execution.logs.streamingReaderPlugin`
    * example value: `MyStreamingLogReaderPlugin`

To disable the **Local File Log Writer**:

* `rundeck.execution.logs.localFileStorageEnabled`
    * value: `false`

To configure a ExecutionFileStorage plugin:

* `rundeck.execution.logs.fileStoragePlugin`
    * example value: `MyFileStoragePlugin`

Also, if `localFileStorageEnabled` is `false`, but no `streamingReaderPlugin` is enabled, then Rundeck will still default to using the **Local File Log Writer**.

The ExecutionFileStorage plugins also have some associated configuration values
that can be used to tune the behavior of the plugins:

* `rundeck.execution.logs.fileStorage.storageRetryCount`
    * The number of `store` attempts to try before giving up for a single log file
    * default value: `1` 
* `rundeck.execution.logs.fileStorage.storageRetryDelay`
    * Time to wait between retry attempts
    * default value: `60` (seconds)
* `rundeck.execution.logs.fileStorage.retrievalRetryCount`
    * The number of `retrieve` attempts to try before giving up for a single log file
    * default value: `3` 
* `rundeck.execution.logs.fileStorage.retrievalRetryDelay`
    * Time to wait between retry attempts
    * default value: `60` (seconds)
* `rundeck.execution.logs.fileStorage.remotePendingDelay`
    * Grace time to allow after an execution finishes. Clients will see a "pending" message within this period after an execution finishes, even if the storage plugin is unable to find the log file. After this time period, they will see a "not found" message if the plugin is unable to find the log file.
    * default value: `120` (seconds)

#### Logging Plugin Configuration

Logging plugins can define configuration properties, which can be set in the `framework.properties` (system-wide) or `project.properties` (project-wide).  Project-level properties override system-level properties.

To add a configuration property, add a value to the appropriate file in the following format:

`SCOPE.plugin.TYPE.PROVIDER.PROPERTY=value`

`SCOPE` is either `framework` or `project`.

The `TYPE` is one of:

* `StreamingLogReader`, `StreamingLogWriter`, or `ExecutionFileStorage`

`PROVIDER` is the provider name of the plugin.

### Storage Plugins

Storage plugins for the [Storage Facility](../administration/storage-facility.html) 
are configured in the `rundeck-config.properties` file.

Two separate "containers" are used, one for Key Storage, and one for Project Definition Storage.

* Key Storage 
    * uses a configuration prefix of `rundeck.storage.provider`
    * uses `file` provider by default, stored at the `${framework.var.dir}/storage` path
    * stores all content under the `/keys` top-level path
* Project Definition Storage 
    * uses a configuration prefix of `rundeck.config.storage.provider`
    * uses `db` provider by default
    * stores all content under the `/projects` top-level path

To configure a different Storage Plugin Provider for either **Key Storage** or **Project Definition Storage**, 
modify your `rundeck-config.properties` file:

To use the `db` storage:

    [prefix].1.type=db
    [prefix].1.path=/

To use the `file` storage:

    [prefix].1.type=file
    [prefix].1.path=/
    [prefix].1.config.baseDir=${framework.var.dir}/storage

Each Storage Plugin defines its own configuration properties, so if you are using a third-party plugin refer to its documentation. You can set the configuration properties via `[prefix].#.config.PROPERTY`.

For the builtin `file` implementation, these are the configuration properties:

* `baseDir` - Local filepath to store files and metadata. Default is `${framework.var.dir}/storage`

The `db` implementation has no configuration properties.

### Storage Converter Plugins

Storage Converter plugins are configured in the `rundeck-config.properties` file.

Two separate "containers" are used, one for Key Storage, and one for Project Definition Storage.

* Key Storage 
    * uses a configuration prefix of `rundeck.storage.converter`
* Project Definition Storage 
    * uses a configuration prefix of `rundeck.config.storage.converter`

Add an entry in your `rundeck-config.properties` file declaring the converter plugin 
which will handle content in subpath of the storage container.

~~~~
[prefix].1.type=my-encryption-plugin
[prefix].1.path=/keys
[prefix].1.config.foo=my config value
~~~~

* `type` - specifies the plugin provider name
* `path` - specifies the storage path the converter will apply to
* `resourceSelector` - specifies a metadata selector to choose which resources to apply the converter to
* `config.PROP` - specifies a plugin configuration property

The `resourceSelector` allows applying the converter to only resources which have the matching metadata.  The format for the value is:

    key OP value [; key OP value]*

Available metadata keys:

* `Rundeck-content-type`: the Content type of the stored file
* `Rundeck-key-type`: a value of `public` or `private` for Keys.

`OP` can be `=` for exact match, or `=~` for regular expression match.

For example, this will apply only to private key files:

    rundeck.storage.converter.1.resourceSelector = Rundeck-key-type=private

If a value for `resourceSelector` is not specified, the converter plugin will apply to all files in the matching path.
