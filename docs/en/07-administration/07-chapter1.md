% RUNDECK(1) RunDeck User Manuals | Version 1.0
% Alex Honor
% November 20, 2010

# Administration

## Startup and shutdown

### RPM

The RPM installation includes the placement of a boot control script
that will automatically start RunDeck when the system boots.

The `/etc/init.d/rundeckd` script provides a number of sub commands:

    /etc/init.d/rundeckd [start|stop|restart|condrestart|status]

*Startup*

    /etc/init.d/rundeckd start

*Shutdown*
    /etc/initd./rundeckd stop
    
### Launcher

    ...


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
* SSH should not prompt for a password. There are many resources
available on how to configure ssh to use public key authentication
instead of passwords such as:
[Password-less logins with OpenSSH](http://www.debian-administration.org/articles/152)
or
[How-To: Password-less SSH](http://www.cs.wustl.edu/~mdeters/how-to/ssh/)

### SSH key generation

* By default, the RunDeck framework is configured to use DSA _not_ RSA
  type keys (however, it can be configured to use RSA, if required).
  
Here's an example of SSH DSA key generation on a Linux system:

    $ ssh-keygen -t dsa
    Generating public/private dsa key pair.
    Enter file in which to save the key (/home/demo/.ssh/id_dsa): 
    Enter passphrase (empty for no passphrase): 
    Enter same passphrase again: 
    Your identification has been saved in /home/demo/.ssh/id_dsa.
    Your public key has been saved in /home/demo/.ssh/id_dsa.pub.
    The key fingerprint is:
    37:d6:3a:b6:17:db:e2:2f:84:ca:b2:ed:7a:43:0d:26 


### Configuring remote machine for SSH 

To be able to directly ssh to remote machines, the SSH public key of
the client should be shared to the remote machine.
  
Follow the steps given below to enable ssh to remote machines.

The ssh public key should be copied to the `authorized_keys` file of
the remote machine. The public key will be available in
`~/etc/id_dsa.pub` file.
  
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
Cygwin on Windows as it includes SSH and a number of
Unix-like tools that are useful when you work in a command line
environment.

## SSL

These instructions document how to convert an existing RunDeck
installation to support SSL.   Both server and client configuration
are documented since the client installations, including the instance
bundled with the server, require ssl modifications.

### SSL prerequisites

* A standard installation exists on a server that is ready to be
  configured for SSL. 
* Familiarity with [Jetty SSL Configuration]
* Identify Local SSL Configuration such as the following which will be
  used for this example: 

`SSL Port`
  ~ the desired SSL Server Port (e.g. _8443_)
`Server Alias`
  ~ the well known DNS Name of the RunDeck server  (e.g. _rundeck.org_)  
`Organizational Unit`
  ~ the name of the internal organization (e.g. _dev2ops_)
`Organization`
  ~ the name of the organization (e.g. _RunDeck Project_)
`Location`
  ~ the name of the location (e.g. _San Mateo_)
`State`
  ~ name of state (e.g. _California_)
`Country`
  ~ name of country (e.g. _US_)
`Key and Keystore Password`
  ~ password for both the private rsa key   and the java keystore  (e.g. _default_, also see Assumptions in next section)  

[Jetty SSL Configuration]:
http://docs.codehaus.org/display/JETTY/How+to+configure+SSL

* You are logged into the _rundeck_ account (or the user running the RunDeck
server) 
* JETTY_HOME, RDECK\_BASE, and JAVA\_HOME (as per standard install) are
defined

Example:

    $ whoami
    rundeck
    echo $JETTY_HOME
    /opt/rundeck/pkgs/jetty-6.1.21
    $ echo $RDECK_BASE
    /opt/rundeck/ctl
    $ echo $JAVA_HOME
    /usr/lib/jvm/java-1.6.0-openjdk-1.6.0.0.x86_64


The keystore will be exclusive to RunDeck and therefore both the
keystore password and the passphrase for the generated private key
will be the same, see  [Java KeyManagement] for more information.

[Java KeyManagement]: http://java.sun.com/j2se/1.5.0/docs/guide/security/CryptoSpec.html#KeyManagement

### Configuring the truststore 

The following is based on the example values shown in the
prerequisite section. Where applicable, unix style here docs are used
for cut and pastable convenience: 


(1)  Change working directory into the JETTY configuration directory

~~~~~~~~~~~~~~~~~~~~~~~~~~
    cd $JETTY_HOME/etc
~~~~~~~~~~~~~~~~~~~~~~~~~~

(2)  Generate the keystore file

~~~~~~~~~~~~~~~~~~~~~~~~~~    
$ keytool -keystore keystore -alias rundeck.org -genkey -keyalg RSA -keypass default -storepass default  <<!
rundeck.org
dev2ops
RunDeck Project
San Mateo
California
US
yes
!

What is your first and last name?
  [Unknown]:  What is the name of your organizational unit?
  [Unknown]:  What is the name of your organization?
  [Unknown]:  What is the name of your City or Locality?
  [Unknown]:  What is the name of your State or Province?
  [Unknown]:  What is the two-letter country code for this unit?
  [Unknown]:  Is CN=rundeck.org, OU=dev2ops, O="RunDeck Project", L=San Mateo, ST=California, C=US correct?
  [no]:  
~~~~~~~~~~~~~~~~~~~~~~~~~~    

(3) Generate the private rsa key, requires passphrase from terminal input

~~~~~~~~~~~~~~~~~~~~~~~~~~    
$ openssl genrsa -des3 -out rundeck.org.key
Generating RSA private key, 512 bit long modulus
.................................++++++++++++
...++++++++++++
e is 65537 (0x10001)
Enter pass phrase for rundeck.org.key:
Verifying - Enter pass phrase for rundeck.org.key:
[rundeck@centos54 etc]$ 
~~~~~~~~~~~~~~~~~~~~~~~~~~    

(4) Generate the self signed certificate, also requires same passphrase
from terminal input

~~~~~~~~~~~~~~~~~~~~~~~~~~    
$ openssl req -new -x509 -key rundeck.org.key -out rundeck.org.crt <<!
US
California
San Mateo
RunDeck Project
dev2ops
rundeck.org
root@rundeck.org
!

Enter pass phrase for rundeck.org.key:
You are about to be asked to enter information that will be incorporated
into your certificate request.
What you are about to enter is what is called a Distinguished Name or a DN.
There are quite a few fields but you can leave some blank
For some fields there will be a default value,
If you enter '.', the field will be left blank.
-----
Country Name (2 letter code) [GB]:State or Province Name (full name)
[Berkshire]:Locality Name (eg, city) [Newbury]:Organization Name (eg,
company) [My Company Ltd]:Organizational Unit Name (eg, section)
[]:Common Name (eg, your name or your server's hostname) []:Email
Address []
[rundeck@centos54 etc]$ 
~~~~~~~~~~~~~~~~~~~~~~~~~~    
      
(5) Convert the public/private crt/key pair into a pkcs12 (x509)
formatted file (<b>NOTE:</b>  the same passphrase from previous steps
is used for both opening the key and establishing the export password)

~~~~~~~~~~~~~~~~~~~~~~~~~~    
$ openssl pkcs12 -inkey rundeck.org.key -in rundeck.org.crt -export -out rundeck.org.pkcs12
Enter pass phrase for rundeck.org.key:
Enter Export Password:
Verifying - Enter Export Password:
[rundeck@centos54 etc]$ 
~~~~~~~~~~~~~~~~~~~~~~~~~~    
      
(6) Prior to importing the pkcs12 file, verify the following files exist:

~~~~~~~~~~~~~~~~~~~~~~~~~~      
$ ls -1 keystore rundeck.org.*
rundeck.org.crt
rundeck.org.key
rundeck.org.pkcs12
~~~~~~~~~~~~~~~~~~~~~~~~~~    


### Adjust Connector Configuration 

(1) Backup and then edit jetty.xml:

~~~~~~~~~~~~~~~~~~~~~~~~~~    
$ cp  jetty.xml jetty.xml.sav 
$ vi  jetty.xml 
~~~~~~~~~~~~~~~~~~~~~~~~~~    


(2) Modify non-secure connector on port 8080 to listen exclusively to
the loopback via `host` parameter:

~~~~~~~~~~~~~~~~~~~~~~~~~~    
<Call name="addConnector">
    <Arg>
        <New class="org.mortbay.jetty.nio.SelectChannelConnector">
<!-- ensure this connectors binds to the loopback only -->
          <Set name="host"><SystemProperty name="jetty.host" default="127.0.0.1"/></Set>
          <Set name="port"><SystemProperty name="jetty.port" default="8080"/></Set>
          <Set name="maxIdleTime">30000</Set>
          <Set name="Acceptors">2</Set>
          <Set name="statsOn">false</Set>
          <Set name="confidentialPort">8443</Set>
          <Set name="headerBufferSize">8192</Set>
          <Set name="lowResourcesConnections">5000</Set>
          <Set name="lowResourcesMaxIdleTime">5000</Set>
          <Call name="open"/>
        </New>
    </Arg>
</Call>
~~~~~~~~~~~~~~~~~~~~~~~~~~    

(3)  Add additional secure connector on port 8443 with keystore and
password parameters:

~~~~~~~~~~~~~~~~~~~~~~~~~~    
<Call name="addConnector">
     <Arg>
      <New class="org.mortbay.jetty.security.SslSocketConnector">
        <Set name="Port">8443</Set>
        <Set name="maxIdleTime">30000</Set>
        <Set name="keystore"><SystemProperty name="jetty.home" default="." />/etc/keystore</Set>
        <Set name="password">default</Set>
        <Set name="keyPassword">default</Set>
        <Set name="truststore"><SystemProperty name="jetty.home" default="." />/etc/keystore</Set>
        <Set name="trustPassword">default</Set>
      </New>
     </Arg>
</Call>
~~~~~~~~~~~~~~~~~~~~~~~~~~     

(4) Restart RunDeck

~~~~~~~~~~~~~~~~~~~~~~~~~~    
$ jetty.sh restart
~~~~~~~~~~~~~~~~~~~~~~~~~~    

(5)  Configure the server `framework properties` file for SSL:

~~~~~~~~~~~~~~~~~~~~~~~~~~    
$ cp framework.properties framework.properties.sav
$ vi framework.properties
$ diff framework.properties framework.properties.sav 
240,241c240,241
< framework.server.port = 8443
< framework.server.url = https://rundeck.local:8443/itnav
---
> framework.server.port = 8080
> framework.server.url = http://rundeck.local:8080/itnav
243c243
< framework.ctlcenter.url = https://rundeck.local:8443/ctlcenter
---
> framework.ctlcenter.url = http://rundeck.local:8080/ctlcenter
~~~~~~~~~~~~~~~~~~~~~~~~~~    


## Managing users

These instructions explain how to manage user credentials for 
RunDeck in the <code>realm.properties</code> file.


### The default user
The *default* user in particular is used by the various applications
to communicate with each other, and the values are hardcoded into the
applications at install time. '''As a result it is recommended that
you do not change the default user's name or password unless you are
re-installing the application and the clients.''' If you do change the
default credentials, they need to be updated in all of these
locations:

* On the server: `$JETTY\_HOME/etc/realm.properties` (as above) and
  $JETTY_HOME/webapps/itnav/WEB-​INF/classes/runtime.properties
  (properties dav.user and dav.password)
  
* On all clients: `$RDECK_BASE/etc/framework.properties` (properties
  framework.server.username, framework.server.password,
  framework.webdav.username, framework.webdav.password)
  
If you do not, you'll run into "HTTP Authorization failure" and "403
Forbidden" errors.


### realm.properties

The default RunDeck webapp handles user authentication via its
container, which in turn is configured to pull its user authentication
from the `$JETTY_HOME/etc/realm.properties` file. $JETTY_HOME is usually at
`$RDECK_BASE/pkgs/jetty-x.y.z`. This file is created at the
time that you install the server, and the out-of-the-box usernames and
passwords can be adjusted through the default.xml file or appropriate
command-line arguments.

Assuming you use the defaults, your realm.properties file will
probably look something like this:


    jetty: MD5:164c884306627e17250af12c89345d44,user
    admin: CRYPT:cxekz..ry.1Ns,server-administrator,content-administrator,admin
    other: OBF:1vmk1x261d9r1r1c1dmq
    plain: plain
    user: password

    # This entry is for digest auth.  
    # The credential is a MD5 hash of username:realmname:password
    digest: MD5:6e110442ad67abfbc485dc2cb784e217
    #
    # This sets the default user accounts for the RunDeck apps
    #
    default:default,user,admin,architect,deploy,build
    admin:admin,user,admin,architect,deploy,build
    deploy:deploy,user,deploy
    build:build,user,build



Adding additional users

You may wish to have additional users with various privileges rather
than giving out role accounts to groups.  You may also want to avoid
having the passwords in plaintext within the configuration file.  

To accomplish this, you'll need a properly hashed or encrypted
password to use in the config.  On the control tier server, move into
the directory that contains your jetty installation and pass the
username and password to the jetty Password utility.  In this example,
we'll setup a new user named "jsmith", with a password of "mypass":

    $ cd $JETTY_HOME
    $ java -cp lib/jetty-6.1.14.jar:lib/jetty-util-6.1.14.jar org.mortbay.jetty.security.Password jsmith mypass
    OBF:1xfd1zt11uha1ugg1zsp1xfp
    MD5:a029d0df84eb5549c641e04a9ef389e5
    CRYPT:jsnDAc2Xk4W4o


Then add this to the etc/realm.properties file with a line like so:

    jsmith: MD5:a029d0df84eb5549c641e04a9ef389e5,user,build,deploy


Then restart RunDeck to ensure jetty picks up the change and you're done.


### Active Directory
WARNING: UNTESTED

Setup the LDAP login module configuration file:

~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
[chuck@centos52 etc]$ cd $JETTY_HOME/etc
[chuck@centos52 etc]$ cat ldap-loginModule.conf 
ldaploginmodule {
    org.mortbay.jetty.plus.jaas.spi.LdapLoginModule required
    debug="true"
    contextFactory="com.sun.jndi.ldap.LdapCtxFactory"
    hostname="localhost"
    port="3890"
    bindDn="cn=Manager,dc=controltier,dc=com"
    bindPassword="secret"
    authenticationMethod="simple"
    forceBindingLogin="true"
    userBaseDn="ou=users,dc=controltier,dc=com"
    userRdnAttribute="cn"
    userIdAttribute="cn"
    userPasswordAttribute="unicodePwd"
    userObjectClass="user"
    roleBaseDn="ou=roles,dc=controltier,dc=com"
    roleNameAttribute="cn"
    roleMemberAttribute="member"
    roleObjectClass="group";
    };
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~


Update $JETTY_HOME/etc/jetty.xml replacing the default UserRealms array with:

~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    <!-- =========================================================== -->
    <!-- Configure Authentication Realms                             -->
    <!-- Realms may be configured for the entire server here, or     -->
    <!-- they can be configured for a specific web app in a context  -->
    <!-- configuration (see $(jetty.home)/contexts/test.xml for an   -->
    <!-- example).                                                   -->
    <!-- =========================================================== -->
    <Set name="UserRealms">
      <Array type="org.mortbay.jetty.security.UserRealm">
        <Item>
          <New class="org.mortbay.jetty.plus.jaas.JAASUserRealm">
            <Set name="name">jackrabbit</Set>
            <Set name="LoginModuleName">ldaploginmodule</Set>
          </New>
        </Item>
       <Item>
          <New class="org.mortbay.jetty.plus.jaas.JAASUserRealm">
            <Set name="name">jobcenterrealm</Set>
            <Set name="LoginModuleName">ldaploginmodule</Set>
          </New>
        </Item>
       <Item>
          <New class="org.mortbay.jetty.plus.jaas.JAASUserRealm">
            <Set name="name">Workbench</Set>
            <Set name="LoginModuleName">ldaploginmodule</Set>
          </New>
        </Item>
      </Array>
    </Set>
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

Update the `CONFIG_PROPS` in the RunDeck server's
`$RDECK_BASE/etc/profile.orig` to include setting the
`java.security.auth.login.config` property:


~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
$ diff $RDECK_BASE/etc/profile $RDECK_BASE/etc/profile.orig
14c14
> CONFIG_PROPS="-Drundeck.config.location=/home/chuck/rundeck/rundeck/rundeck-config.properties -Djava.security.auth.login.config=etc/ldap-loginModule.conf"
---
< CONFIG_PROPS="-Drundeck.config.location=/home/chuck/rundeck/rundeck/rundeck-config.properties"
29c29
&lt;                 
---
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

(Re)source your environment by logging out and logging in again or by
directly sourcing the `$RDECK_BASE/etc/profile` file:

    $ source $RDECK_BASE/etc/profile

RunDeck should now be configured for authentication and role
membership using the Active Directory server.


## Backup and recovery

RunDeck backup should only be done with the server down. 
Begin by shutting the server down.

Copy the data files 

* RPM install: `/var/lib/rundeck/data`
* Launcher install: `$RDECK_BASE/server/data`

## Configuration

rdeck.base

* linux: /etc/rundeck/client
* launcher: $RDECK\_BASE/etc

for server

* linux: /etc/rundeck/server (like jetty.home)
* laucher: $RDECK_BASE/server


## Logs

* linux: /var/log/rundeck
* launcher: $RDECK_BASE/var/logs

 
 
## Summary