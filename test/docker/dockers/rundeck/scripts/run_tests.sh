#!/bin/bash

set -e

export API_KEY=letmein99
echo "API_KEY=$API_KEY"

#bash $HOME/scripts/start_rundeck.sh

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

for node in $RUNDECK_NODE $REMOTE_NODE ; do
    echo "waiting for $node startup..."
    wait_for $HOME/resources/$node.ready 
done

echo "Uploading private key to key storage"
test -f $HOME/resources/$REMOTE_NODE.rsa

bash $HOME/scripts/upload_key_storage.sh $HOME/resources/$REMOTE_NODE.rsa $API_KEY id_rsa.pem


sudo chown -R $USERNAME:$USERNAME /tests

export PATH=$PATH:$HOME/tools/bin
export RDECK_BASE=$HOME
source $RDECK_BASE/etc/profile

echo "starting test-all.sh"

set +e
chmod -w /tests/rundeck/test-all.sh


/tests/rundeck/test-all.sh \
	--rdeck-base $HOME \
	--rundeck-project testproj1 \
	--rundeck-user $USERNAME \
	--remote-node $REMOTE_NODE
EC=$?

echo "test-all.sh finished with $EC"

touch $HOME/resources/tests.done

exit $EC