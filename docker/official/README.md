# Supported tags

- `SNAPSHOT` (latest master build)
- `3.0.5`
- `3.0.4`
- `3.0.3`

# What is Rundeck?

![logo](https://www.rundeck.com/hs-fs/hubfs/rundeck-logotype-512.png?t=1532143389217&width=171&name=rundeck-logotype-512.png)

**Check out the [Docker Zoo](https://github.com/rundeck/docker-zoo) for configuration examples
in Docker Compose!**

## start with persistent storage

The simplest way to persist data between container starts/upgrades is to
utilize named volumes:  
`$ docker run --name some-rundeck -v data:/home/rundeck/server/data rundeck/rundeck`

## ssh keys

You can provide private ssh keys by mounting them into `/home/rundeck/.ssh`:  
`$ docker run --name some-rundeck -v /home/protip/.ssh:/home/rundeck/.ssh rundeck/rundeck`

**Kubernetes** users may wish to mount a private key in through the secrets system. See
Kubernetes documentation [Use-Case: Pod with ssh keys](https://kubernetes.io/docs/concepts/configuration/secret/#use-cases) for details.

[**Rundeck Key Storage**](http://rundeck.org/docs/plugins-user-guide/ssh-plugins.html#using-key-storage-for-ssh) can be used to provide ssh keys to the ssh plugin as well.

## control JVM heap allocation

`$ docker run -m 1024m`

The JVM is configured to use cgroup information to set the max heap allocation size.
The RAM ratio is set to `1`, so the JVM will utilize up to about the container limit.

## key store security
By defualt keystorage is set to use the database, and the encryption converters are
**disabled**. To enable encryption, supply a password for one or both of the default converters:
```
RUNDECK_STORAGE_CONVERTER_1_CONFIG_PASSWORD=supersecret
RUNDECK_CONFIG_STORAGE_CONVERTER_1_CONFIG_PASSWORD=supersecret
```

> **Note:** It is not recommended to enable/disable encryption after initial project setup!
Refer to the [docs](http://rundeck.org/docs/administration/configuration/storage-facility.html) for more information.

## user authentication
> **NOTE:** For extra reference and clarity, refer to the official docs.
For example configurations check out the Zoo.

* [Docs](http://rundeck.org/docs/administration/security/authenticating-users.html#ldap)
* [Zoo](https://github.com/rundeck/docker-zoo/tree/master/ldap-combined)

**Default**  
The default setup utilizes the `/home/rundeck/server/config/realm.properties` file. Mount
or otherwise replace this file to manage further users through this method.

**JAAS**  
There is initial support for composing the JAAS modules talk about in the docks.
The convention for listing the modules to use in environment variables:
```
RUNDECK_JAAS_MODULES_0=JettyCombinedLdapLoginModule
RUNDECK_JAAS_MODULES_1=PropertyFileLoginModule
```

Config keys are located under:
```
RUNDECK_JAAS_LDAP_*
RUNDECK_JAAS_FILE_*
```

By convention the module name matches the name in the docs, and the config keys match
the config options listed in the docs uppercase, and all one word.

## Environment Variables

### `RUNDECK_GRAILS_URL=http://127.0.0.1:4440`

Controls the base URL the app will use for links, redirects, etc.
This is the URL users will use to access the site.

### `RUNDECK_SERVER_ADDRESS=0.0.0.0`

This is the address or hostname the application will attempt to bind to within
the container.

### `RUNDECK_DATABASE_URL`

Defaults to `jdbc:h2:file:/home/rundeck/server/data/grailsdb;MVCC=true`. The default configuration utilizes an h2 file for data storage.

### `RUNDECK_DATABASE_DRIVER`

Set this if using an alternative backend from h2.

- `org.postgresql.Driver`
- `org.mariadb.jdbc.Driver`
- `com.mysql.jdbc.Driver`

### `RUNDECK_DATABASE_USERNAME`

### `RUNDECK_DATABASE_PASSWORD`

### `RUNDECK_LOGGING_STRATEGY=CONSOLE`

The default console strategy configures log4j to send all output to stdout
to be collected by the container logging driver.

Set to `FILE` to log into `/home/rundeck/server/logs` .

### `RUNDECK_LOGGING_AUDIT_ENABLED`

Set to anything enables audit logging. This can be very verbose so use with caution.

### `RUNDECK_STORAGE_PROVIDER_#_[[TYPE|PATH]|CONFIG_[...]]`
### `RUNDECK_STORAGE_CONVERTER_#_[[TYPE|PATH]|CONFIG_[...]]`

Configuration options for key storage providers and converts. These map to the
[Storage Facility Docs](http://rundeck.org/docs/administration/configuration/storage-facility.html).