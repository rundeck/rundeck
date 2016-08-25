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
cat > $FWKPROPS <<END
framework.server.name = $RUNDECK_NODE
framework.server.hostname = $RUNDECK_NODE
framework.server.port = 4440
framework.server.url = http://$RUNDECK_NODE:4440
# Username/password used by CLI tools.
framework.server.username = admin
framework.server.password = admin

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
END

cat > $HOME/etc/profile <<END
RDECK_BASE=$RDECK_BASE
export RDECK_BASE

JAVA_HOME=/usr/lib/jvm/java-8-oracle
export JAVA_HOME

PATH=\$JAVA_HOME/bin:\$RDECK_BASE/tools/bin:\$PATH
export PATH


export LIBDIR=\$RDECK_BASE/tools/lib

CLI_CP=
for i in \`ls \$LIBDIR/*.jar\`
do
 CLI_CP=\${CLI_CP}:\${i}
done
export CLI_CP

# force UTF-8 default encoding
export RDECK_JVM="-Dfile.encoding=UTF-8"
END

# prevent CLI tool warning
cat > $HOME/etc/cli-log4j.properties <<END
log4j.rootCategory=ERROR, stdout

#
# stdout - ConsoleAppender
#
log4j.appender.stdout=org.apache.log4j.ConsoleAppender
log4j.appender.stdout.layout=org.apache.log4j.PatternLayout
log4j.appender.stdout.layout.ConversionPattern=%-5p %c{1}: %m%n
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

# (start rundeck)
#sudo su - rundeck bash -c "RDECK_BASE=$RDECK_BASE $HOME/server/sbin/rundeckd start"
$HOME/server/sbin/rundeckd start

echo "started rundeck"

# Wait for server to start
SUCCESS_MSG="Started SelectChannelConnector@0.0.0.0:"
MAX_ATTEMPTS=30
SLEEP=10
echo "Waiting for $RUNDECK_NODE to start. This will take about 2 minutes... "
declare -i count=0
while (( count <= MAX_ATTEMPTS ))
do
    if ! grep "${SUCCESS_MSG}" "$LOGFILE"
    then  echo "Still working. hang on..."; # output a progress character.
    else  break; # found successful startup message.
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
