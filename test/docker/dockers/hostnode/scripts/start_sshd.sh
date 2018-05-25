#!/bin/bash

set -e

# Configure SSHD to pass environment variables to shell.
if ! grep -q "^AcceptEnv RD_" /etc/ssh/sshd_config
then    
    cat  >> $HOME/new_sshd_config <<END
#
#
AcceptEnv RD_*
END
	sudo bash -c "cat $HOME/new_sshd_config >> /etc/ssh/sshd_config"
fi

# start SSHD
sudo /usr/sbin/sshd

echo "SSHD Started up."