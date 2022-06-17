#!/bin/sh

INIT_DIR="${RPM_INSTALL_PREFIX4:-/etc/rc.d/init.d}"

if [ "$1" = 0 ]; then
    if [[ "$INIT_DIR" == "/etc/rc.d/init.d" ]]; then
        /sbin/service rundeckd stop
        /sbin/chkconfig --del rundeckd
    fi
fi
