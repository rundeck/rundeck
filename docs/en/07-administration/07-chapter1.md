# Administration

## Startup and shutdown

RunDeck installation includes a control script used for starting and
stopping the RunDeck server process.
The control script provides a number of actions:

    rundeckd [start|stop|restart|condrestart|status]

### RPM

The RPM installation includes the placement of the boot control script
that will automatically start RunDeck when the system boots.

The script is located here: `/etc/init.d/rundeckd` 

*Startup*

    /etc/init.d/rundeckd start

*Shutdown*

    /etc/initd./rundeckd stop
    
#### Setting JAVA_HOME

When using the RPM, by default rundeck will use _java_ found in your path.  Various RPM based 
distributions provide ways of managing which version of java is found.  CentOS uses 
_/usr/sbin/alternatives_ and the processing of setting alternatives can be found here: 
[http://wiki.centos.org/HowTos/JavaOnCentOS](http://wiki.centos.org/HowTos/JavaOnCentO).

If you have installed a JDK or JRE in a unique directory and do not want to alter the global system
configuration, then simply setting JAVA_HOME before running any command will use the version of java
found in JAVA_HOME/bin.  Updating /etc/rundeck/profile with JAVA_HOME is another option as 
well.
    
### Launcher

The Launcher installation generates the script into the RDECK_BASE directory.

The script is located here: `$RDECK_BASE/server/sbin/rundeckd`.

*Startup*

    $RDECK_BASE/server/sbin/rundeckd start

*Shutdown*

    $RDECK_BASE/server/sbin/rundeckd stop

You may choose to incorporate this script into your server's operating
system specific boot process.

## Configuration

### Configuration layout

Configuration file layout differs between the RPM and Launcher
installation methods. See [RPM layout](#rpm-layout) and
[Launcher layout](#launcher-layout) for details.

#### RPM layout

    /etc/rundeck
    |-- admin.aclpolicy
    |-- framework.properties
    |-- log4j.properties
    |-- profile
    |-- project.properties
    |-- jaas-loginmodule.conf
    |-- log4j.properties
    |-- realm.properties
    |-- rundeck-config.properties
    `-- ssl
        |-- ssl.properties
        |-- keystore (not packaged)
        `-- truststore (not packaged)

#### Launcher layout

    $RDECK_BASE/etc
    |-- admin.aclpolicy
    |-- framework.properties
    |-- log4j.properties
    |-- profile
    `-- project.properties
    $RDECK_BASE/server/config
    |-- jaas-loginmodule.conf
    |-- realm.properties
    `-- rundeck-config.properties

### Configuration files
Configuration is specified in a number of standard RunDeck
configuration files generated during the installation process.

See the [Configuration layout](#configuration-layout) section for where these
files reside for RPM and Launcher installations.

The purpose of each configuration file is described in its own section.

#### admin.aclpolicy

Administrator access control policy defined with a "aclpolicy(5)" XML
document.

This file governs the access for the "admin" group and role. 

See [Authorization](#authorization) for information about setting up
policy files for other user groups.

#### framework.properties

Configuration file used by shell tools and core RunDeck services. This file will be created for you at install time.

Some important settings:

* `framework.node.hostname`: Hostname of the RunDeck server node
* `framework.node.name`: Name (identity) of the RunDeck server node
* `framework.projects.dir`: Path to the directory containing RunDeck Project directories.  Default is `$RDECK_BASE/projects`.
* `framework.var.dir`: Base directory for output and temp files used by the server and CLI tools. Default is `$RDECK_BASE/var`.
* `framework.logs.dir`: Directory for log files written by core services and RunDeck Server's Job executions. Default is `$RDECK_BASE/var/logs`
* `framework.server.username`: Username for connection to the RunDeck server
* `framework.server.password`: Password for connection to the RunDeck server
* `framework.rundeck.url`: Base URL for RunDeck server.

Resource Provider settings:

* `framework.resources.allowedURL.X`: a sequence of regular expressions (for `X` starting at 0 and increasing). These are matched against requested providerURL values when
the `/project/name/resources/refresh` API endpoint is called. See [Refreshing Resources for a Project](api/index.html#refreshing-resources-for-a-project).

SSH Connection settings:

* `framework.ssh.keypath`: Path to the SSH private key file used for SSH connections
* `framework.ssh.user`: Default username for SSH Connections, if not overridden by Node specific value.
* `framework.ssh.timeout`: timeout in milliseconds for SSH connections and executions. The default is "0" (no timeout).  You can modify this to change the maximum time allowed for SSH connections.

Other settings:

* `framework.log.dispatch.console.format`: Default format for non-terse node execution logging run by the `dispatch` CLI tool.

#### log4j.properties

RunDeck uses [log4j] as its application logging facility. This file
defines the logging configuration for the RunDeck server. 

[log4j]: http://logging.apache.org/log4j/

#### profile

Shell environment variables used by the shell tools. This file
contains several parameters needed during the startup of the shell
tools like umask, Java home and classpath, and SSL options.

#### project.properties

RunDeck [project](#project) configuration file. One of these is
generated at project setup time. There are two important settings in
this file:

* `project.resources.file`: Path to the project resource model document
  (see [resource-v10(5)](resource-v10.html) and [resource-yaml-v12(5)](resource-yaml-v12.html)).
* `project.resources.url`: (Optional) The URL to an external
  [Resource Model Provider](#resource-model-provider).
* `project.resources.allowedURL.X`: a sequence of regular expressions (for `X` starting at 0 and increasing). These are matched against requested providerURL values when
the `/project/name/resources/refresh` API endpoint is called. See [Refreshing Resources for a Project](api/index.html#refreshing-resources-for-a-project).

#### jaas-loginmodule.conf

[JAAS] configuration for the RunDeck server. The listing below
shows the file content for a normal RPM installation. One can see it
specifies the use of the [PropertyFileLoginModule]:

    RDpropertyfilelogin {
      org.mortbay.jetty.plus.jaas.spi.PropertyFileLoginModule required
      debug="true"
      file="/etc/rundeck/realm.properties";
    };

[JAAS]: http://docs.codehaus.org/display/JETTY/JAAS
[PropertyFileLoginModule]: http://jetty.codehaus.org/jetty/jetty-6/apidocs/org/mortbay/jetty/plus/jaas/spi/PropertyFileLoginModule.html

#### realm.properties

Property file user directory when PropertyFileLoginModule is
used. Specified from [jaas-loginmodule.conf](#jaas-loginmodule.conf).

#### rundeck-config.properties

The primary RunDeck webapp configuration file. Defines default
loglevel, datasource configuration, [role mapping](#role-mapping), and
[GUI customization](#customizing-rundeck-gui).

## Logs

Depending on the installer used, the log files will be under a base
directory:

*   RPM: `/var/log/rundeck`
*   Launcher: `$RDECK_BASE/server/logs`

The following files will be found in the log directory:

     .
     |-- command.log
     |-- rundeck.audit.log
     |-- rundeck.jobs.log
     |-- rundeck.options.log
     |-- rundeck.log
     `-- service.log

Different facilities log to their own files:

* `command.log`: Shell tools log their activity to the command.log
* `rundeck.audit.log`: Authorization messages pertaining to aclpolicy
* `rundeck.job.log`: Log of all job definition changes
* `rundeck.options.log`: Logs remote HTTP requests for Options JSON data
* `rundeck.log`: General RunDeck application messages
* `service.log`: Standard input and output generated during runtime

See the [#log4j.properties](#log4j.properties) section for information 
about customizing log message formats and location.

## Backup and recovery

RunDeck backup should only be done with the server down. 

(1) Export the jobs

         rd-jobs -f /path/to/backup/dir/job.xml

(2) Stop the server. See: [startup and shutdown](#startup-and-shtudown)

        rundeckd stop

(3) Copy the data files. (Assumes file datastore configuration). The
location of the data directory depends on the installation method:

    * RPM install: `/var/lib/rundeck/data`
    * Launcher install: `$RDECK_BASE/server/data`

             cp -r data /path/to/backup/dir

(4) Start the server

         rundeckd start    

## SSH

RunDeck uses [SSH] for remote execution. 
You do _not_ need to have root account access on either the server or
the remote hosts.  

[SSH]: http://en.wikipedia.org/wiki/Secure_Shell

### SSH configuration requirements

* The SSH configuration requires that the RunDeck server machine can
  ssh commands to the client machines. 
* SSH is assumed to be installed and configured appropriately to allow
  this access.   
* No passphrase should be set.
* SSH should not prompt for a password. There are many resources
available on how to configure ssh to use public key authentication
instead of passwords such as:
[Password-less logins with OpenSSH](http://www.debian-administration.org/articles/152)
or
[How-To: Password-less SSH](http://www.cs.wustl.edu/~mdeters/how-to/ssh/)

### SSH key generation

* The RunDeck installation can be configured to use RSA _or_ DSA
  type keys.
  
Here's an example of SSH RSA key generation on a Linux system:

    $ ssh-keygen -t rsa
    Generating public/private rsa key pair.
    Enter file in which to save the key (/home/demo/.ssh/id_rsa): 
    Enter passphrase (empty for no passphrase): 
    Enter same passphrase again: 
    Your identification has been saved in /home/demo/.ssh/id_rsa.
    Your public key has been saved in /home/demo/.ssh/id_rsa.pub.
    The key fingerprint is:
    a7:31:01:ca:f0:62:42:9d:ab:c8:b7:9c:d1:80:76:c6 demo@ubuntu
    The key's randomart image is:
    +--[ RSA 2048]----+
    | .o . .          |
    |.  * . .         |
    |. = =   .        |
    | = E     .       |
    |+ + o   S .      |
    |.o o .   =       |
    |  o +   .        |
    |   +             |
    |                 |
    +-----------------+

### Configuring remote machine for SSH 

To be able to directly ssh to remote machines, the SSH public key of
the client should be shared to the remote machine.
  
Follow the steps given below to enable ssh to remote machines.

The ssh public key should be copied to the `authorized_keys` file of
the remote machine. The public key will be available in
`~/.ssh/id_rsa.pub` file.
  
The `authorized_keys` file should be created in the `.ssh` directory of
the remote machine.
  
The file permission of the authorized key should be read/write for
the user and nothing for group and others. To do this check the
permission and change it as shown below.

    $ cd ~/.ssh
    $ ls -la
    -rw-r--r--   1 raj  staff     0 Nov 22 18:14 authorized_keys

    $ chmod 600 authorized_keys 
    $ ls -la
    -rw-------   1 raj  staff     0 Nov 22 18:14 authorized_keys

The permission for the .ssh directory of the remote machine should
be read/write/execute for the user and nothing for the group and
others. To do this, check the permission and change it as shown
below.  

    $ ls -la
    drwxr-xr-x   2 raj  staff    68 Nov 22 18:19 .ssh
    $ chmod 700 .ssh
    $ ls -la
    drwx------   2 raj  staff    68 Nov 22 18:19 .ssh

If you are running RunDeck on Windows, we heartily recommend using
[Cygwin] on Windows as it includes SSH and a number of
Unix-like tools that are useful when you work in a command line
environment.

[Cygwin]: http://www.cygwin.org

#### Passing environment variables through remote command

To pass environment variables through remote command
dispatches, it is required to properly configure the SSH server on the
remote end. See the `AcceptEnv` directive in the "sshd\_config(5)"
manual page for instructions. 

Use a wild card pattern to permit `RD_` prefixed variables to provide
open access to RunDeck generated environment variables.

## Managing logins

### realm.properties

These instructions explain how to manage user credentials for 
RunDeck in the <code>realm.properties</code> file.

The default RunDeck webapp handles user authentication via its
container, which in turn is configured to pull its user authentication
from the `$RDECK_BASE/server/config/realm.properties` file. 
This file is created at the time that you install the server.

Assuming it wasn't modified, your realm.properties file will
probably look something like this:

    #
    # This file defines users passwords and roles for a HashUserRealm
    #
    # The format is
    #  <username>: <password>[,<rolename> ...]
    #
    # Passwords may be clear text, obfuscated or checksummed.  
    #
    # This sets the default user accounts for the RunDeck apps
    #
    admin:admin,user,admin
    user:user,user

*Adding additional users*

You may wish to have additional users with various privileges rather
than giving out role accounts to groups.  You may also want to avoid
having the passwords in plaintext within the configuration file.  

To accomplish this, you'll need a properly hashed or encrypted
password to use in the config.  On the RunDeck server, move into
the directory that contains your installation and pass the
username and password to the `Password` utility.  In this example,
we'll setup a new user named "jsmith", with a password of "mypass":

    $ cd $RDECK_BASE
    $ java -cp server/lib/jetty-6.1.21.jar:server/lib/jetty-util-6.1.21.jar org.mortbay.jetty.security.Password jsmith mypass
    OBF:1xfd1zt11uha1ugg1zsp1xfp
    MD5:a029d0df84eb5549c641e04a9ef389e5
    CRYPT:jsnDAc2Xk4W4o

Then add this to the `realm.properties` file with a line like so:

    jsmith: MD5:a029d0df84eb5549c641e04a9ef389e5,user,admin

Then restart RunDeck to ensure it picks up the change and you're done.


### Active Directory

*note* Because the underlying security mechanism relies on JAAS, you are free to use what ever JAAS provider you feel is suitable for your environment.

(1)  Setup the LDAP login module configuration file

    Create a `jaas-activedirectory.conf` file in the same directory as the `jaas-loginmodule.conf` file.
    
    * RPM install: /etc/rundeck/
    * Launcher install: $RDECK_BASE/server/config
    
    ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    activedirectory {
        com.dtolabs.rundeck.jetty.jaas.JettyCachingLdapLoginModule required
        debug="true"
        contextFactory="com.sun.jndi.ldap.LdapCtxFactory"
        providerUrl="ldap://localhost:389"
        bindDn="cn=Manager,dc=rundeck,dc=com"
        bindPassword="secret"
        authenticationMethod="simple"
        forceBindingLogin="true"
        userBaseDn="ou=users,dc=rundeck,dc=com"
        userRdnAttribute="cn"
        userIdAttribute="cn"
        userPasswordAttribute="unicodePwd"
        userObjectClass="user"
        roleBaseDn="ou=roles,dc=rundeck,dc=com"
        roleNameAttribute="cn"
        roleMemberAttribute="member"
        roleObjectClass="group"
        cacheDurationMillis="300000"
        reportStatistics="true";
        };
    ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~


(2) To override the default JAAS configuration file, you will need to supply the RunDeck server with the proper path to the new one, and a `loginmodule.name` Java system property to identify the new login module by name.

    The JAAS configuration file location is specified differently between The Launcher and the RPM.
    
    **For the Launcher**:  the `loginmodule.conf.name` Java system property is used to identify the *name* of the config file, which must be located in the `$RDECK_BASE/server/config` dir.
    
    You can simply specify the system properties on the java commandline:
    
        java -Dloginmodule.conf.name=jaas-activedirectory.conf \
            -Dloginmodule.name=activedirectory \
            -jar rundeck-launcher-x.x.jar
            
    Otherwise, using the launcher with the supplied `rundeckd` script, you can modify the `RDECK_JVM` value in the `$RDECK_BASE/etc/profile` file to add two JVM arguments:
    
        export RDECK_JVM="-Dloginmodule.conf.name=jaas-activedirectory.conf \
            -Dloginmodule.name=activedirectory"
    
    Note: more information about using the launcher and useful properties are under [Getting Started - Launcher Options](#launcher-options).
    
    **For the RPM installation**: the absolute path to the JAAS config file must be specified with the `java.security.auth.login.config` property.
    
    Update the `RDECK_JVM` in `/etc/rundeck/profile` by changing the following two JVM arguments:
    
        export RDECK_JVM="-Djava.security.auth.login.config=/etc/rundeck/jaas-loginmodule.conf \
               -Dloginmodule.name=RDpropertyfilelogin \
           
    to
    
        export RDECK_JVM="-Djava.security.auth.login.config=/etc/rundeck/jaas-activedirectory.conf \
               -Dloginmodule.name=activedirectory \


(3) Restart rundeckd

    `sudo /etc/init.d/rundeckd restart`

(4) Attempt to logon

    If everything was configured correctly, you will be able to access RunDeck using your AD credentials.  If something did not go smoothly, look at `/var/log/rundeck/service.log` for stack traces that may indicate what is wrong.

#### Communicating over secure ldap (ldaps://)

The default port for communicating with active directory is 389, which is insecure.  The secure port is 686, but the LoginModule describe above requires that the AD certificate or organizations CA certificate be placed in a truststore.  The truststore provided with rundeck `/etc/rundeck/ssl/truststore` is used for the local communication between the cli tools and the rundeck server.

Before you can establish trust, you need to get the CA certificate.  Typically, this would require a request to the organization's security officer to have them send you the certificate.  It's also often found publicly if your organization does secure transactions.

Another option is to interrogate the secure ldap endpoint with openssl.  The example below shows a connection to paypal.com on port 443.  The first certificate is the machine and that last is the CA.  Pick the last certificate.  

*note* that for Active Directory, the host would be the Active Directory server and port 686.  
*note* Certificates are PEM encoded and start with -----BEGIN CERTIFICATE----- end with -----END CERTIFICATE----- inclusive.

~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
$ openssl s_client -showcerts -connect paypal.com:443
CONNECTED(00000003)
depth=1 C = US, O = "VeriSign, Inc.", OU = VeriSign Trust Network, OU = Terms of use at https://www.verisign.com/rpa (c)09, CN = VeriSign Class 3 Secure Server CA - G2
verify error:num=20:unable to get local issuer certificate
verify return:0
---
Certificate chain
 0 s:/C=US/ST=California/L=San Jose/O=PayPal, Inc./OU=Information Systems/CN=paypal.com
   i:/C=US/O=VeriSign, Inc./OU=VeriSign Trust Network/OU=Terms of use at https://www.verisign.com/rpa (c)09/CN=VeriSign Class 3 Secure Server CA - G2
-----BEGIN CERTIFICATE-----
MIIFDjCCA/agAwIBAgIQL0NdM6l74HplIwrcygDcCTANBgkqhkiG9w0BAQUFADCB
tTELMAkGA1UEBhMCVVMxFzAVBgNVBAoTDlZlcmlTaWduLCBJbmMuMR8wHQYDVQQL
ExZWZXJpU2lnbiBUcnVzdCBOZXR3b3JrMTswOQYDVQQLEzJUZXJtcyBvZiB1c2Ug
YXQgaHR0cHM6Ly93d3cudmVyaXNpZ24uY29tL3JwYSAoYykwOTEvMC0GA1UEAxMm
VmVyaVNpZ24gQ2xhc3MgMyBTZWN1cmUgU2VydmVyIENBIC0gRzIwHhcNMTAwNTAz
MDAwMDAwWhcNMTIwNjExMjM1OTU5WjB/MQswCQYDVQQGEwJVUzETMBEGA1UECBMK
Q2FsaWZvcm5pYTERMA8GA1UEBxQIU2FuIEpvc2UxFTATBgNVBAoUDFBheVBhbCwg
SW5jLjEcMBoGA1UECxQTSW5mb3JtYXRpb24gU3lzdGVtczETMBEGA1UEAxQKcGF5
cGFsLmNvbTCBnzANBgkqhkiG9w0BAQEFAAOBjQAwgYkCgYEArlvu+86iVb4RXdX+
8MjmGynNSl+Hu2/ZJ7nU1sj5O2jASWwFH7PFUv10qlRtL+gi3Rjw+zFN958iUetz
ef4CxQYf52PA7Uj9YlFEzLz7f8UDotu4WNLM3QGbLrqS28pPb2qKyyOQDvwNpI1c
Jt4JDa0ofVnCdICZEnf+cJB121MCAwEAAaOCAdEwggHNMAkGA1UdEwQCMAAwCwYD
VR0PBAQDAgWgMEUGA1UdHwQ+MDwwOqA4oDaGNGh0dHA6Ly9TVlJTZWN1cmUtRzIt
Y3JsLnZlcmlzaWduLmNvbS9TVlJTZWN1cmVHMi5jcmwwRAYDVR0gBD0wOzA5Bgtg
hkgBhvhFAQcXAzAqMCgGCCsGAQUFBwIBFhxodHRwczovL3d3dy52ZXJpc2lnbi5j
b20vcnBhMB0GA1UdJQQWMBQGCCsGAQUFBwMBBggrBgEFBQcDAjAfBgNVHSMEGDAW
gBSl7wsRzsBBA6NKZZBIshzgVy19RzB2BggrBgEFBQcBAQRqMGgwJAYIKwYBBQUH
MAGGGGh0dHA6Ly9vY3NwLnZlcmlzaWduLmNvbTBABggrBgEFBQcwAoY0aHR0cDov
L1NWUlNlY3VyZS1HMi1haWEudmVyaXNpZ24uY29tL1NWUlNlY3VyZUcyLmNlcjBu
BggrBgEFBQcBDARiMGChXqBcMFowWDBWFglpbWFnZS9naWYwITAfMAcGBSsOAwIa
BBRLa7kolgYMu9BSOJsprEsHiyEFGDAmFiRodHRwOi8vbG9nby52ZXJpc2lnbi5j
b20vdnNsb2dvMS5naWYwDQYJKoZIhvcNAQEFBQADggEBADbOGDkzZy22y+fW4OR7
wkx+1E3BxnRMZYx89OOykzTEUt2UV5DVuccUbqxTxg9/4pKMYJLywYn9UIOPHpwx
fbvMQNpdqV3JSuGMTwpROrMvC3bT13aCxxDnozeCjd/lH74m6G5ef2EUd3m5Y+iC
fMPo2NMrVyQYOCtpJurh9Tre1gQFHUYAXw8ty0YxfMoR/7FwYbd4spiZJwL2Mvfn
9gn24dWuKY7JaFutomwOM78rGzBDZZ/spEx9rcNa3OuVHcqBamnnXQZlZJilj4LE
buMBx8ti5Oqy4z1u1vzA8HalseiZerqFtBGOIakXdto8qLnwYEHQvVa/ih5iTsi3
Ja8=
-----END CERTIFICATE-----
 1 s:/C=US/O=VeriSign, Inc./OU=VeriSign Trust Network/OU=Terms of use at https://www.verisign.com/rpa (c)09/CN=VeriSign Class 3 Secure Server CA - G2
   i:/C=US/O=VeriSign, Inc./OU=Class 3 Public Primary Certification Authority - G2/OU=(c) 1998 VeriSign, Inc. - For authorized use only/OU=VeriSign Trust Network
-----BEGIN CERTIFICATE-----
MIIGLDCCBZWgAwIBAgIQbk/6s8XmacTRZ8mSq+hYxDANBgkqhkiG9w0BAQUFADCB
wTELMAkGA1UEBhMCVVMxFzAVBgNVBAoTDlZlcmlTaWduLCBJbmMuMTwwOgYDVQQL
EzNDbGFzcyAzIFB1YmxpYyBQcmltYXJ5IENlcnRpZmljYXRpb24gQXV0aG9yaXR5
IC0gRzIxOjA4BgNVBAsTMShjKSAxOTk4IFZlcmlTaWduLCBJbmMuIC0gRm9yIGF1
dGhvcml6ZWQgdXNlIG9ubHkxHzAdBgNVBAsTFlZlcmlTaWduIFRydXN0IE5ldHdv
cmswHhcNMDkwMzI1MDAwMDAwWhcNMTkwMzI0MjM1OTU5WjCBtTELMAkGA1UEBhMC
VVMxFzAVBgNVBAoTDlZlcmlTaWduLCBJbmMuMR8wHQYDVQQLExZWZXJpU2lnbiBU
cnVzdCBOZXR3b3JrMTswOQYDVQQLEzJUZXJtcyBvZiB1c2UgYXQgaHR0cHM6Ly93
d3cudmVyaXNpZ24uY29tL3JwYSAoYykwOTEvMC0GA1UEAxMmVmVyaVNpZ24gQ2xh
c3MgMyBTZWN1cmUgU2VydmVyIENBIC0gRzIwggEiMA0GCSqGSIb3DQEBAQUAA4IB
DwAwggEKAoIBAQDUVo9XOzcopkBj0pXVBXTatRlqltZxVy/iwDSMoJWzjOE3JPMu
7UNFBY6J1/raSrX4Po1Ox/lJUEU3QJ90qqBRVWHxYISJpZ6AjS+wIapFgsTPtBR/
RxUgKIKwaBLArlwH1/ZZzMtiVlxNSf8miKtUUTovStoOmOKJcrn892g8xB85essX
gfMMrQ/cYWIbEAsEHikYcV5iy0PevjG6cQIZTiapUdqMZGkD3pz9ff17Ybz8hHyI
XLTDe+1fK0YS8f0AAZqLW+mjBS6PLlve8xt4+GaRCMBeztWwNsrUqHugffkwer/4
3RlRKyC6/qfPoU6wZ/WAqiuDLtKOVImOHikLAgMBAAGjggKpMIICpTA0BggrBgEF
BQcBAQQoMCYwJAYIKwYBBQUHMAGGGGh0dHA6Ly9vY3NwLnZlcmlzaWduLmNvbTAS
BgNVHRMBAf8ECDAGAQH/AgEAMHAGA1UdIARpMGcwZQYLYIZIAYb4RQEHFwMwVjAo
BggrBgEFBQcCARYcaHR0cHM6Ly93d3cudmVyaXNpZ24uY29tL2NwczAqBggrBgEF
BQcCAjAeGhxodHRwczovL3d3dy52ZXJpc2lnbi5jb20vcnBhMDQGA1UdHwQtMCsw
KaAnoCWGI2h0dHA6Ly9jcmwudmVyaXNpZ24uY29tL3BjYTMtZzIuY3JsMA4GA1Ud
DwEB/wQEAwIBBjBtBggrBgEFBQcBDARhMF+hXaBbMFkwVzBVFglpbWFnZS9naWYw
ITAfMAcGBSsOAwIaBBSP5dMahqyNjmvDz4Bq1EgYLHsZLjAlFiNodHRwOi8vbG9n
by52ZXJpc2lnbi5jb20vdnNsb2dvLmdpZjApBgNVHREEIjAgpB4wHDEaMBgGA1UE
AxMRQ2xhc3MzQ0EyMDQ4LTEtNTIwHQYDVR0OBBYEFKXvCxHOwEEDo0plkEiyHOBX
LX1HMIHnBgNVHSMEgd8wgdyhgcekgcQwgcExCzAJBgNVBAYTAlVTMRcwFQYDVQQK
Ew5WZXJpU2lnbiwgSW5jLjE8MDoGA1UECxMzQ2xhc3MgMyBQdWJsaWMgUHJpbWFy
eSBDZXJ0aWZpY2F0aW9uIEF1dGhvcml0eSAtIEcyMTowOAYDVQQLEzEoYykgMTk5
OCBWZXJpU2lnbiwgSW5jLiAtIEZvciBhdXRob3JpemVkIHVzZSBvbmx5MR8wHQYD
VQQLExZWZXJpU2lnbiBUcnVzdCBOZXR3b3JrghB92f4Hz6getxB5Z/uniTTGMA0G
CSqGSIb3DQEBBQUAA4GBAGN0Lz1Tqi+X7CYRZhr+8d5BJxnSf9jBHPniOFY6H5Cu
OcUgdav4bC1nHynCIdcUiGNLsJsnY5H48KMBJLb7j+M9AgtvVP7UzNvWhb98lR5e
YhHB2QmcQrmy1KotmDojYMyimvFu6M+O0Ro8XhnF15s1sAIjJOUFuNWI4+D6ufRf
-----END CERTIFICATE-----
---
Server certificate
subject=/C=US/ST=California/L=San Jose/O=PayPal, Inc./OU=Information Systems/CN=paypal.com
issuer=/C=US/O=VeriSign, Inc./OU=VeriSign Trust Network/OU=Terms of use at https://www.verisign.com/rpa (c)09/CN=VeriSign Class 3 Secure Server CA - G2
---
No client certificate CA names sent
---
SSL handshake has read 3039 bytes and written 401 bytes
---
New, TLSv1/SSLv3, Cipher is DES-CBC3-SHA
Server public key is 1024 bit
Secure Renegotiation IS NOT supported
Compression: NONE
Expansion: NONE
SSL-Session:
    Protocol  : TLSv1
    Cipher    : DES-CBC3-SHA
    Session-ID: A8AAA4F22E9A4B3F12F76303464643525178846D96CA0BC0B81F35368BF55B89
    Session-ID-ctx: 
    Master-Key: 9F767B91FC2450E291CBB21E3438CA9A73FE8D5B825AD98F821F5EB912C088DFB66FCBF2D53591E2D1ED77E9B6A22504
    Key-Arg   : None
    PSK identity: None
    PSK identity hint: None
    Start Time: 1295242116
    Timeout   : 300 (sec)
    Verify return code: 20 (unable to get local issuer certificate)
---
^C
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

Once a certificate has been obtained.  There are two options for adding the certificate.  The first involves updating the truststore for the JRE.  If that is not possible or not desirable, then one can set the truststore to be used by the jvm, using any arbitrary truststore that contains the appropriate certificate.

Both options require importing a certificate.  The following would import a certificate called, AD.cert into the `/etc/rundeck/ssl/truststore`.

    keytool -import -alias CompanyAD -file AD.cert -keystore /etc/rundeck/ssl/truststore -storepass adminadmin 

To add the certificate to the JRE, locate the file $JAVA_HOME/lib/security/cacerts and run

    keytool -import -alias CompanyAD -file AD.cert -keystore $JAVA_HOME/lib/security/cacerts -storepass changeit 

To verify your CA has been added, run keytool list and look for CompanyAD in the output.

    keytool -list -keystore $JAVA_HOME/lib/security/cacerts -storepass changeit

Refer to: http://download.oracle.com/javase/1.5.0/docs/tooldocs/solaris/keytool.html for more information how how to import a certificate.

Finally, in your `ldap-activedirectory.conf` be sure to change the *providerUrl* to be `ldaps://ad-server`.  Including the port is optional as the default is 686.

#### Redundant Connection Options

*providerUrl* can take multiple, space delimited, urls.  For example: 

     providerUrl=ldaps://ad1 ldaps://ad2  

Use this to provide connection redundancy if a particular host is unavailable.

## Authorization

Three dimensions of information dictate authorization inside RunDeck:

* *group* memberships assigned to a *user* login.  
* access control policy that grants access to one or more *policy
  action*s to a *group*  or *user*.
* role mapping that ties *policy action*s to a set of user *group*s.

The section on [managing logins](#managing-logins) discusses how to
assign group memberships to users.

The remainder of this section will describe how to use the access
control policy and role mappings.

*Note from the project team*: The authorization layer is an early work
 in progress. Please share your ideas on the IRC or mailing list.

### Access control policy

Access to running or modifying Jobs is managed in an access control
policy defined using the aclpolicy XML document (aclpolicy-v10(5)). 
This file contains a number of policy elements that describe what user
group is allowed to perform which actions.

Policies can be organized into more than one file to help organize
access by group or pattern of use. The normal RunDeck install will
have generated a policy for the "admin" group. Not all users will need
to be given "admin" access level to control and modify all Jobs. More
typically, a group of users will be given access to just a subset of
Jobs.

File listing: admin.aclpolicy example

    <policies>
      <policy description="Administrative group that has access to
    execute all actions.">
        <context project="*">
          <command group="*" job="*" actions="*"/>
        </context>
        <by>
          <group name="admin"/>
        </by>
      </policy>
    </policies>

The example policy document above demonstrates the access granted to
the users in group "admin". The asterisks indicate a wild card for
those attributes (eg, semantically it means "ALL"). 

### Special API Token Authentication group

Clients of the [Web API](api/index.html) may use the [Token Authentication](api/index.html#token-authentication) method.  These clients are
placed in the special authorization group called `api_token_group`.

`api_token_group`
~   Special role given to all [API Token](api/index.html#token-authentication) authenticated access.

### Access control policy actions

The authorization defines a number of actions that can be referenced
inside the access control policy document. Reading their names, one
might see these actions as a granular set of roles.

`admin`
~   Enables the user or group "super user" access.
`user_admin`
~   Modify user profiles.
`workflow_read`
~   Read and view jobs.
`workflow_create`
~   Create new jobs.
`workflow_update`
~   Edit existing jobs.
`workflow_delete`
~   Remove jobs.
`workflow_kill`
~   Kill running jobs.
`workflow_run`
~   Execute a job.
`events_read`
~   List and view history.
`events_create`
~   Create new events.
`events_update`
~   Modify history (unused).
`events_delete`
~   Delete events (unused).
`resources_read`
~   List and view resources.
`resources_create`
~   Create resources (unused).
`resources_update`
~   Modify resources (unused).
`resources_delete`
~   Delete resources (unused).
`job_view_unauthorized`
~   Special role for viewing jobs that the user is unauthorized to run.

#### Access control policy actions example

Below is an example policy document demonstrating policy actions
to create limited access for a group of users.
Users in the group "restart_user", are allowed to run three jobs in the "adm"
group, Restart, stop and start. By allowing ``workflow_run`` but not ``workflow_read``,
the "stop" and "start" jobs will not be visible.

File listing: restart_user.aclpolicy example

    <policies>
      <policy description="Limited user access for adm restart action">
        <context project="*">
          <command group="adm" job="Restart" actions="workflow_run,workflow_read"/>
          <command group="adm" job="stop"  actions="workflow_run"/>
          <command group="adm" job="start" actions="workflow_run"/>
        </context>
        <by>
          <group name="restart_user"/>
        </by>
      </policy>
    </policies>
    
    
### Role mapping

Role mapping is a way of adapting the User Roles provided by your
authentication system to the Application Roles used by the RunDeck web
app. This lets you work with whatever authentication provider based
roles you have. Role mappings are defined in the
[rundeck-config.properties](#rundeck-config.properties) configuration file.

*Application Roles*

:   Role names used by the RunDeck application for testing whether the
    user is allowed to perform certain actions

*User Roles*

:   Role names used by an authentication system

These properties provide a mapping of allowed *Application Roles* to a
set of specified *User Roles*. The defaults shown here match the set of
default User Roles installed in the file based login mechanism (ie,
[realm.properties](#realm.properties) when you install RunDeck.

If you use your own directory-based authentication (eg [Active Directory](#active-directory)) you
may need to modify the roles you use, especially if you are unable to
change the roles/groups that User profiles are assigned to in your
directory.

These are the default Application Roles that the role mapping can override:

+-----------------------+-----------------------+
|**Application Role**   |**User Role**          |
+-----------------------+-----------------------+
|`admin`                |admin                  |
+-----------------------+-----------------------+
|`user_admin`           |admin                  |
+-----------------------+-----------------------+
|`workflow_read`        |user                   |
+-----------------------+-----------------------+
|`workflow_create`      |admin                  |
+-----------------------+-----------------------+
|`workflow_update`      |admin                  |
+-----------------------+-----------------------+
|`workflow_kill`        |user                   |
+-----------------------+-----------------------+
|`workflow_run`         |user                   |
+-----------------------+-----------------------+
|`events_read`          |user                   |
+-----------------------+-----------------------+
|`events_create`        |user                   |
+-----------------------+-----------------------+
|`events_update`        |admin                  |
+-----------------------+-----------------------+
|`events_delete`        |admin                  |
+-----------------------+-----------------------+
|`resources_read`       |user                   |
+-----------------------+-----------------------+
|`resources_create`     |admin                  |
+-----------------------+-----------------------+
|`resources_update`     |admin                  |
+-----------------------+-----------------------+
|`resources_delete`     |admin                  |
+-----------------------+-----------------------+
|`job_view_unauthorized`|job\_view\_unauthorized|
+-----------------------+-----------------------+


Note

:    Setting the mapping value to a comma-separated list of Role names
     grants that Application Role to a user in any of the mapped roles.


If no role mapping is defined for an Application Role, then the
literal name of the Application Role will be tested as the role
name. E.g. If "mappedRoles.admin" is not defined, then a role named
"admin" will be used.

The listing below contains the role mappings that appear after a
normal RunDeck installation you will find in [rundeck-config.properties](#rundeck-config.properties). 

    #
    # Map rundeck actions to allowed roles
    #       mappedRoles.X=A,B,C
    # means allow X to users in role A, B or C
    #
    mappedRoles.admin=admin
    mappedRoles.user_admin=admin
    mappedRoles.workflow_read=user
    mappedRoles.workflow_create=admin
    mappedRoles.workflow_update=admin
    mappedRoles.workflow_delete=admin
    mappedRoles.workflow_kill=user
    mappedRoles.workflow_run=user
    mappedRoles.events_read=user
    mappedRoles.events_create=user
    mappedRoles.events_update=user
    mappedRoles.events_delete=user
    mappedRoles.resources_read=user
    mappedRoles.resources_create=admin
    mappedRoles.resources_update=admin
    mappedRoles.resources_delete=admin
    #special role for viewing jobs unauthorized to run
    mappedRoles.job_view_unauthorized=job_view_unauthorized

You can replace all of the *User Roles* shown in this file with your own
custom role names from your directory service.

### Troubleshooting access control policy

After defining an aclpolicy file to grant access to a particular group
of users, you may find them getting "unauthorized" messages or
complaints that certain actions are not possible. 

To diagnose this, begin by checking two bits:

1. The user's group membership. This can be done by going to the
   user's profile page in RunDeck. That page will list the groups the
   user is a member. 
2. Read the messages inside the `rundeck.audit.log` log file. The
   authorization facility generates fairly low level messages describing
   how the policy is matched to the user context.

If you don't see any output in the audit log for a user's action and
they are able to login, then make sure [role mapping](#role-mapping)
is set correctly.

Once the user role mappings are defined correctly ask the user to
login again and attempt accessing their jobs. You should watch the
stream of messages flowing through the audit log.
For each entry, you'll see all decisions leading up to either a
AUTHORIZED or a REJECTED message.  It's not uncommon to see REJECTED
messages followed by AUTHORIZED.  The important thing is to look at
the last decision made.

### Authorization caveats

* aclpolicy is used to determine what UI widgets are displayed (e.g., run,
  edit, delete, etc).  But Authorization for pages still depends on Roles.
  (i.e. the AuthorizationFilter code looks at role membership, not
  aclpolicy).
* role mapping changes require a server restart, while aclpolicy
  changes do not.
* new and deleted aclpolicy files are detected after restarts
  

## Configuring Rundeck for SSL

This document describes how to configure Rundeck for SSL/HTTPS
support, and assumes you are using the rundeck-launcher standalone
launcher.

(1) Before beginning, do a first-run of the launcher, as it will create
the base directory for Rundeck and generate configuration files.

        cd $RDECK_BASE;  java -jar rundeck-launcher-1.1.0.jar

This will start the server and generate necessary config files.  Press
control-c to shut down the server.

(2)  Using the [keytool] command, generate a keystore for use as the
server cert and client truststore. Specify passwords for key and keystore:

        keytool -keystore etc/keystore -alias rundeck -genkey -keyalg RSA -keypass admin -storepass admin
    
Be sure to specify the correct hostname of the server as the response
to the question "What is your first and last name?".  Answer "yes" to
the final question.

You can pass all the answers to the tool on the command-line by using
a HERE document.

Replace the first line "Venkman.local" with the hostname for your
server, and use any other organizational values you like:
    
        keytool -keystore etc/keystore -alias rundeck -genkey -keyalg RSA -keypass adminadmin -storepass adminadmin  <<!
        Venkman.local
        devops
        My org
        my city
        my state
        US
        yes
        !

[keytool]: http://linux.die.net/man/1/keytool-java-1.6.0-openjdk

(3) CLI tools that communicate to the Rundeck server need to trust the
SSL certificate provided by the server.  They are preconfigured to
look for a truststore at the location:
`$RDECK_BASE/etc/truststore`. Copy the keystore as the truststore for
CLI tools: 

        cp etc/keystore etc/truststore

(4) Modify the ssl.properties file to specify the location of the
keystore and the appropriate passwords:

        vi server/config/ssl.properties

An example ssl.properties file (from the RPM package).

        keystore=/etc/rundeck/ssl/keystore
        keystore.password=adminadmin
        key.password=adminadmin
        truststore=/etc/rundeck/ssl/truststore
        truststore.password=adminadmin
        
(5) Configure client properties.  Modify the file
`$RDECK_BASE/etc/framework.properties` and change these properties: 

    * `framework.server.url`
    * `framework.rundeck.url`
    * `framework.server.port` 
    
Set them to the appropriate https protocol, and change the port to
4443, or to the value of your `-Dserver.https.port` runtime
configuration property.

(6) Launch the rundeck launcher and tell it where to read the ssl.properties

        java -Drundeck.ssl.config=$RDECK_BASE/server/config/ssl.properties -jar rundeck-launcher-1.1.0.jar
    
You can change port by adding `-Dserver.https.port`:
    
        java -Drundeck.ssl.config=$RDECK_BASE/server/config/ssl.properties -Dserver.https.port=1234 rundeck-launcher-1.1.0.jar
    
If successful, you will see a line indicating the SSl connector has started:

    Started SslSocketConnector@0.0.0.0:4443

### Securing passwords

Passwords do not have to be stored in the ssl.config.  If they are not
set, then the server will prompt on the console for a user to enter
the passwords.

If you want the server to start without prompting then you need to set
the passwords in the config file.  

The passwords stored in ssl.properties can be obfuscated so they are
not in plaintext:

Run the jetty "Password" utility:

    $ java -cp server/lib/jetty-6.1.21.jar:server/lib/jetty-util-6.1.21.jar org.mortbay.jetty.security.Password <password>
    
This will produce two lines, one starting with "OBF:"

Use the entire OBF: output as the password in the ssl.properties file, eg:

    key.password=OBF:1lk2j1lkj321lj13lj
    

### Troubleshooting keystore

Some common error messages and causes:


java.io.IOException: Keystore was tampered with, or password was incorrect

:    A password specified in the file was incorrect.

2010-12-02 10:07:29.958::WARN:  failed SslSocketConnector@0.0.0.0:4443: java.io.FileNotFoundException: /Users/greg/rundeck/etc/keystore (No such file or directory)

:    The keystore/truststore file specified in ssl.properties doesn't exist


### Optional PEM export

You can export the PEM formatted server certificate for use by HTTPS
clients (web browsers or e.g. curl).


Export pem cacert for use by e.g. curl: 

    keytool -export -keystore etc/keystore -rfc -alias rundeck > rundeck.server.pem

## Customizing RunDeck GUI

You can modify some display features of the RunDeck GUI by setting
these properties in the [rundeck-config.properties](#rundeck-config.properties) file:

+-------------------------+-------------------------------------+--------------------+
|**Property**             |   **Description**                   |**example**         |
+-------------------------+-------------------------------------+--------------------+
|`rundeck.gui.title`      |Title shown in app header            |Test App            |
+-------------------------+-------------------------------------+--------------------+
|`rundeck.gui.logo`       |Logo icon path relative to           | test-logo.png      |
|                         |webapps/rundeck/images dir           |                    |
+-------------------------+-------------------------------------+--------------------+
|`rundeck.gui.logo-width` |Icon width for proper display (32px  |32px                |
|                         |is best)                             |                    |
+-------------------------+-------------------------------------+--------------------+
|`rundeck.gui.logo-height`|Icon height for proper display (32px |32px                |
|                         |is best)                             |                    |
+-------------------------+-------------------------------------+--------------------+
|`rundeck.gui.titleLink`  |URL for the link used by the app     |http://rundeck.org  |
|                         |header icon.                         |                    |
+-------------------------+-------------------------------------+--------------------+
|`rundeck.gui.helpLink`   |URL for the "help" link in the app   |http://rundeck.org/ |
|                         |header.                              |   docs             |
+-------------------------+-------------------------------------+--------------------+
|`rundeck.gui.realJobTree`|Displaying a real tree in the Jobs   |false               |
|                         |overview instead of collapsing       |                    |
|                         |empty groups. **Default: true**      |                    |
+-------------------------+-------------------------------------+--------------------+
