#!/bin/bash

TMP_DIR=/tmp RDECK_BASE=/var/lib/rundeck \
    RDECK_ETC=/etc/rundeck \
    RDECK_PROJECTS=/var/rundeck/projects \
    XMLSTARLET=xmlstarlet \
    sh /rundeck_api_test/src/test.sh http://localhost:4440 admin admin