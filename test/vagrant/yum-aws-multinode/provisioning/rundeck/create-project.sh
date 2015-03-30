#!/usr/bin/env bash

set -eu

RDECK_BASE=$1 ; shift;
DIR=$1 ; shift;
PROJECT=$1; shift;
RUNDECK_USER=$1; shift;
RESOURCES_DIR=$1; shift;

echo "Adding project: $PROJECT"

mkdir -p $DIR/$PROJECT/etc
mkdir -p $RESOURCES_DIR

PROPSFILE=$DIR/$PROJECT/etc/project.properties

# Generate resource model for each node.
cat > $PROPSFILE <<EOF
project.name=$PROJECT
resources.source.1.type=directory
resources.source.1.config.directory=$RESOURCES_DIR
project.ssh-keypath=$RDECK_BASE/.ssh/id_rsa
project.ssh-authentication=privateKey
service.NodeExecutor.default.provider=jsch-ssh
service.FileCopier.default.provider=jsch-scp
EOF
echo "Added project: ${PROJECT}, properties at $PROPSFILE"

#chown -R ${RUNDECK_USER}:${RUNDECK_USER} $DIR
#chown -R ${RUNDECK_USER}:${RUNDECK_USER} $RESOURCES_DIR
