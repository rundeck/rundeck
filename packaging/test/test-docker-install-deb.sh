#!/bin/bash

LDIR=$( cd $(dirname $0) ; echo $PWD )
export DIR="${PACKAGING_DIR}/test/docker/debinstall"
export COMMON="ubuntu1604"
export PACKAGE_TYPE="deb"


echo "$LDIR"

. "$LDIR/test-docker-install-common.sh"

build
run