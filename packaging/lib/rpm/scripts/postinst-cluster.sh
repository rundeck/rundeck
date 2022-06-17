#!/bin/sh

DIR="${RPM_INSTALL_PREFIX1:-/etc/rundeck}"

RDECK_CONFIG="$DIR/rundeck-config.properties"

if [ -f "$DIR/rundeck-config.properties.rpmnew" ]; then
    RDECK_CONFIG="$DIR/rundeck-config.properties.rpmnew"
fi

# enabling cluster mode
cat >> "$RDECK_CONFIG" <<END

rundeck.clusterMode.enabled=true
END