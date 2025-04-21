#!/bin/sh

JAVA_VERSION_OUTPUT=$(java -version 2>&1)
JAVA_MAJOR_VERSION=$(echo "$JAVA_VERSION_OUTPUT" | awk -F '"' '/version/ {print $2}' | cut -d. -f1)

if [ "$JAVA_MAJOR_VERSION" = "1" ]; then
  JAVA_MAJOR_VERSION=$(echo "$JAVA_VERSION_OUTPUT" | awk -F '"' '/version/ {print $2}' | cut -d. -f2)
fi

if [[ "$JAVA_MAJOR_VERSION" != "11" && "$JAVA_MAJOR_VERSION" != "17" ]]; then
  echo "Java 11 or 17 is required"
  exit 1
fi

HOME_DIR="${RPM_INSTALL_PREFIX0:-/var/lib/rundeck}"

HOME_DIRNAME=$(dirname "${HOME_DIR}")

# Create the home parent directory in case it does not exist(it was relocated)
if [[ ! -d "$HOME_DIRNAME" ]] ; then
    mkdir -p "${HOME_DIRNAME}"
fi

getent group rundeck >/dev/null || groupadd rundeck
getent passwd rundeck >/dev/null || useradd -d "${HOME_DIR}" -m -g rundeck rundeck
