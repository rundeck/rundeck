#!/bin/bash
set -x

# Create temporary directories for config partials
mkdir -p /tmp/config/framework
mkdir -p /tmp/config/rundeck-config

confd -onetime -backend env

# Generate a new server UUID
echo "rundeck.server.uuid = $(uuidgen)" > /tmp/config/framework/server-uuid.properties

# Combine partial config files
cat /tmp/config/framework/* >> etc/framework.properties
cat /tmp/config/rundeck-config/* >> server/config/rundeck-config.properties

exec java \
    -XX:+UnlockExperimentalVMOptions \
    -XX:MaxRAMFraction=1 \
    -XX:+UseCGroupMemoryLimitForHeap \
    -Dloginmodule.conf.name=jaas-loginmodule.conf \
    -Dloginmodule.name=rundeck \
    -Drundeck.jaaslogin=true \
    "${@}" \
    -jar rundeck.war
