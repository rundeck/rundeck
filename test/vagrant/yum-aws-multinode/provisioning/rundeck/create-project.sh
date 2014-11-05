#!/usr/bin/env bash

set -eu

DIR=$1 ; shift;
PROJECT=$1; shift;
RUNDECK_USER=$1; shift;
RESOURCES_DIR=$1; shift;

echo "Adding project: $PROJECT"

mkdir -p $DIR/projects/$PROJECT/etc
mkdir -p $RESOURCES_DIR

# Generate resource model for each node.
cat > $DIR/projects/$PROJECT/etc/project.properties <<EOF
project.name=$PROJECT
resources.source.1.type=directory
resources.source.1.config.directory=$RESOURCES_DIR
project.ssh-keypath=$DIR/.ssh/id_rsa
project.ssh-authentication=privateKey
service.NodeExecutor.default.provider=jsch-ssh
service.FileCopier.default.provider=jsch-scp
EOF
echo "Added project: ${PROJECT}."

chown -R ${RUNDECK_USER}:${RUNDECK_USER} $DIR
chown -R ${RUNDECK_USER}:${RUNDECK_USER} $RESOURCES_DIR
