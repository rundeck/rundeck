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

See [Bundled Plugins - Jasypt Encryption Plugin](bundled-plugins.html#jasypt-encryption-plugin)

## Develop your own

See:

* [Storage Plugin Development](../developer/storage-plugin.html).
* [Storage Converter Plugin Development](../developer/storage-converter-plugin.html).
