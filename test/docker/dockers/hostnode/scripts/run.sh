#!/bin/bash

set -e

bash $HOME/scripts/start_sshd.sh

sudo cp $HOME/$RUNDECK_NODE.xml $HOME/resources/
mkdir -p $HOME/.ssh
chmod go-rwx $HOME/.ssh
ssh-keygen -t rsa -N '' -f $HOME/.ssh/id_rsa
sudo cp $HOME/.ssh/id_rsa $HOME/resources/$RUNDECK_NODE.rsa
cat $HOME/.ssh/id_rsa.pub >> $HOME/.ssh/authorized_keys2
sudo chown -R $USERNAME:$USERNAME $HOME

touch $HOME/resources/$RUNDECK_NODE.ready

echo "$RUNDECK_NODE ready."

#sudo tail -F /var/log/dmesg

FILE=$HOME/resources/tests.done

# Wait for server to start
MAX_ATTEMPTS=1000
SLEEP=10
echo "Waiting for $FILE to be created."

declare -i count=0
while (( count <= MAX_ATTEMPTS ))
do
    if test -f $FILE
    then  break; # found successful startup message.
    fi
    (( count += 1 ))  ; # increment attempts counter.
    (( count == MAX_ATTEMPTS )) && {
        echo >&2 "FAIL: Reached max attempts to find $FILE. Exiting."
        exit 1
    }
    sleep $SLEEP; # wait before trying again.
done

echo "Saw $FILE, Exiting."