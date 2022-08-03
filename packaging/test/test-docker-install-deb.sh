#!/bin/bash

LDIR=$( cd $(dirname $0) ; echo $PWD )

if [[ "${UPSTREAM_PROJECT}" == "rundeckpro" ]] ; then
  export PACKAGING_DIR_PARENT="rundeck/"
fi

export DIR="${PACKAGING_DIR_PARENT}"packaging/test/docker/debinstall
export COMMON="ubuntu1604"
export PACKAGE_TYPE="deb"


echo "$LDIR"

. "$LDIR/test-docker-install-common.sh"

build
run