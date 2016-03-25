% Bundled Plugins

Rundeck comes with several plugins out of the box.  *Built-in* plugins are part of the core installation, and do not
have an associated plugin file. *Bundled* plugins come packaged in a plugin file,
and are installed in the `libext` dir automatically at installation time.


## SSH Plugins

Defines SSH Node Executor and SCP File Copier.

* See [SSH Plugins](ssh-plugins.html)

File: *none* (built-in)

## Built-in Resource Model Sources

Rundeck comes with three built-in Resource Model Source providers, see [Resource Model Source Plugins](resource-model-source-plugins.html):

* File: Parses a file in one of the supported [Model Source Formats](#built-in-resource-model-formats)
* Directory: Scans all files in a directory in one of the supported formats
* URL: Loads a file from a URL in one of the supported formats

File: *none* (built-in)

## Built-in Resource Model Formats

Rundeck comes with three Resource Model Format plugins, see [Resource Model Source Plugins](resource-model-source-plugins.html#resource-model-document-formats):

* XML: the [resourcexml][] format
* YAML: the [resourceyaml][] format
* JSON: the [resourcejson][] format

[resourcexml]: ../man5/resource-xml.html
[resourceyaml]: ../man5/resource-yaml.html
[resourcejson]: ../man5/resource-json.html

File: *none* (built-in)

## Script Plugin

Defines Script Node Executor and Script File Copier.

For more detail see [Script Plugin](script-plugin.html).

Executes an external script file to perform the command, useful for developing your own plugin with the [Script Plugin Development](../developer/plugin-development.html#script-plugin-development) model.

File: `rundeck-script-plugin-${VERSION}.jar`

## Stub Plugin

Provides a Node Executor, File Copier, and Resource Model Source.  This plugin can be used for testing.

The `stub-plugin` includes these providers:

* `stub` for the NodeExecutor service
* `stub` for the FileCopier service
* `stub` for the ResourceModelSource service

(Refer to [Configuring - Node Execution](configuring.html#node-execution) to enable them.)

This plugin does not actually perform any remote file copy or command execution,
instead it simply echoes the command that was supposed to be executed, and
pretends to have copied a file. 

This is intended for use in testing new Nodes, Jobs or Workflow sequences without
affecting any actual runtime environment.  

You can also test some failure scenarios by configuring the following node attributes:

`stub-exec-success`="true/false"

:   If set to false, the stub command execution will simulate command failure

`stub-result-code`

:   Simulate the return result code from execution

You could, for example, disable or test an entire project's workflows or jobs by
simply setting the `project.properties` node executor provider to `stub`.


File: `rundeck-stub-plugin-${VERSION}.jar`

## Orchestrator Plugin

Provides several [Orchestrator plugin](orchestrator-plugins.html) implementations:

* Random Subset: Selects a maximum number of the target nodes at random
* Rank Tiered: Processes nodes in a tiered manner, ordered by rank
* Max Percentage: Processes at maximum a percentage of the target nodes

File: `rundeck-orchestrator-plugin-${VERSION}.jar`

## Local Execution Plugin

A Node Step plugin which executes a command locally instead of on a target node.

File: `rundeck-localexec-plugin-${VERSION}.jar`

## Job State Plugin

Provides a Workflow Step:

* Job State Conditional: Can query and assert the state of another Job, such as running, succeeded, failed, etc, and optionally halt the current execution.

File: `rundeck-job-state-plugin-${VERSION}.jar`

## Flow Control Plugin

Provides a Workflow Step:  

* Flow Control: Can halt the execution with a custom status, useful as an Error handler.

File: `rundeck-flow-control-plugin-${VERSION}.jar`

## Jasypt Encryption Plugin

Provides an encryption [storage converter](storage-plugins.html#storage-converter) for the Storage facility.  Can be used to encrypt the contents of Key Storage,
and Project Configuration stored in the DB or on disk.

This plugin provides password based encryption for storage contents.  
It uses the [Jasypt][] encryption library. The built in Java JCE is used unless another provider is specified, [Bouncycastle][] can be used by specifying the 'BC' provider name.

[Jasypt]: (http://jasypt.org)
[Bouncycastle]: http://www.bouncycastle.org/

Password, algorithm, provider, etc can be specified directly, or via environment variables (the `*EnvVarName` properties), or Java System properties (the `*SysPropName` properties).

To enable it, see [Configuring - Storage Converter Plugins](configuring.html#storage-converter-plugins).

See also: [Key Storage](../administration/key-storage.html)

Provider type: `jasypt-encryption`

The following encryption properties marked with `*` can be set directly, 
using the property name shown,
but they can all also be set dynamically using either an Environment variable, 
or a Java System Property.  
Append either `EnvVarName` for the environment variable, 
or `SysPropName` to use the Java System Property.  
If a System Property is specified: it is read in once and used by the initialization of the converter plugin,
then the Java System Property is set to null so it cannot be read again.

Configuration properties:  

`encryptorType`

:   Jasypt Encryptor to use. Either `basic`, `strong`, or `custom`. Default: 'basic'.

	* `basic` uses algorithm `PBEWithMD5AndDES`
	* `strong` requires use of the JCE Unlimited Strength policy files. (Algorithm: `PBEWithMD5AndTripleDES`)
	* `custom` is required to specify the algorithm.

`password*`
:   the password.

`algorithm*`
:   the encryption algorithm.

`provider*`
:   the provider name. 'BC' indicates Bouncycastle.

`providerClassName*`
:   Java class name of the provider.

`keyObtentionIterations*`
:   Number of hashes to use for the password when generating the key, default is 1000.

Example configuration for the Key Storage facility:

	rundeck.storage.converter.1.type=jasypt-encryption
	rundeck.storage.converter.1.path=keys
	rundeck.storage.converter.1.config.encryptorType=custom
	rundeck.storage.converter.1.config.passwordEnvVarName=ENC_PASSWORD
	rundeck.storage.converter.1.config.algorithm=PBEWITHSHA256AND128BITAES-CBC-BC
	rundeck.storage.converter.1.config.provider=BC

Example configuration for the Project Configuration storage facility:

	rundeck.config.storage.converter.1.type=jasypt-encryption
	rundeck.config.storage.converter.1.path=/
	rundeck.config.storage.converter.1.config.password=sekrit
	rundeck.config.storage.converter.1.config.encryptorType=custom
	rundeck.config.storage.converter.1.config.algorithm=PBEWITHSHA256AND128BITAES-CBC-BC
	rundeck.config.storage.converter.1.config.provider=BC


File: `rundeck-jasypt-encryption-plugin-${VERSION}.jar`

## Git Plugin

Provides SCM Export and SCM Import providers for Git.

File: `rundeck-git-plugin-${VERSION}.jar`

## Copy File Plugin

Provides a Node Step that can copy a file to a node, using the Node's File Copier.

File: `rundeck-copyfile-plugin-${VERSION}.jar`