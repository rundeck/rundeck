
set RDECK_BASE=/tmp/waka

set JAVA_HOME=/home/greg/.sdkman/candidates/java/8.0.172-zulu/jre

:: Unsetting JRE_HOME to ensure there is no conflict with JAVA_HOME
(set JRE_HOME=)

set Path=%JAVA_HOME%\bin;%RDECK_BASE%\tools\bin;%Path%

set RDECK_SSL_OPTS="-Djavax.net.ssl.trustStore=%RDECK_BASE%\etc\truststore -Djavax.net.ssl.trustStoreType=jks -Djava.protocol.handler.pkgs=com.sun.net.ssl.internal.www.protocol"
set RDECK_CLI_OPTS=-Xms64m -Xmx128m
set RD_LIBDIR=%RDECK_BASE%\tools\lib
