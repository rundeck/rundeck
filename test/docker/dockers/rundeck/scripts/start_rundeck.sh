#!/bin/bash

#exit on error
set -e

#Fix folder permissions
sudo chown -R $USERNAME:$USERNAME $HOME;

test ! -f $HOME/resources/$RUNDECK_NODE.ready || rm $HOME/resources/$RUNDECK_NODE.ready

#TODO SACAR
echo "######### start_rundeck on $RUNDECK_NODE ######### "
if test -f $HOME/resources/$RUNDECK_NODE.ready ; then
  echo "Already started, skipping..."
  exit 0
fi

# Some Cleanup
rm -rfv $HOME/server/logs/*
rm -fv $HOME/testdata/*

# Configure general stuff.
# configure hostname, nodename, url


# RUN TEST PRESTART SCRIPT
if [[ ! -z "$CONFIG_SCRIPT_PRESTART" && -f $CONFIG_SCRIPT_PRESTART ]];
then
  . $CONFIG_SCRIPT_PRESTART
else
  echo "### Prestart config not set. skipping..."
fi

export RDECK_BASE=$HOME
LOGFILE=$RDECK_BASE/var/log/service.log
mkdir -p $(dirname $LOGFILE)
FWKPROPS=$HOME/etc/framework.properties
mkdir -p $(dirname $FWKPROPS)

export RUNDECK_PORT=${RUNDECK_PORT:-4440}
export RUNDECK_URL=${RUNDECK_URL:-http://$RUNDECK_NODE:$RUNDECK_PORT}


# if [ -n "$SETUP_SSL" ] ; then
#   export RUNDECK_PORT=4443
#   export RUNDECK_URL=https://$RUNDECK_NODE:$RUNDECK_PORT
# fi

cat > $FWKPROPS <<END
framework.server.name = $RUNDECK_NODE
framework.server.hostname = $RUNDECK_NODE
framework.server.port = $RUNDECK_PORT
framework.server.url = $RUNDECK_URL

# ----------------------------------------------------------------
# Installation locations
# ----------------------------------------------------------------

rdeck.base=$RDECK_BASE

framework.projects.dir=$RDECK_BASE/projects
framework.etc.dir=$RDECK_BASE/etc
framework.var.dir=$RDECK_BASE/var
framework.tmp.dir=$RDECK_BASE/var/tmp
framework.logs.dir=$RDECK_BASE/var/logs
framework.libext.dir=$RDECK_BASE/libext

# ----------------------------------------------------------------
# SSH defaults for node executor and file copier
# ----------------------------------------------------------------

framework.ssh.keypath = $RDECK_BASE/.ssh/id_rsa
framework.ssh.user = $USERNAME

# ssh connection timeout after a specified number of milliseconds.
# "0" value means wait forever.
framework.ssh.timeout = 0
rundeck.tokens.file=$HOME/etc/tokens.properties

# force UTF-8
#framework.remote.charset.default=UTF-8

rundeck.enable.ref.stats=true
END

cat > $HOME/etc/profile <<END
RDECK_BASE=$RDECK_BASE
export RDECK_BASE

JAVA_HOME=${JAVA_HOME:-/usr/lib/jvm/java-8-openjdk-amd64}
export JAVA_HOME

PATH=\$JAVA_HOME/bin:\$RDECK_BASE/tools/bin:\$PATH
export PATH

# force UTF-8 default encoding
export RDECK_JVM="-Dfile.encoding=UTF-8 -Drundeck.bootstrap.build.info=true $RDECK_JVM_OPTS"
END

API_KEY=${API_KEY:-letmein99}
cat > $HOME/etc/tokens.properties <<END
admin: $API_KEY
END

cat > $HOME/etc/admin.aclpolicy <<END
description: Admin, all access.
context:
  project: '.*' # all projects
for:
  resource:
    - allow: '*' # allow read/create all kinds
  adhoc:
    - allow: '*' # allow read/running/killing adhoc jobs
  job:
    - allow: '*' # allow read/write/delete/run/kill of all jobs
  node:
    - allow: '*' # allow read/run for all nodes
by:
  group: admin

---

description: Admin, all access.
context:
  application: 'rundeck'
for:
  resource:
    - allow: '*' # allow create of projects
  project:
    - allow: '*' # allow view/admin of all projects
  project_acl:
    - allow: '*' # allow admin of all project-level ACL policies
  storage:
    - allow: '*' # allow read/create/update/delete for all /keys/* storage content
by:
  group: admin
END

# open permissions via api
cp $HOME/etc/admin.aclpolicy $HOME/etc/apitoken.aclpolicy
sed -i -e "s:admin:api_token_group:" $HOME/etc/apitoken.aclpolicy


#sudo chown -R $USERNAME:$USERNAME $HOME;

mkdir -p $HOME/projects/testproj1/etc

cat > $HOME/projects/testproj1/etc/project.properties <<END
project.name=testproj1
project.nodeCache.delay=30
project.nodeCache.enabled=true
project.ssh-authentication=privateKey
project.ssh-keypath=/home/rundeck/.ssh/id_rsa
resources.source.1.config.file=$HOME/projects/testproj1/etc/resources.xml
resources.source.1.config.format=resourcexml
resources.source.1.config.generateFileAutomatically=true
resources.source.1.config.includeServerNode=true
resources.source.1.config.requireFileExists=false
resources.source.1.type=file
resources.source.2.config.directory=/home/rundeck/resources
resources.source.2.type=directory
service.FileCopier.default.provider=jsch-scp
service.NodeExecutor.default.provider=jsch-ssh
END


setup_project(){
  local FARGS=("$@")
  local DIR=${FARGS[0]}
  local PROJ=${FARGS[1]}
  echo "setup test project: $PROJ in dir $DIR"
  mkdir -p $DIR/projects/$PROJ/etc
  cat >$DIR/projects/$PROJ/etc/project.properties<<END
project.name=$PROJ
project.nodeCache.delay=30
project.nodeCache.enabled=true
project.ssh-authentication=privateKey
#project.ssh-keypath=
resources.source.1.config.file=$DIR/projects/\${project.name}/etc/resources.xml
resources.source.1.config.format=resourcexml
resources.source.1.config.generateFileAutomatically=true
resources.source.1.config.includeServerNode=true
resources.source.1.config.requireFileExists=false
resources.source.1.type=file
service.FileCopier.default.provider=jsch-scp
service.NodeExecutor.default.provider=jsch-ssh
END
}

append_project_config(){
  local FARGS=("$@")
  local DIR=${FARGS[0]}
  local PROJ=${FARGS[1]}
  local FILE=${FARGS[2]}
  echo "Append config for test project: $PROJ in dir $DIR"
  
  cat >>$DIR/projects/$PROJ/etc/project.properties< $FILE
}

setup_ssl(){
  local FARGS=("$@")
  local DIR=${FARGS[0]}
  TRUSTSTORE=$DIR/etc/truststore 
  KEYSTORE=$DIR/etc/keystore 
  if [ ! -f $TRUSTSTORE ]; then
     echo "=>Generating ssl cert"
     sudo -u rundeck keytool -keystore $KEYSTORE -alias $RUNDECK_NODE -genkey -keyalg RSA \
      -keypass adminadmin -storepass adminadmin -dname "cn=$RUNDECK_NODE, o=test, o=rundeck, o=org, c=US" && \
     cp $KEYSTORE $TRUSTSTORE
  fi

cat >> $HOME/etc/profile <<END
export RDECK_JVM="$RDECK_JVM -Drundeck.ssl.config=$DIR/server/config/ssl.properties -Dserver.https.port=$RUNDECK_PORT"
END
}

if [ -n "$SETUP_TEST_PROJECT" ] ; then
    setup_project $RDECK_BASE $SETUP_TEST_PROJECT
    if [ -n "$CONFIG_TEST_PROJECT_FILE" ] ; then
      append_project_config $RDECK_BASE $SETUP_TEST_PROJECT $CONFIG_TEST_PROJECT_FILE
    fi
fi

if [ -n "$SETUP_SSL" ] ; then
    setup_ssl $RDECK_BASE
fi

cat > $HOME/server/config/rundeck-config.properties <<END
loglevel.default=INFO
rdeck.base=/home/rundeck

#rss.enabled if set to true enables RSS feeds that are public (non-authenticated)
rss.enabled=false
server.address=0.0.0.0
grails.serverURL=${RUNDECK_URL}
dataSource.dbCreate = none
dataSource.url = jdbc:h2:file:/home/rundeck/server/data/grailsdb;DB_CLOSE_ON_EXIT=FALSE
grails.plugin.databasemigration.updateOnStart=true

#dataSource.properties.removeAbandoned=true
#dataSource.properties.removeAbandonedTimeout=10

# Pre Auth mode settings
rundeck.security.authorization.preauthenticated.enabled=false
rundeck.security.authorization.preauthenticated.attributeName=REMOTE_USER_GROUPS
rundeck.security.authorization.preauthenticated.delimiter=,
# Header from which to obtain user name
rundeck.security.authorization.preauthenticated.userNameHeader=X-Forwarded-Uuid
# Header from which to obtain list of roles
rundeck.security.authorization.preauthenticated.userRolesHeader=X-Forwarded-Roles
# Redirect to upstream logout url
rundeck.security.authorization.preauthenticated.redirectLogout=false
rundeck.security.authorization.preauthenticated.redirectUrl=/oauth2/sign_in

rundeck.log4j.config.file=/home/rundeck/server/config/log4j.properties
END

if [ -n "$NODE_CACHE_FIRST_LOAD_SYNCH" ] ; then
  cat - >>$RDECK_BASE/server/config/rundeck-config.properties <<END
rundeck.nodeService.nodeCache.firstLoadAsynch=false
END
fi

if [ -n "$PLUGIN_BLOCKLIST_FILE" ] ; then
    cat - >>$RDECK_BASE/server/config/rundeck-config.properties <<END
  rundeck.plugins.providerBlockListFile=$PLUGIN_BLOCKLIST_FILE
END
fi

### PRE CONFIG
# RUN TEST PRESTART SCRIPT
if [[ ! -z "$CONFIG_SCRIPT_PRESTART" && -f $CONFIG_SCRIPT_PRESTART ]];
then
  . $CONFIG_SCRIPT_PRESTART
else
  echo "### Pre start config not set. skipping..."
fi

# (start rundeck)
#sudo su - rundeck bash -c "RDECK_BASE=$RDECK_BASE $HOME/server/sbin/rundeckd start"
$HOME/server/sbin/rundeckd start

echo "started rundeck"

# Wait for server to start
SUCCESS_MSG="Grails application running"
MAX_ATTEMPTS=30
SLEEP=10
echo "Waiting for $RUNDECK_NODE to start. This will take about 2 minutes... "
declare -i count=0
while (( count <= MAX_ATTEMPTS ))
do
    if ! [ -f "$LOGFILE" ] 
    then  echo "Waiting. hang on..."; # output a progress character.
    elif ! grep "${SUCCESS_MSG}" "$LOGFILE" ; then
      echo "Still working. hang on..."; # output a progress character.
    else  break; # found successful startup message.
    fi
    if [ -n "$STARTUP_FAILURE_MSG" ] ; then
      if grep "${STARTUP_FAILURE_MSG}" "$LOGFILE" ; then
        >&2 grep "${STARTUP_FAILURE_MSG}" "$LOGFILE"
        echo >&2 "FAIL: found startup failure message: ${STARTUP_FAILURE_MSG}"
        exit 1
      fi
    fi
    (( count += 1 ))  ; # increment attempts counter.
    (( count == MAX_ATTEMPTS )) && {
        echo >&2 "FAIL: Reached max attempts to find success message in logfile. Exiting."
        exit 1
    }
    tail -n 5 $LOGFILE
    $HOME/server/sbin/rundeckd status || {
        echo >&2 "FAIL: rundeckd is not running. Exiting."
        exit 1
    }
    sleep $SLEEP; # wait before trying again.

done
echo "RUNDECK NODE $RUNDECK_NODE started successfully!!"


### POST CONFIG
# RUN TEST POSTSTART SCRIPT
if [[ ! -z "$CONFIG_SCRIPT_POSTSTART" && -f $CONFIG_SCRIPT_POSTSTART ]];
then
  . $CONFIG_SCRIPT_POSTSTART
else
  echo "### Post start config not set. skipping..."
fi

### Signal READY
# here we should leave some file in a shared folder to signal that the server is ready. so tests can begin.
touch $HOME/resources/$RUNDECK_NODE.ready
