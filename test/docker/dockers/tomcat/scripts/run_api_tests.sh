#!/bin/bash

set -e

TEST_DIR=$1
TEST_NAME=$2

echo "Running api tests from directory: $TEST_DIR"

if [ -n "$TEST_NAME" ] ; then
    echo "Running single test: $TEST_NAME"
fi

export API_KEY=letmein99
# define env vars used by rd tool
export RD_TOKEN=$API_KEY
export RD_URL="http://127.0.0.1:8080/rundeck"
export RD_COLOR=0
export RD_OPTS="-Dfile.encoding=utf-8"

echo "API_KEY=$API_KEY"

wait_for(){
    # Wait for resource file to be created
    MAX_ATTEMPTS=30
    SLEEP=10
    echo "Waiting for $REMOTE_NODE to start... "
    declare -i count=0
    while (( count <= MAX_ATTEMPTS ))
    do
        RESP=$(curl -o /dev/null -s -w "%{http_code}\n" ${RD_URL}/user/login || true)
        if [[ $RESP = "200" ]]; then break; fi

        (( count += 1 ))  ; # increment attempts counter.
        (( count == MAX_ATTEMPTS )) && {
            echo >&2 "FAIL: Reached max attempts to start. Exiting."
            return 1
        }
        
        sleep $SLEEP; # wait before trying again.
    done
}

setup_project_api(){
  local FARGS=("$@")
  local DIR=${FARGS[0]}
  local PROJ=${FARGS[1]}
  local TOK=${FARGS[2]}
  local XFILE=${FARGS[3]}
  echo "setup test project: $PROJ in dir $DIR"

  local PDIR=$DIR/projects/$PROJ/etc

  mkdir -p "$PDIR"
  local PFILE=$DIR/projects/$PROJ/etc/project-import.properties
  cat >"$PFILE" <<END
project.name=$PROJ
project.nodeCache.delay=30
project.nodeCache.enabled=true
project.ssh-authentication=privateKey
resources.source.1.config.file=$DIR/projects/\${project.name}/etc/resources.xml
resources.source.1.config.format=resourcexml
resources.source.1.config.generateFileAutomatically=true
resources.source.1.config.includeServerNode=true
resources.source.1.config.requireFileExists=false
resources.source.1.type=file
service.FileCopier.default.provider=jsch-scp
service.NodeExecutor.default.provider=jsch-ssh
END

  if [ -n "$XFILE" ] ; then
    cat "$XFILE" >> "$PFILE"
  fi

  RD_OPTS="-Djavax.net.ssl.trustStore=$DIR/etc/truststore" \
  RD_URL=$RUNDECK_URL RD_TOKEN=$TOK rd projects create -p "$PROJ" -f "$PFILE" -F properties

}

wait_for

export PATH=$PATH:$HOME/tools/bin
export RDECK_BASE=/usr/local/tomcat/webapps/rundeck/rundeck

# create 'test' project
setup_project_api $RDECK_BASE test $API_KEY

#sudo chown -R $USERNAME:$USERNAME $TEST_DIR

#source $RDECK_BASE/etc/profile

echo "starting test.sh"

set +e
chmod -w $TEST_DIR/src/test.sh

export TEST_NAME=$TEST_NAME
bash -c $TEST_DIR/src/test.sh $RD_URL admin admin

EC=$?

echo "test.sh finished with $EC"


exit $EC