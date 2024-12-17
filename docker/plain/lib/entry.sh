#!/bin/bash
set -eou pipefail

DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" >/dev/null && pwd )"

for inc in $(ls $DIR/includes | sort -n); do
    source $DIR/includes/$inc
done

export HOSTNAME=$(hostname)

export RUNDECK_HOME=${RUNDECK_HOME:-/home/rundeck}
export HOME=$RUNDECK_HOME

# Store custom exec command if set so it will not be lost when unset later
EXEC_CMD="${RUNDECK_EXEC_CMD:-}"


# Generate a new server UUID
if [[ "${RUNDECK_SERVER_UUID}" = "RANDOM" ]] ; then
    RUNDECK_SERVER_UUID=$(uuidgen)
fi


# Store settings that may be unset in script variables
SETTING_RUNDECK_FORWARDED="${RUNDECK_SERVER_FORWARDED:-false}"

# Unset all RUNDECK_* environment variables
#if [[ "${RUNDECK_ENVARS_UNSETALL:-true}" = "true" ]] ; then
#    unset `env | awk -F '=' '{print $1}' | grep -e '^RUNDECK_'`
#fi

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
        sed -i "\#rundeck#c\rundeck:x:$(id -u):0:rundeck user:${HOME}:/bin/bash" "${TMP_PASSWD}"
        cat "${TMP_PASSWD}" > /etc/passwd
        rm "${TMP_PASSWD}"
    fi
fi

# Exec custom command if provided
if [[ -n "${EXEC_CMD}" ]] ; then
    # shellcheck disable=SC2086
    exec $EXEC_CMD
fi

exec java \
    -XX:MaxRAMPercentage="${JVM_MAX_RAM_PERCENTAGE}" \
    -Dlog4j.configurationFile="${HOME}/server/config/log4j2.properties" \
    -Dlogging.config="file:${HOME}/server/config/log4j2.properties" \
    -Dloginmodule.conf.name=jaas-loginmodule.conf \
    -Dloginmodule.name=rundeck \
    -Drundeck.jaaslogin=true \
    -Drundeck.jetty.connector.forwarded="${SETTING_RUNDECK_FORWARDED}" \
    "${@}" \
    -jar rundeck.war
