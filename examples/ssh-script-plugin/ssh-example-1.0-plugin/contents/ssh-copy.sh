#!/bin/bash

#####
# ssh-copy.sh
# This script executes the system "scp" command to copy a file
# to a remote node.
# usage: ssh-copy.sh [username] [hostname] [file] [destination]
#
# It uses some environment variables set by RunDeck if they exist.  
#
# RD_NODE_SCP_DIR: the "scp-dir" attribute indicating the target
#   directory to copy the file to if destination is not set.
# RD_NODE_SSH_PORT:  the "ssh-port" attribute value for the node to specify
#   the target port, if it exists
# RD_NODE_SSH_KEYFILE: the "ssh-keyfile" attribute set for the node to
#   specify the identity keyfile, if it exists
# RD_NODE_SSH_OPTS: the "ssh-opts" attribute, to specify custom options
#   to pass directly to ssh.  Eg. "-o ConnectTimeout=30"
# RD_NODE_SCP_OPTS: the "scp-opts" attribute, to specify custom options
#   to pass directly to scp.  Eg. "-o ConnectTimeout=30". overrides ssh-opts.
# RD_NODE_SSH_TEST: if "ssh-test" attribute is set to "true" then do
#   a dry run of the ssh command
#####

USER=$1
shift
HOST=$1
shift
FILE=$1
shift
DEST=$1

if [ -z "$DEST" ] ; then
    DEST=${RD_NODE_SCP_DIR:?"scp-dir attribute was not set for the node $RD_NODE_NAME"}/$(basename $FILE)
fi

# use RD env variable from node attributes for ssh-port value, default to 22:
PORT=${RD_NODE_SSH_PORT:-22}

# extract any :port from hostname
XHOST=$(expr "$HOST" : '\(.*\):')
if [ ! -z $XHOST ] ; then
    PORT=${HOST#"$XHOST:"}
    #    echo "extracted port $PORT and host $XHOST from $HOST"
    HOST=$XHOST
fi

SSHOPTS="-p -P $PORT -o UserKnownHostsFile=/dev/null -o StrictHostKeyChecking=no -o LogLevel=quiet"

#use ssh-keyfile node attribute from env vars
if [ ! -z "$RD_NODE_SSH_KEYFILE" ] ; then
    SSHOPTS="$SSHOPTS -i $RD_NODE_SSH_KEYFILE"
fi

#use any node-specified ssh options
if [ ! -z "$RD_NODE_SCP_OPTS" ] ; then
    SSHOPTS="$SSHOPTS $RD_NODE_SCP_OPTS"
elif [ ! -z "$RD_NODE_SSH_OPTS" ] ; then
    SSHOPTS="$SSHOPTS $RD_NODE_SSH_OPTS"
fi


RUNSSH="scp $SSHOPTS $FILE $USER@$HOST:$DEST"

#if ssh-test is set to "true", do a dry run
if [ "true" = "$RD_NODE_SSH_TEST" ] ; then
    echo "[ssh-example]" $RUNSSH 1>&2
    echo $DEST # echo remote filepath
    exit 0
fi

#finally, execute scp but don't print to STDOUT
$RUNSSH 1>&2 || exit $? # exit if not successful
echo $DEST # echo remote filepath
