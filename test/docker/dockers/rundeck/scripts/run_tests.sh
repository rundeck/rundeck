#!/bin/bash

set -e

TEST_DIR=$1
TEST_SCRIPT=${2:-/tests/run-tests.sh}
TEST_PROJECT=${3:-testproj1}

: ${TEST_DIR?"Dir required"}
: ${TEST_SCRIPT?"Script required"}
: ${TEST_PROJECT?"Project required"}
echo "run_tests with $TEST_DIR and $TEST_SCRIPT for project $TEST_PROJECT"

export API_KEY=letmein99
# define env vars used by rd tool
export RD_TOKEN=$API_KEY
export RD_URL="http://$RUNDECK_NODE:4440"
export RD_COLOR=0
export RD_OPTS="-Dfile.encoding=utf-8"
if [ -n "$SETUP_SSL" ] ; then
  export RD_URL=https://$RUNDECK_NODE:4443
  export TRUSTSTORE="$HOME/etc/truststore"
  export RD_OPTS="$RD_OPTS -Djavax.net.ssl.trustStore=$TRUSTSTORE"
fi

echo "API_KEY=$API_KEY"

#bash $HOME/scripts/start_rundeck.sh

wait_for(){
    local FILE=$1
    # Wait for resource file to be created
    MAX_ATTEMPTS=30
    SLEEP=10
    echo "Waiting for node to start... "
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

for node in $WAIT_NODES ; do
    echo "waiting for $node startup..."
    wait_for $HOME/resources/$node.ready 
done

if [ -n "$SETUP_SSH_KEY" ] ; then
    echo "Uploading private key to key storage"
    test -f $HOME/resources/$REMOTE_NODE.rsa

    bash $HOME/scripts/upload_key_storage.sh $HOME/resources/$REMOTE_NODE.rsa $API_KEY id_rsa.pem
fi

sudo chown -R $USERNAME:$USERNAME /tests

export PATH=$PATH:$HOME/tools/bin
export RDECK_BASE=$HOME
source $RDECK_BASE/etc/profile

echo "starting tests"

set +e
chmod -w $TEST_SCRIPT
chmod +x $TEST_SCRIPT
sync

$TEST_SCRIPT \
	--rdeck-base $HOME \
	--rundeck-project $TEST_PROJECT \
	--rundeck-user $USERNAME \
    --test-dir $TEST_DIR
EC=$?

echo "tests finished with $EC"

touch $HOME/resources/tests.done

exit $EC