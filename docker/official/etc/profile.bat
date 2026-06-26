
set RDECK_BASE=/home/rundeck

REM Grails 7: Java 17 required
set JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64

:: Unsetting JRE_HOME to ensure there is no conflict with JAVA_HOME
(set JRE_HOME=)

set Path=%JAVA_HOME%\bin;%Path%

set RDECK_SSL_OPTS="-Djavax.net.ssl.trustStore=%RDECK_BASE%\etc\truststore -Djavax.net.ssl.trustStoreType=jks -Djava.protocol.handler.pkgs=com.sun.net.ssl.internal.www.protocol"
set RDECK_CLI_OPTS=-Xms64m -Xmx128m
