#!/usr/bin/env bash

set -eu

# Create User

RUNDECK_USER=$1
HOME_DIR=$2

if ! id $RUNDECK_USER ; then
    useradd -d $HOME_DIR -m $RUNDECK_USER
    echo "Added user $RUNDECK_USER home dir $HOME_DIR"
    id $RUNDECK_USER
fi
