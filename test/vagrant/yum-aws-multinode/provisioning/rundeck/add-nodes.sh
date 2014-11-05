#!/usr/bin/env bash

set -eu

RESOURCES_D=$1 ; shift;
RUNDECK_USER=$1; shift;
TAGS=$1; shift;
NODE=$1; shift;
HOSTNAME=$1; shift;

echo "Adding node: $NODE"

mkdir -p $RESOURCES_D

# Generate resource model for each node.
cat > $RESOURCES_D/$NODE.xml <<EOF
<?xml version="1.0" encoding="UTF-8"?>

<project>    
  <node name="${NODE}" tags="${TAGS}" hostname="${HOSTNAME}"  osFamily="unix" osName="Linux" osArch="x86_64" username="${RUNDECK_USER}"/>
</project>
EOF
echo "Added node: ${NODE}."

chown -R ${RUNDECK_USER}:${RUNDECK_USER} $RESOURCES_D
