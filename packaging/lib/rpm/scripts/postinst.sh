#!/bin/sh

HOME_DIR="${RPM_INSTALL_PREFIX0:-/var/lib/rundeck}"
ETC_DIR="${RPM_INSTALL_PREFIX1:-/etc/rundeck}"
BIN_DIR="${RPM_INSTALL_PREFIX2:-/usr/bin}"
LOG_DIR="${RPM_INSTALL_PREFIX3:-/var/log/rundeck}"
INIT_DIR="${RPM_INSTALL_PREFIX4:-/etc/rc.d/init.d}"

if [ ! -e ~rundeck/.ssh/id_rsa ]; then
	su -c "ssh-keygen -q -t rsa -C '' -N '' -f ~rundeck/.ssh/id_rsa" rundeck
fi

DIR="${ETC_DIR}"

SSL_CONFIG="${DIR}/ssl/"
RDECK_CONFIG="$DIR/rundeck-config.properties"
FW_CONFIG="$DIR/framework.properties"
PROJECT_CONFIG="$DIR/project.properties"
LOG4J_CONFIG="$DIR/log4j2.properties"
JAAS_CONFIG="$DIR/jaas-loginmodule.conf"

if [ -f "$DIR/rundeck-config.properties.rpmnew" ]; then
    RDECK_CONFIG="$DIR/rundeck-config.properties.rpmnew"
fi

if [ -f "$DIR/framework.properties.rpmnew" ]; then
    FW_CONFIG="$DIR/framework.properties.rpmnew"
fi

if [ -f "$PROJECT_CONFIG.rpmnew" ]; then
    PROJECT_CONFIG="$PROJECT_CONFIG.rpmnew"
fi

if [ -f "$LOG4J_CONFIG.rpmnew" ]; then
    LOG4J_CONFIG="$LOG4J_CONFIG.rpmnew"
fi


if [ -f "$JAAS_CONFIG.rpmnew" ]; then
    JAAS_CONFIG="$JAAS_CONFIG.rpmnew"
fi


if  ! grep -E '^\s*rundeck.server.uuid\s*=\s*.{8}-.{4}-.{4}-.{4}-.{12}\s*$' "$FW_CONFIG" ; then
    uuid=$(uuidgen)
    echo -e "\n# ----------------------------------------------------------------" >> "$FW_CONFIG"
    echo "# Auto generated server UUID: $uuid" >> "$FW_CONFIG"
    echo "# ----------------------------------------------------------------" >> "$FW_CONFIG"
    echo "rundeck.server.uuid = $uuid" >> "$FW_CONFIG"
fi

#setting a random password for encryption
STORAGE_PASS=$(openssl rand -hex 8)
sed -i -E 's/^rundeck\.storage\.converter\.([0-9]+)\.config\.password=default\.encryption\.password$/rundeck.storage.converter.\1.config.password='"$STORAGE_PASS"'/' "$RDECK_CONFIG"
sed -i -E 's/^rundeck\.config\.storage\.converter\.([0-9]+)\.config\.password=default\.encryption\.password$/rundeck.config.storage.converter.\1.config.password='"$STORAGE_PASS"'/' "$RDECK_CONFIG"

if [[ "${HOME_DIR}" != "/var/lib/rundeck" ]] ; then
    sed -i -E "s#/var/lib/rundeck#${HOME_DIR}#g" "${DIR}/profile"

    sed -i -E "s#/var/lib/rundeck#${HOME_DIR}#g" "${FW_CONFIG}"
    sed -i -E "s#/var/lib/rundeck#${HOME_DIR}#g" "${RDECK_CONFIG}"
    sed -i -E "s#/var/lib/rundeck#${HOME_DIR}#g" "${PROJECT_CONFIG}"
fi

if [[ "${ETC_DIR}" != "/etc/rundeck" ]] ; then
    sed -i -E "s#/etc/rundeck#${ETC_DIR}#g" "${DIR}/profile"
    sed -i -E "s#/etc/sysconfig#${ETC_DIR}#g" "${DIR}/profile"

    sed -i -E "s#/etc/rundeck#${ETC_DIR}#g" "${FW_CONFIG}"
    sed -i -E "s#/etc/rundeck#${ETC_DIR}#g" "${RDECK_CONFIG}"
    sed -i -E "s#/etc/rundeck#${ETC_DIR}#g" "${JAAS_CONFIG}"

    sed -i -E "s#/etc/rundeck#${ETC_DIR}#g" "${INIT_DIR}/rundeckd"
    sed -i -E "s#/etc/sysconfig#${ETC_DIR}#g" "${INIT_DIR}/rundeckd"
fi

if [[ "${LOG_DIR}" != "/var/log/rundeck" ]] ; then
    sed -i -E "s#/var/log/rundeck#${LOG_DIR}#g" "${INIT_DIR}/rundeckd"
    sed -i -E "s#/var/log/rundeck#${LOG_DIR}#g" "${LOG4J_CONFIG}"
fi

if [[ "$INIT_DIR" == "/etc/rc.d/init.d" ]]; then
    /sbin/chkconfig --add rundeckd
fi