#!/bin/bash

export RUNDECK_VERSION=${RUNDECK_VERSION:-2.7.0}
export LAUNCHER_URL=${LAUNCHER_URL:-http://dl.bintray.com/rundeck/rundeck-maven/rundeck-launcher-${RUNDECK_VERSION}.jar}
export CLI_DEB_URL=${CLI_DEB_URL:-https://dl.bintray.com/rundeck/rundeck-deb}
export CLI_VERS=${CLI_VERS:-1.0.0-1}