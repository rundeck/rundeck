#########
# Rundeck Profile sourced from /etc/rc.d/init.d/rundeckd
#########
#
# NOTE: DO NOT MODIFY THIS FILE
# It will be replaced when the package is upgraded and your changes will not be saved.
#
# ##################
#
# To override variables in this file, you can instead create a file at:
#
#     # Centos/Redhat default:
#
#     /etc/sysconfig/rundeckd
#
# Or
#
#     # Ubuntu/Debian default:
#
#     /etc/default/rundeckd
#
# which contains exports for any of the variables listed below. E.g.:
#
#     RUNDECK_TEMPDIR=/path/to/tmpdir
#
# That file will be sourced before this one, allowing your exports to take precedence.
#
###############

prog="rundeckd"
[ -e /etc/sysconfig/$prog ] && . /etc/sysconfig/$prog
[ -e /etc/default/$prog ] && . /etc/default/$prog

RDECK_INSTALL="${RDECK_INSTALL:-/var/lib/rundeck}"
RDECK_BASE="${RDECK_BASE:-/var/lib/rundeck}"
RDECK_CONFIG="${RDECK_CONFIG:-/etc/rundeck}"
RDECK_CONFIG_FILE="${RDECK_CONFIG_FILE:-$RDECK_CONFIG/rundeck-config.properties}"
RDECK_SERVER_BASE="${RDECK_SERVER_BASE:-$RDECK_BASE}"
RDECK_SERVER_CONFIG="${RDECK_SERVER_CONFIG:-$RDECK_CONFIG}"
RDECK_SERVER_DATA="${RDECK_SERVER_DATA:-$RDECK_BASE/data}"
RDECK_PROJECTS="${RDECK_PROJECTS:-$RDECK_BASE/projects}"
RUNDECK_TEMPDIR="${RUNDECK_TEMPDIR:-/tmp/rundeck}"
RUNDECK_WORKDIR="${RUNDECK_TEMPDIR:-$RDECK_BASE/work}"
RUNDECK_LOGDIR="${RUNDECK_LOGDIR:-$RDECK_BASE/logs}"
RDECK_JVM_SETTINGS="${RDECK_JVM_SETTINGS:- -Xmx1024m -Xms256m -XX:MaxMetaspaceSize=256m -server}"
RDECK_TRUSTSTORE_FILE="${RDECK_TRUSTSTORE_FILE:-$RDECK_CONFIG/ssl/truststore}"
RDECK_TRUSTSTORE_TYPE="${RDECK_TRUSTSTORE_TYPE:-jks}"
JAAS_LOGIN="${JAAS_LOGIN:-true}"
JAAS_CONF="${JAAS_CONF:-$RDECK_CONFIG/jaas-loginmodule.conf}"
LOGIN_MODULE="${LOGIN_MODULE:-RDpropertyfilelogin}"
RDECK_HTTP_PORT=${RDECK_HTTP_PORT:-4440}
RDECK_HTTPS_PORT=${RDECK_HTTPS_PORT:-4443}


# If no JAVA_CMD, try to find it in $JAVA_HOME
if [ -z "$JAVA_CMD" ] && [ -n "$JAVA_HOME" ] && [ -x "$JAVA_HOME/bin/java" ] ; then
  JAVA_CMD=$JAVA_HOME/bin/java
  PATH=$PATH:$JAVA_HOME/bin
  export JAVA_HOME
elif [ -z "$JAVA_CMD" ] ; then
  JAVA_CMD=java
fi

# build classpath without lone : that includes .
for jar in $(find $RDECK_INSTALL/cli -name '*.jar') ; do
  CLI_CP=${CLI_CP:+$CLI_CP:}$jar
done
for war in $(find $RDECK_INSTALL/bootstrap -name '*.war') ; do
  EXECUTABLE_WAR=$war
done

RDECK_JVM="-Drundeck.jaaslogin=$JAAS_LOGIN \
           -Djava.security.auth.login.config=$JAAS_CONF \
           -Dloginmodule.name=$LOGIN_MODULE \
           -Drdeck.config=$RDECK_CONFIG \
           -Drundeck.server.configDir=$RDECK_SERVER_CONFIG \
           -Dserver.datastore.path=$RDECK_SERVER_DATA/rundeck \
           -Drundeck.server.serverDir=$RDECK_INSTALL \
           -Drdeck.projects=$RDECK_PROJECTS \
           -Drdeck.runlogs=$RUNDECK_LOGDIR \
           -Drundeck.config.location=$RDECK_CONFIG_FILE \
           -Djava.io.tmpdir=$RUNDECK_TEMPDIR \
           -Drundeck.server.workDir=$RUNDECK_WORKDIR \
           -Dserver.http.port=$RDECK_HTTP_PORT \
           -Drdeck.base=$RDECK_BASE"
#
# Set min/max heap size
#
RDECK_JVM="$RDECK_JVM $RDECK_JVM_SETTINGS"
#
# SSL Configuration - Uncomment the following to enable.  Check SSL.properties for details.
#
if [ -n "$RUNDECK_WITH_SSL" ] ; then
  RDECK_SSL_OPTS="${RDECK_SSL_OPTS:- -Djavax.net.ssl.trustStore=$RDECK_TRUSTSTORE_FILE -Djavax.net.ssl.trustStoreType=$RDECK_TRUSTSTORE_TYPE -Djava.protocol.handler.pkgs=com.sun.net.ssl.internal.www.protocol}"
  RDECK_JVM="$RDECK_JVM -Drundeck.ssl.config=$RDECK_SERVER_CONFIG/ssl/ssl.properties -Dserver.https.port=${RDECK_HTTPS_PORT} ${RDECK_SSL_OPTS}"
fi

unset JRE_HOME

umask 002

rundeckd="$JAVA_CMD $RDECK_JVM $RDECK_JVM_OPTS -jar $EXECUTABLE_WAR --skipinstall"
