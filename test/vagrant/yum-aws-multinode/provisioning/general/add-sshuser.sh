#!/usr/bin/env bash

SSH_USER=$1
PUB_KEY=$2

set -eu

mkdir -p /home/$SSH_USER/.ssh

if [[ ! -f /home/$SSH_USER/.ssh/authorized_keys ]]
then
    cat $PUB_KEY >> /home/$SSH_USER/.ssh/authorized_keys
    chmod 600 /home/$SSH_USER/.ssh/authorized_keys
fi

chown -R $SSH_USER:$SSH_USER /home/$SSH_USER/.ssh

# Configure SSHD to pass environment variables to shell.
if ! grep -q "^AcceptEnv RD_" /etc/ssh/sshd_config
then    
    echo '#' >> /etc/ssh/sshd_config
    echo '#' >> /etc/ssh/sshd_config
    echo 'AcceptEnv RD_*' >> /etc/ssh/sshd_config
    /etc/init.d/sshd stop
    /etc/init.d/sshd start
fi
