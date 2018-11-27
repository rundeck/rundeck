#!/bin/bash
set -eou pipefail

DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" >/dev/null && pwd )"

for inc in $(ls $DIR/includes | sort -n); do
    source $DIR/includes/$inc
done

export HOSTNAME=$(hostname)

export RUNDECK_HOME=/home/rundeck

export REMCO_HOME=/etc/remco
export REMCO_RESOURCE_DIR=${REMCO_HOME}/resources.d
export REMCO_TEMPLATE_DIR=${REMCO_HOME}/templates
export REMCO_TMP_DIR=/tmp/remco-partials

# Create temporary directories for config partials
mkdir -p ${REMCO_TMP_DIR}/framework
mkdir -p ${REMCO_TMP_DIR}/rundeck-config

remco -config ${REMCO_HOME}/config.toml

# Generate a new server UUID
if [[ "${RUNDECK_SERVER_UUID}" = "RANDOM" ]] ; then
    RUNDECK_SERVER_UUID=$(uuidgen)
fi
echo "rundeck.server.uuid = ${RUNDECK_SERVER_UUID}" > ${REMCO_TMP_DIR}/framework/server-uuid.properties

# Combine partial config files
cat ${REMCO_TMP_DIR}/framework/* >> etc/framework.properties
cat ${REMCO_TMP_DIR}/rundeck-config/* >> server/config/rundeck-config.properties

exec java \
    -XX:+UnlockExperimentalVMOptions \
    -XX:MaxRAMFraction="${JVM_MAX_RAM_FRACTION}" \
    -XX:+UseCGroupMemoryLimitForHeap \
    -Dloginmodule.conf.name=jaas-loginmodule.conf \
    -Dloginmodule.name=rundeck \
    -Drundeck.jaaslogin=true \
    -Drundeck.jetty.connector.forwarded="${RUNDECK_SERVER_FORWARDED:-false}" \
    "${@}" \
    -jar rundeck.war
