#!/usr/bin/env bash

set -eu

SSH_USER=$1
SSH_DIR=$2
SRC_DIR=$3
SSH_KEY=$4

if [[ ! -d ${SSH_DIR} ]]
then
    mkdir -p ${SSH_DIR}
    chown ${SSH_USER} ${SSH_DIR}
fi 

cp ${SRC_DIR}/${SSH_KEY}* ${SSH_DIR}/

chown ${SSH_USER} ${SSH_DIR}/${SSH_KEY}*
chmod 600 ${SSH_DIR}/${SSH_KEY}*
