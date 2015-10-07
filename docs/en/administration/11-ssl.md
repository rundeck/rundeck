% Configuring SSL

This document describes how to configure Rundeck for SSL/HTTPS
support, and assumes you are using the rundeck-launcher standalone
launcher.  If you are using RPM/deb install, refer to the appropriate configuration file paths from [Configuration -> Configuration Layout](configuration-file-reference.html#configuration-layout).

(1) Before beginning, do a first-run of the launcher, as it will create
the base directory for Rundeck and generate configuration files.

        cd $RDECK_BASE;  java -jar rundeck-launcher-1.1.0.jar

    This will start the server and generate necessary config files.  Press
    control-c to shut down the server.

(2)  Using the [keytool] command, generate a keystore for use as the
server cert and client truststore. Specify passwords for key and keystore:

[keytool]: http://linux.die.net/man/1/keytool-java-1.6.0-openjdk

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
        
(6) Configure server URL so that Rundeck knows its external address.  Modify the file
`$RDECK_BASE/server/config/rundeck-config.properties` and change the `grails.serverURL`: 

    * `grails.serverURL=https://myhostname:4443`
    
    Set the URL to include the appropriate https protocol, and change the port to
    4443, or to the value of your `-Dserver.https.port` runtime
    configuration property.

(7) For Debian/RPM installation, uncomment line 43 in `/etc/rundeck/profile`:

        export RDECK_JVM="$RDECK_JVM -Drundeck.ssl.config=/etc/rundeck/ssl/ssl.properties -Dserver.https.port=${RDECK_HTTPS_PORT}"

(8) Start the server.  For the rundeck launcher, tell it where to read the ssl.properties

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

    $ java -cp server/lib/jetty-all-7.6.0.v20120127.jar org.eclipse.jetty.util.security.Password [username] [password]

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

## Using an SSL Terminated Proxy

You can tell Jetty to honor
`X-Forwarded-Proto`,  `X-Forwarded-Host`,
`X-Forwarded-Server` and `X-Forwarded-For` headers
by adding the following JVM property:

* `rundeck.jetty.connector.forwarded` set to "true" to enable proxy forwarded support.

E.g. modify the `RDECK_JVM` variable
in the file `/etc/rundeck/profile`
and add:

    -Drundeck.jetty.connector.forwarded=true

This will enable Jetty to respond correctly
when a forwarded request is first received.

**Note:** You will still need to modify the `grails.serverURL` value
in [rundeck-config.properties][]
to let Rundeck know how to properly generate absolute URLs.

## Disabling SSL Protocols

You can disable SSL protocols or cipher suites using these
JVM variables:

* `rundeck.jetty.connector.ssl.includedProtocols` set to a comma-separated list of SSL protocols to enable. Default will be based on the available protocols.
* `rundeck.jetty.connector.ssl.excludedProtocols` set to a comma-separated list of SSL protocols to disable. Default value: 'SSLv3'
* `rundeck.jetty.connector.ssl.includedCipherSuites` set to a comma-separated list of Cipher suites to enable. Default will be based on the available cipher suites.
* `rundeck.jetty.connector.ssl.excludedCipherSuites` set to a comma-separated list of Cipher suites to disable. No default value.

The `included` settings determine what protocols or cipher suites are enabled, and the `excluded` settings then remove values from that list.

E.g. modify the `RDECK_JVM` variable
in the file `/etc/rundeck/profile`
and add:

    -Drundeck.jetty.connector.ssl.excludedProtocols=SSLv3,SSLv2Hello

When starting up the Jetty container will log a list of the disabled protocols:

    2014-10-27 11:08:41.225:INFO:oejus.SslContextFactory:Enabled Protocols [SSLv2Hello, TLSv1] of [SSLv2Hello, SSLv3, TLSv1]

To see the list of enabled Cipher Suites, turn on DEBUG level logging for Jetty SSL utils: `-Dorg.eclipse.jetty.util.ssl.LEVEL=DEBUG`.


[rundeck-config.properties]: configuration-file-reference.html#rundeck-config.properties
