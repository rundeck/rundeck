% Rundeck SSH Key Storage

This document describes the Rundeck SSH Key Storage mechanism for a developer to implement a secure data flow of sensitive private key data that can be used for SSH sessions via a Rundeck Node Executor.

## Storage Facility

All SSH Keys are stored within the Rundeck Storage facility. The Storage facility provides a filesystem-like structure for storing files.  Each file is located with a "/"-separated "path" and a name, similar to a file system path.

The structure or hierarchy used for organizing SSH Keys is up to you.

A typical way to store shared keys might be under a "common" or "shared" root. Specific user or project keys might be stored under "user/[username]/" or "project/[name]" paths:

* `common/qa-dev.pem`
* `user/bob/dev1.pem`
* `user/bob/prod1.pem`
* `role/qa/web1.pem`
* `project/project1/default.pem`

## ACL Policies

Access to the SSH Keys in the Storage facility are restricted by use of [ACL policies](access-control-policy.html#).

Access to the `ssh-key` path requires an [Application scope](access-control-policy.html#application-scope-resources-and-actions) authorization.

Within the application scope definition, define access with a `for` entry of `storage`.

Authorization can be granted for these actions:

* `create` - create files
* `update` - modify files
* `read` - list directories and view and read files
* `delete` - delete files

### Examples

~~~~ {.yaml}
description: authorize non-project specific storage files
context:
  application: 'rundeck'
for:
  storage:
    - match:
        path: 'ssh-key/.*'
      allow: [read]
    - equals:
        path: 'ssh-key/test1.pub'
      allow: [read,create,update,delete]
    - match:
        path: 'ssh-key/scratch/.*'
      allow: [read,create,update,delete]
~~~~

## API Usage

The [SSH Key Storage API](../api/index.html#ssh-key-storage) is provided through the standard Rundeck HTTP API. Rundeck should be configured to use HTTPS, and all API access requires either an authentication token, or username and password authentication.

Creating an SSH key entry:

* `POST /api/11/storage/ssh-key/{path}/{name}`
    - Stores a key file named "{name}" at a particular path
    - Request content is stored as-is, and the `Content-Type` is stored with the data.
    - using Content-Type "application/pgp-keys" creates a **Public key**
    - using Content-Type "application/octet-stream" creates a **Private key**

Listing entries:

* `GET /api/11/storage/ssh-key/{path}/`
    - Lists all entries in the directory, provides a JSON or XML response based on the `Accept` request header

Retrieving keys:

* `GET /api/11/storage/ssh-key/{path}/{name}`
    - Retrieve the key file data.
    - Provides a JSON or XML response based on the `Accept` request header

If the `Accept` header specifies `*/*` or the content-type of the file, then the response will be:

* For a **Public Key**: the public key content
* For a **Private Key**: a `403 Unauthorized` response.

Deleting an entry:

* `DELETE /api/11/storage/ssh-key/{path}/{name}`
    - deletes the entry if it exists and returns `204` response

## Storage backends

The location of stored SSH Key data can be either on the filesystem, the database, or some external system via usage of a **Storage Plugin**.

Rundeck provides these built-in implementations:

* `file` - stores files locally on the filesystem (default)
* `db` - stores file data as BLOBs in the database

### Configuring Storage Plugins

By default, the `file` implementation is used, and files are stored at the `${framework.var.dir}/storage` path.

To configure a different Storage Plugin, modify your `rundeck-config.properties` file:

To use the `db` storage:

    rundeck.storage.provider.1.type=db
    rundeck.storage.provider.1.path=/

To use the `file` storage:

    rundeck.storage.provider.1.type=file
    rundeck.storage.provider.1.path=/
    rundeck.storage.provider.1.config.baseDir=${framework.var.dir}/storage

Each Storage Plugin defines its own configuration properties, so if you are using a third-party plugin refer to its documentation. You can set the configuration properties via `rundeck.storage.provider.#.config.PROPERTY`.

For the builtin `file` implementation, these are the configuration properties:

* `baseDir` - Local filepath to store files and metadata. Default is `${framework.var.dir}/storage`

The `db` implementation has no configuration properties.

## SSH Key Data Storage Converter

SSH Keys can be encrypted in the storage backend by use of a [Storage Converter plugin](../developer/storage-converter-plugin.html). A typical plugin would encrypt any private-key data at write time, and decrypt it at read time.

The Storage Converter Plugin handles reading and writing the content for any matching resources.  The subsequent data is stored in the storage backend (on-disk or in a database) alongside the metadata for the file.  If necessary, the metadata content can also be encrypted by modifying the data map that is provided.

Converter plugins do not have to manage storing the data, that will be handled by the Storage backend.

### Configuring Storage Converter Plugins

Add an entry in your `rundeck-config.properties` file declaring the converter plugin which will handle content in the `/ssh-key` subpath of the storage container.

~~~~
rundeck.storage.converter.1.type=my-encryption-plugin
rundeck.storage.converter.1.path=/ssh-key
rundeck.storage.converter.1.config.foo=my config value
~~~~

* `type` - specifies the plugin provider name
* `path` - specifies the storage path the converter will apply to
* `resourceSelector` - specifies a metadata selector to choose which resources to apply the converter to
* `config.PROP` - specifies a plugin configuration property

The `resourceSelector` allows applying the converter to only resources which have the matching metadata.  The format for the value is:

    key OP value [; key OP value]*

Available metadata keys:

* `Rundeck-content-type`: the Content type of the stored file
* `Rundeck-ssh-key-type`: a value of `public` or `private` for SSH Keys.

`OP` can be `=` for exact match, or `=~` for regular expression match.

For example, this will apply only to private key files:

    rundeck.storage.converter.1.resourceSelector = Rundeck-ssh-key-type=private

## Using SSH Keys via Rundeck Node Executors

### Built-in JschNodeExecutor

The provided java-based JschNodeExecutor, which is the default used for Node execution, uses Node attributes to determine the type of authentication used when connecting to the Node via SSH. To select private-key based authentication the Node attribute `ssh-authentication` is used:

* `ssh-authentication="privateKey"` (default value)

Typically a file path to a private key is specified via the `ssh-keypath` attribute.

However, we introduce a new Node attribute which can be used to select one of the stored SSH Keys for authentication.

Attribute
:    `ssh-key-storage-path`

Value
:    `/{path}/{name}` - the relative path to a SSH Key entry under `/ssh-key`

The value of the `ssh-key-storage-path` attribute can embed values taken from the execution context of the Rundeck job or execution, for example the username of the user running the job.  This would be embedded as `${job.username}`, so to specify use of a key named "default.pem" stored in a path with the username of the executing user, the attribute might be set as:

    ssh-key-storage-path="/users/${job.username}/default.pem"

When resolved, this will evaluate to `/ssh-key/users/bob/default.pem` (for example).
