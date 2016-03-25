% Authenticating Users

Rundeck uses *Container Authentication* to determine
the logged in user name and the user's authorized roles.

For the default installation (Rundeck Launcher, RPM, Deb),
the Servlet Container is Jetty,
and the underlying security mechanism is JAAS,
so you are free to use what ever JAAS provider
you feel is suitable for your environment.
See [JAAS](http://en.wikipedia.org/wiki/Java_Authentication_and_Authorization_Service)
and specifically for Jetty,
[JAAS for Jetty](http://docs.codehaus.org/display/JETTY/JAAS).

If you use the Rundeck war file with a different container, such as Tomcat, refer to [Container authentication and authorization](#container-authentication-and-authorization) below.

# Jetty and JAAS authentication

Rundeck has three basic ways of defining authentication.

1. [PropertyFileLoginModule](#propertyfileloginmodule)
2. [LDAP](#ldap)
3. [PAM](#pam)

By default a new installation uses the realm.properties method.

## Security Role

There is a "required role" which any user you wish to allow access to Rundeck must belong to. By default that role name is user (As of Rundeck 2.0 and earlier). The required role name must be manually changed if you want a different required role.

To modify the required role name, edit the web.xml file:

* RPM/debian install path: /var/lib/rundeck/exp/webapp/WEB-INF/web.xml
* Jar/launcher: After running once or executing `--installonly`, modify the path $RDECK_BASE/server/exp/webapp/WEB-INF/web.xml
  * after modifying the file you must run the launcher jar with --skipinstall to avoid overwriting the file with the original value

Modify the section shown below and replace user with your own required role name:

~~~~~~ {.xml }
<security-role>
    <role-name>user</role-name>
</security-role>
~~~~~~

Here's an example that changes the required role to one called "myADgroup" (the hypothetical group name from Active Directory):

~~~~~~ {.diff .numberLines }
--- /var/lib/rundeck/exp/webapp/WEB-INF/web.xml
+++ /var/lib/rundeck/exp/webapp/WEB-INF/web.xml.new
@@ -9,7 +9,7 @@
                <param-value>rundeck-production-2.1.2</param-value>
        </context-param>
        <security-role>
-               <role-name>user</role-name>
+               <role-name>myADgroup</role-name>
        </security-role>
        <security-constraint>
                <web-resource-collection>
~~~~~~

## PropertyFileLoginModule

These instructions explain how to manage user credentials for
Rundeck using a text file containing usernames, passwords and role definitions.
Usually this file is called <code>realm.properties</code>.

The default Rundeck webapp handles user authentication via its
container, which in turn is configured to pull its user authentication
from the realm.properties file.
This file is created at the time that you install the server.

Location:

* Launcher: `$RDECK_BASE/server/config/realm.properties`
* RPM/DEB: `/etc/rundeck/realm.properties`

Assuming it wasn't modified, your realm.properties file will
probably look something like this:

~~~~~~ {.bash .numberLines }
#
# This file defines users passwords and roles for a HashUserRealm
#
# The format is
#  <username>: <password>[,<rolename> ...]
#
# Passwords may be clear text, obfuscated or checksummed.  
#
# This sets the default user accounts for the Rundeck apps
#
admin:admin,user,admin
user:user,user
~~~~~~

*Adding additional users*

You may wish to have additional users with various privileges rather
than giving out role accounts to groups.  You may also want to avoid
having the passwords in plaintext within the configuration file.  

To accomplish this, you'll need a properly hashed or encrypted
password to use in the config.  Pass the
username and password to the `Password` utility which is part of the
`jetty-all-7.6.0.v20120127.jar` file.

Location:

* Launcher install: `$RDECK_BASE/server/lib/jetty-all-7.6.0.v20120127.jar`
* RPM/Deb install: `/var/lib/rundeck/bootstrap/jetty-all-7.6.0.v20120127.jar`

Use the correct path below.

In this example,
we'll setup a new user named "jsmith", with a password of "mypass":

~~~~~~ {.bash }
$ java -cp jetty-all-7.6.0.v20120127.jar org.eclipse.jetty.util.security.Password jsmith mypass
~~~~~~

~~~~~~
OBF:1xfd1zt11uha1ugg1zsp1xfp
MD5:a029d0df84eb5549c641e04a9ef389e5
CRYPT:jsnDAc2Xk4W4o
~~~~~~

Then add this to the `realm.properties` file with a line like so:

    jsmith: MD5:a029d0df84eb5549c641e04a9ef389e5,user,admin

Then restart Rundeck to ensure it picks up the change and you're done.

## LDAP

LDAP and Active Directory configurations are created in the same way, but your LDAP structure may be different than Active Directory's structure.

Rundeck includes two JAAS login modules you can use for LDAP directory authentication:

* `JettyCachingLdapLoginModule` Performs LDAP authentication and looks up user roles based on LDAP group membership
* `JettyCombinedLdapLoginModule` (**Since Rundeck 2.5**).   Performs LDAP authentication, and can use "shared authentication credentials" to allow another module to provide authorization lookup for user roles.  See [Login module configuration](#login-module-configuration) and [JettyRolePropertyFileLoginModule](#jettyrolepropertyfileloginmodule) and [Multiple Authentication Modules](#multiple-authentication-modules) below.

These are an enhanced version of the default Jetty JAAS Ldap login module that caches authorization results for a period of time.

JAAS supports evaluating `MD5` and `CRYPT` password hashes.

You must change some configuration values to change the authentication module to use.

### Configuration

Configuring LDAP consists of defining a JAAS config file (e.g. "jaas-ldap.conf"), and changing the server startup script to use this file and use the correct Login Module configuration inside it.

#### Step 1: Setup the LDAP login module configuration file

Create a `jaas-ldap.conf` file in the same directory as the `jaas-loginmodule.conf` file.

* RPM install: /etc/rundeck/
* Launcher install: $RDECK_BASE/server/config

Make sure the name of your Login Module configuration is the same as you use in the next step.  The Login Module configuration is defined like this:

~~~~~~ {.c }
    myloginmodule {
        // comment line
        ...
    }
~~~~~~

Where "myloginmodule" is the name.

#### Step 2: Specify login module

To override the default JAAS configuration file, you will need to supply the Rundeck server with the proper path to the new one, and a `loginmodule.name` Java system property to identify the new login module by name.

The JAAS configuration file location is specified differently between the Launcher and the RPM.

**For the Launcher**:  the `loginmodule.conf.name` Java system property is used to identify the *name* of the config file, which must be located in the `$RDECK_BASE/server/config` dir.

You can simply specify the system properties on the java commandline:

~~~~~~ {.bash}
java -Dloginmodule.conf.name=jaas-ldap.conf \
    -Dloginmodule.name=ldap \
    -jar rundeck-launcher-x.x.jar
~~~~~~

Otherwise, if you are starting the Launcher via the supplied `rundeckd` script, you can modify the `RDECK_JVM` value in the `$RDECK_BASE/etc/profile` file to add two JVM arguments:

~~~~~~ {.bash}
export RDECK_JVM="-Dloginmodule.conf.name=jaas-ldap.conf \
    -Dloginmodule.name=ldap"
~~~~~~

Note: more information about using the launcher and useful properties are under [Getting Started - Launcher Options](installation.html#launcher-options).

**For the RPM installation**: the absolute path to the JAAS config file must be specified with the `java.security.auth.login.config` property.

Update the `RDECK_JVM` in `/etc/rundeck/profile` by changing the following two JVM arguments:

~~~~~~ {.bash}
export RDECK_JVM="-Djava.security.auth.login.config=/etc/rundeck/jaas-loginmodule.conf \
       -Dloginmodule.name=RDpropertyfilelogin \
~~~~~~

to

~~~~~~ {.bash}
export RDECK_JVM="-Djava.security.auth.login.config=/etc/rundeck/jaas-ldap.conf \
       -Dloginmodule.name=ldap \
~~~~~~


#### Step 3: Restart rundeckd

~~~~~~ {.bash}
sudo /etc/init.d/rundeckd restart
~~~~~~

#### Step 4: Attempt to logon

If everything was configured correctly, you will be able to access Rundeck using your AD credentials.  If something did not go smoothly, look at `/var/log/rundeck/service.log` for stack traces that may indicate what is wrong.
To make troubleshooting easier, you may want to add the `-Dcom.dtolabs.rundeck.jetty.jaas.LEVEL=DEBUG` Java system property to the `RDECK_JVM` environment variable above, to have enable DEBUG logging for the authentication module.

### Login module configuration

Here is an example configuration file for the `JettyCachingLdapLoginModule`:

~~~~ {.c .numberLines}
ldap {
    com.dtolabs.rundeck.jetty.jaas.JettyCachingLdapLoginModule required
      debug="true"
      contextFactory="com.sun.jndi.ldap.LdapCtxFactory"
      providerUrl="ldap://server:389"
      bindDn="cn=Manager,dc=example,dc=com"
      bindPassword="secrent"
      authenticationMethod="simple"
      forceBindingLogin="false"
      userBaseDn="ou=People,dc=test1,dc=example,dc=com"
      userRdnAttribute="uid"
      userIdAttribute="uid"
      userPasswordAttribute="userPassword"
      userObjectClass="account"
      roleBaseDn="ou=Groups,dc=test1,dc=example,dc=com"
      roleNameAttribute="cn"
      roleUsernameMemberAttribute="memberUid"
      roleMemberAttribute="memberUid"
      roleObjectClass="posixGroup"
      cacheDurationMillis="300000"
      supplementalRoles="user"
      reportStatistics="true"
      timeoutRead="10000"
      timeoutConnect="20000"
      nestedGroups="false";
};
~~~~

The `JettyCachingLdapLoginModule` has these configuration properties:

`debug`
:    "true/false" - turn on or off debug output

`contextFactory`
:    The LDAP context factory class, e.g. "com.sun.jndi.ldap.LdapCtxFactory"

`providerUrl`
:    ldap URL for the server, e.g. "ldap://server:389"

`bindDn`
:    Optional. If not using "binding" authentication, set this to the root DN that should bind, e.g. "cn=Manager,dc=example,dc=com"

`bindPassword`
:    password for root DN. **Note**: The `bindDn` and `bindPassword` must escape any special characters with `\` character. Special characters include `\` (backslash), as well as `!` (exclamation).

`authenticationMethod`
:    Authentication method, e.g. "simple"

`forceBindingLogin`
:    "true/false" - if true, bind as the user that is authenticating, otherwise bind as the manager and perform a search to verify user password. NOTE: This module can only verify passwords hashed with `MD5` or `CRYPT`. If your LDAP directory contains other hashes you'll likely need to set this to true to be able to authenticate.

`forceBindingLoginUseRootContextForRoles`
:    "true/false" - if true and forceBindingLogin is true, then role membership searches will be performed in the root context, rather than in the bound user context.

`userBaseDn`
:    base DN to search for users, example: "ou=People,dc=test1,dc=example,dc=com"

`userRdnAttribute`
:    Attribute name for username, used when searching for user role membership by DN, default "uid".

`userIdAttribute`
:    Attribute name to identify user by username, default "cn".

`userPasswordAttribute`
:    Attribute name for user password, default "userPassword".

`userObjectClass`
:    Attribute name for user object class, default "inetOrgPerson".

`roleBaseDn`
:    Base DN for role membership search, e.g. "ou=Groups,dc=test1,dc=example,dc=com".

`roleNameAttribute`
:    Attribute name for role name, default "roleName".

`roleMemberAttribute`
:    Attribute name for a role that would contain a user's DN, default "uniqueMember".

`roleUsernameMemberAttribute`
:    Attribute name for a role that would contain a user's username. If set, this overrides the `roleMemberAttribute` behavior.

`roleObjectClass`
:    Object class for role, default "groupOfUniqueNames".

`rolePrefix`
:    Prefix string to remove from role names before returning to the application, e.g. "rundeck_".

`cacheDurationMillis`
:   Duration that authorization should be cached, in milliseconds. Default "0". A value of "0" indicates no caching should be used.

`reportStatistics`
:    "true/false" - if true, output cache statistics to the log.

`supplementalRoles`
:    Comma-separated list of role names. All of the given role names will be automatically added to authenticated users.  You can use this to provide a "default" role or roles for all users.

`timeoutRead`
:    Read timeout value (ms). Default: 0 (no timeout)

`timeoutConnect`
:    Connect timeout value (ms). Default: 0 (no timeout)

`nestedGroups`
:    "true/false" - Default: false. If true, will resolve all nested groups for authenticated users. For the first user to login after a fresh start it will take a couple of seconds longer, this is when the cache of all nested groups is built. This will happen as often as the cache is refreshed. Uses the cacheDurationMillis for cache timeout.

The `JettyCombinedLdapLoginModule` is extends the previous module, so is configured in almost exactly the same way, but adds these additional configuration options:


~~~~ {.c .numberLines}
ldap {
    com.dtolabs.rundeck.jetty.jaas.JettyCombinedLdapLoginModule required
      ...
      ignoreRoles="true"
      storePass="true"
      clearPass="true"
      useFirstPass="false"
      tryFirstPass="false";
};
~~~~


`ignoreRoles`
:    Do not look up role membership via LDAP. May be used with `storePass` and combined with another login module for authorization roles. (See [JettyRolePropertyFileLoginModule](#jettyrolepropertyfileloginmodule))

`storePass`
:    Store the user/password for use by subsequent login module.

`clearPass`
:    Clear the user/password that was stored after login is successful. (This would prevent any subsequent login modules from re-using the stored credentials.  Set it to `false` if you want to continue using the credentials.)

`useFirstPass`
:    Use the stored user/password for authentication, and do not attempt to use other mechanism (e.g. login callback).

`tryFirstPass`
:    Try to use the stored user/password for authentication, if it fails then proceed with normal mechanism.

### Combining LDAP with other modules

Use the `JettyCombinedLdapLoginModule` to do LDAP authentication, then combine it with the [JettyRolePropertyFileLoginModule](#jettyrolepropertyfileloginmodule) (or some other module) to supply the authorization roles.

The first module should set `storePass="true"`, and the second module should set `useFirstPass="true"` or `tryFirstPass="true"`.

Finally, see the section [Multiple Authentication Modules](#multiple-authentication-modules) about the appropriate configuration Flag to use.

The [PAM](#pam) section is a useful comparison as it uses the same method to combine modules.

### Active Directory

Here is an example configuration for Active Directory. The string *sAMAccountName* refers to the short user name and is valid in a default Active Directory installation, but may vary in some environments.

~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~{.c .numberLines}
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
    userRdnAttribute="sAMAccountName"
    userIdAttribute="sAMAccountName"
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

### Communicating over secure ldap (ldaps://)

The default port for communicating with active directory is 389, which is insecure.  The secure port is 686, but the LoginModule describe above requires that the AD certificate or organizations CA certificate be placed in a truststore.  The truststore provided with rundeck `/etc/rundeck/ssl/truststore` is used for the local communication between the cli tools and the rundeck server.

Before you can establish trust, you need to get the CA certificate.  Typically, this would require a request to the organization's security officer to have them send you the certificate.  It's also often found publicly if your organization does secure transactions.

Another option is to interrogate the secure ldap endpoint with openssl.  The example below shows a connection to paypal.com on port 443.  The first certificate is the machine and that last is the CA.  Pick the last certificate.  

*note* that for Active Directory, the host would be the Active Directory server and port 686.  
*note* Certificates are PEM encoded and start with -----BEGIN CERTIFICATE----- end with -----END CERTIFICATE----- inclusive.

~~~~~~~~~~~~~~~~~~~~~~~~~~~~~ {.bash}
$ openssl s_client -showcerts -connect paypal.com:443
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
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

~~~~~~~~~~~~~~~~~~~~~~~~~~~~~ {.bash}
keytool -import -alias CompanyAD -file AD.cert -keystore  /etc/rundeck/ssl/truststore -storepass adminadmin
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

To add the certificate to the JRE, locate the file $JAVA_HOME/lib/security/cacerts and run

~~~~~~~~~~~~~~~~~~~~~~~~~~~~~ {.bash}
keytool -import -alias CompanyAD -file AD.cert -keystore $JAVA_HOME/lib/security/cacerts -storepass changeit
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

To verify your CA has been added, run keytool list and look for CompanyAD in the output.

~~~~~~~~~~~~~~~~~~~~~~~~~~~~~ {.bash}
keytool -list -keystore $JAVA_HOME/lib/security/cacerts -storepass changeit
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

Refer to: http://download.oracle.com/javase/1.5.0/docs/tooldocs/solaris/keytool.html for more information how how to import a certificate.

Finally, in your `ldap-activedirectory.conf` be sure to change the *providerUrl* to be `ldaps://ad-server`.  Including the port is optional as the default is 686.

## PAM

Rundeck includes a [PAM](https://en.wikipedia.org/wiki/Pluggable_authentication_module) JAAS login module, which uses [libpam4j](https://github.com/kohsuke/libpam4j) to authenticate.

This module can work with existing properties-file based authorization roles by enabling shared credentials between the modules, and introducing a Property file module that can be used only for authorization.

Modules:

* `org.rundeck.jaas.jetty.JettyPamLoginModule` authenticates via PAM, can add a default set of roles to authenticated users, and can use local unix group membership for role info.
* `org.rundeck.jaas.jetty.JettyAuthPropertyFileLoginModule` authenticates via property file, but does not supply user authorization information.
* `org.rundeck.jaas.jetty.JettyRolePropertyFileLoginModule` does not authenticate and only uses authorization roles from a property file. Can be combined with previous modules.

sample jaas config:

~~~~~~ {.c .numberLines}
RDpropertyfilelogin {
  org.rundeck.jaas.jetty.JettyPamLoginModule requisite
        debug="true"
        service="sshd"
        supplementalRoles="readonly"
        storePass="true";

    org.rundeck.jaas.jetty.JettyRolePropertyFileLoginModule required
        debug="true"
        useFirstPass="true"
        file="/etc/rundeck/realm.properties";

};
~~~~~~~

When combining the two login modules, note that the `storePass` and
`useFirstPass` are set to true, allowing the two modules to share the user information necessary for the second module to load the user roles.

**Common configuration properties:**

These JAAS configuration properties are used by all of the Jetty PAM modules:

* `useFirstPass`
* `tryFirstPass`
* `storePass`
* `clearPass`
* `debug`

### JettyPamLoginModule

Configuration properties:

* `serviceName` - name of the PAM service configuration to use. (Required). Example: "sshd".
* `useUnixGroups` - true/false. If true, the unix Groups defined for the user will be included as authorization roles. Default: false.
* `supplementalRoles` - a comma-separated list of additional user roles to add to any authenticated user. Example: 'user,readonly'


### JettyRolePropertyFileLoginModule

This module does not authenticate, and requires that `useFirstPass` or `tryFirstPass` is set to `true`, and that a previous module has `storePass` set to `true`.

It then looks the username up in the Properties file, and applies any roles for the matching user, if found.

Configuration properties:

* `file` - path to a Java Property formatted file in the format defined under [realm.properties](#PropertyFileLoginModule)
* `caseInsensitive` - true/false. If true, usernames are converted to lowercase before being looked up in the property file, otherwise they are compared as entered. Default: true.

Note that since the user password is not used for authentication, you can have a dummy value in the password field of the file, but *some value is required* in that position.

Example properties file with dummy passwords and roles:

    admin: -,user,admin
    user1: -,user,readonly

### JettyAuthPropertyFileLoginModule

This module provides authentication in the same way as the [realm.properties](#PropertyFileLoginModule) mechanism, but does not use any of the role names found in the file.  It can be combined with `JettyRolePropertyFileLoginModule` by using `storePass=true`.

Configuration properties:

* `file` - path to a Java Property formatted file in the format defined under [realm.properties](#realm.properties)

## Multiple Authentication Modules

JAAS configurations can contain multiple LoginModule definitions, which are processed in order and according to the logic of the configuration Flag.

In your config file, separate the LoginModule definitions with a `;` and be sure to select the appropriate Flag for the module, one of `required`, `requisite`, `sufficient`, or `optional`.  

The full syntax and the description of how these Flags work is described in more detail under the [JAAS Configuration Documentation](http://docs.oracle.com/javase/6/docs/api/javax/security/auth/login/Configuration.html).

Here is an example combining an LDAP module flagged as `sufficient`, and a flat file realm.properties config flagged as `required`:

~~~~~~~~ {.c .numberLines}
multiauth {

  com.dtolabs.rundeck.jetty.jaas.JettyCachingLdapLoginModule sufficient
    debug="true"
    contextFactory="com.sun.jndi.ldap.LdapCtxFactory"
    providerUrl="ldap://server:389"
    bindDn="cn=Manager,dc=example,dc=com"
    bindPassword="secrent"
    authenticationMethod="simple"
    forceBindingLogin="false"
    userBaseDn="ou=People,dc=test1,dc=example,dc=com"
    userRdnAttribute="uid"
    userIdAttribute="uid"
    userPasswordAttribute="userPassword"
    userObjectClass="account"
    roleBaseDn="ou=Groups,dc=test1,dc=example,dc=com"
    roleNameAttribute="cn"
    roleUsernameMemberAttribute="memberUid"
    roleMemberAttribute="memberUid"
    roleObjectClass="posixGroup"
    cacheDurationMillis="300000"
    reportStatistics="true";

  org.eclipse.jetty.plus.jaas.spi.PropertyFileLoginModule required
    debug="true"
    file="/etc/rundeck/realm.properties";
};
~~~~~~~~~~~~
Based on the flags, JAAS would attempt the following for authentication:

1. Check username/pass against LDAP
  1. If auth succeeds, finish with successful authentication
  2. If auth fails, continue to the next module
2. Check username/pass against the properties file
  1. If auth succeeds, finish with successful authentication
  2. If auth fails, finish with failed authentication

# Container authentication and authorization

Container Authentication provides the Servlet context used by Rundeck
with a few mechanisms to determine what roles the user has.

`containerPrincipal`

:   JAAS authentication modules define a "Principal"
that represents the authenticated user,
and which can list the "roles" the user has.

`container`
:   The Container also provides a query mechanism `isUserInRole`.

Both of these methods are used by default,
although they can be disabled
with the following configuration flags in `rundeck-config.properties`:


    rundeck.security.authorization.containerPrincipal.enabled=false
    rundeck.security.authorization.container.enabled=false

## Preauthenticated Mode

`preauthenticated`

:   "Preauthenticated" means that the Servlet Container (e.g. Tomcat)
is not being used for authentication/authorization.
The user name and role list are provided to Rundeck
from another system, usually a reverse proxy set up "in front"
of the Rundeck web application, such as Apache HTTPD.
Rundeck accepts the "REMOTE_USER" as the username,
and allows a configurable Request Attribute to contain
the list of user roles.

**Note**: If you use this method, make sure that *only* your proxy
 has direct access to the ports Rundeck is listening on
 (e.g. firewall them),
 otherwise you are opening access to rundeck
 without requiring authentication.

This method can be enabled with this config in `rundeck-config.properties`:

    rundeck.security.authorization.preauthenticated.enabled=true
    rundeck.security.authorization.preauthenticated.attributeName=REMOTE_USER_GROUPS
    rundeck.security.authorization.preauthenticated.delimiter=:

This configuration requires some additional setup to enable:

1. The file `WEB-INF/web.xml` inside the war contents **must** be modified to remove the `<auth-constraint>` element.  This disables the behavior which causes the Container to trigger its authentication mechanism when a user browses to a Rundeck page requiring authorizaton.

2. Apache HTTPD and Tomcat must be configured to communicate so that a list of User Roles is sent to Tomcat as a request Attribute with the given "attributeName".

For Tomcat and Apache HTTPd with `mod_proxy_ajp`, here are some additional instructions:

1. Modify the tomcat server.xml, and make sure to set `tomcatAuthentication="false"` on the AJP connector:

        <Connector port="8009" protocol="AJP/1.3" redirectPort="4440" tomcatAuthentication="false"/>

2. Configure Apache to perform the necessary authentication, and to pass an environment variable named "REMOTE_USER_GROUPS", the value should be all colon-separated e.g.: "user:admin:ops" (or using the `delimiter` you have configured.)

Here is an example using just `mod_proxy_ajp`, and passing a static list of roles. A real solution should use [mod_lookup_identity](http://www.adelton.com/apache/mod_lookup_identity/):


    <Location /rundeck>
        ProxyPass  ajp://localhost:8009/rundeck

        AuthType basic
        AuthName "private area"
        AuthBasicProvider file

        AuthUserFile /etc/httpd/users.htpasswd
        SetEnv AJP_REMOTE_USER_GROUPS "admin:testrole1:testrole2"
        Require valid-user
    </Location>

**Note**: `mod_proxy_ajp` requires prefixing the environment variable with "AJP_", but `mod_jk` can pass the environment variable directly.

Once authenticated via Apache, you should be able to access rundeck.
You might see a page saying "You have no authorized access to projects",
and then "(User roles: role1, role2, ...)"
with a list of all of the user roles seen by Rundeck.
This page just means that there are no aclpolicy files
that match those roles,
but the apache->tomcat authorization is still working correctly.
At this point, move on to [Access Control Policy](access-control-policy.html)
to set up access control for the listed roles.

If the "User roles: " part is blank, then it may not be working correctly.
