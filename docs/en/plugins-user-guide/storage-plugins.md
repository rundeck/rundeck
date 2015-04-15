% Storage Plugins

Storage plugins define ways to store and dynamically modify file contents such as store [Private Key Files](../administration/key-storage.html) and [Project Configuration](../administration/project-setup.html).

For configuration see [Configuring - Storage Plugins](configuring.html#storage-plugins).

## Storage Backend

A storage backend is a "Storage" plugin, which defines how to list, create, update, delete and retrieve file contents and metadata.  

Rundeck bundles two storage plugin types:

* `file` - stores file contents on disk
* `db` - stores file contents in the database

## Storage Converter

A storage converter is a "StorageConverter" plugin, which can modify the contents or metadata of a stored file as it is being created, updated, or read.

### Jasypt Encryption Converter Plugin

Rundeck includes an Encryption plugin called `jasypt-encryption`.  
This plugin provides password based encryption for storage contents.  
It uses the [Jasypt][] encryption library. The built in Java JCE is used unless another provider is specified, [Bouncycastle][] can be used by specifying the 'BC' provider name.

[Jasypt]: (http://jasypt.org)
[Bouncycastle]: http://www.bouncycastle.org/

Password, algorithm, provider, etc can be specified directly, or via environment variables (the `*EnvVarName` properties), or Java System properties (the `*SysPropName` properties).

To enable it, see [Configuring - Storage Converter Plugins](configuring.html#storage-converter-plugins).

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
	rundeck.storage.converter.1.config.passwordEnvVarName=ENC_PASSWORD
	rundeck.storage.converter.1.config.algorithm=PBEWITHSHA256AND128BITAES-CBC-BC
	rundeck.storage.converter.1.config.provider=BC

Example configuration for the Project Configuration storage facility:

	rundeck.config.storage.converter.1.type=jasypt-encryption
	rundeck.config.storage.converter.1.path=/
	rundeck.config.storage.converter.1.config.password=sekrit
	rundeck.config.storage.converter.1.config.algorithm=PBEWITHSHA256AND128BITAES-CBC-BC
	rundeck.config.storage.converter.1.config.provider=BC

## Develop your own

See:

* [Storage Plugin Development](../developer/storage-plugin.html).
* [Storage Converter Plugin Development](../developer/storage-converter-plugin.html).
