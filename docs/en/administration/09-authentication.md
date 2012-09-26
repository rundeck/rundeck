% Authentication

**Note:** The underlying security mechanism relies on JAAS, so you are free to use what ever JAAS provider you feel is suitable for your environment. See [JAAS](http://en.wikipedia.org/wiki/Java_Authentication_and_Authorization_Service) and specifically for Jetty, [JAAS for Jetty](http://docs.codehaus.org/display/JETTY/JAAS).

Rundeck has two basic ways of defining authentication.

1. A text file containing usernames, passwords and role definitions. Usually called [realm.properties](#realm.properties).
2. [LDAP](#ldap)

By default a new installation uses the realm.properties method.

### realm.properties

These instructions explain how to manage user credentials for 
Rundeck in the <code>realm.properties</code> file.

The default Rundeck webapp handles user authentication via its
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
    # This sets the default user accounts for the Rundeck apps
    #
    admin:admin,user,admin
    user:user,user

*Adding additional users*

You may wish to have additional users with various privileges rather
than giving out role accounts to groups.  You may also want to avoid
having the passwords in plaintext within the configuration file.  

To accomplish this, you'll need a properly hashed or encrypted
password to use in the config.  On the Rundeck server, move into
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

Then restart Rundeck to ensure it picks up the change and you're done.

### LDAP

LDAP and Active Directory configurations are created in the same way, but your LDAP structure may be different than Active Directory's structure.

Rundeck includes a JAAS login module you can use for LDAP directory authentication, called `JettyCachingLdapLoginModule`.  This is an enhanced version of the default Jetty JAAS Ldap login module that caches authorization results for a period of time.

You must change some configuration values to change the authentication module to use.

#### Configuration

Configuring LDAP consists of defining a JAAS config file (e.g. "jaas-ldap.conf"), and changing the server startup script to use this file and use the correct Login Module configuration inside it.

(1)  Setup the LDAP login module configuration file (see the [Login module configuration](#login-module-configuration) section).

    Create a `jaas-ldap.conf` file in the same directory as the `jaas-loginmodule.conf` file.
    
    * RPM install: /etc/rundeck/
    * Launcher install: $RDECK_BASE/server/config
    
    Make sure the name of your Login Module configuration is the same as you use in the next step.  The Login Module configuration is defined like this:
    
        myloginmodule {
            ...
        }
    
    Where "myloginmodule" is the name.

(2) To override the default JAAS configuration file, you will need to supply the Rundeck server with the proper path to the new one, and a `loginmodule.name` Java system property to identify the new login module by name.

    The JAAS configuration file location is specified differently between the Launcher and the RPM.

    **For the Launcher**:  the `loginmodule.conf.name` Java system property is used to identify the *name* of the config file, which must be located in the `$RDECK_BASE/server/config` dir.

    You can simply specify the system properties on the java commandline:

        java -Dloginmodule.conf.name=jaas-ldap.conf \
            -Dloginmodule.name=ldap \
            -jar rundeck-launcher-x.x.jar

    Otherwise, if you are starting the Launcher via the supplied `rundeckd` script, you can modify the `RDECK_JVM` value in the `$RDECK_BASE/etc/profile` file to add two JVM arguments:

        export RDECK_JVM="-Dloginmodule.conf.name=jaas-ldap.conf \
            -Dloginmodule.name=ldap"

    Note: more information about using the launcher and useful properties are under [Getting Started - Launcher Options](installation.html#launcher-options).

    **For the RPM installation**: the absolute path to the JAAS config file must be specified with the `java.security.auth.login.config` property.

    Update the `RDECK_JVM` in `/etc/rundeck/profile` by changing the following two JVM arguments:

        export RDECK_JVM="-Djava.security.auth.login.config=/etc/rundeck/jaas-loginmodule.conf \
               -Dloginmodule.name=RDpropertyfilelogin \

    to

        export RDECK_JVM="-Djava.security.auth.login.config=/etc/rundeck/jaas-ldap.conf \
               -Dloginmodule.name=ldap \


(3) Restart rundeckd

    `sudo /etc/init.d/rundeckd restart`

(4) Attempt to logon

    If everything was configured correctly, you will be able to access Rundeck using your AD credentials.  If something did not go smoothly, look at `/var/log/rundeck/service.log` for stack traces that may indicate what is wrong.    

#### Login module configuration

Here is an example configuration file:

~~~~
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
      reportStatistics="true";
};
~~~~

The `JettyCachingLdapLoginModule` has these configuration properties:

debug
:    "true/false" - turn on or off debug output

contextFactory
:    The LDAP context factory class, e.g. "com.sun.jndi.ldap.LdapCtxFactory"

providerUrl
:    ldap URL for the server, e.g. "ldap://server:389"

bindDn
:    Optional. If not using "binding" authentication, set this to the root DN that should bind, e.g. "cn=Manager,dc=example,dc=com"

bindPassword
:    password for root DN. **Note**: The `bindDn` and `bindPassword` must escape any special characters with `\` character. Special characters include `\` (backslash), as well as `!` (exclamation).

authenticationMethod
:    Authentication method, e.g. "simple"

forceBindingLogin
:    "true/false" - if true, bind as the user that is authenticating, otherwise bind as the manager and perform a search to verify user password

forceBindingLoginUseRootContextForRoles
:    "true/false" - if true and forceBindingLogin is true, then role membership searches will be performed in the root context, rather than in the bound user context.

userBaseDn
:    base DN to search for users, example: "ou=People,dc=test1,dc=example,dc=com"

userRdnAttribute
:    Attribute name for username, used when searching for user role membership by DN, default "uid".

userIdAttribute
:    Attribute name to identify user by username, default "cn".

userPasswordAttribute
:    Attribute name for user password, default "userPassword".

userObjectClass
:    Attribute name for user object class, default "inetOrgPerson".

roleBaseDn
:    Base DN for role membership search, e.g. "ou=Groups,dc=test1,dc=example,dc=com".

roleNameAttribute
:    Attribute name for role name, default "roleName".

roleMemberAttribute
:    Attribute name for a role that would contain a user's DN, default "uniqueMember".

roleUsernameMemberAttribute
:    Attribute name for a role that would contain a user's username. If set, this overrides the `roleMemberAttribute` behavior.

roleObjectClass
:    Object class for role, default "groupOfUniqueNames".

rolePrefix
:    Prefix string to remove from role names before returning to the application, e.g. "rundeck_".

cacheDurationMillis
:   Duration that authorization should be cached, in milliseconds. Default "0". A value of "0" indicates no caching should be used.

reportStatistics
:    "true/false" - if true, output cache statistics to the log.

#### Active Directory

Here is an example configuration for Active Directory

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

### Multiple Authentication Modules

JAAS configurations can contain multiple LoginModule definitions, which are processed in order and according to the logic of the configuration Flag.

In your config file, separate the LoginModule definitions with a `;` and be sure to select the appropriate Flag for the module, one of `required`, `requisite`, `sufficient`, or `optional`.  

The full syntax and the description of how these Flags work is described in more detail under the [JAAS Configuration Documentation](http://docs.oracle.com/javase/6/docs/api/javax/security/auth/login/Configuration.html).

Here is an example combining an LDAP module flagged as `sufficient`, and a flat file realm.properties config flagged as `required`:

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

      org.mortbay.jetty.plus.jaas.spi.PropertyFileLoginModule required
        debug="true"
        file="/etc/rundeck/realm.properties";
    };

Based on the flags, JAAS would attempt the following for authentication:

1. Check username/pass against LDAP
  1. If auth succeeds, finish with successful authentication
  2. If auth fails, continue to the next module
2. Check username/pass against the properties file
  1. If auth succeeds, finish with successful authentication
  2. If auth fails, finish with failed authentication