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
export RD_URL="http://$RUNDECK_NODE:4440"
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
        if ! test -f $FILE
        then  echo "."; # output a progress character.
        else  break; # found successful startup message.
        fi
        (( count += 1 ))  ; # increment attempts counter.
        (( count == MAX_ATTEMPTS )) && {
            echo >&2 "FAIL: Reached max attempts to find see $FILE. Exiting."
            return 1
        }
        
        sleep $SLEEP; # wait before trying again.
    done
}

for node in $RUNDECK_NODE  ; do
    echo "waiting for $node startup..."
    wait_for $HOME/resources/$node.ready 
done

run_tests(){
	local FARGS=("$@")
	local SRC=${FARGS[0]}
    export TEST_NAME=$TEST_NAME
	bash -c  $SRC/src/test.sh $RD_URL admin admin
}

#sudo chown -R $USERNAME:$USERNAME $TEST_DIR

export PATH=$PATH:$HOME/tools/bin
export RDECK_BASE=$HOME
#source $RDECK_BASE/etc/profile

echo "starting test.sh"

set +e
chmod -w $TEST_DIR/src/test.sh

run_tests $TEST_DIR

EC=$?

echo "test.sh finished with $EC"


exit $EC