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
mkdir -p ${REMCO_TMP_DIR}/artifact-repositories

remco -config ${REMCO_HOME}/config.toml

# Generate a new server UUID
if [[ "${RUNDECK_SERVER_UUID}" = "RANDOM" ]] ; then
    RUNDECK_SERVER_UUID=$(uuidgen)
fi
echo "rundeck.server.uuid = ${RUNDECK_SERVER_UUID}" > ${REMCO_TMP_DIR}/framework/server-uuid.properties

# Combine partial config files
cat ${REMCO_TMP_DIR}/framework/* >> etc/framework.properties
cat ${REMCO_TMP_DIR}/rundeck-config/* >> server/config/rundeck-config.properties
cat ${REMCO_TMP_DIR}/artifact-repositories/* >> server/config/artifact-repositories.yaml


# Store settings that may be unset in script variables
SETTING_RUNDECK_FORWARDED="${RUNDECK_SERVER_FORWARDED:-false}"

# Unset all RUNDECK_* environment variables
if [[ "${RUNDECK_ENVARS_UNSETALL:-true}" = "true" ]] ; then
    unset `env | awk -F '=' '{print $1}' | grep -e '^RUNDECK_'`
fi

# Unset specific environment variables
if [[ ! -z "${RUNDECK_ENVARS_UNSETS:-}" ]] ; then
    unset $RUNDECK_ENVARS_UNSETS
    unset RUNDECK_ENVARS_UNSETS
fi

# Support Arbitrary User IDs on OpenShift
if ! whoami &> /dev/null; then
    if [ -w /etc/passwd ]; then
        TMP_PASSWD=$(mktemp)
        cat /etc/passwd > "${TMP_PASSWD}"
        sed -i "\#rundeck#c\rundeck:x:$(id -u):0:rundeck user:/home/rundeck:/bin/bash" "${TMP_PASSWD}"
        cat "${TMP_PASSWD}" > /etc/passwd
        rm "${TMP_PASSWD}"
    fi
fi

exec java \
    -XX:+UnlockExperimentalVMOptions \
    -XX:MaxRAMPercentage="${JVM_MAX_RAM_PERCENTAGE}" \
    -Dlog4j.configurationFile="/home/rundeck/server/config/log4j2.properties" \
    -Dlogging.config="file:/home/rundeck/server/config/log4j2.properties" \
    -Dloginmodule.conf.name=jaas-loginmodule.conf \
    -Dloginmodule.name=rundeck \
    -Drundeck.jaaslogin=true \
    -Drundeck.jetty.connector.forwarded="${SETTING_RUNDECK_FORWARDED}" \
    "${@}" \
    -jar rundeck.war
