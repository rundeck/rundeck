#!/bin/sh

HOME_DIR="${RPM_INSTALL_PREFIX0:-/var/lib/rundeck}"

HOME_DIRNAME=$(dirname "${HOME_DIR}")

# Create the home parent directory in case it does not exist(it was relocated)
if [[ ! -d "$HOME_DIRNAME" ]] ; then
    mkdir -p "${HOME_DIRNAME}"
fi

getent group rundeck >/dev/null || groupadd rundeck
getent passwd rundeck >/dev/null || useradd -d "${HOME_DIR}" -m -g rundeck rundeck
