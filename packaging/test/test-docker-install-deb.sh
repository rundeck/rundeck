#!/bin/bash

LDIR=$( cd $(dirname $0) ; echo $PWD )
export DIR=packaging/test/docker/debinstall
export COMMON="ubuntu1604"
export PACKAGE_TYPE="deb"
export PACKAGING_DIR_PARENT=""

if [[ "${UPSTREAM_PROJECT}" == "rundeckpro" ]] ; then
  export PACKAGING_DIR_PARENT="rundeck/"
fi

echo "$LDIR"

. "$LDIR/test-docker-install-common.sh"

build
run