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
    local FILE=$1
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
            echo >&2 "FAIL: Reached max attempts to find see $FILE. Exiting."
            return 1
        }
        
        sleep $SLEEP; # wait before trying again.
    done
}

wait_for

#sudo chown -R $USERNAME:$USERNAME $TEST_DIR

export PATH=$PATH:$HOME/tools/bin
export RDECK_BASE=/usr/local/tomcat/webapps/rundeck/rundeck
#source $RDECK_BASE/etc/profile

echo "starting test.sh"

set +e
chmod -w $TEST_DIR/src/test.sh

export TEST_NAME=$TEST_NAME
bash -c $TEST_DIR/src/test.sh $RD_URL admin admin

EC=$?

echo "test.sh finished with $EC"


exit $EC