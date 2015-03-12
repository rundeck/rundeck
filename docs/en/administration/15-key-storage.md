% Rundeck Key Storage

This document describes the Rundeck Key Storage mechanism for a developer to implement a secure data flow of sensitive private key data that can be used for sessions via a Rundeck Node Executor.

The structure or hierarchy used for organizing Keys is up to you.

A typical way to store shared keys might be under a "common" or "shared" root. Specific user or project keys might be stored under "user/[username]/" or "project/[name]" paths:

* `common/qa-dev.pem`
* `user/bob/dev1.pem`
* `user/bob/prod1.pem`
* `role/qa/web1.pem`
* `project/project1/default.pem`

## ACL Policies

Access to the Keys in the Storage facility are restricted by use of [ACL policies](access-control-policy.html#).

Access to the `keys` path requires an [Application scope](access-control-policy.html#application-scope-resources-and-actions) authorization.

Within the application scope definition, define access with a `for` entry of `storage`.

Authorization can be granted for these actions:

* `create` - create files
* `update` - modify files
* `read` - list directories and view and read files
* `delete` - delete files

### Examples

~~~~ {.yaml}
description: authorize keys/ storage files
context:
  application: 'rundeck'
for:
  storage:
    - match:
        path: 'keys/.*'
      allow: [read]
    - equals:
        path: 'keys/test1.pub'
      allow: [read,create,update,delete]
    - match:
        path: 'keys/scratch/.*'
      allow: [read,create,update,delete]
~~~~

## API Usage

The [Key Storage API](../api/index.html#key-storage) is provided through the standard Rundeck HTTP API. Rundeck should be configured to use HTTPS, and all API access requires either an authentication token, or username and password authentication.

Creating an key entry:

* `POST /api/11/storage/keys/{path}/{name}`
    - Stores a key file named "{name}" at a particular path
    - Request content is stored as-is, and the `Content-Type` is stored with the data.
    - using Content-Type "application/pgp-keys" creates a **Public key**
    - using Content-Type "application/octet-stream" creates a **Private key**

Listing entries:

* `GET /api/11/storage/keys/{path}/`
    - Lists all entries in the directory, provides a JSON or XML response based on the `Accept` request header

Retrieving keys:

* `GET /api/11/storage/keys/{path}/{name}`
    - Retrieve the key file data.
    - Provides a JSON or XML response based on the `Accept` request header

If the `Accept` header specifies `*/*` or the content-type of the file, then the response will be:

* For a **Public Key**: the public key content
* For a **Private Key**: a `403 Unauthorized` response.

Deleting an entry:

* `DELETE /api/11/storage/keys/{path}/{name}`
    - deletes the entry if it exists and returns `204` response

## Storage backends

The location of stored Key data can be either on the filesystem, the database, or some external system via usage of a **Storage Plugin**.

Rundeck provides these built-in implementations:

* `file` - stores files locally on the filesystem (default)
* `db` - stores file data as BLOBs in the database

### Configuring Storage Plugins

See [Plugins User Guide - Configuring Storage Plugins](../plugins-user-guide/configuring.html#storage-plugins).

## Key Data Storage Converter

 Keys can be encrypted in the storage backend by use of a [Storage Converter plugin](../developer/storage-converter-plugin.html). A typical plugin would encrypt any private-key data at write time, and decrypt it at read time.

The Storage Converter Plugin handles reading and writing the content for any matching resources.  The subsequent data is stored in the storage backend (on-disk or in a database) alongside the metadata for the file.  If necessary, the metadata content can also be encrypted by modifying the data map that is provided.

Converter plugins do not have to manage storing the data, that will be handled by the Storage backend.

### Configuring Storage Converter Plugins

See [Plugins User Guide - Configuring Storage Converter Plugins](../plugins-user-guide/configuring.html#storage-converter-plugins).

## Using Keys via Rundeck Node Executors

### Built-in JschNodeExecutor

The provided java-based JschNodeExecutor, which is the default used for Node execution, uses Node attributes to determine the type of authentication used when connecting to the Node via SSH. To select private-key based authentication the Node attribute `ssh-authentication` is used:

* `ssh-authentication="privateKey"` (default value)

The default and typical usage is to use a private key stored on the local file system specified via the `ssh-keypath` attribute.

Use the following attribute to select one of the stored Keys for authentication.

Attribute
:    `ssh-key-storage-path`

Value
:    `/keys/{path}/{name}` - the storage path to the key. Currently all keys are stored under the `/keys` top-level path.

The value of the `ssh-key-storage-path` attribute can embed values taken from the execution context of the Rundeck job or execution, for example the username of the user running the job.  This would be embedded as `${job.username}`, so to specify use of a key named "default.pem" stored in a path with the username of the executing user, the attribute might be set as:

    ssh-key-storage-path="/keys/users/${job.username}/default.pem"

When resolved, this will evaluate to `/keys/users/bob/default.pem` (for example).
