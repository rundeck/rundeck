# Supported tags

- `SNAPSHOT` (latest master build)
- `3.0.20`
- `3.0.19` , `3.0.18` , `3.0.17` , `3.0.16` , `3.0.15` , `3.0.14` , `3.0.13` , `3.0.12` , `3.0.11` , `3.0.9` , `3.0.8` , `3.0.7` , `3.0.6` , `3.0.5` , `3.0.3`

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
The RAM ratio is set to `1` by default, so the JVM will utilize up to about the container limit.
See `JVM_MAX_RAM_FRACTION` for information on changing this.

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

## Extending Configuration
[Remco](https://github.com/HeavyHorst/remco) is used to generate configuration files
from templates. It supports different key/value sources such as vault, etcd, and dynamodb.
The default configuration uses environment variables.

Extending the configuration involves building a derived image
with additional template files.

See the [Docker Zoo Exhibit](https://github.com/rundeck/docker-zoo/tree/master/config) for a complete example.


## Environment Variables

### `JVM_MAX_RAM_FRACTION=1`

The JVM will use `1/x` of the max RAM for heap. For example, a setting of `2` will cause
the JVM to utilize up to half the container limit for heap. This is replaced in
openjdk 10 with a percentage setting that will offer finer control.

### `RUNDECK_SERVER_UUID`

Identifies Rundeck instances when multiple are running in the same cluster. While hard-coded
to a default for getting started, this should be set manually for more advanced configurations.

### `RUNDECK_GRAILS_URL=http://127.0.0.1:4440`

Controls the base URL the app will use for links, redirects, etc.
This is the URL users will use to access the site.

### `RUNDECK_GRAILS_UPLOAD_MAXSIZE`

Controls both the `maxFileSize` and `maxRequest` for the grails controller config.
The internal default is approximately `25Mib` or `26214400`.

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

### `RUNDECK_PREAUTH_ENABLED=false`
### `RUNDECK_PREAUTH_ATTRIBUTE_NAME=REMOTE_USER_GROUPS`
### `RUNDECK_PREAUTH_DELIMITER=,`
### `RUNDECK_PREAUTH_USERNAME_HEADER=X-Forwarded-Uuid`
### `RUNDECK_PREAUTH_ROLES_HEADER=X-Forwarded-Roles`
### `RUNDECK_PREAUTH_REDIRECT_LOGOUT=false`
### `RUNDECK_PREAUTH_REDIRECT_URL=/oauth2/sign_in`
Configuration options for using the
[preauthenticated mode](http://rundeck.org/docs/administration/security/authenticating-users.html#preauthenticated-mode).

### `RUNDECK_TOKENS_FILE`
Specify location of a static tokens file. See [configuration file reference](https://docs.rundeck.com/docs/administration/configuration/configuration-file-reference.html) for details.

### `RUNDECK_SECURITY_HTTPHEADERS_ENABLED=true`
### `RUNDECK_SECURITY_HTTPHEADERS_PROVIDER_XCTO_ENABLED=true`
### `RUNDECK_SECURITY_HTTPHEADERS_PROVIDER_XXSSP_ENABLED=true`
### `RUNDECK_SECURITY_HTTPHEADERS_PROVIDER_XFO_ENABLED=true`
### `RUNDECK_SECURITY_HTTPHEADERS_PROVIDER_CSP_ENABLED=true`
### `RUNDECK_SECURITY_HTTPHEADERS_PROVIDER_CSP_CONFIG_INCLUDEXCSPHEADER=false`
### `RUNDECK_SECURITY_HTTPHEADERS_PROVIDER_CSP_CONFIG_INCLUDEXWKCSPHEADER=false`
### `RUNDECK_SECURITY_HTTPHEADERS_PROVIDER_CSP_CONFIG_POLICY`
Controls for CSP headers.


### `RUNDECK_MAIL_SMTP_HOST`
### `RUNDECK_MAIL_SMTP_PORT`
### `RUNDECK_MAIL_SMTP_USERNAME`
### `RUNDECK_MAIL_SMTP_PASSWORD`
### `RUNDECK_MAIL_FROM`
Default from address.
### `RUNDECK_MAIL_DEFAULT_TEMPLATE_SUBJECT`
### `RUNDECK_MAIL_DEFAULT_TEMPLATE_FILE`
### `RUNDECK_MAIL_DEFAULT_TEMPLATE_LOG_FORMATTED`

### `RUNDECK_MAIL_PROPS`
Mail properties that get passed through to Grails. For example, to use StartTLS(required by many servers including AWS SES), `["mail.smtp.starttls.enable":"true","mail.smtp.port":"587"]`.