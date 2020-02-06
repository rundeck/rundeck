if [[ ! -z "${RUNDECK_HOST}" ]] && ! grep -q "${RUNDECK_HOST}" /etc/hosts; then
    RUNDECK_IP=$(set -o pipefail; getent ahostsv4 host.docker.internal | head -n 1 | awk '{print $1}' || echo '127.0.0.1')
    echo "${RUNDECK_IP} ${RUNDECK_HOST}" >> /etc/hosts
fi

bash -l -c "${@}"